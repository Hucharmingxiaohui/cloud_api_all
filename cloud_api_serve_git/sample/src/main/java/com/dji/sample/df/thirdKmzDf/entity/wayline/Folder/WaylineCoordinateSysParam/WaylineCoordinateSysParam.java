package com.dji.sample.df.thirdKmzDf.entity.wayline.Folder.WaylineCoordinateSysParam;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class WaylineCoordinateSysParam {
    @JsonProperty("coordinateMode")//坐标系类型
    private String coordinateMode="WGS84";
    @JsonProperty("heightMode")//高度模式
    private String heightMode="relativeToStartPoint";
    @JsonProperty("positioningType")//信号源
    private String positioningType="GPS";
}
