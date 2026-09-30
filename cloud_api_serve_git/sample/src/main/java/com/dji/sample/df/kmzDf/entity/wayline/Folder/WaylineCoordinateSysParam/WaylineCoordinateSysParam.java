package com.dji.sample.df.kmzDf.entity.wayline.Folder.WaylineCoordinateSysParam;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class WaylineCoordinateSysParam {
    @JsonProperty("coordinateMode")//坐标系类型
    private String coordinateMode="WGS84";
    @JsonProperty("heightMode")//高度模式：相对起飞点高度（实际执行语义，已实飞验证；如需椭球高改 WGS84 并同步前端标签）
    private String heightMode="relativeToStartPoint";
    @JsonProperty("positioningType")//信号源
    private String positioningType="GPS";
}
