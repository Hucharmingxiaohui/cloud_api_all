package com.dji.sdk.cloudapi.wayline;

public class InFlighttaskProgress {

    private String inFlightWaylineId;

    private ProgressParam progress;

    private Integer status;

    private Integer result;

    private Integer wayPointIndex;

    // 无参构造
    public InFlighttaskProgress() {
    }

    // 全参构造
    public InFlighttaskProgress(String inFlightWaylineId, ProgressParam progress,
                                Integer status, Integer result, Integer wayPointIndex) {
        this.inFlightWaylineId = inFlightWaylineId;
        this.progress = progress;
        this.status = status;
        this.result = result;
        this.wayPointIndex = wayPointIndex;
    }

    public String getInFlightWaylineId() {
        return inFlightWaylineId;
    }

    public void setInFlightWaylineId(String inFlightWaylineId) {
        this.inFlightWaylineId = inFlightWaylineId;
    }

    public ProgressParam getProgress() {
        return progress;
    }

    public void setProgress(ProgressParam progress) {
        this.progress = progress;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getResult() {
        return result;
    }

    public void setResult(Integer result) {
        this.result = result;
    }

    public Integer getWayPointIndex() {
        return wayPointIndex;
    }

    public void setWayPointIndex(Integer wayPointIndex) {
        this.wayPointIndex = wayPointIndex;
    }

    @Override
    public String toString() {
        return "InFlighttaskProgress{" +
                "inFlightWaylineId='" + inFlightWaylineId + '\'' +
                ", progress=" + progress +
                ", status=" + status +
                ", result=" + result +
                ", wayPointIndex=" + wayPointIndex +
                '}';
    }

}
