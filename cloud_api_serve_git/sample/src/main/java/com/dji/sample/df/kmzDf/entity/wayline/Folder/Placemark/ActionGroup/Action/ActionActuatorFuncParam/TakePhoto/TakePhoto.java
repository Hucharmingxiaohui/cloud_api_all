package com.dji.sample.df.kmzDf.entity.wayline.Folder.Placemark.ActionGroup.Action.ActionActuatorFuncParam.TakePhoto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
//拍照
@Data
public class TakePhoto {
    @JsonProperty("actionId")
    private int actionId;//动作序号
    @JsonProperty("payloadPositionIndex")//负载挂载位置
    private int payloadPositionIndex=0;
    @JsonProperty("fileSuffix")//拍摄照片文件后缀
    private String fileSuffix="航点";
    @JsonProperty("payloadLensIndex")//拍摄照片存储类型
    private String payloadLensIndex="wide,ir,zoom";
    @JsonProperty("useGlobalPayloadLensIndex")//是否使用全局存储类型
    private int useGlobalPayloadLensIndex=1;
}
