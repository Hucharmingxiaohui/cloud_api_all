package com.dji.sample.df.wind.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@RestController
@RequestMapping("/api/file")
public class DefectFileController {

    @GetMapping("/defect")
    public void getDefectImage(@RequestParam String path,
                               HttpServletResponse response) throws IOException {
        try {
            File file = new File(path);
            if (!file.exists()) {
                response.setStatus(404);
                return;
            }

            String contentType = Files.probeContentType(file.toPath());
            if (contentType == null) {
                contentType = "image/jpeg"; // 默认类型
            }

            response.setContentType(contentType);
            Files.copy(file.toPath(), response.getOutputStream());

        } catch (Exception e) {
            response.setStatus(500);
        }
    }
}
