package com.df.server.patrol.control.sendDto;

import lombok.Data;

/**
 * 请求功能参数
 */

@Data
public class RobotParamsConstant {
    /**
     * 机器人运动：前进
     */
    public static final String GO_QIANJIN = "w";
    /**
     * 机器人运动：后退
     */
    public static final String GO_HOUTUI = "s";
    /**
     * 机器人运动：左转
     */
    public static final String GO_ZUOZHUAN = "q";
    /**
     * 机器人运动：右转
     */
    public static final String GO_YOUZHUAN = "e";
    /**
     * 机器人运动：左移
     */
    public static final String GO_ZUOYI = "a";
    /**
     * 机器人运动：右移
     */
    public static final String GO_YOUYI = "d";
    /**
     * 机器人运动：站立
     */
    public static final String GO_ZHANLI = "f";
    /**
     * 机器人运动：趴下
     */
    public static final String GO_PAXIA = "g";
    /**
     * 机器人运动：行走步态
     */
    public static final String GO_BUTAI_XINGZOU = "1";
    /**
     * 机器人运动：小跑步态
     */
    public static final String GO_BUTAI_XIAOPAO = "2";
    /**
     * 机器人运动：上楼步态
     */
    public static final String GO_BUTAI_SHANGLOU = "3";
    /**
     * 机器人运动：下楼步态
     */
    public static final String GO_BUTAI_XIALOU = "4";
    /**
     * 机器人云台：上仰
     */
    public static final String CAMERA_SHANGYANG = "w";
    /**
     * 机器人云台：下俯
     */
    public static final String CAMERA_XIAFU = "s";
    /**
     * 机器人云台：左转
     */
    public static final String CAMERA_ZUOZHUAN = "a";
    /**
     * 机器人云台：右转
     */
    public static final String CAMERA_YOUZHUAN = "d";
    /**
     * 机器人云台：焦距增加
     */
    public static final String CAMERA_JIAOJV_ZNEGJIA = "z";
    /**
     * 机器人云台：焦距减少
     */
    public static final String CAMERA_JIAOJV_JIANSHAO = "c";
    /**
     * 机器人云台：预置位保存
     */
    public static final String CAMERA_YUZHIWEIBAOCUN = "p";
    /**
     * 任务控制：任务暂停
     */
    public static final String TASK_PAUSE = "s";
    /**
     * 任务控制：任务启动
     */
    public static final String TASK_START = "a";
    /**
     * 任务控制：任务恢复
     */
    public static final String TASK_RECOVER = "d";
    /**
     * 任务控制：任务停止
     */
    public static final String TASK_STOP = "r";
    /**
     * 任务控制：任务点位更新
     */
    public static final String TASK_RELOAD = "reload";
    /**
     * 扫图建点：开始建图
     */
    public static final String COMMAND_START_JIANTU = "w";
    /**
     * 扫图建点：结束建图
     */
    public static final String COMMAND_STOP_JIANTU = "e";
    /**
     * 扫图建点：定位初始化
     */
    public static final String COMMAND_CHUSHIHUA = "z";
    /**
     * 扫图建点：新建导航点
     */
    public static final String COMMAND_XINJIAN_DAOHANGDIAN = "x";
    /**
     * 扫图建点：保存点信息
     */
    public static final String COMMAND_BAOCUN_DAINWEI = "c";
    /**
     * 扫图建点：关闭所有节点
     */
    public static final String COMMAND_CLOSE_POINT = "q";

}
