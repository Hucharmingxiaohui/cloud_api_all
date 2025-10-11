package com.df.server.dto.uniPoint;

import com.df.framework.annotation.Excel;
import lombok.Data;

/**
 * @author yuzaobo
 * @date 2021/6/10
 */
@Data
public class UniPointImportExcel {

    @Excel(name = "操作 2=删除")
    private String operate;

    @Excel(name = "变电站编码")
    private String subCode;
    @Excel(name = "巡视系统编码")
    private String sysCode;
    @Excel(name = "区域名称")
    private String areaName;
    @Excel(name = "间隔名称")
    private String bayName;
    @Excel(name = "设备名称")
    private String deviceName;
    /**
     * "1=油浸式变压器(电抗器),2=断路器,3=组合电器,4=隔离开关,5=开关柜," +
     * "6=电流互感器,7=电压互感器,8=避雷器,9=并联电容器组,10=干式电抗器," +
     * "11=串联补偿装置,12=母线及绝缘子,13=穿墙套管,14=消弧线圈,15=高频阻波器," +
     * "16=耦合电容器,17=高压熔断器,18=中性点隔直(限直)装置,19=接地装置,20=端子箱及检修电源箱," +
     * "21=站用变压器,22=站用交流电源系统,23=站用直流电源系统,24=设备构架,25=辅助设施," +
     * "26=土建设施,27=独立避雷针,28=避雷器动作次数表"
     */
    @Excel(name = "设备类型")
    private String deviceType;
    @Excel(name = "部件名称")
    private String componentName;
    /**
     * 置空时，后台以UUID填充做新增；
     * 非空时，按编码查询，有则更新，无则按填写编码新增，导入时，不判断重复，若表格中编码重复，一律更新。
     */
    @Excel(name = "点位编码(看说明)")
    private String pointCode;

    @Excel(name = "点位名称")
    private String pointName;
    /**
     * camera=摄像机点位,robot=机器人点位,uav=无人机点位,voice=声纹点位,online=在线监测点位
     */
    @Excel(name = "点位类型")
    private String pointType;
    /**
     * "1=油位表," +
     * "2=避雷器动作次数表," +
     * "3=泄漏电流表," +
     * "4=SF6 压力表," +
     * "5=液压表," +
     * "6=开关动作次数表," +
     * "7=油温表," +
     * "8=档位表," +
     * "9=气压表"
     */
    @Excel(name = "表计类型")
    private String meterType;
    /**
     * "1=电子围栏," +
     * "2=红外对射," +
     * "3=泡沫喷淋," +
     * "4=消防水泵," +
     * "5=消防栓," +
     * "6=消防室," +
     * "7=设备室," +
     * "8=照明灯," +
     * "9=摄像头," +
     * "10=水位线," +
     * "11=排水泵," +
     * "12=沉降监测点"
     */
    @Excel(name = "外观类型")
    private String appearanceType;
    /**
     * "1=红外图谱,2=可见光照片,3=音频,4=数值结果"
     */
    @Excel(name = "采集文件类型")
    private String saveTypeList;
    /**
     * "1=表计读取,2=位置状态识别,3=设备外观查看," +
     * "4=红外测温,5=声音检测,6=闪烁检测"
     */
    @Excel(name = "识别类型")
    private String recognitionTypeList;

    /**
     * "1=A相,2=B相,3=C相"
     */
    @Excel(name = "相位")
    private String phase;

    @Excel(name = "重要等级")
    private String level;

    @Excel(name = "点位描述-非必填")
    private String pointInfo;
    @Excel(name = "在地图上的坐标-非必填")
    private String mapPos;
    @Excel(name = "正常范围上限-非必填")
    private String upperValue;
    @Excel(name = "正常范围下限-非必填")
    private String lowerValue;
    /**
     * "1=自定义巡视,2=例行巡视,3=专项巡视,4=特殊巡视,5=熄灯巡视"
     */
    @Excel(name = "巡视类型")
    private String taskType;
    @Excel(name = "巡视子类型")
    private String taskSubType;
    /**
     * "0=否,1=是"
     */
    @Excel(name = "是否实物识别")
    private String isObj;
    @Excel(name = "关联机器人编码")
    private String robotCode;

    @Excel(name = "关联预置位号")
    private String robotPos;
    @Excel(name = "关联航线预置位号")
    private String waylinePos;
    @Excel(name = "关联航点预置位号")
    private String waylinePointPos;
    @Excel(name = "分析识别种类")
    private String pointAnalyseCategory;
    @Excel(name = "智能分析子类")
    private String pointAnalyseType;

    /**
     * 点位是否设置，0：未设置，1：已设置（涉及到点位覆盖率的统计）
     */
    //@Excel(name = "点位是否设置")
    //private String isSet;
    /**
     * 静默监视频次（每5分钟：5，每小时：60)
     */
    //@Excel(name = "静默监视频次")
    //private String monitorTimes;
    /**
     * 是否一键顺控点位（0：否，1：是）默认否
     */
    //@Excel(name = "是否一键顺控")
    //private String isOneKeyPoint;
    /**
     * 是否守望点位（0：否，1：是）默认否
     */
    //@Excel(name = "是否守望点位")
    //private String isKeepWatchPoint;
    /**
     * 设备类型大类(1:一次设备，2：二次设备，3：辅助设备，4：部件附件）
     */
    //@Excel(name = "设备类型大类")
    //private String deviceMainType;


    /**
     * 判别基准图路径
     */
    /*@Excel(name = "基准图片",cellType = Excel.ColumnType.IMAGE)
    private String pbBaseImage;*/

    private Integer rowNum;
    private Integer excelId;
}
