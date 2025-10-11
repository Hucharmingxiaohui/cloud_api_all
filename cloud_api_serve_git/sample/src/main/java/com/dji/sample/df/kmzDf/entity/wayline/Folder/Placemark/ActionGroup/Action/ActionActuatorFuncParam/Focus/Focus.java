package com.dji.sample.df.kmzDf.entity.wayline.Folder.Placemark.ActionGroup.Action.ActionActuatorFuncParam.Focus;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
//对焦
@Data
public class Focus {
    @JsonProperty("payloadPositionIndex")
    private int payloadPositionIndex;//负载挂载位置
    @JsonProperty("isPointFocus")
    private int isPointFocus;//是否点对焦
    @JsonProperty("focusX")
    private int focusX;//对焦点位置
    @JsonProperty("focusY")
    private int focusY;//对焦点位置
    @JsonProperty("focusRegionWidth")
    private float focusRegionWidth;//对焦区域宽度比
    @JsonProperty("focusRegionHeight")
    private float focusRegionHeight;//对焦区域高度比
    @JsonProperty("isInfiniteFocus")
    private int isInfiniteFocus;//是否无穷远对焦
}
