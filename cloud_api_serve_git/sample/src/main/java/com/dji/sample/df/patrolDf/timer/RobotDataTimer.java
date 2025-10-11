package com.dji.sample.df.patrolDf.timer;

import com.df.framework.config.VTaskConfig;
import com.df.server.patrol.control.RobotAPIService;
import com.df.server.service.uni.UniRobotMappathService;
import com.df.server.service.uni.UniRobotService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class RobotDataTimer {
    @Autowired
    private VTaskConfig vTaskConfig;
    @Autowired
    private UniRobotMappathService uniRobotMappathService;
    @Autowired
    private UniRobotService uniRobotService;
    @Autowired
    private RobotAPIService robotAPIService;


    /**
     * 巡视路线同步
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000L)
    public void getDogMapCoordinatePixel() {
        if (!vTaskConfig.isEnabled()) {
            return;
        }
        uniRobotMappathService.syncRobotMapCoordinatePixel();
    }

    /**
     * 检查机器人是否在线
     */
    @Scheduled(fixedDelay = 1000)
    public void fixedDelayJob() {
        String goApi = robotAPIService.getGoApi();
        String ipv4Pattern = "\\b(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\b";
        Pattern pattern = Pattern.compile(ipv4Pattern);
        Matcher matcher = pattern.matcher(goApi);
        String ip = null;
        boolean isOnline = false;
        // 首先检查是否找到匹配项
        if (matcher.find()) {
            ip = matcher.group();
            if (StringUtils.isNotBlank(ip)) {
                // 超时应该在3秒以上
                int timeOut = 3 * 1000;
                try {
                    isOnline = InetAddress.getByName(ip).isReachable(timeOut);
                } catch (IOException e) {
                    // 处理异常，例如记录日志
                    log.error("无法连接到IP地址: {}，{}", ip, e.getMessage());
                }
            }
        }
        // 使用ip和isOnline变量
        /*if (ip != null) {
            log.info("机器人IP地址： {}，在线：{}", ip, isOnline);
        }*/
        int commState = isOnline ? 0 : 1;
        int online = isOnline ? 1 : 0;
        uniRobotService.updateRobotOnlineState(commState, online);
    }
}
