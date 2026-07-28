package com.dji.sample.df.cqDockDf.controller;

import com.alibaba.fastjson.JSONObject;
import com.dji.sample.component.AuthInterceptor;
import com.dji.sample.df.cqDockDf.model.dto.CqAssignTaskRequest;
import com.dji.sample.df.cqDockDf.model.dto.CqTaskIdRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * 本地模拟下级无人机平台接口，用于无外部环境时验证调用链路。
 */
@Slf4j
@RestController
@AuthInterceptor.IgnoreAuth
@RequestMapping("/machineNest/noauth/third")
public class CqDockMockController {

    @PostMapping("/task/assign_task")
    public JSONObject assignTask(@RequestBody CqAssignTaskRequest request,
                                 @RequestHeader(value = "access_key", required = false) String accessKey,
                                 @RequestHeader(value = "access_secret", required = false) String accessSecret) {
        log.info("[cq-dock-mock] assign_task headers access_key={}, access_secret={}", accessKey, mask(accessSecret));
        log.info("[cq-dock-mock] assign_task body={}", request);

        JSONObject data = new JSONObject();
        data.put("businessId", request.getBusinessId());
        data.put("taskId", "mock-task-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        return success(data, "任务下发成功(mock)");
    }

    @PostMapping("/picture/list")
    public JSONObject pictureList(@RequestBody CqTaskIdRequest request,
                                  @RequestHeader(value = "access_key", required = false) String accessKey,
                                  @RequestHeader(value = "access_secret", required = false) String accessSecret) {
        log.info("[cq-dock-mock] picture/list headers access_key={}, access_secret={}", accessKey, mask(accessSecret));
        log.info("[cq-dock-mock] picture/list body={}", request);

        JSONObject picture1 = new JSONObject();
        picture1.put("id", "mock-pic-001");
        picture1.put("pictureUrl", "http://127.0.0.1:6789/machineNest/noauth/third/mock/picture/DJI_0001.jpg");
        picture1.put("pictureName", "DJI_0001.jpg");

        JSONObject picture2 = new JSONObject();
        picture2.put("id", "mock-pic-002");
        picture2.put("pictureUrl", "http://127.0.0.1:6789/machineNest/noauth/third/mock/picture/DJI_0002.jpg");
        picture2.put("pictureName", "DJI_0002.jpg");

        List<JSONObject> pictureList = new ArrayList<>();
        pictureList.add(picture1);
        pictureList.add(picture2);

        JSONObject data = new JSONObject();
        data.put("taskId", request.getTaskId());
        data.put("pictureList", pictureList);
        return success(data, "图片列表获取成功(mock)");
    }

    @GetMapping(value = "/mock/picture/{fileName:.+}", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> mockPicture(@PathVariable String fileName) {
        log.info("[cq-dock-mock] mock picture fileName={}", fileName);
        byte[] image = Base64.getDecoder().decode(MOCK_JPEG_BASE64);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .contentType(MediaType.IMAGE_JPEG)
                .body(image);
    }

    @PostMapping("/task/status")
    public JSONObject taskStatus(@RequestBody CqTaskIdRequest request,
                                 @RequestHeader(value = "access_key", required = false) String accessKey,
                                 @RequestHeader(value = "access_secret", required = false) String accessSecret) {
        log.info("[cq-dock-mock] task/status headers access_key={}, access_secret={}", accessKey, mask(accessSecret));
        log.info("[cq-dock-mock] task/status body={}", request);

        JSONObject data = new JSONObject();
        data.put("taskId", request.getTaskId());
        data.put("status", "1");
        return success(data, "任务状态查询成功(mock)");
    }

    private JSONObject success(JSONObject data, String msg) {
        JSONObject result = new JSONObject();
        result.put("code", 200);
        result.put("data", data);
        result.put("msg", msg);
        result.put("success", true);
        return result;
    }

    private String mask(String value) {
        if (value == null || value.length() <= 4) {
            return value;
        }
        return value.substring(0, 2) + "***" + value.substring(value.length() - 2);
    }

    private static final String MOCK_JPEG_BASE64 =
            "/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAP//////////////////////////////////////////////////////////////////////////////////////2wBDAf//////////////////////////////////////////////////////////////////////////////////////wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAX/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIQAxAAAAH/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/9oACAEBAAEFAqf/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oACAEDAQE/Aaf/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oACAECAQE/Aaf/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/9oACAEBAAY/Aqf/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/9oACAEBAAE/ISf/2gAMAwEAAgADAAAAEP/EABQRAQAAAAAAAAAAAAAAAAAAABD/2gAIAQMBAT8QH//EABQRAQAAAAAAAAAAAAAAAAAAABD/2gAIAQIBAT8QH//EABQQAQAAAAAAAAAAAAAAAAAAABD/2gAIAQEAAT8QH//Z";
}
