package com.dji.sample.df.substationDf.controller;

import com.df.framework.vo.Result;
import com.df.server.utils.ExcelUtil;
import com.dji.sample.df.substationDf.model.entity.UniPointImportExcel2;
import com.dji.sample.df.substationDf.service.ImportPointService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/point")
@Slf4j
public class ImportPointController {


    @Value("${importPoint.modelfilePath}")
    String modelfilePath;

    // 点位编码前缀，从配置文件读取
    @Value("${importPoint.pointCodePrefix}")
    String pointCodePrefix;

    @Autowired
    ImportPointService importPointService;


    @PostMapping("/import")
    public ResponseEntity<String> importPoint(@RequestPart("file") MultipartFile file ,
                                              HttpServletResponse response) throws Exception {

        // 1. 验证文件是否为空
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("请选择要上传的文件");
        }

        // 2. 验证文件类型
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null ||
                (!originalFilename.toLowerCase().endsWith(".xlsx") &&
                        !originalFilename.toLowerCase().endsWith(".xls"))) {
            return ResponseEntity.badRequest().body("只支持.xlsx或.xls格式的Excel文件");
        }

        // 3. 记录上传信息
        log.info("开始导入点位数据，文件名: {}, 文件大小: {} bytes",
                originalFilename, file.getSize());

        try {
            // 4. 读取Excel文件
            ExcelUtil<UniPointImportExcel2> util = new ExcelUtil<>(UniPointImportExcel2.class);
            List<UniPointImportExcel2> points;

            try (InputStream inputStream = file.getInputStream()) {
                points = util.importExcel(inputStream);
            }

            if (points == null || points.isEmpty()) {
                return ResponseEntity.ok("Excel文件中没有数据");
            }

            // 5. 处理导入数据
            int successCount = 0;
            int failCount = 0;
            UniPointImportExcel2 point = new  UniPointImportExcel2();
            for (int i=0;i<points.size();i++) {
                try {
                    // 针对填写的每行点位数据新增点位信息
                    point = points.get(i);
                    UUID uuid = UUID.randomUUID();
                    // 生成8位数字
                    String format = String.format("%08d", Math.abs(uuid.getLeastSignificantBits() % 100000000L));
                    point.setPointCode(pointCodePrefix+format);
                    importPointService.importPoint(point);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    log.error("[配置管理-点位标准库-导入点位文件]同步过程中，出现异常，填写的数据:{}，异常:{}",
                            point, e.getMessage());
                }
            }

            // 6. 返回导入结果
            String result = String.format("导入完成！成功: %d 条，失败: %d 条", successCount, failCount);
            log.info("点位数据导入完成: {}", result);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("导入点位数据失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("导入失败: " + e.getMessage());
        }
    }
