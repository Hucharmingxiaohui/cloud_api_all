package com.dji.sample.df.kmzDf.entity.wayline.Folder.Placemark.Point;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Point {
    @JsonProperty("coordinates")//经纬度坐标
    private String coordinates;
}
