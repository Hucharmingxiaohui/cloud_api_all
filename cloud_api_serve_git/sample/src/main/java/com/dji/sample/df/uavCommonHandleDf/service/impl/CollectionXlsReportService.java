package com.dji.sample.df.uavCommonHandleDf.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dji.sample.center.entity.UniPoint;
import com.dji.sample.df.mediaDf.dao.IFileMapperDf;
import com.dji.sample.df.mediaDf.model.MediaFileEntity;
import com.dji.sample.df.substationDf.dao.UniPointMapper2;
import com.dji.sample.df.windDf.config.FjFileConfig;
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
import java.util.regex.*;
import java.util.stream.Collectors;

@Service
public class CollectionXlsReportService {
    @Autowired private IFileMapperDf mediaMapper;
    @Autowired private UniPointMapper2 pointMapper;
    @Autowired private FjFileConfig fileConfig;
    @Value("${ordinary-report.template-path:./templates/report-excel-templates.xls}")
    private String templatePath;
    private static final Pattern PHOTO = Pattern.compile("^DJI_\\d{14}_\\d+_([TV])_+(.+)\\.[^.]+$");

    public Path reportPath(String jobId) {
        if (jobId == null || !jobId.matches("[a-zA-Z0-9_-]+")) throw new IllegalArgumentException("任务标识无效");
        return Paths.get(fileConfig.getFileReportPath()).resolve("collection_" + jobId + ".xls");
    }

    public static String pointLabel(String name) {
        if (name == null) return "";
        name = name.trim();
        while (name.endsWith("-")) name = name.substring(0, name.length() - 1).trim();
        return name.substring(name.lastIndexOf('-') + 1).trim();
    }

    public static String photoKey(String name) {
        if (name == null || name.contains("/") || name.contains("\\")) throw new IllegalArgumentException("图片名称无效：" + name);
        Matcher matcher = PHOTO.matcher(name);
        if (!matcher.matches() || matcher.group(2).trim().isEmpty())
            throw new IllegalArgumentException("图片文件名不符合 DJI_14位时间_序号_T/V_点位标签.扩展名：" + name);
        return ("V".equals(matcher.group(1)) ? "0:" : "1:") + matcher.group(2).trim();
    }

