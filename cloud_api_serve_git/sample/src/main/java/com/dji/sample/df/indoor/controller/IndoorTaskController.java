package com.dji.sample.df.indoor.controller;

import com.alibaba.fastjson.JSONObject;
import com.df.framework.utils.HttpUtils;
import com.df.framework.vo.Result;
import com.dji.sample.df.indoor.config.IndoorUavConfig;
import com.dji.sample.df.indoor.model.dto.MissionStartResponse;
import com.dji.sample.df.indoor.model.dto.MissionStatusResponse;
import com.dji.sample.df.indoor.model.dto.MissionTaskRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/*
室内飞行任务控制类
 */

@RestController
@RequestMapping("/indoorTask")
@RequiredArgsConstructor
public class IndoorTaskController {

    @Autowired
    private HttpUtils httpUtils;
    @Autowired
    private IndoorUavConfig indoorUavConfig;
    @Autowired
    private ObjectMapper objectMapper;

    // 发送任务点位
    @PostMapping("/start")
    public Result start(@RequestBody MissionTaskRequest request) throws JsonProcessingException {
        String jsonString = JSONObject.toJSONString(request);
        String response = httpUtils.sendPostJson(indoorUavConfig.getStartTask(), jsonString);
        MissionStartResponse missionResponse = objectMapper.readValue(response, MissionStartResponse.class);
        return Result.success(missionResponse);
    }

    // 查询任务状态
    @GetMapping("/status")
    public Result status(@RequestParam String taskId) throws JsonProcessingException {
        Map<String, Object> pathParams = new HashMap<>();
        pathParams.put("taskId", taskId);
        String response = httpUtils.sendGetWithPathParams(indoorUavConfig.getTaskStatus(), pathParams, null);
        MissionStatusResponse missionResponse = objectMapper.readValue(response, MissionStatusResponse.class);
        return Result.success(missionResponse);
    }


}
