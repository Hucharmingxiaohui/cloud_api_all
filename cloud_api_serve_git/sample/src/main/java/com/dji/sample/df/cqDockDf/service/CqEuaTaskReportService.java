package com.dji.sample.df.cqDockDf.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dji.sample.center.entity.UniPoint;
import com.dji.sample.df.substationDf.dao.UniPointMapper2;
import com.dji.sample.df.uavCommonHandleDf.service.impl.PatrolXlsRenderer;
import com.dji.sample.df.cqDockDf.dao.CqDockTaskRecordMapper;
import com.dji.sample.df.cqDockDf.model.dto.CqEuaTaskPictureListRequest;
import com.dji.sample.df.cqDockDf.model.entity.CqDockTaskPictureEntity;
import com.dji.sample.df.cqDockDf.model.entity.CqDockTaskRecordEntity;
import com.dji.sample.df.cqDockDf.config.CqDockProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.PreDestroy;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CqEuaTaskReportService implements AutoCloseable {
    private final CqDockTaskRecordMapper records;
    private final CqEuaPlanService plans;
    private final CqDockProperties config;
    private final UniPointMapper2 points;
    @Value("${ordinary-report.template-path:./templates/report-excel-templates.xls}")
    private String templatePath;
    private final ObjectMapper json = new ObjectMapper();
    private final Set<String> active = new HashSet<>();
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(16), runnable -> {
                Thread thread = new Thread(runnable, "eua-xls-report");
                thread.setDaemon(true);
                return thread;
            }, new ThreadPoolExecutor.AbortPolicy());
    private boolean closed;

    public CqEuaTaskReportService(CqDockTaskRecordMapper records, CqEuaPlanService plans,
                                CqDockProperties config, UniPointMapper2 points) {
        this.records = records;
        this.plans = plans;
        this.config = config;
        this.points = points;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportStatus {
        private String status;
        private String message;
        private String fileName;
    }

    public synchronized ReportStatus generate(CqEuaTaskPictureListRequest request) {
        CqDockTaskRecordEntity record = resolve(request);
        String key = key(record);
        Path root = storage();
        if (active.contains(key)) return read(root, key);
        if (closed) throw error(HttpStatus.SERVICE_UNAVAILABLE, "报告服务已关闭");
        ReportStatus state = new ReportStatus("GENERATING", "报告正在生成", fileName(record));
        write(root, key, state);
        active.add(key);
        try {
            executor.execute(() -> generateFile(root, key, record, state.getFileName()));
        } catch (RejectedExecutionException e) {
            active.remove(key);
            write(root, key, new ReportStatus("FAILED", "生成队列已满，请稍后重试", state.getFileName()));
            throw error(HttpStatus.SERVICE_UNAVAILABLE, "生成队列已满，请稍后重试");
        }
        return state;
    }

    public synchronized ReportStatus status(CqEuaTaskPictureListRequest request) {
        CqDockTaskRecordEntity record = resolve(request);
        return read(storage(), key(record));
    }

    public synchronized ByteArrayResource download(CqEuaTaskPictureListRequest request) {
        CqDockTaskRecordEntity record = resolve(request);
        Path root = storage();
        String key = key(record);
        ReportStatus state = read(root, key);
        if ("NOT_GENERATED".equals(state.getStatus())) throw error(HttpStatus.NOT_FOUND, "报告尚未生成");
        if (!"COMPLETED".equals(state.getStatus())) throw error(HttpStatus.CONFLICT, state.getMessage());
        try {
            // Snapshot under the generation lock: a subsequent regeneration cannot change this response.
            return new ByteArrayResource(Files.readAllBytes(root.resolve(key + ".xls"))) {
                @Override
                public String getFilename() { return state.getFileName(); }
            };
        } catch (IOException e) {
            throw error(HttpStatus.INTERNAL_SERVER_ERROR, "无法读取报告文件");
        }
    }

    private CqDockTaskRecordEntity resolve(CqEuaTaskPictureListRequest request) {
        if (request == null) throw error(HttpStatus.BAD_REQUEST, "任务标识不能为空");
        String business = identifier(request.getBusinessId());
        String eua = identifier(request.getEuaTaskId());
        if (business == null && eua == null) throw error(HttpStatus.BAD_REQUEST, "任务编码或EUA任务ID不能为空");
        QueryWrapper<CqDockTaskRecordEntity> query = new QueryWrapper<>();
        if (business != null) query.eq("business_id", business);
        else query.eq("eua_task_id", eua);
        List<CqDockTaskRecordEntity> rows = records.selectList(query);
        if (rows.isEmpty()) throw error(HttpStatus.NOT_FOUND, "任务记录不存在");
        if (rows.size() != 1) throw error(HttpStatus.CONFLICT, "任务标识不唯一");
        CqDockTaskRecordEntity row = rows.get(0);
        String canonical = identifier(row.getBusinessId());
        if (canonical == null || !canonical.equals(row.getBusinessId()))
            throw error(HttpStatus.BAD_REQUEST, "任务记录编码无效");
        if ((business != null && !business.equals(canonical)) || (eua != null && !eua.equals(row.getEuaTaskId())))
            throw error(HttpStatus.BAD_REQUEST, "任务编码与EUA任务ID不匹配");
        return row;
    }

    private String identifier(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        String id = value.trim();
        if (!id.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,199}")) throw error(HttpStatus.BAD_REQUEST, "任务标识格式无效");
        return id;
    }

    private String key(CqDockTaskRecordEntity record) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(record.getBusinessId().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder key = new StringBuilder();
            for (byte b : digest) key.append(String.format("%02x", b & 0xff));
            return key.toString();
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    private Path storage() {
        try {
            if (config.getReportLocalPath() == null || config.getReportLocalPath().trim().isEmpty())
                throw new IOException("cq-dock.report-local-path is not configured");
            Path root = Paths.get(config.getReportLocalPath()).toAbsolutePath().normalize();
            if (Files.isSymbolicLink(root)) throw new IOException("Report directory is a symbolic link");
            Files.createDirectories(root);
            if (!root.equals(root.toRealPath())) throw new IOException("Report directory contains a link");
            return root.toRealPath();
        } catch (IOException | InvalidPathException e) {
            log.error("Cannot access EUA report storage", e);
            throw error(HttpStatus.INTERNAL_SERVER_ERROR, "报告存储不可用");
        }
    }

    private ReportStatus read(Path root, String key) {
        Path path = root.resolve(key + ".json");
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return new ReportStatus("NOT_GENERATED", "报告尚未生成", null);
        try {
            if (Files.isSymbolicLink(path)) throw new IOException("Invalid state file");
            ReportStatus state = json.readValue(path.toFile(), ReportStatus.class);
            if (!Arrays.asList("GENERATING", "COMPLETED", "FAILED").contains(state.getStatus())) throw new IOException("Invalid report state");
            if ("GENERATING".equals(state.getStatus()) && !active.contains(key)) {
                state = new ReportStatus("FAILED", "报告生成已中断，请重新生成", state.getFileName());
                write(root, key, state);
            }
            Path xls = root.resolve(key + ".xls");
            if ("COMPLETED".equals(state.getStatus()) && (state.getFileName() == null || !state.getFileName().endsWith(".xls")
                    || !Files.isRegularFile(xls, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(xls))) {
                state = new ReportStatus("FAILED", "报告文件缺失或为旧版Word，请重新生成Excel报告", state.getFileName());
                write(root, key, state);
            }
            return state;
        } catch (IOException e) {
            log.error("Cannot read EUA report state {}", key, e);
            throw error(HttpStatus.INTERNAL_SERVER_ERROR, "报告状态不可用");
        }
    }

    private void write(Path root, String key, ReportStatus state) {
        Path temp = null;
        try {
            temp = Files.createTempFile(root, key + "-", ".state.tmp");
            json.writeValue(temp.toFile(), state);
            publish(temp, root.resolve(key + ".json"));
        } catch (IOException e) {
            log.error("Cannot persist EUA report state {}", key, e);
            throw error(HttpStatus.INTERNAL_SERVER_ERROR, "无法保存报告状态");
        } finally { deleteTemp(temp); }
    }

    private void publish(Path from, Path to) throws IOException {
        Files.move(from, to, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    private synchronized void generateFile(Path root, String key, CqDockTaskRecordEntity record, String fileName) {
        if (!active.contains(key) || records.selectById(record.getId()) == null) {
            active.remove(key);
            return;
        }
        Path temp = null;
        try {
            CqEuaTaskPictureListRequest canonical = new CqEuaTaskPictureListRequest();
            canonical.setBusinessId(record.getBusinessId()); canonical.setEuaTaskId(record.getEuaTaskId());
            List<CqDockTaskPictureEntity> pictures = plans.taskPictureList(canonical);
            log.info("EUA开始生成Excel报告，任务={}，图片数={}，仅使用已存结果，不重新分析测温或上报", record.getBusinessId(), pictures.size());
            temp = Files.createTempFile(root, key + "-", ".xls.tmp");
            try (HSSFWorkbook document = render(record, pictures); OutputStream out = Files.newOutputStream(temp)) { document.write(out); }
            if (Thread.currentThread().isInterrupted() || closed) throw new InterruptedException();
            publish(temp, root.resolve(key + ".xls"));
            write(root, key, new ReportStatus("COMPLETED", "报告生成完成", fileName));
            active.remove(key);
            log.info("EUA Excel报告生成完成，任务={}，图片数={}", record.getBusinessId(), pictures.size());
        } catch (Exception e) {
            log.error("EUA Excel报告生成失败，任务={}", record.getBusinessId(), e);
            try { write(root, key, new ReportStatus("FAILED", "报告生成失败，请检查模板和服务日志后重试", fileName)); }
            catch (RuntimeException persistenceFailure) { log.error("Cannot persist failed EUA report {}", key, persistenceFailure); }
            finally { active.remove(key); }
        } finally { deleteTemp(temp); }
    }

    HSSFWorkbook render(CqDockTaskRecordEntity task, List<CqDockTaskPictureEntity> pictures) throws IOException {
        if (pictures.size() > 65529) throw new IllegalArgumentException("图片数量超出 XLS 行数上限");
        Map<String, List<UniPoint>> mapping = new HashMap<>();
        Set<String> codes = pictures.stream().map(CqDockTaskPictureEntity::getPointId)
                .filter(this::hasText).collect(Collectors.toCollection(LinkedHashSet::new));
        if (hasText(task.getSubCode()) && !codes.isEmpty()) {
            List<UniPoint> rows = points.selectList(new QueryWrapper<UniPoint>().eq("sub_code", task.getSubCode()).in("point_code", codes));
            for (UniPoint point : rows) if (task.getSubCode().equals(point.getSubCode()) && codes.contains(point.getPointCode()))
                mapping.computeIfAbsent(point.getPointCode(), ignored -> new ArrayList<>()).add(point);
        }
        HSSFWorkbook book;
        try (InputStream in = Files.newInputStream(Paths.get(templatePath))) { book = new HSSFWorkbook(in); }
        try {
            PatrolXlsRenderer renderer = new PatrolXlsRenderer(book);
            HSSFSheet sheet = book.getSheetAt(0);
            put(sheet, 0, 4, text(task.getTaskName())); put(sheet, 0, 11, "未记录");
            put(sheet, 1, 4, "未记录"); put(sheet, 1, 11, CqEuaPlanService.beijingTime(new Date(), "yyyy-MM-dd HH:mm:ss") + "（生成时间）");
            // 执行表没有实际起止时间，不能把记录创建/更新时间冒充飞行时间。
            put(sheet, 2, 4, "未记录"); put(sheet, 2, 11, "未记录");
            Map<String, Long> statuses = pictures.stream().collect(Collectors.groupingBy(
                    p -> analysisStatus(p.getAnalysisStatus()), LinkedHashMap::new, Collectors.counting()));
            put(sheet, 3, 4, "点位" + codes.size() + "个，图片" + pictures.size() + "张；" + statuses.entrySet().stream()
                    .map(e -> e.getKey() + "：" + e.getValue()).collect(Collectors.joining("；")));
            put(sheet, 4, 4, "仅汇总本次已存图片和结果，分析状态数量非缺陷数量；待分析、失败、未知均不代表正常。采集时间缺失不以入库时间替代。");
            Set<String> stations = new LinkedHashSet<>();
            int missing = 0;
            for (int i = 0; i < pictures.size(); i++) {
                if (Thread.currentThread().isInterrupted()) throw new IOException("Report generation interrupted");
                CqDockTaskPictureEntity picture = pictures.get(i);
                List<UniPoint> matches = mapping.getOrDefault(picture.getPointId(), Collections.emptyList());
                UniPoint point = matches.size() == 1 ? matches.get(0) : null;
                String issue = point != null ? "" : !hasText(task.getSubCode()) ? "厂站编码缺失，未关联点位"
                        : matches.size() > 1 ? "同站点位编码重复，未关联点位" : "同站点位未找到";
                if (point != null) stations.add(text(point.getSubName())); else missing++;
                String[] values = {String.valueOf(i + 1), point == null ? "未记录" : text(point.getAreaName()),
                        point == null ? "未记录" : text(point.getBayName()), point == null ? "未记录" : text(point.getDeviceName()),
                        point == null ? "未记录" : text(point.getComponentName()), point == null ? text(picture.getPointName()) : text(point.getPointName()),
                        point == null ? "未记录" : text(point.getLevel()), "EUA采集：" + text(picture.getPictureName()) + "；pointId=" + text(picture.getPointId()),
                        Integer.valueOf(0).equals(picture.getPointType()) ? "可见光" : Integer.valueOf(1).equals(picture.getPointType()) ? "红外" : "未知类型",
                        parseCaptureTime(picture), storedResult(picture) + (issue.isEmpty() ? "" : "；" + issue), ""};
                byte[] png = null;
                BufferedImage image = null;
                boolean analysis = Integer.valueOf(0).equals(picture.getPointType());
                try {
                    if (analysis && !hasText(picture.getAnalysisImageUrl())) throw new IOException("Missing analysis image");
                    try (InputStream in = plans.taskPictureImage(picture.getId(), analysis).getInputStream()) { image = ImageIO.read(in); }
                    if (image == null) throw new IOException("Unsupported image");
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    if (!ImageIO.write(image, "png", out)) throw new IOException("Cannot encode image");
                    png = out.toByteArray();
                } catch (ResponseStatusException e) {
                    if (e.getStatus() != HttpStatus.NOT_FOUND) throw e;
                    values[11] = "本地图片缺失或不可用"; image = null;
                } catch (IOException e) { values[11] = "本地图片缺失、不可读或格式不支持"; image = null; }
                renderer.row(7 + i, values, png, image, Workbook.PICTURE_TYPE_PNG);
            }
            put(sheet, 5, 0, "巡检单位名称：" + (stations.isEmpty() ? "未记录" : String.join("、", stations))
                    + (missing == 0 ? "" : "；" + missing + "张图片点位资料未关联"));
            if (missing > 0) log.warn("EUA报告部分点位资料未关联，任务={}，厂站={}，图片数={}，未跨站匹配", task.getBusinessId(), task.getSubCode(), missing);
            renderer.finish(6 + pictures.size());
            return book;
        } catch (Exception e) {
            book.close();
            if (e instanceof IOException) throw (IOException) e;
            throw e;
        }
    }

    private String storedResult(CqDockTaskPictureEntity picture) {
        String result = analysisStatus(picture.getAnalysisStatus()) + "；" + text(picture.getPointVal());
        if (hasText(picture.getPointValUnit()) && !picture.getPointValUnit().trim().equals(
                picture.getPointVal() == null ? "" : picture.getPointVal().trim())) result += " " + picture.getPointValUnit();
        return result;
    }

    private String parseCaptureTime(CqDockTaskPictureEntity picture) {
        if (picture.getRawData() == null) return "未记录";
        try {
            com.fasterxml.jackson.databind.JsonNode data = json.readTree(picture.getRawData());
            if (data == null || !data.isObject()) return "未记录";
            for (String field : Arrays.asList("shootTime", "acquisitionTime", "captureTime")) {
                com.fasterxml.jackson.databind.JsonNode value = data.get(field);
                if (value == null || !value.isTextual()) continue;
                java.time.format.DateTimeFormatter format = java.time.format.DateTimeFormatter
                        .ofPattern("uuuu-MM-dd HH:mm:ss").withResolverStyle(java.time.format.ResolverStyle.STRICT);
                try { return java.time.LocalDateTime.parse(value.textValue(), format).format(format); }
                catch (java.time.format.DateTimeParseException ignored) { }
            }
        } catch (IOException ignored) { }
        return "未记录";
    }

    private String analysisStatus(Integer status) {
        if (status == null) return "未记录";
        switch (status) {
            case 0: return "待分析";
            case 1: return "已分析";
            case 2: return "分析失败";
            case 3: return "红外测温完成";
            default: return "未知";
        }
    }

    private boolean hasText(String value) { return value != null && !value.trim().isEmpty(); }
    private String text(Object value) { return value == null || value.toString().trim().isEmpty() ? "未记录" : value.toString(); }
    private void put(HSSFSheet sheet, int r, int c, String value) {
        PatrolXlsRenderer.put(sheet, r, c, value);
    }

    private String fileName(CqDockTaskRecordEntity record) {
        String name = text(record.getTaskName()).replaceAll("[^\\p{L}\\p{N}_-]", "_");
        return name.substring(0, Math.min(60, name.length())) + "_巡检报告.xls";
    }

    public synchronized void deleteTaskFiles(CqDockTaskRecordEntity record) {
        String key = key(record);
        Path root = storage();
        try {
            Files.deleteIfExists(root.resolve(key + ".xls"));
            Files.deleteIfExists(root.resolve(key + ".docx"));
            Files.deleteIfExists(root.resolve(key + ".json"));
            try (java.util.stream.Stream<Path> files = Files.list(root)) {
                for (Path file : files.filter(path -> path.getFileName().toString().startsWith(key + "-")
                        && path.getFileName().toString().endsWith(".tmp")).collect(Collectors.toList())) Files.deleteIfExists(file);
            }
            active.remove(key);
            log.info("EUA任务报告及排队生成状态已清理：recordId={}", record.getId());
        } catch (IOException e) { throw new IllegalStateException("任务报告清理失败，请重试删除", e); }
    }

    private void deleteTemp(Path path) {
        if (path == null) return;
        try { Files.deleteIfExists(path); }
        catch (IOException e) { log.warn("Cannot remove EUA report temporary file", e); }
    }

    private ResponseStatusException error(HttpStatus status, String message) { return new ResponseStatusException(status, message); }

    @Override
    @PreDestroy
    public void close() {
        synchronized (this) { closed = true; }
        executor.shutdown();
        try { if (!executor.awaitTermination(5, TimeUnit.SECONDS)) executor.shutdownNow(); }
        catch (InterruptedException e) { executor.shutdownNow(); Thread.currentThread().interrupt(); }
    }
}
