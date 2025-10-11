package com.dji.sample.df.thirdKmzDf.entity.wayline.Folder.Placemark.ActionGroup.Action.ActionActuatorFuncParam.StartRecord;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class StartRecord {
    @JsonProperty("payloadPositionIndex")
    private int payloadPositionIndex=0;//负载挂载位置
    @JsonProperty("fileSuffix")
    private String fileSuffix="航点";//字符串后缀
    @JsonProperty("payloadLensIndex")
    private String payloadLensIndex="ir,zoom,wide";//视频存储类型
    @JsonProperty("useGlobalPayloadLensIndex")
    private int useGlobalPayloadLensIndex=1;
}
