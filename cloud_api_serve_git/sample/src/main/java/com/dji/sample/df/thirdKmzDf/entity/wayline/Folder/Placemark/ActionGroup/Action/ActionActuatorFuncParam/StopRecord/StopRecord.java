package com.dji.sample.df.thirdKmzDf.entity.wayline.Folder.Placemark.ActionGroup.Action.ActionActuatorFuncParam.StopRecord;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class StopRecord {
    @JsonProperty("payloadPositionIndex")
    private int payloadPositionIndex=0;//负载挂载位置
    @JsonProperty("payloadLensIndex")
    private String payloadLensIndex="ir,zoom,wide";//视频存储类型

}
