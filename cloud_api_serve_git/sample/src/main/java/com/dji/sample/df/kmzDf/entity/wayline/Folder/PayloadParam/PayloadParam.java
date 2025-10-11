package com.dji.sample.df.kmzDf.entity.wayline.Folder.PayloadParam;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PayloadParam {
    @JsonProperty("payloadPositionIndex")
    private int payloadPositionIndex=0;//负载挂载位置
    @JsonProperty("meteringMode")
    private String meteringMode="average";
    @JsonProperty("dewarpingEnable")
    private int dewarpingEnable=0;
    @JsonProperty("returnMode")
    private String returnMode="singleReturnStrongest";
    @JsonProperty("samplingRate")
    private int samplingRate=240000;
    @JsonProperty("scanningMode")
    private String scanningMode="nonRepetitive";
    @JsonProperty("modelColoringEnable")
    private int modelColoringEnable=0;
    @JsonProperty("imageFormat")
    private String imageFormat="wide,ir,zoom";//照片格式
}
