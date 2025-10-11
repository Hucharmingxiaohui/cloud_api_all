package com.dji.sample.df.thirdKmzDf.entity.wayline.Folder.Placemark.ActionGroup.Action.ActionActuatorFuncParam.PanoShot;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
//全景拍照m30t
@Data
public class PanoShot {
    @JsonProperty("payloadPositionIndex")
    private int payloadPositionIndex=0;//负载挂载位置
    @JsonProperty("payloadLensIndex")
    private String payloadLensIndex="wide";//拍摄照片存储类型
    @JsonProperty("useGlobalPayloadLensIndex")
    private int useGlobalPayloadLensIndex=0;//是否使用全局存储类型
    @JsonProperty("panoShotSubMode")
    private String panoShotSubMode="panoShot_360";//全景拍照模式
}
