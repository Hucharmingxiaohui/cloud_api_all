package com.dji.sample.center.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.df.framework.annotation.Excel;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
@TableName("df_uni_point")
public class UniPoint {
    /**
     * df_uni_point主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 点位id，对应标准规范的device_id
     */
    @TableField("point_code")
    private String pointCode;

    /**
     * 点位名称，对应标准规范device_name
     */
    @TableField("point_name")
    private String pointName;

    /**
     * 点位类型 2机器人点位 3视频点位 4都有
     */
    @TableField("point_type")
    private Integer pointType;

    /**
     * 厂站编号，关联df_substation_node的sub_code
     */
    @TableField("sub_code")
    private String subCode;

    /**
     * 厂站名称，关联df_substation_node的sub_name
     */
    @TableField("sub_name")
    private String subName;

    /**
     * 系统编码
     */
    @TableField("sys_code")
    private String sysCode;

    /**
     * 部件 ID，关联部件表的component_id
     */
    @TableField("component_id")
    private String componentId;

    /**
     * 部件名称
     */
    @TableField("component_name")
    private String componentName;

    /**
     * 实物 ID
     */
    @TableField("material_id")
    private String materialId;

    /**
     * 间隔 ID,关联间隔表的bay_id
     */
    @TableField("bay_id")
    private String bayId;

    /**
     * 间隔名称
     */
    @TableField("bay_name")
    private String bayName;

    /**
     * 主设备 ID,关联设备表device_id
     */
    @TableField("device_id")
    private String deviceId;

    /**
     * 主设备名称
     */
    @TableField("device_name")
    private String deviceName;

    /**
     * 主设备类型
     * <1>: = 油浸式变压器(电抗器)
     * <2>: = 断路器
     * <3>: = 组合电器
     * <4>: = 隔离开关
     * <5>: = 开关柜
     * <6>: = 电流互感器
     * <7>: = 电压互感器
     * <8>: = 避雷器
     * <9>: = 并联电容器组
     * <10>: = 干式电抗器
     * <11>: = 串联补偿装置
     * <12>: = 母线及绝缘子
     * <13>: = 穿墙套管
     * <14>: = 消弧线圈
     * <15>: = 高频阻波器
     * <16>: = 耦合电容器
     * <17>: = 高压熔断器
     * <18>: = 中性点隔直(限直)装置
     * <19>: = 接地装置
     * <20>: = 端子箱及检修电源箱
     * <21>: = 站用变压器
     * <22>: = 站用交流电源系统
     * <23>: = 站用直流电源系统
     * <24>: = 设备构架
     * <25>: = 辅助设施
     * <26>: = 土建设施
     * <27>: = 独立避雷针
     * <28>: = 避雷器动作次数表
     */
    @TableField("device_type")
    private Integer deviceType;

    /**
     * 表计类型
     * <1>: = 油位表
     * <2>: = 避雷器动作次数表
     * <3>: = 泄漏电流表
     * <4>: = SF6 压力表
     * <5>: = 液压表
     * <6>: = 开关动作次数表
     * <7>: = 油温表
     * <8>: = 档位表
     * <9>: = 气压表
     */
    @TableField("meter_type")
    private Integer meterType;

    /**
     * 外观类型
     * <1>: = 电子围栏
     * <2>: = 红外对射
     * <3>: = 泡沫喷淋
     * <4>: = 消防水泵
     * <5>: = 消防栓
     * <6>: = 消防室
     * <7>: = 设备室
     * <8>: = 照明灯
     * <9>: = 摄像头
     * <10>: = 水位线
     * <11>: = 排水泵
     * <12>: = 沉降监测点
     */
    @TableField("appearance_type")
    private Integer appearanceType;

    /**
     * 采集/保存文件的类型（数据格式），多个采集文件类型用" , "分隔
     * <1>:= 红外图谱
     * <2>:= 可见光照片
     * <3>:= 音频
     * <4>:= 数值结果
     */
    @TableField("save_type_list")
    private String saveTypeList;

    /**
     * 识别类型列表,多个识别类型用" , "分隔
     * <1>: = 表计读取
     * <2>: = 位置状态识别
     * <3>: = 设备外观查看
     * <4>: = 红外测温
     * <5>: = 声音检测
     * <6>: = 闪烁检测
     * 其他类型备用，如：1，2，3
     */
    @TableField("recognition_type_list")
    private String recognitionTypeList;

    /**
     * 相位
     * <1>: = A相
     * <2>: = B相
     * <3>: = C相
     */
    @TableField("phase")
    private String phase;

    /**
     * 点位描述
     */
    @TableField("point_des")
    private String pointDes;

    /**
     * df_uni_mapfile表主键
     */
    @TableField("mapfile_id")
    private Long mapfileId;

    /**
     * 在地图上的坐标，格式：（x，y，z）
     */
    @TableField("map_pos")
    private String mapPos;

    /**
     * 正常范围下限
     */
    @TableField("lower_value")
    private String lowerValue;

    /**
     * 正常范围上限
     */
    @TableField("upper_value")
    private String upperValue;

    /**
     * 是否加入白名单 0否 1是
     */
    @TableField("is_whitelist")
    private Integer isWhitelist;

    /**
     * 点位关注 0未关注 1关注
     */
    @TableField("isfocus")
    private Integer isFocus;

    /**
     * 巡视任务类型,对应字典表task_type
     */
    @TableField("task_type")
    private String taskType;

    /**
     * 巡视任务子类型,字典表task_sub_type_3，task_sub_type_4，task_sub_type_5
     */
    @TableField("task_sub_type")
    private String taskSubType;

    /**
     * 是否实物识别，0否1是
     */
    @TableField("is_obj")
    private Integer isObj;

    /**
     * 点位级别：I、II、III类
     */
    @TableField("level")
    private Integer level;

    /**
     * 区域id
     */
    @TableField("area_id")
    private String areaId;

    /**
     * 智能分析识别大类：1设备状态类识别 2 缺陷类识别 3 判别类型
     */
    @TableField("point_analyse_category")
    private Integer pointAnalyseCategory;

    /**
     * 智能分析子类，多个用英文逗号分割
     */
    @TableField("point_analyse_type")
    private String pointAnalyseType;

    /**
     * 判别基准图新的字段名，当配置为判别类型时需要有值（之前是：image_normal_url_path）
     */
    @TableField("base_image_path")
    private String baseImagePath;

    /**
     * 是1否0 有非同源冗余，按照规范只要是I类点位都是有，II类自己定
     */
    @TableField("is_redundancy")
    private Integer isRedundancy;

    /**
     * 区域名称（新加字段）
     */
    @TableField("area_name")
    private String areaName;

    /**
     * 关联视频编码及预置位
     */
    @TableField("video_pos")
    private String videoPos;

    /**
     * 点位标签属性，多个附加属性逗号分隔。目前只有1：人工关注
     */
    @TableField("label_attri")
    private String labelAttri;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("create_time")
    private Date createTime;

    /**
     * 修改时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("update_time")
    private Date updateTime;

    /**
     * 巡视方式 0智能巡视 1人工巡视
     */
    @TableField("patrol_way")
    private Integer patrolWay;

    @TableField("wayline_id")
    private String waylineId;

    @TableField("pic_type")
    private Integer picType;

    @TableField("wayline_point_pos")
    private String waylinePointPos;

    @TableField("infrared_image_coordinate")
    private String infraredImageCoordinate;
}
