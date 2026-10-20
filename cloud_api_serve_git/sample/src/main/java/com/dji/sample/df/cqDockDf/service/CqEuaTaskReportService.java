package com.dji.sample.df.cqDockDf.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.PreDestroy;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigInteger;
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
    private final ObjectMapper json = new ObjectMapper();
    private final Set<String> active = new HashSet<>();
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(16), runnable -> {
                Thread thread = new Thread(runnable, "eua-word-report");
                thread.setDaemon(true);
                return thread;
            }, new ThreadPoolExecutor.AbortPolicy());
    private boolean closed;

    public CqEuaTaskReportService(CqDockTaskRecordMapper records, CqEuaPlanService plans, CqDockProperties config) {
        this.records = records;
        this.plans = plans;
        this.config = config;
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
            return new ByteArrayResource(Files.readAllBytes(root.resolve(key + ".docx"))) {
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
        // Query by one identity, then compare both against the authoritative row: mismatches are not 'not found'.
        QueryWrapper<CqDockTaskRecordEntity> query = new QueryWrapper<>();
        if (business != null) query.eq("business_id", business);
        else query.eq("eua_task_id", eua);
        List<CqDockTaskRecordEntity> rows = records.selectList(query);
        if (rows.isEmpty()) throw error(HttpStatus.NOT_FOUND, "任务记录不存在");
        if (rows.size() != 1) throw error(HttpStatus.CONFLICT, "任务标识不唯一");
        CqDockTaskRecordEntity row = rows.get(0);
        String canonical = identifier(row.getBusinessId());
        if (canonical == null || !canonical.equals(row.getBusinessId())) {
            throw error(HttpStatus.BAD_REQUEST, "任务记录编码无效");
        }
        if ((business != null && !business.equals(canonical)) || (eua != null && !eua.equals(row.getEuaTaskId()))) {
            throw error(HttpStatus.BAD_REQUEST, "任务编码与EUA任务ID不匹配");
        }
        return row;
    }

    private String identifier(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        String id = value.trim();
        if (!id.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,199}")) {
            throw error(HttpStatus.BAD_REQUEST, "任务标识格式无效");
        }
        return id;
    }

    private String key(CqDockTaskRecordEntity record) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(record.getBusinessId().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder key = new StringBuilder();
            for (byte b : digest) key.append(String.format("%02x", b & 0xff));
            return key.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private Path storage() {
        try {
            if (config.getReportLocalPath() == null || config.getReportLocalPath().trim().isEmpty()) {
                throw new IOException("cq-dock.report-local-path is not configured");
            }
            Path root = Paths.get(config.getReportLocalPath()).toAbsolutePath().normalize();
            if (Files.isSymbolicLink(root)) throw new IOException("Report directory is a symbolic link");
            Files.createDirectories(root);
            return root.toRealPath();
        } catch (IOException | InvalidPathException e) {
            log.error("Cannot access EUA report storage", e);
            throw error(HttpStatus.INTERNAL_SERVER_ERROR, "报告存储不可用");
        }
    }

    private ReportStatus read(Path root, String key) {
        Path path = root.resolve(key + ".json");
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            return new ReportStatus("NOT_GENERATED", "报告尚未生成", null);
        }
        try {
            if (Files.isSymbolicLink(path)) throw new IOException("Invalid state file");
            ReportStatus state = json.readValue(path.toFile(), ReportStatus.class);
            if (!Arrays.asList("GENERATING", "COMPLETED", "FAILED").contains(state.getStatus())) {
                throw new IOException("Invalid report state");
            }
            if ("GENERATING".equals(state.getStatus()) && !active.contains(key)) {
                state = new ReportStatus("FAILED", "报告生成已中断，请重新生成", state.getFileName());
                write(root, key, state);
            }
            Path docx = root.resolve(key + ".docx");
            if ("COMPLETED".equals(state.getStatus())
                    && (!Files.isRegularFile(docx, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(docx))) {
                state = new ReportStatus("FAILED", "报告文件缺失，请重新生成", state.getFileName());
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
        } finally {
            deleteTemp(temp);
        }
    }

    private void publish(Path from, Path to) throws IOException {
        Files.move(from, to, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    private void generateFile(Path root, String key, CqDockTaskRecordEntity record, String fileName) {
        Path temp = null;
        try {
            CqEuaTaskPictureListRequest canonical = new CqEuaTaskPictureListRequest();
            canonical.setBusinessId(record.getBusinessId());
            canonical.setEuaTaskId(record.getEuaTaskId());
            List<CqDockTaskPictureEntity> pictures = plans.taskPictureList(canonical);
            temp = Files.createTempFile(root, key + "-", ".docx.tmp");
            try (XWPFDocument document = render(record, pictures); OutputStream out = Files.newOutputStream(temp)) {
                document.write(out);
            }
            synchronized (this) {
                if (Thread.currentThread().isInterrupted() || closed) throw new InterruptedException();
                publish(temp, root.resolve(key + ".docx"));
                write(root, key, new ReportStatus("COMPLETED", "报告生成完成", fileName));
                active.remove(key);
            }
        } catch (Exception e) {
            log.error("EUA report generation failed for {}", key, e);
            synchronized (this) {
                try {
                    write(root, key, new ReportStatus("FAILED", "报告生成失败，请重试", fileName));
                } catch (RuntimeException persistenceFailure) {
                    log.error("Cannot persist failed EUA report {}", key, persistenceFailure);
                } finally {
                    active.remove(key);
                }
            }
        } finally {
            deleteTemp(temp);
        }
    }

    private XWPFDocument render(CqDockTaskRecordEntity task, List<CqDockTaskPictureEntity> pictures) throws IOException {
        XWPFDocument doc = new XWPFDocument();
        try {
            CTSectPr section = doc.getDocument().getBody().addNewSectPr();
            section.addNewPgSz().setW(BigInteger.valueOf(11906));
            section.getPgSz().setH(BigInteger.valueOf(16838));
            section.addNewPgMar().setTop(BigInteger.valueOf(1134));
            section.getPgMar().setBottom(BigInteger.valueOf(1134));
            section.getPgMar().setLeft(BigInteger.valueOf(1134));
            section.getPgMar().setRight(BigInteger.valueOf(1134));
            XWPFParagraph footer = doc.createFooter(org.apache.poi.wp.usermodel.HeaderFooterType.DEFAULT).createParagraph();
            footer.setAlignment(ParagraphAlignment.CENTER);
            styledRun(footer, "页码：", 9, false);
            footer.getCTP().addNewFldSimple().setInstr("PAGE");
            XWPFParagraph cover = paragraph(doc, "巡视结果分析报告", 28, true, ParagraphAlignment.CENTER);
            cover.setSpacingBefore(1440);
            cover.setSpacingAfter(960);
            paragraph(doc, "东方电子股份有限公司", 18, true, ParagraphAlignment.CENTER);
            paragraph(doc, new java.text.SimpleDateFormat("yyyy年MM月dd日").format(new Date()),
                    14, false, ParagraphAlignment.CENTER).setSpacingAfter(720);
            XWPFTable coverInfo = table(doc, "项目", "信息");
            metadata(coverInfo, "任务名称", task.getTaskName());
            metadata(coverInfo, "编制单位", "东方电子股份有限公司");

            heading(doc, "注意事项", true);
            doc.getParagraphs().get(doc.getParagraphs().size() - 1).setAlignment(ParagraphAlignment.CENTER);
            paragraph(doc, "1. 本报告仅汇总本次任务已存储的任务记录、图片及分析/测温结果，不包含未采集或未入库的设备与点位。", 12, false, ParagraphAlignment.LEFT);
            paragraph(doc, "2. 图片仅从现有本地存储读取，不访问远程图片地址。缺失、不可读取或格式不支持的图片以占位说明标识，不据此推定设备状态。", 12, false, ParagraphAlignment.LEFT);
            paragraph(doc, "3. 分析状态与结果按存储记录展示，分析状态数量非缺陷数量；待分析、失败、未知或未记录状态均不代表设备正常。", 12, false, ParagraphAlignment.LEFT);
            paragraph(doc, "4. 可见光结果与红外测温值仅供参考，设备缺陷认定及处置应结合现场情况进行人工复核；不补充未记录的诊断或阈值结论。", 12, false, ParagraphAlignment.LEFT);
            paragraph(doc, "5. 当前图片模型未记录采集时间，明细统一标注“未记录”，不以图片入库或更新时间替代采集时间。", 12, false, ParagraphAlignment.LEFT);

            Map<String, List<CqDockTaskPictureEntity>> groups = pictures.stream().collect(Collectors.groupingBy(
                    p -> pointKey(p.getPointId()), LinkedHashMap::new, Collectors.toList()));
            long points = groups.keySet().stream().filter(k -> !k.isEmpty()).count();
            long visible = pictures.stream().filter(p -> Integer.valueOf(0).equals(p.getPointType())).count();
            long infrared = pictures.stream().filter(p -> Integer.valueOf(1).equals(p.getPointType())).count();
            long analyzed = pictures.stream().filter(p -> Integer.valueOf(0).equals(p.getPointType())
                    && Integer.valueOf(1).equals(p.getAnalysisStatus())).count();
            long measured = pictures.stream().filter(p -> Integer.valueOf(1).equals(p.getPointType())
                    && Integer.valueOf(3).equals(p.getAnalysisStatus())).count();
            Map<String, Long> statuses = pictures.stream().collect(Collectors.groupingBy(
                    p -> analysisStatus(p.getAnalysisStatus()), LinkedHashMap::new, Collectors.counting()));
            heading(doc, "1 报告概述", true);
            heading(doc, "1.1 分析状态统计", false);
            XWPFTable statusTable = table(doc, "实际分析状态", "图片数量");
            statuses.forEach((status, count) -> metadata(statusTable, status, count));
            if (statuses.isEmpty()) metadata(statusTable, "无图片数据", 0);
            paragraph(doc, "以上统计为图片分析状态数量，非缺陷数量，不推定设备是否正常。", 11, false, ParagraphAlignment.LEFT);
            heading(doc, "1.2 巡视内容", false);
            paragraph(doc, "本次巡视任务共巡视" + points + "条点位，包括" + visible + "张可见光图片及" + infrared
                    + "张红外图片，" + analyzed + "张可见光图片已进行分析，" + measured
                    + "张红外图片已进行测温，具体内容如下。", 11, false, ParagraphAlignment.LEFT);
            heading(doc, "2 巡视结果汇总", true);
            paragraph(doc, "点位数：" + points + "  图片数：" + pictures.size() + "  可见光：" + visible
                    + "  红外：" + infrared + "  未知类型：" + (pictures.size() - visible - infrared), 12, true, ParagraphAlignment.LEFT);
            XWPFTable summary = table(doc, "点位编码", "点位名称", "图片数量", "分析状态分布");
            for (Map.Entry<String, List<CqDockTaskPictureEntity>> group : groups.entrySet()) {
                String distribution = group.getValue().stream().collect(Collectors.groupingBy(
                        p -> analysisStatus(p.getAnalysisStatus()), LinkedHashMap::new, Collectors.counting()))
                        .entrySet().stream().map(e -> e.getKey() + "：" + e.getValue()).collect(Collectors.joining("；"));
                tableRow(summary.createRow(), text(group.getKey()), text(group.getValue().get(0).getPointName()),
                        String.valueOf(group.getValue().size()), distribution);
            }
            if (pictures.isEmpty()) paragraph(doc, "暂无任务图片，不作巡检结论。", 12, false, ParagraphAlignment.LEFT);
            int number = 0;
            for (List<CqDockTaskPictureEntity> group : groups.values()) {
                for (CqDockTaskPictureEntity p : group) {
                    if (Thread.currentThread().isInterrupted()) throw new IOException("Report generation interrupted");
                    imagePage(doc, p, ++number);
                }
            }
            return doc;
        } catch (Exception e) {
            doc.close();
            if (e instanceof IOException) throw (IOException) e;
            throw new IOException("Cannot render report", e);
        }
    }

    private String pointKey(String value) { return value == null ? "" : value.trim(); }

    private void imagePage(XWPFDocument doc, CqDockTaskPictureEntity picture, int number) {
        String type = Integer.valueOf(0).equals(picture.getPointType()) ? "可见光"
                : Integer.valueOf(1).equals(picture.getPointType()) ? "红外" : "未知类型";
        XWPFParagraph pageBreak = paragraph(doc, "", 1, false, ParagraphAlignment.LEFT);
        pageBreak.setPageBreak(true);
        pageBreak.setSpacingBefore(0);
        pageBreak.setSpacingAfter(0);
        pageBreak.setSpacingBetween(1, LineSpacingRule.EXACT);
        pageBreak.setKeepNext(true);
        XWPFTable details = table(doc, "序号", "点位名称", "图片类型", "采集时间", "分析状态", "结果描述");
        int[] widths = {550, 1600, 1400, 1050, 1450, 3588};
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGrid grid = details.getCTTbl().addNewTblGrid();
        for (int width : widths) grid.addNewGridCol().setW(BigInteger.valueOf(width));
        String result = text(picture.getPointVal());
        if (picture.getPointValUnit() != null && !picture.getPointValUnit().trim().isEmpty()) result += " " + picture.getPointValUnit();
        tableRow(details.createRow(), String.valueOf(number), text(picture.getPointName()), type,
                "未记录", analysisStatus(picture.getAnalysisStatus()), result);
        for (XWPFTableRow row : details.getRows()) for (int i = 0; i < widths.length; i++) row.getCell(i).setWidth(String.valueOf(widths[i]));
        image(doc, picture, Integer.valueOf(0).equals(picture.getPointType()));
    }

    private void image(XWPFDocument doc, CqDockTaskPictureEntity picture, boolean analysis) {
        String label = analysis ? "可见光结果图片" : Integer.valueOf(1).equals(picture.getPointType()) ? "红外图片" : "未知类型图片";
        if (analysis && (picture.getAnalysisImageUrl() == null || picture.getAnalysisImageUrl().trim().isEmpty())) {
            paragraph(doc, label + "缺失：本地图片不可用", 11, false, ParagraphAlignment.CENTER);
            return;
        }
        try {
            org.springframework.core.io.Resource resource = plans.taskPictureImage(picture.getId(), analysis);
            byte[] data;
            try (InputStream input = resource.getInputStream()) { data = input.readAllBytes(); }
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(data));
            if (image == null) throw new IOException("Unsupported image");
            // Re-encode as PNG so embedded format never depends on a URL or a misleading extension.
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            if (!ImageIO.write(image, "png", png)) throw new IOException("Cannot encode image");
            double scale = Math.min(450.0 / image.getWidth(), 400.0 / image.getHeight());
            XWPFParagraph paragraph = doc.createParagraph();
            paragraph.setAlignment(ParagraphAlignment.CENTER);
            styledRun(paragraph, "", 11, false).addPicture(new ByteArrayInputStream(png.toByteArray()), Document.PICTURE_TYPE_PNG,
                    "picture.png", Units.toEMU(image.getWidth() * scale), Units.toEMU(image.getHeight() * scale));
        } catch (ResponseStatusException e) {
            if (e.getStatus() != HttpStatus.NOT_FOUND) throw e;
            paragraph(doc, label + "缺失：本地图片不可用", 11, false, ParagraphAlignment.CENTER);
        } catch (IOException | org.apache.poi.openxml4j.exceptions.InvalidFormatException e) {
            paragraph(doc, label + "缺失：本地图片不可用或格式不支持", 11, false, ParagraphAlignment.CENTER);
        }
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

    private void heading(XWPFDocument doc, String title, boolean pageBreak) {
        XWPFParagraph p = paragraph(doc, title, pageBreak ? 18 : 14, true, ParagraphAlignment.LEFT);
        p.setPageBreak(pageBreak);
        p.setKeepNext(true);
    }

    private XWPFParagraph paragraph(XWPFDocument doc, String value, int size, boolean bold,
                                    ParagraphAlignment alignment) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(alignment);
        p.setSpacingAfter(160);
        styledRun(p, value, size, bold);
        return p;
    }

    private XWPFRun styledRun(XWPFParagraph paragraph, String value, int size, boolean bold) {
        XWPFRun run = paragraph.createRun();
        run.setFontFamily("宋体");
        run.setFontFamily("宋体", XWPFRun.FontCharRange.eastAsia);
        run.setFontSize(size);
        run.setBold(bold);
        run.setColor("000000");
        run.setText(value);
        return run;
    }

    private void metadata(XWPFTable table, String name, Object value) {
        tableRow(table.createRow(), name, value instanceof Date
                ? new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((Date) value) : text(value));
    }

    private XWPFTable table(XWPFDocument doc, String... headers) {
        XWPFTable table = doc.createTable(1, headers.length);
        table.setWidth("100%");
        table.setInsideHBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "B7B7B7");
        table.setInsideVBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "B7B7B7");
        table.setTopBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "B7B7B7");
        table.setBottomBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "B7B7B7");
        table.setLeftBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "B7B7B7");
        table.setRightBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "B7B7B7");
        tableRow(table.getRow(0), headers);
        table.getRow(0).setRepeatHeader(true);
        return table;
    }

    private void tableRow(XWPFTableRow row, String... values) {
        row.setCantSplitRow(true);
        for (int i = 0; i < values.length; i++) {
            XWPFTableCell cell = row.getCell(i);
            cell.setColor("FFFFFF");
            cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
            XWPFParagraph p = cell.getParagraphs().get(0);
            p.setSpacingAfter(80);
            p.setSpacingBefore(80);
            boolean header = row == row.getTable().getRow(0);
            p.setAlignment(header ? ParagraphAlignment.CENTER : ParagraphAlignment.LEFT);
            styledRun(p, values[i], 10, header);
        }
    }

    private String text(Object value) { return value == null || value.toString().trim().isEmpty() ? "未记录" : value.toString(); }

    private String fileName(CqDockTaskRecordEntity record) {
        String name = text(record.getTaskName()).replaceAll("[^\\p{L}\\p{N}_-]", "_");
        return name.substring(0, Math.min(60, name.length())) + "_巡检报告.docx";
    }

    private void deleteTemp(Path path) {
        if (path == null) return;
        try { Files.deleteIfExists(path); }
        catch (IOException e) { log.warn("Cannot remove EUA report temporary file", e); }
    }

    private ResponseStatusException error(HttpStatus status, String message) {
        return new ResponseStatusException(status, message);
    }

    @Override
    @PreDestroy
    public void close() {
        synchronized (this) { closed = true; }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) executor.shutdownNow();
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
