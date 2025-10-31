package com.dji.sdk.cloudapi.control.api;

import com.dji.sdk.cloudapi.control.FileParam;
import com.dji.sdk.cloudapi.device.ExitWaylineWhenRcLostEnum;
import com.dji.sdk.cloudapi.wayline.OutOfControlActionEnum;
import com.dji.sdk.cloudapi.wayline.RthModeEnum;
import com.dji.sdk.cloudapi.wayline.WaylinePrecisionTypeEnum;
import com.dji.sdk.common.BaseModel;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

public class InFlightWaylineDeliverRequest extends BaseModel {

    @NotNull
    private FileParam file;

    @NotNull
    @Pattern(regexp = "^[^<>:\"/|?*._\\\\]+$")
    private String inFlightWaylineId;

    @NotNull
    private OutOfControlActionEnum outOfControlAction;

    @NotNull
    private ExitWaylineWhenRcLostEnum exitWaylineWhenRcLost;

    @Min(20)
    @Max(1500)
    @NotNull
    private Integer rthAltitude;

    @NotNull
    private RthModeEnum rthMode;

    @NotNull
    private WaylinePrecisionTypeEnum waylinePrecisionType;

    public InFlightWaylineDeliverRequest() {
    }

    // Getters and Setters
    public FileParam getFile() {
        return file;
    }

    public void setFile(FileParam file) {
        this.file = file;
    }

    public String getInFlightWaylineId() {
        return inFlightWaylineId;
    }

    public void setInFlightWaylineId(String inFlightWaylineId) {
        this.inFlightWaylineId = inFlightWaylineId;
    }

    public OutOfControlActionEnum getOutOfControlAction() {
        return outOfControlAction;
    }

    public void setOutOfControlAction(OutOfControlActionEnum outOfControlAction) {
        this.outOfControlAction = outOfControlAction;
    }

    public ExitWaylineWhenRcLostEnum getExitWaylineWhenRcLost() {
        return exitWaylineWhenRcLost;
    }

    public void setExitWaylineWhenRcLost(ExitWaylineWhenRcLostEnum exitWaylineWhenRcLost) {
        this.exitWaylineWhenRcLost = exitWaylineWhenRcLost;
    }

    public Integer getRthAltitude() {
        return rthAltitude;
    }

    public void setRthAltitude(Integer rthAltitude) {
        this.rthAltitude = rthAltitude;
    }

    public RthModeEnum getRthMode() {
        return rthMode;
    }

    public void setRthMode(RthModeEnum rthMode) {
        this.rthMode = rthMode;
    }

    public WaylinePrecisionTypeEnum getWaylinePrecisionType() {
        return waylinePrecisionType;
    }

    public void setWaylinePrecisionType(WaylinePrecisionTypeEnum waylinePrecisionType) {
        this.waylinePrecisionType = waylinePrecisionType;
    }

    @Override
    public String toString() {
        return "InFlightWaylineDeliverRequest{" +
                "file=" + file +
                ", inFlightWaylineId='" + inFlightWaylineId + '\'' +
                ", outOfControlAction=" + outOfControlAction +
                ", exitWaylineWhenRcLost=" + exitWaylineWhenRcLost +
                ", rthAltitude=" + rthAltitude +
                ", rthMode=" + rthMode +
                ", waylinePrecisionType=" + waylinePrecisionType +
                '}';
    }
}
