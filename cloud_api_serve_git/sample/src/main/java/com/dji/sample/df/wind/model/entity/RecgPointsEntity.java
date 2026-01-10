package com.dji.sample.df.wind.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.Id;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("recg_points")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecgPointsEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Id
    @TableId(type = IdType.INPUT)
    private Integer id;

    /**
     * 唯一编码
     */
    @TableField("request_id")
    private String requestId;

    /**
     * 任务执行ID，格式为：任务编码_任务执行开始时间
     */
    @TableField("task_patrolled_id")
    private String taskPatrolledId;

    /**
     * 任务计划编码
     */
    @TableField("plan_no")
    private String planNo;

    /**
     * 点位编码
     */
    @TableField("point_code")
    private String pointCode;

    /**
     * 点位名称
     */
    @TableField("point_name")
    private String pointName;

    /**
     * 任务总编码
     */
    @TableField("task_code")
    private String taskCode;

    /**
     * 任务名称
     */
    @TableField("task_name")
    private String taskName;

    /**
     * 变电站编码
     */
    @TableField("sub_code")
    private String subCode;

    /**
     * 巡视设备编码
     */
    @TableField("patroldevice_code")
    private String patroldeviceCode;

    /**
     * 巡视设备名称
     */
    @TableField("patroldevice_name")
    private String patroldeviceName;

    /**
     * 实物ID
     */
    @TableField("obj_id")
    private String objId;

    /**
     * 巡视时间
     */
    @TableField("run_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date runTime;

    /**
     * 数据来源：1摄像机 2机器人 3无人机 4声纹 5在线监测
     */
    @TableField("data_type")
    private Integer dataType;

    /**
     * 识别结果值
     */
    @TableField("point_val")
    private String pointVal;

    /**
     * 识别结果值带单位(识别结果描述)
     */
    @TableField("point_val_unit")
    private String pointValUnit;

    /**
     * 识别结果单位
     */
    @TableField("point_unit")
    private String pointUnit;

    /**
     * 值类型 0-默认值类型 11-局放放电频次 12-局放信号峰值 13-局放信号均值
     */
    @TableField("value_type")
    private Integer valueType;

    /**
     * 识别类型，详见字典表类型recognition_type。
     * 1表计读取
     * 2位置状态识别
     * 3设备外观查看
     * 4红外测温
     * 5声音检测
     * 6闪烁检测
     */
    @TableField("recognition_type")
    private Integer recognitionType;

    /**
     * 数据结果：0 失败(抓拍超时、分析超时、分析失败、没有结果) 1正常 2异常（点位分析判别异常、阈值告警）
     */
    @TableField("valid")
    private Integer valid;

    /**
     * 是否报警 0 否 1 是
     */
    @TableField("is_alarm")
    private Integer isAlarm;

    /**
     * 人工处理：0待确认 1识别正常 2识别异常
     */
    @TableField("manual_hand")
    private Integer manualHand;

    /**
     * 人工修正结果
     */
    @TableField("set_val")
    private String setVal;

    /**
     * 点位是否巡视完成，视频任务判断用  0否 1是
     */
    @TableField("is_finished")
    private Integer isFinished;

    /**
     * 审核人姓名
     */
    @TableField("confirm_user")
    private String confirmUser;

    /**
     * 人工识别异常描述
     */
    @TableField("confirm_result")
    private String confirmResult;

    /**
     * 审核时间
     */
    @TableField("confirm_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date confirmTime;

    /**
     * 点位关联的预置位号
     */
    @TableField("preset_no")
    private Integer presetNo;

    /**
     * 结束时间
     */
    @TableField("finished_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date finishedTime;

    /**
     * 抓拍时间
     */
    @TableField("capture_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date captureTime;
}
