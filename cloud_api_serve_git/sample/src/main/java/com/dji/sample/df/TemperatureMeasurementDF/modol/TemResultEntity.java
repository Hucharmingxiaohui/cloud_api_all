package com.dji.sample.df.TemperatureMeasurementDF.modol;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TemResultEntity {
    @JsonProperty("point_tem")
    private Double point_tem;//点测温结果
    @JsonProperty("point_position")
    private String point_position;//点测温位置
    @JsonProperty("max_tem")
    private Double max_tem;//区域测温最大值
    @JsonProperty("left_top_position")
    private String left_top_position;//左上角位置
    @JsonProperty("min_tem")
    private Double min_tem;//区域测温最小值
    @JsonProperty("right_bottum_position")
    private String right_bottum_position;//右下角坐标
    @JsonProperty("average_tem")
    private Double average_tem;//区域测温平均温度
    @JsonProperty("error")//出现错误
    private String error ;
    @JsonProperty("range")
    private String range;//温度范围

}
