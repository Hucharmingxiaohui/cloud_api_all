package com.dji.sample.df.uavCommonHandleDf.service.impl;

import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;

import java.awt.image.BufferedImage;

/** 普通巡检与EUA共用的模板布局；不查询业务数据、不分析或测温。 */
public final class PatrolXlsRenderer {
    private final HSSFWorkbook book;
    private final HSSFSheet sheet;
    private final HSSFCellStyle[] styles = new HSSFCellStyle[12];
    private final HSSFCellStyle resultStyle;
    private final HSSFPatriarch drawing;

    public PatrolXlsRenderer(HSSFWorkbook book) {
        this.book = book;
        sheet = book.getSheetAt(0);
        if (sheet.getRow(7) == null || sheet.getRow(0) == null || sheet.getRow(6) == null)
            throw new IllegalArgumentException("普通报告模板结构不符合约定的前7行表头和第8行明细");
        boolean hasPictureType = "图片类型".equals(new DataFormatter().formatCellValue(sheet.getRow(6).getCell(8)));
        HSSFCellStyle[] headerStyles = new HSSFCellStyle[12];
        int[] widths = new int[12];
        for (int c = 0; c < 12; c++) {
            int source = hasPictureType || c < 8 ? c : c == 8 ? 7 : c - 1;
            styles[c] = sheet.getRow(7).getCell(source, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getCellStyle();
            headerStyles[c] = sheet.getRow(6).getCell(source, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).getCellStyle();
            widths[c] = sheet.getColumnWidth(source);
        }
        for (int r = sheet.getLastRowNum(); r >= 7; r--) if (sheet.getRow(r) != null) sheet.removeRow(sheet.getRow(r));
        for (int i = sheet.getNumMergedRegions() - 1; i >= 0; i--) {
            CellRangeAddress region = sheet.getMergedRegion(i);
            if (region.getLastRow() >= 6) sheet.removeMergedRegion(i);
            else if (region.getLastColumn() > 11 || (region.getFirstRow() == 5 && region.getFirstColumn() == 0)) {
                sheet.removeMergedRegion(i);
                if (region.getFirstColumn() <= 11 && (region.getFirstColumn() < 11 || region.getFirstRow() < region.getLastRow()))
                    sheet.addMergedRegion(new CellRangeAddress(region.getFirstRow(), region.getLastRow(), region.getFirstColumn(), 11));
            }
        }
        for (int r = 0; r <= 6; r++) {
            HSSFRow row = sheet.getRow(r);
            if (row == null) continue;
            for (int c = row.getLastCellNum() - 1; c >= (r == 6 ? 0 : 12); c--)
                if (row.getCell(c) != null) row.removeCell(row.getCell(c));
        }
        String[] headers = {"序号", "区域", "间隔", "设备", "部件", "点位", "重要等级", "数据来源", "图片类型", "采集时间", "巡视结果", "巡视图像"};
        for (int c = 0; c < headers.length; c++) {
            HSSFCell cell = sheet.getRow(6).createCell(c);
            cell.setCellValue(headers[c]); cell.setCellStyle(headerStyles[c]);
            sheet.setColumnWidth(c, widths[c]);
        }
        sheet.setColumnWidth(7, Math.max(widths[7], 24 * 256));
        sheet.setColumnWidth(8, Math.max(widths[8], 10 * 256));
        sheet.setColumnWidth(9, Math.max(widths[9], 22 * 256));
        sheet.setColumnWidth(11, Math.max(widths[11], 24 * 256));
        sheet.setColumnWidth(10, Math.max(widths[10], 24 * 256));
        resultStyle = book.createCellStyle(); resultStyle.cloneStyleFrom(styles[10]);
        resultStyle.setWrapText(true); resultStyle.setVerticalAlignment(VerticalAlignment.TOP);
        drawing = sheet.createDrawingPatriarch();
    }

    public void row(int index, String[] values, byte[] bytes, BufferedImage image, int pictureType) {
        HSSFRow row = sheet.createRow(index); row.setHeightInPoints(160);
        for (int c = 0; c < values.length; c++) {
            HSSFCell cell = row.createCell(c); cell.setCellStyle(styles[c]); cell.setCellValue(values[c]);
        }
        row.getCell(10).setCellStyle(resultStyle);
        if (image == null) return;
        double width = sheet.getColumnWidthInPixels(11);
        double height = 160 * 96.0 / 72;
        double scale = Math.min((width - 4) / image.getWidth(), (height - 4) / image.getHeight());
        int dx = Math.max(1, Math.min(1022, (int) (image.getWidth() * scale / width * 1024)));
        int dy = Math.max(1, Math.min(254, (int) (image.getHeight() * scale / height * 256)));
        HSSFClientAnchor anchor = new HSSFClientAnchor(1, 1, dx, dy, (short) 11, index, (short) 11, index);
        anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_DONT_RESIZE);
        drawing.createPicture(anchor, book.addPicture(bytes, pictureType));
    }

    public void finish(int lastRow) {
        if (lastRow > 65535) throw new IllegalArgumentException("报告超出 XLS 行数上限");
        for (Sheet s : book) for (Row r : s) for (Cell c : r)
            if (c.getCellType() == CellType.STRING && (c.getStringCellValue().contains("{{") || c.getStringCellValue().contains("}}")))
                throw new IllegalArgumentException("模板存在未绑定占位符：" + s.getSheetName() + "!" + c.getAddress());
        sheet.getPrintSetup().setPaperSize(PrintSetup.A4_PAPERSIZE);
        sheet.getPrintSetup().setFitWidth((short) 1); sheet.getPrintSetup().setFitHeight((short) 0);
        sheet.setFitToPage(true); sheet.setAutobreaks(true);
        sheet.setRepeatingRows(new CellRangeAddress(0, 6, -1, -1));
        book.setPrintArea(0, 0, 11, 0, lastRow);
    }

    HSSFCellStyle detailStyle(int column) { return styles[column]; }

    public static void put(HSSFSheet sheet, int row, int column, String value) {
        for (CellRangeAddress region : sheet.getMergedRegions()) if (region.isInRange(row, column)) {
            row = region.getFirstRow(); column = region.getFirstColumn(); break;
        }
        sheet.getRow(row).getCell(column, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK).setCellValue(value);
    }
}
