package com.dji.sample.df.uavCommonHandleDf.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dji.sample.center.entity.UniPoint;
import com.dji.sample.df.mediaDf.OrdinaryPhotoMatcher;
import com.dji.sample.df.mediaDf.dao.IFileMapperDf;
import com.dji.sample.df.mediaDf.model.MediaFileEntity;
import com.dji.sample.df.substationDf.dao.UniPointMapper2;
import com.dji.sample.df.windDf.config.FjFileConfig;
import com.dji.sample.df.temperatureMeasurementDF.service.TemMeasureService;
import com.dji.sample.df.temperatureMeasurementDF.modol.TemResultEntity;
import lombok.extern.slf4j.Slf4j;
import com.dji.sample.wayline.model.entity.WaylineJobEntity;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CollectionXlsReportService {
    @Autowired private TemMeasureService temMeasureService;
    @Autowired private IFileMapperDf mediaMapper;
    @Autowired private UniPointMapper2 pointMapper;
    @Autowired private FjFileConfig fileConfig;
    @Autowired private com.dji.sample.df.centerOrdinary.CenterOrdinaryTaskService centerOrdinaryTasks;
    @Value("${ordinary-report.template-path:./templates/report-excel-templates.xls}")
    private String templatePath;

    public Path reportPath(String jobId) {
        if (jobId == null || !jobId.matches("[a-zA-Z0-9_-]+")) throw new IllegalArgumentException("任务标识无效");
        return Paths.get(fileConfig.getFileReportPath()).resolve("collection_" + jobId + ".xls");
    }

    public static String pointLabel(String name) {
        return OrdinaryPhotoMatcher.pointName(name);
    }

    public static String photoKey(String name) {
        return OrdinaryPhotoMatcher.photoKey(name);
    }

    public synchronized Path generate(WaylineJobEntity job, String operator) {
        Path output = reportPath(job.getJobId());
        if (job.getFileId() == null || job.getFileId().isBlank()) throw new IllegalArgumentException("任务未绑定航线 file_id，无法匹配点位");
        if (job.getWorkspaceId() == null || job.getWorkspaceId().isBlank()) throw new IllegalArgumentException("任务工作空间无效");
        Path template = Paths.get(templatePath);
        if (!Files.isRegularFile(template) || !Files.isReadable(template))
            throw new IllegalArgumentException("普通报告 XLS 模板不存在或不可读，请放置模板并配置 ordinary-report.template-path：" + template);
        com.dji.sample.df.centerOrdinary.CenterOrdinaryTaskEntity managed = centerOrdinaryTasks==null ? null : centerOrdinaryTasks.findJob(job.getJobId());
        Map<MediaFileEntity,UniPoint> frozenMapping=new LinkedHashMap<>();
        Map<MediaFileEntity,String> storedPaths=new HashMap<>();
        if (managed!=null) {
            com.dji.sample.df.centerOrdinary.CenterOrdinarySelection.routePoints(managed.getPointSnapshot(),managed.getSubCode(),managed.getRouteId());
            if (!"COMPLETED".equals(managed.getStatus())) throw new IllegalArgumentException("选中点位采集尚未完整完成，不能生成报告");
            List<String> codes=com.alibaba.fastjson.JSON.parseArray(managed.getPointCodes(),String.class);
            if (managed.getResultSnapshot()!=null) for (com.alibaba.fastjson.JSONObject row:com.alibaba.fastjson.JSON.parseArray(managed.getResultSnapshot(),com.alibaba.fastjson.JSONObject.class)) {
                UniPoint point=row.getObject("point",UniPoint.class); MediaFileEntity file=row.getObject("media",MediaFileEntity.class);
                String path=row.getString("localPath");
                if (point!=null && file!=null && codes.contains(point.getPointCode()) && job.getJobId().equals(file.getJobId())
                        && job.getWorkspaceId().equals(file.getWorkspaceId()) && path!=null && !path.isBlank()) {
                    frozenMapping.put(file,point); storedPaths.put(file,path);
                }
            }
        }
        List<MediaFileEntity> photos = managed!=null ? new ArrayList<>(frozenMapping.keySet()) : mediaMapper.selectList(new LambdaQueryWrapper<MediaFileEntity>()
                .eq(MediaFileEntity::getJobId, job.getJobId()).eq(MediaFileEntity::getWorkspaceId, job.getWorkspaceId())
                .orderByAsc(MediaFileEntity::getId));
        if (photos.isEmpty()) throw new IllegalArgumentException("当前任务暂无采集图片");
        if (photos.size() > 65529) throw new IllegalArgumentException("图片数量超出 XLS 行数上限");
        List<UniPoint> points = managed!=null ? new ArrayList<>(frozenMapping.values()) : pointMapper.selectList(new LambdaQueryWrapper<UniPoint>().eq(UniPoint::getWaylineId, job.getFileId()));
        int totalPhotos = photos.size();
        OrdinaryPhotoMatcher.ReportMatchResult reportMatch = managed!=null ? null : OrdinaryPhotoMatcher.matchForReport(job.getFileId(), points, photos);
        Map<MediaFileEntity,UniPoint> mapping = managed!=null ? frozenMapping : reportMatch.getMatchedMap();
        List<String> issues = reportMatch==null ? Collections.emptyList() : reportMatch.getIssues();
        List<String> thermalIssues = new ArrayList<>();
        int thermalMeasured = 0;
        photos = photos.stream().filter(mapping::containsKey).distinct().collect(Collectors.toList());
        List<UniPoint> matched = new ArrayList<>();
        for (MediaFileEntity photo : photos) matched.add(mapping.get(photo));
        Set<String> stations = (matched.isEmpty() ? points.stream().filter(p -> Objects.equals(job.getFileId(), p.getWaylineId())) : matched.stream())
                .map(p -> text(p.getSubName())).collect(Collectors.toCollection(LinkedHashSet::new));
        if (!matched.isEmpty() && stations.contains("未提供")) throw new IllegalArgumentException("匹配点位未提供厂站名称，无法填写巡检单位");
        if (stations.isEmpty()) stations.add("未提供");
        int extraRows = issues.isEmpty() ? 0 : 1 + issues.size() + (photos.isEmpty() ? 1 : 0);
        if (7L + photos.size() + extraRows > 65536) throw new IllegalArgumentException("报告明细及匹配异常说明超出 XLS 行数上限");
        Path temp = null;
        try (InputStream input = Files.newInputStream(template); HSSFWorkbook book = new HSSFWorkbook(input)) {
            HSSFSheet sheet = book.getSheetAt(0);
            if (sheet.getRow(7) == null || sheet.getRow(0) == null || sheet.getRow(6) == null)
                throw new IllegalArgumentException("普通报告模板结构不符合约定的前7行表头和第8行明细");
            HSSFCellStyle[] styles = new HSSFCellStyle[14];
            for (int c = 0; c < 14; c++) styles[c] = sheet.getRow(7).getCell(c, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getCellStyle();
            for (int r = sheet.getLastRowNum(); r >= 7; r--) if (sheet.getRow(r) != null) sheet.removeRow(sheet.getRow(r));
            for (int i = sheet.getNumMergedRegions() - 1; i >= 0; i--) if (sheet.getMergedRegion(i).getLastRow() >= 7) sheet.removeMergedRegion(i);
            put(sheet, 0, 4, text(job.getName())); put(sheet, 0, 10, "未提供");
            put(sheet, 1, 4, text(operator) + "（生成操作人）"); put(sheet, 1, 10, time(System.currentTimeMillis()) + "（生成时间）");
            put(sheet, 2, 4, time(job.getExecuteTime())); put(sheet, 2, 10, time(job.getCompletedTime()));
            long unique = matched.stream().map(p -> p.getId()).distinct().count();
            put(sheet, 5, 0, "巡检单位名称：" + String.join("、", stations));
            Path directory = null;
            if (!photos.isEmpty()) {
                Path base = Paths.get(fileConfig.getRecfilePath() + fileConfig.getRecfileNativePath()).toRealPath();
                directory = base.resolve(job.getJobId()).toRealPath();
                if (!directory.startsWith(base)) throw new IllegalArgumentException("原图目录无效");
            }
            HSSFPatriarch drawing = sheet.createDrawingPatriarch();
            HSSFCellStyle resultStyle = book.createCellStyle(); resultStyle.cloneStyleFrom(styles[9]);
            resultStyle.setWrapText(true); resultStyle.setVerticalAlignment(VerticalAlignment.TOP);
            sheet.setColumnWidth(9, Math.max(sheet.getColumnWidth(9), 24 * 256));
            for (int i = 0; i < photos.size(); i++) {
                MediaFileEntity photo = photos.get(i); UniPoint point = matched.get(i);
                String name = photo.getFileName();
                Path imagePath;
                byte[] bytes;
                BufferedImage image;
                try {
                    imagePath = managed!=null ? Paths.get(storedPaths.get(photo)).toRealPath() : directory.resolve("1_" + name.substring(0, name.lastIndexOf('.')) + ".jpg").toRealPath();
                    if (!imagePath.startsWith(directory) || !Files.isRegularFile(imagePath)) throw new IOException("原图路径无效");
                    bytes = Files.readAllBytes(imagePath);
                    image = ImageIO.read(new ByteArrayInputStream(bytes));
                    if (image == null || bytes.length < 3 || (bytes[0] & 255) != 255 || (bytes[1] & 255) != 216) throw new IOException("原图不是可读JPEG");
                } catch (IOException e) { throw new IllegalArgumentException("原图缺失或不可读：" + name + "，" + e.getMessage(), e); }
                String result = "正常";
                if (Integer.valueOf(1).equals(point.getPicType())) {
                    try {
                        TemResultEntity temperature = temMeasureService.measureWholeImage(imagePath.toString());
                        if (temperature == null) throw new IllegalArgumentException("测温服务未返回结果");
                        if (temperature.getError() != null && !temperature.getError().isBlank())
                            throw new IllegalArgumentException(temperature.getError());
                        if (!finite(temperature.getMax_tem()) || !finite(temperature.getMin_tem()) || !finite(temperature.getAverage_tem()))
                            throw new IllegalArgumentException("温度结果缺失或非有限值");
                        result = String.format(Locale.ROOT, "最高温：%.1f℃；最低温：%.1f℃；平均温度：%.1f℃",
                                temperature.getMax_tem(), temperature.getMin_tem(), temperature.getAverage_tem());
                        thermalMeasured++;
                    } catch (RuntimeException e) {
                        String reason = measurementFailure(e);
                        result = "测温失败：" + reason;
                        thermalIssues.add("文件名=" + bounded(name) + "；" + result);
                        log.warn("普通报告红外测温失败，job={}，file={}，原因={}", bounded(job.getJobId()), bounded(name), reason);
                    }
                }
                int rowNumber = 7 + i;
                HSSFRow row = sheet.createRow(rowNumber); row.setHeightInPoints(160);
                String[] values = {String.valueOf(i + 1), text(point.getAreaName()), text(point.getBayName()), text(point.getDeviceName()),
                        text(point.getComponentName()), text(point.getPointName()), text(point.getLevel()),
                        "无人机采集：" + name, time(photo.getCreateTime()), result, ""};
                for (int c = 0; c < 14; c++) { HSSFCell cell = row.createCell(c); cell.setCellStyle(styles[c]); cell.setCellValue(c < values.length ? values[c] : ""); }
                row.getCell(9).setCellStyle(resultStyle);
                double width = sheet.getColumnWidthInPixels(10);
                double height = 160 * 96.0 / 72;
                double scale = Math.min((width - 4) / image.getWidth(), (height - 4) / image.getHeight());
                int dx = Math.max(1, Math.min(1022, (int) (image.getWidth() * scale / width * 1024)));
                int dy = Math.max(1, Math.min(254, (int) (image.getHeight() * scale / height * 256)));
                HSSFClientAnchor anchor = new HSSFClientAnchor(1, 1, dx, dy, (short) 10, rowNumber, (short) 10, rowNumber);
                anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_DONT_RESIZE);
                drawing.createPicture(anchor, book.addPicture(bytes, Workbook.PICTURE_TYPE_JPEG));
            }
            int thermalCount = thermalMeasured + thermalIssues.size();
            String summary = "总照片" + totalPhotos + "张，匹配" + photos.size() + "张，跳过" + issues.size() + "张，匹配点位" + unique + "个。";
            if (thermalCount > 0) {
                summary += "可见光正常" + (photos.size() - thermalCount) + "条，红外测温成功" + thermalMeasured + "条，红外测温失败" + thermalIssues.size() + "条。";
            } else if (issues.isEmpty()) {
                summary = "匹配点位" + unique + "个，采集图片" + photos.size() + "张，结果正常" + photos.size() + "条。";
            }
            put(sheet, 3, 4, summary);
            put(sheet, 4, 4, (thermalCount > 0 ? "红外按整图区域测温处理，仅列温度未做阈值判定。" : "")
                    + (!thermalIssues.isEmpty() ? "部分红外测温失败，详见红外测温异常说明。" : "")
                    + (!issues.isEmpty() ? "部分照片未纳入，详见匹配异常说明" : thermalCount == 0 ? stations + "飞行航线正常，采集图片正常" : ""));
            extraRows += thermalIssues.isEmpty() ? 0 : 1 + thermalIssues.size();
            if (7L + photos.size() + extraRows > 65536) throw new IllegalArgumentException("报告明细及异常说明超出 XLS 行数上限");
            if (!issues.isEmpty() || !thermalIssues.isEmpty()) {
                HSSFCellStyle issueStyle = book.createCellStyle(); issueStyle.cloneStyleFrom(styles[0]);
                issueStyle.setWrapText(true); issueStyle.setVerticalAlignment(VerticalAlignment.TOP);
                issueStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex()); issueStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                int next = 7 + photos.size();
                if (!issues.isEmpty()) {
                    appendIssue(sheet, next++, "匹配异常说明", issueStyle);
                    if (photos.isEmpty()) appendIssue(sheet, next++, "无成功匹配点位，本报告未生成正常明细", issueStyle);
                    for (String issue : issues) appendIssue(sheet, next++, issue, issueStyle);
                }
                if (!thermalIssues.isEmpty()) {
                    appendIssue(sheet, next++, "红外测温异常说明", issueStyle);
                    for (String issue : thermalIssues) appendIssue(sheet, next++, issue, issueStyle);
                }
            }
            for (Sheet s : book) for (Row r : s) for (Cell c : r) {
                if (c.getCellType() == CellType.STRING && (c.getStringCellValue().contains("{{") || c.getStringCellValue().contains("}}")))
                    throw new IllegalArgumentException("模板存在未绑定占位符：" + s.getSheetName() + "!" + c.getAddress());
            }
            sheet.getPrintSetup().setPaperSize(PrintSetup.A4_PAPERSIZE);
            sheet.getPrintSetup().setFitWidth((short) 1); sheet.getPrintSetup().setFitHeight((short) 0);
            sheet.setFitToPage(true); sheet.setAutobreaks(true);
            sheet.setRepeatingRows(new CellRangeAddress(0, 6, -1, -1));
            book.setPrintArea(0, 0, 13, 0, 6 + photos.size() + extraRows);
            Files.createDirectories(output.getParent());
            temp = Files.createTempFile(output.getParent(), "collection-", ".tmp");
            try (OutputStream stream = Files.newOutputStream(temp)) { book.write(stream); }
            Files.move(temp, output, StandardCopyOption.REPLACE_EXISTING);
            return output;
        } catch (IOException e) { throw new IllegalArgumentException("普通报告生成失败：" + e.getMessage(), e); }
        finally { if (temp != null) try { Files.deleteIfExists(temp); } catch (IOException ignored) { } }
    }

    private static boolean finite(Double value) { return value != null && Double.isFinite(value); }

    private static String bounded(String value) {
        return value == null ? "未提供" : value.replaceAll("[\\r\\n\\t]", " ").substring(0, Math.min(value.length(), 160));
    }

    private static String measurementFailure(RuntimeException error) {
        String message = error.getMessage();
        if (message == null) return "测温服务异常（" + error.getClass().getSimpleName() + "）";
        // Only publish known diagnostic categories, never raw paths, subprocess output or credentials.
        for (String reason : Arrays.asList("CLI 测温超时", "CLI 测温被中断", "CLI 测温失败", "CLI 输出读取失败",
                "无法读取本地红外图片", "本地红外图片不存在", "生成raw格式温度图出错", "温度处理异常", "温度值异常",
                "测温服务未返回结果", "温度结果缺失或非有限值", "红外图片为空", "红外图片超过"))
            if (message.contains(reason)) return reason;
        if (message.contains("float32") || message.contains("TIFF") || message.contains("no-data")) return "温度矩阵格式或数据无效";
        if (message.contains("thermal.cli")) return "红外测温 CLI 配置或资源限制错误";
        return "测温服务异常（" + error.getClass().getSimpleName() + "），请检查测温配置及服务日志";
    }

    private static void appendIssue(HSSFSheet sheet, int index, String value, HSSFCellStyle style) {
        if (value.length() > 32767) throw new IllegalArgumentException("匹配异常说明超出 XLS 单元格文本上限");
        HSSFRow row = sheet.createRow(index);
        row.setHeightInPoints(Math.min(400, 30 + 15 * (value.length() / 80)));
        for (int c = 0; c < 14; c++) { HSSFCell cell = row.createCell(c); cell.setCellStyle(style); }
        row.getCell(0).setCellValue(value);
        sheet.addMergedRegion(new CellRangeAddress(index, index, 0, 13));
    }
    private static void put(HSSFSheet sheet, int r, int c, String value) { sheet.getRow(r).getCell(c, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).setCellValue(value); }
    private static String text(Object value) { return value == null || value.toString().isBlank() ? "未提供" : value.toString(); }
    private static String time(Long value) { return value == null ? "未提供" : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(value)); }
    private static String escape(String value) { return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;"); }

    public synchronized String preview(String jobId) {
        try (InputStream input = Files.newInputStream(reportPath(jobId)); HSSFWorkbook book = new HSSFWorkbook(input)) {
            HSSFSheet sheet = book.getSheetAt(0);
            Map<Integer, HSSFPictureData> images = new HashMap<>();
            if (sheet.getDrawingPatriarch() != null) for (HSSFShape shape : sheet.getDrawingPatriarch().getChildren()) {
                if (shape instanceof HSSFPicture) images.put(((HSSFClientAnchor) shape.getAnchor()).getRow1(), ((HSSFPicture) shape).getPictureData());
            }
            StringBuilder html = new StringBuilder("<!doctype html><html><head><meta charset=\"UTF-8\"><style>body{font-family:Arial,sans-serif}table{border-collapse:collapse;width:100%;min-width:1100px}td{border:1px solid #777;padding:6px;white-space:pre-wrap;overflow-wrap:anywhere}img{max-width:180px;max-height:210px;object-fit:contain}tr:nth-child(-n+7){background:#f2f4f7}</style></head><body><table>");
            DataFormatter formatter = new DataFormatter();
            for (int r = 0; r <= sheet.getLastRowNum(); r++) {
                html.append("<tr>");
                for (int c = 0; c < 14; c++) {
                    CellRangeAddress merged = null;
                    for (CellRangeAddress region : sheet.getMergedRegions()) if (region.isInRange(r, c)) { merged = region; break; }
                    if (merged != null && (r != merged.getFirstRow() || c != merged.getFirstColumn())) continue;
                    html.append("<td");
                    if (merged != null) html.append(" colspan=\"").append(merged.getNumberOfCells() / (merged.getLastRow() - merged.getFirstRow() + 1)).append("\" rowspan=\"").append(merged.getLastRow() - merged.getFirstRow() + 1).append("\"");
                    html.append('>');
                    Row row = sheet.getRow(r);
                    html.append(escape(row == null ? "" : formatter.formatCellValue(row.getCell(c))));
                    if (c == 10 && images.containsKey(r)) html.append("<img alt=\"巡检图片\" src=\"data:image/jpeg;base64,").append(Base64.getEncoder().encodeToString(images.get(r).getData())).append("\">");
                    html.append("</td>");
                }
                html.append("</tr>");
            }
            return html.append("</table></body></html>").toString();
        } catch (IOException e) { throw new IllegalArgumentException("报告不存在或无法预览，请重新生成", e); }
    }
}
