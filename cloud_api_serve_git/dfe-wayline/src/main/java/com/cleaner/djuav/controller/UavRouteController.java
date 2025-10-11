package com.cleaner.djuav.controller;

import com.cleaner.djuav.domain.UavRouteReq;
import com.cleaner.djuav.domain.kml.KmlInfo;
import com.cleaner.djuav.service.UavRouteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
public class UavRouteController {

    @Resource
    private UavRouteService routeService;

    @Autowired
    private ObjectMapper mapper;
    /**
     * 编辑kmz文件
     */
    @PostMapping("/updateKmz")
    public void updateKmz(@RequestBody UavRouteReq uavRouteReq) {
        this.routeService.updateKmz(uavRouteReq);
    }

    /**
     * 生成kmz文件
     */
//    @PostMapping("/buildKmz")
//    public void buildKmz(@RequestBody UavRouteReq uavRouteReq) {
//        this.routeService.buildKmz(uavRouteReq);
//    }
    @PostMapping(value = "/buildKmz", produces = "application/vnd.google-earth.kmz")
    public ResponseEntity<org.springframework.core.io.Resource> buildKmz(@RequestBody UavRouteReq uavRouteReq) {

        // 确保目录存在（若工具类不会自动建）
        new File("file/kmz").mkdirs();

        String localPath = routeService.buildKmzAndReturnPath(uavRouteReq);
        Path p = Paths.get(localPath);

        org.springframework.core.io.Resource fileResource = new FileSystemResource(p); // 这里用全限定名
        String filename = p.getFileName().toString();
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encoded)
                .contentType(MediaType.parseMediaType("application/vnd.google-earth.kmz"))
                // 不要调用 contentLength(resource.contentLength())，它会 throws IOException
                .body(fileResource);  // 泛型将被推断为 org.springframework.core.io.Resource
    }




    /**
     * 解析kmz文件
     *
     * @param fileUrl
     */
    @PostMapping("/parseKmz")
    public KmlInfo parseKmz(@RequestParam("fileUrl") String fileUrl) throws IOException {
        return this.routeService.parseKmz(fileUrl);
    }
}
