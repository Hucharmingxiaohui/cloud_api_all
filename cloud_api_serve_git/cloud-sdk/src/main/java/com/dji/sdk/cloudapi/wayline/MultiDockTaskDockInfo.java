package com.dji.sdk.cloudapi.wayline;

import com.dji.sdk.common.BaseModel;

import javax.validation.constraints.NotNull;
import java.util.Map;

public class MultiDockTaskDockInfo extends BaseModel {

    @NotNull
    private String dockType;

    @NotNull
    private String sn;

    @NotNull
    private Integer index;

    @NotNull
    private Object latitude;

    @NotNull
    private Object longitude;

    @NotNull
    private Object height;

    @NotNull
    private Object heading;

    @NotNull
    private Object homePositionIsValid;

    @NotNull
    private Object alternateLandPoint;

    @NotNull
    private Object rtcmInfo;

    public MultiDockTaskDockInfo() {
    }

    public String getDockType() {
        return dockType;
    }

    public MultiDockTaskDockInfo setDockType(String dockType) {
        this.dockType = dockType;
        return this;
    }

    public String getSn() {
        return sn;
    }

    public MultiDockTaskDockInfo setSn(String sn) {
        this.sn = sn;
        return this;
    }

    public Integer getIndex() {
        return index;
    }

    public MultiDockTaskDockInfo setIndex(Integer index) {
        this.index = index;
        return this;
    }

    public Object getLatitude() {
        return latitude;
    }

    public MultiDockTaskDockInfo setLatitude(Object latitude) {
        this.latitude = latitude;
        return this;
    }

    public Object getLongitude() {
        return longitude;
    }

    public MultiDockTaskDockInfo setLongitude(Object longitude) {
        this.longitude = longitude;
        return this;
    }

    public Object getHeight() {
        return height;
    }

    public MultiDockTaskDockInfo setHeight(Object height) {
        this.height = height;
        return this;
    }

    public Object getHeading() {
        return heading;
    }

    public MultiDockTaskDockInfo setHeading(Object heading) {
        this.heading = heading;
        return this;
    }

    public Object getHomePositionIsValid() {
        return homePositionIsValid;
    }

    public MultiDockTaskDockInfo setHomePositionIsValid(Object homePositionIsValid) {
        this.homePositionIsValid = homePositionIsValid;
        return this;
    }

    public Object getAlternateLandPoint() {
        return alternateLandPoint;
    }

    public MultiDockTaskDockInfo setAlternateLandPoint(Object alternateLandPoint) {
        this.alternateLandPoint = alternateLandPoint;
        return this;
    }

    public Object getRtcmInfo() {
        return rtcmInfo;
    }

    public MultiDockTaskDockInfo setRtcmInfo(Object rtcmInfo) {
        this.rtcmInfo = rtcmInfo;
        return this;
    }
}
