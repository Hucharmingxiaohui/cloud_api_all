package com.df.server.entity.his;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 巡视点位执行智能分析表
 */
@Data
public class HisExePointAnalyse {
    /**
     * 主键(分析请求ID)
     */
    private String id;

    /**
     * 任务执行ID
     */
    private String task_patrolled_id;

    /**
     * 变电站编码
     */
    private String sub_code;

    /**
     * 点位编码
     */
    private String point_code;

    /**
     * 数据来源(字典point_result_srouce)：1摄像机，2机器人，3无人机，4声纹，5在线监测
     */
    private Integer data_type;

    /**
     * 运行状态：0待执行，1执行中，2执行完毕
     */
    private Integer run_state;

    /**
     * 分析请求来源：1本地视频巡检，2 机器人/无人机巡检，3本地静默监视，4下级(边缘)巡检，5 下级(边缘)静默监视，6其他
     */
    private Integer request_source;

    /**
     * 发送分析请求时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date request_time;

    /**
     * 分析请求状态描述
     */
    private String status_desc;

    /**
     * 分析请求参数JSON
     */
    private String request_json;

    /**
     * 分析请求返回code：200成功，400客户端请求有语法错误，500服务端异常
     */
    private Integer response_code;

    /**
     * 分析返回结果JSON
     */
    private String result_json;

    /**
     * 记录创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date create_time;

    /**
     * 记录更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date update_time;
}