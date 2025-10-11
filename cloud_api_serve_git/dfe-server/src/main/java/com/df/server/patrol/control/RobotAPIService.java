package com.df.server.patrol.control;

import com.df.server.service.sys.SysConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 机器人URLAPI
 */
@Component
public class RobotAPIService {
    /**
     * API：机器人运动
     */
    public static final String GO_API = "/api/uni_go2";
    /**
     * API：机器人云台\预置位保存
     */
    public static final String CAMERA_API = "/api/camera";
    /**
     * API：任务控制\扫图建点
     */
    public static final String COMMAND_API = "/api/command";
    public static final String EXIT_RECHARGE_API = "/api/exit_recharge";

    /**
     * API：机器人路径点位
     */
    public static final String NODE_API = "/api/node";
    /**
     * API：任务点位下发
     */
    public static final String SAVE_POINT_API = "/api/save_point";
    /**
     * URL：机器人运动
     */
    public static final String GO_URL_KEY = "go_url";
    /**
     * URL：机器人云台\预置位保存
     */
    public static final String CAMERA_URL_KEY = "camera_url";
    /**
     * URL：任务控制\扫图建点
     */
    public static final String COMMAND_URL_KEY = "command_url";
    public static final String COMMAND_RECHARGE_KEY = "command_url_recharge";
    public static final String COMMAND_URL_WAY = "command_url_way";

    /**
     * URL：任务更新
     */
    public static final String COMMAND_URL_RELOAD_KEY = "command_url_reload";
    @Autowired
    private SysConfigService sysConfigService;

    /**
     * 机器人运动控制
     *
     * @return
     */
    public String getGoApi() {
        return sysConfigService.getValueByKey(GO_URL_KEY) + GO_API;
    }

    /**
     * 机器人云台控制、预置位保存
     *
     * @return
     */
    public String getCameraApi() {
        return sysConfigService.getValueByKey(CAMERA_URL_KEY) + CAMERA_API;
    }

    /**
     * 任务控制、扫图建点
     *
     * @return
     */
    public String getCommandApi() {
        return sysConfigService.getValueByKey(COMMAND_URL_KEY) + COMMAND_API;
    }

    public String getCommandRecharge() {
        return sysConfigService.getValueByKey(COMMAND_RECHARGE_KEY) + EXIT_RECHARGE_API;
    }

    /**
     * 任务重载
     *
     * @return
     */
    public String getCommandReloadApi() {
        return sysConfigService.getValueByKey(COMMAND_URL_RELOAD_KEY) + COMMAND_API;
    }

    /**
     * 机器人路径点位
     *
     * @return
     */
    public String getNodeApi() {
        return sysConfigService.getValueByKey(COMMAND_URL_WAY) + NODE_API;
    }

    /**
     * 任务点位下发
     *
     * @return
     */
    public String getTaskPointSync() {
        return sysConfigService.getValueByKey(COMMAND_URL_KEY) + SAVE_POINT_API;
    }
}