    public synchronized Path generate(WaylineJobEntity job, String operator) {
        Path output = reportPath(job.getJobId());
        if (job.getFileId() == null || job.getFileId().isBlank()) throw new IllegalArgumentException("任务未绑定航线 file_id，无法匹配点位");
        if (job.getWorkspaceId() == null || job.getWorkspaceId().isBlank()) throw new IllegalArgumentException("任务工作空间无效");
        Path template = Paths.get(templatePath);
        if (!Files.isRegularFile(template) || !Files.isReadable(template))
            throw new IllegalArgumentException("普通报告 XLS 模板不存在或不可读，请放置模板并配置 ordinary-report.template-path：" + template);
        List<MediaFileEntity> photos = mediaMapper.selectList(new LambdaQueryWrapper<MediaFileEntity>()
                .eq(MediaFileEntity::getJobId, job.getJobId()).eq(MediaFileEntity::getWorkspaceId, job.getWorkspaceId())
                .orderByAsc(MediaFileEntity::getId));
        if (photos.isEmpty()) throw new IllegalArgumentException("当前任务暂无采集图片");
        if (photos.size() > 65529) throw new IllegalArgumentException("图片数量超出 XLS 行数上限");
        List<UniPoint> points = pointMapper.selectList(new LambdaQueryWrapper<UniPoint>().eq(UniPoint::getWaylineId, job.getFileId()));
        Map<String, List<UniPoint>> index = new LinkedHashMap<>();
        for (UniPoint point : points) {
            if (!job.getFileId().equals(point.getWaylineId()) || pointLabel(point.getPointName()).isEmpty()) continue;
            if (!Integer.valueOf(0).equals(point.getPicType()) && !Integer.valueOf(1).equals(point.getPicType())) continue;
            index.computeIfAbsent(point.getPicType() + ":" + pointLabel(point.getPointName()), k -> new ArrayList<>()).add(point);
        }
        Set<String> conflicts = new LinkedHashSet<>();
        List<UniPoint> matched = new ArrayList<>();
        List<String> unmatched = new ArrayList<>();
        for (MediaFileEntity photo : photos) {
            String key = photoKey(photo.getFileName());
            List<UniPoint> candidates = index.get(key);
            if (candidates == null) unmatched.add(photo.getFileName() + "（航线=" + job.getFileId() + "，类型/标签=" + key + "，未匹配点位名称去除末尾 '-' 后的最后非空标签）");
            else if (candidates.size() > 1) conflicts.add("航线=" + job.getFileId() + " 类型/标签=" + key
                    + " 类型=" + (key.startsWith("0:") ? "可见光(V，picType=0)" : "红外(T，picType=1)") + " 点位="
                    + candidates.stream().map(p -> p.getId() + "(" + p.getPointName() + ")").collect(Collectors.joining(",")));
            else matched.add(candidates.get(0));
        }
        if (!conflicts.isEmpty()) throw new IllegalArgumentException("点位匹配冲突：" + String.join("；", conflicts));
        if (!unmatched.isEmpty()) throw new IllegalArgumentException(String.join("；", unmatched));
        Set<String> stations = matched.stream().map(p -> text(p.getSubName())).collect(Collectors.toCollection(LinkedHashSet::new));
        if (stations.contains("未提供")) throw new IllegalArgumentException("匹配点位未提供厂站名称，无法填写巡检单位");
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
            put(sheet, 3, 4, "匹配点位" + unique + "个，采集图片" + photos.size() + "张，结果正常" + photos.size() + "条。");
            put(sheet, 4, 4, stations+"飞行航线正常，采集图片正常");
            put(sheet, 5, 0, "巡检单位名称：" + String.join("、", stations));
            Path base = Paths.get(fileConfig.getRecfilePath() + fileConfig.getRecfileNativePath()).toRealPath();
            Path directory = base.resolve(job.getJobId()).toRealPath();
            if (!directory.startsWith(base)) throw new IllegalArgumentException("原图目录无效");
            HSSFPatriarch drawing = sheet.createDrawingPatriarch();
            for (int i = 0; i < photos.size(); i++) {
                MediaFileEntity photo = photos.get(i); UniPoint point = matched.get(i);
                String name = photo.getFileName();
                Path imagePath;
                byte[] bytes;
                BufferedImage image;
                try {
                    imagePath = directory.resolve("1_" + name.substring(0, name.lastIndexOf('.')) + ".jpg").toRealPath();
                    if (!imagePath.startsWith(directory) || !Files.isRegularFile(imagePath)) throw new IOException("原图路径无效");
                    bytes = Files.readAllBytes(imagePath);
                    image = ImageIO.read(new ByteArrayInputStream(bytes));
                    if (image == null || bytes.length < 3 || (bytes[0] & 255) != 255 || (bytes[1] & 255) != 216) throw new IOException("原图不是可读JPEG");
                } catch (IOException e) { throw new IllegalArgumentException("原图缺失或不可读：" + name + "，" + e.getMessage(), e); }
                int rowNumber = 7 + i;
                HSSFRow row = sheet.createRow(rowNumber); row.setHeightInPoints(160);
                String[] values = {String.valueOf(i + 1), text(point.getAreaName()), text(point.getBayName()), text(point.getDeviceName()),
                        text(point.getComponentName()), text(point.getPointName()), text(point.getLevel()),
                        "无人机采集：" + name, time(photo.getCreateTime()), "正常", ""};
                for (int c = 0; c < 14; c++) { HSSFCell cell = row.createCell(c); cell.setCellStyle(styles[c]); cell.setCellValue(c < values.length ? values[c] : ""); }
                double width = sheet.getColumnWidthInPixels(10);
                double height = 160 * 96.0 / 72;
                double scale = Math.min((width - 4) / image.getWidth(), (height - 4) / image.getHeight());
                int dx = Math.max(1, Math.min(1022, (int) (image.getWidth() * scale / width * 1024)));
                int dy = Math.max(1, Math.min(254, (int) (image.getHeight() * scale / height * 256)));
                HSSFClientAnchor anchor = new HSSFClientAnchor(1, 1, dx, dy, (short) 10, rowNumber, (short) 10, rowNumber);
                anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_DONT_RESIZE);
                drawing.createPicture(anchor, book.addPicture(bytes, Workbook.PICTURE_TYPE_JPEG));
            }
            for (Sheet s : book) for (Row r : s) for (Cell c : r) {
                if (c.getCellType() == CellType.STRING && (c.getStringCellValue().contains("{{") || c.getStringCellValue().contains("}}")))
                    throw new IllegalArgumentException("模板存在未绑定占位符：" + s.getSheetName() + "!" + c.getAddress());
            }
            sheet.getPrintSetup().setPaperSize(PrintSetup.A4_PAPERSIZE);
            sheet.getPrintSetup().setFitWidth((short) 1); sheet.getPrintSetup().setFitHeight((short) 0);
            sheet.setFitToPage(true); sheet.setAutobreaks(true);
            sheet.setRepeatingRows(new CellRangeAddress(0, 6, -1, -1));
            book.setPrintArea(0, 0, 13, 0, 6 + photos.size());
            Files.createDirectories(output.getParent());
            temp = Files.createTempFile(output.getParent(), "collection-", ".tmp");
            try (OutputStream stream = Files.newOutputStream(temp)) { book.write(stream); }
            Files.move(temp, output, StandardCopyOption.REPLACE_EXISTING);
            return output;
        } catch (IOException e) { throw new IllegalArgumentException("普通报告生成失败：" + e.getMessage(), e); }
        finally { if (temp != null) try { Files.deleteIfExists(temp); } catch (IOException ignored) { } }
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
