package com.dji.sample.df.wind.controller;

import com.df.server.utils.ExcelUtil;
import com.dji.sample.df.wind.model.entity.UniPointImportExcel2;
import com.dji.sample.df.wind.service.ImportPointService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpHeaders;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/point")
@Slf4j
public class ImportPointController {


    @Value("${importPoint.modelfilePath}")
    String modelfilePath;

    @Autowired
    ImportPointService importPointService;

    @GetMapping("/import")
    public ResponseEntity<String> importPoint(@RequestParam("file") MultipartFile file,
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

            for (UniPointImportExcel2 point : points) {
                try {
                    // 针对填写的每行点位数据新增点位信息
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

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(resource);
    }

}
