package com.dji.sample.df.kmzDf.entity.wayline.Folder.Placemark.ActionGroup.Action.ActionActuatorFuncParam.RecordPointCloud;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RecordPointCloud {
    @JsonProperty("payloadPositionIndex")
    private int payloadPositionIndex;
    @JsonProperty("recordPointCloudOperate")
    private String recordPointCloudOperate;
}
