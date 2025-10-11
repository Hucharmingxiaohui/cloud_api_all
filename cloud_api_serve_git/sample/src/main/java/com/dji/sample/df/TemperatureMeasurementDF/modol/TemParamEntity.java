package com.dji.sample.df.TemperatureMeasurementDF.modol;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TemParamEntity {
    //点测温
    @JsonProperty("point_x")//宽
    private Integer point_x;
    @JsonProperty("point_y")//高
    private Integer point_y;
    //区域测温参数
    @JsonProperty("left_top_x")//左上角
    private Integer left_top_x;
    @JsonProperty("left_top_y")
    private Integer left_top_y;
    @JsonProperty("right_bottom_x")//右下角
    private Integer right_bottom_x;
    @JsonProperty("right_bottom_y")
    private Integer right_bottom_y;
}
