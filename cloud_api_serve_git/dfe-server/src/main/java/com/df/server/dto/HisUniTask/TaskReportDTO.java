package com.df.server.dto.HisUniTask;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 巡视报告bean
 *
 * @author 姜学云
 * @Time 2022/3/16 9:11
 */
@Data
public class TaskReportDTO {
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
    String report_confirm_status;

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

    public void addAbnormaldPoint(PointDTO pointDTO) {
        this.abnormalPoints.add(pointDTO);
    }

    public void addUnconfirmPoint(PointDTO pointDTO) {
        this.unconfirmPoints.add(pointDTO);
    }

    public void addbnormaldPoint(PointDTO pointDTO) {
        this.normalPoints.add(pointDTO);
    }

    /**
     * 点位信息
     */
    @Data
    public static class PointDTO {
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
        //点位结果
        List<PointResultDTO> results = new ArrayList<>();

        public void addResult(PointResultDTO resultDTO) {
            this.results.add(resultDTO);
        }
    }

    /**
     * 点位结果
     */
    @Data
    public static class PointResultDTO {
        //数据来源
        String source;
        //采集信息
        String time;
        //巡视结果
        String result;
        //状态
        String status;
        //图片信息
        List<PicDTO> pics = new ArrayList<>();

        public void addPic(PicDTO pic) {
            this.pics.add(pic);
        }
    }

    /**
     * 图片信息
     */
    @Data
    public static class PicDTO {
        //图片base64
        String picData;
        //图片名称
        String picName;
        //图片url地址
        String picUrl;

        public PicDTO(String picName, String picData, String picUrl) {
            this.picData = picData;
            this.picName = picName;
            this.picUrl = picUrl;
        }
    }
}