//  导出模版
    @GetMapping("/export")
    public ResponseEntity<Resource> exportPoint() throws Exception {
        // 验证文件路径和扩展名
        if (!modelfilePath.toLowerCase().endsWith(".xlsx") && !modelfilePath.toLowerCase().endsWith(".xls")) {
            throw new IllegalArgumentException("只支持导出Excel文件(.xlsx/.xls)");
        }

        Path path = Paths.get(modelfilePath);
        File file = path.toFile();

        if (!file.exists()) {
            throw new RuntimeException("Excel文件不存在: " + modelfilePath);
        }

        Resource resource = new FileSystemResource(file);
        String fileName = file.getName();

        // 编码文件名
        String encodedFileName = java.net.URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");

        // 设置Content-Disposition
        String contentDisposition = String.format(
                "attachment; filename=\"%s\"; filename*=UTF-8''%s",
                encodedFileName, encodedFileName
        );

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(getContentType(fileName)))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .contentLength(file.length())
                .body(resource);
    }

    /** 使用原始导入模板导出完整点位数据，保留模板的首行格式和列结构。 */
    @GetMapping("/exportData")
    public ResponseEntity<Resource> exportPointData(@RequestParam Map<String, Object> params) throws Exception {
        File file = Paths.get(modelfilePath).toFile();
        if (!file.exists()) {
            throw new RuntimeException("Excel文件不存在: " + modelfilePath);
        }
        if (!modelfilePath.toLowerCase().endsWith(".xlsx")) {
            throw new IllegalArgumentException("带数据导出只支持xlsx模板");
        }

        Map<String, Object> query = new HashMap<>(params);
        query.put("page", 1);
        query.put("pageSize", 100000);
        Map<String, Object> result = importPointService.selectList(query);
        List<com.dji.sample.center.entity.UniPoint> points =
                (List<com.dji.sample.center.entity.UniPoint>) result.get("list");

        try (Workbook workbook = new XSSFWorkbook(file);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.getSheetAt(0);
            Row templateRow = sheet.getLastRowNum() >= 1 ? sheet.getRow(1) : null;
            CellStyle[] styles = new CellStyle[sheet.getRow(0).getLastCellNum()];
            for (int i = 0; i < styles.length; i++) {
                Cell sourceCell = templateRow == null ? null : templateRow.getCell(i);
                styles[i] = sourceCell == null ? null : sourceCell.getCellStyle();
            }
            short rowHeight = templateRow == null ? -1 : templateRow.getHeight();
            for (int rowIndex = sheet.getLastRowNum(); rowIndex >= 1; rowIndex--) {
                Row row = sheet.getRow(rowIndex);
                if (row != null) sheet.removeRow(row);
            }
            for (int i = 0; i < points.size(); i++) {
                Row row = sheet.createRow(i + 1);
                if (rowHeight > 0) row.setHeight(rowHeight);
                writePointRow(row, points.get(i), styles);
            }
            sheet.setAutoFilter(new CellRangeAddress(0, Math.max(0, points.size()), 0, styles.length - 1));
            workbook.write(output);
            String fileName = "点位数据.xlsx";
            String encoded = java.net.URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
            Resource resource = new org.springframework.core.io.ByteArrayResource(output.toByteArray());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded)
                    .contentLength(output.size())
                    .body(resource);
        }
    }

    private void writePointRow(Row row, com.dji.sample.center.entity.UniPoint point, CellStyle[] styles) {
        Object[] values = {
                "", point.getSubCode(), point.getAreaName(), point.getBayName(), point.getDeviceName(), point.getDeviceType(),
                point.getComponentName(), point.getPointCode(), point.getPointName(), "uav",
                point.getMeterType(), point.getAppearanceType(), point.getSaveTypeList(), point.getRecognitionTypeList(),
                point.getPhase(), point.getLevel(), point.getPointDes(), point.getMapPos(), point.getUpperValue(), point.getLowerValue(),
                point.getTaskType(), point.getTaskSubType(), point.getIsObj(), point.getWaylineId(), point.getPicType(),
                point.getWaylinePointPos(), point.getPointAnalyseCategory(), point.getPointAnalyseType()
        };
        for (int i = 0; i < values.length; i++) {
            Cell cell = row.createCell(i);
            if (i < styles.length && styles[i] != null) cell.setCellStyle(styles[i]);
            Object value = values[i];
            if (value instanceof Number) cell.setCellValue(((Number) value).doubleValue());
            else cell.setCellValue(value == null ? "" : String.valueOf(value));
        }
    }

        private String getContentType(String fileName) {
            if (fileName.toLowerCase().endsWith(".xlsx")) {
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            } else if (fileName.toLowerCase().endsWith(".xls")) {
                return "application/vnd.ms-excel";
            } else {
                return "application/octet-stream";
            }
        }

    /**
     * 查询点表列表
     */
    @GetMapping("selectList")
    public Result<Map> selectList(@RequestParam Map <String, Object> map) {
        Map<String, Object> stringObjectMap = importPointService.selectList(map);
        return Result.success(stringObjectMap);
    }

    /** 查询点位层级树：变电站-区域-间隔-设备-部件-点位。 */
    @GetMapping("tree")
    public Result<List<Map<String, Object>>> selectTree() {
        return Result.success(importPointService.selectTree());
    }

    @PostMapping("batchDelete")
    public Result<String> batchDelete(@RequestBody Map<String, Object> params) {
        try {
            // 从参数中获取ID列表
            List<Integer> ids = (List<Integer>) params.get("ids");

            if (ids == null || ids.isEmpty()) {
                return Result.error("请选择要删除的数据");
            }
            // 调用服务层进行批量删除
            int deletedCount = importPointService.batchDelete(ids);

            return Result.success("成功删除 " + deletedCount + " 条数据");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("删除失败: " + e.getMessage());
        }
    }

}
