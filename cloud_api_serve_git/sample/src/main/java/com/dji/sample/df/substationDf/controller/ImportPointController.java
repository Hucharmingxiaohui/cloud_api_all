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

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
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
