package com.df.server.vo.his;

import cn.afterturn.easypoi.entity.ImageEntity;
import com.deepoove.poi.data.HyperlinkTextRenderData;
import com.deepoove.poi.data.PictureRenderData;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 巡视报告bean
 *
 * @author 姜学云
 * @Time 2022/3/16 9:11
 */
@Data
public class TaskReportNewVO implements Serializable {
    private static final long serialVersionUID = -5003579783624370329L;
    //变电站名称
    String subName;
    //电压等级
    String level;
    //巡视日期
    String day;
    //变电站类别
    String subType;
    //巡视任务名称
    String planName;
    //环境信息: 气温5℃，气压101 Kpa，风速2m/s
    String envInfo;
    //审核人
    String confirmUser;
    //审核时间
    String confirmTime;
    //巡视开始时间
    String startTime;
    //巡视结束时间
    String endTime;
    //巡视统计
    String description;
    //巡视结果
    String result;

    //总点位数
    int cntAll;
    //已检点位数
    int cntFinished;
    //未检点位数
    int cntUnFinished;
    //异常点位数
    int cntAbnormal;
    //正常点位数
    int cntNormal;
    //待人工确认点位数
    int cntUnconfirm;

    //异常点位汇总
    List<PointDTO> abnormalPoints = new ArrayList<>();
    //待人工确认点位汇总
    List<PointDTO> unconfirmPoints = new ArrayList<>();
    //正常点位汇总
    List<PointDTO> normalPoints = new ArrayList<>();


    /**
     * 点位信息
     */
    @Data
    public static class PointDTO {
        Integer no;
        //区域名称
        String areaName;
        //间隔名称
        String bayName;
        //设备名称
        String deviceName;
        //部件名称
        String componentName;
        //点位名称
        String pointName;
        //数据来源
        String source;
        //采集信息
        String time;
        //巡视结果
        String result;
        //人工修正结果
        String setVal;
        //状态
        String status;
        //重要等级
        String level;
        //图片
        PictureRenderData image;
        PictureRenderData imageOtherPress;
        //图片名称
        String picName;
        //图片url地址
        HyperlinkTextRenderData picUrl;
        HyperlinkTextRenderData picOtherPress;
        ImageEntity excelPicUrl;
    }
}
