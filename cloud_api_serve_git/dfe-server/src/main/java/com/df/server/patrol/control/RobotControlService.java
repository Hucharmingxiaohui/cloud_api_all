package com.df.server.patrol.control;

import com.alibaba.fastjson.JSONObject;
import com.df.framework.utils.HttpUtils;
import com.df.server.patrol.control.sendDto.RobotAPIDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 机器人控制服务
 */
@Slf4j
@Component
public class RobotControlService {

    @Autowired
    private HttpUtils httpUtils;
    @Autowired
    private RobotAPIService robotAPIService;

    public String sendCustomGo(RobotAPIDTO params) {
        String url = robotAPIService.getGoApi();
        String jsonString = JSONObject.toJSONString(params);
        log.info("发送机器人API：{} 参数：{}", url, jsonString);
        return httpUtils.sendPostJson(url, jsonString);
    }

    public String sendCustomCamera(RobotAPIDTO params) {
        String url = robotAPIService.getCameraApi();
        String jsonString = JSONObject.toJSONString(params);
        log.info("发送机器人API：{} 参数：{}", url, jsonString);
        return httpUtils.sendPostJson(url, jsonString);
    }

    public String sendCustomCommand(RobotAPIDTO params) {
        String url = robotAPIService.getCommandApi();
        String jsonString = JSONObject.toJSONString(params);
        log.info("发送机器人API：{} 参数：{}", url, jsonString);
        return httpUtils.sendPostJson(url, jsonString);
    }

    public String sendCustomReloadCommand(RobotAPIDTO params) {
        String url = robotAPIService.getCommandReloadApi();
        String jsonString = JSONObject.toJSONString(params);
        log.info("发送机器人API：{} 参数：{}", url, jsonString);
        return httpUtils.sendPostJson(url, jsonString);
    }

    /**
     * 机器人路径点位
     *
     * @param params
     * @return
     */
    public String sendCustomNode(RobotAPIDTO params) {
        String url = robotAPIService.getNodeApi();
        String jsonString = JSONObject.toJSONString(params);
        log.info("发送机器人API：{} 参数：{}", url, jsonString);
        return httpUtils.sendGet(url, null, "", "");
    }

    /**
     * 任务点位下发
     *
     * @param params
     * @return
     */
    public String sendTaskPointSync(RobotAPIDTO params) {
        String url = robotAPIService.getTaskPointSync();
        String jsonString = JSONObject.toJSONString(params);
        log.info("发送机器人API：{} 参数：{}", url, jsonString);
        return httpUtils.sendPostJson(url, jsonString);
    }

    public String sendRecharge(RobotAPIDTO params) {
        String url = robotAPIService.getCommandRecharge();
        String jsonString = JSONObject.toJSONString(params);
        log.info("发送机器人API：{} 参数：{}", url, jsonString);
        return httpUtils.sendPostJson(url, jsonString);
    }


}
