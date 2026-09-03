package com.dji.sdk.cloudapi.wayline;

import com.dji.sdk.common.BaseModel;

public class FlighttaskProgressGetResponse extends BaseModel {

    private String flightId;

    private FlighttaskProgressData progress;

    private FlighttaskStatusEnum status;

    public FlighttaskProgressGetResponse() {
    }

    public String getFlightId() {
        return flightId;
    }

    public FlighttaskProgressGetResponse setFlightId(String flightId) {
        this.flightId = flightId;
        return this;
    }

    public FlighttaskProgressData getProgress() {
        return progress;
    }

    public FlighttaskProgressGetResponse setProgress(FlighttaskProgressData progress) {
        this.progress = progress;
        return this;
    }

    public FlighttaskStatusEnum getStatus() {
        return status;
    }

    public FlighttaskProgressGetResponse setStatus(FlighttaskStatusEnum status) {
        this.status = status;
        return this;
    }
}
