package com.dji.sdk.cloudapi.wayline;

public class FlighttaskProgressGetRequest {

    private String flightId;

    private String targetSn;

    private String sn;

    public FlighttaskProgressGetRequest() {
    }

    public String getFlightId() {
        return flightId;
    }

    public FlighttaskProgressGetRequest setFlightId(String flightId) {
        this.flightId = flightId;
        return this;
    }

    public String getTargetSn() {
        return targetSn;
    }

    public FlighttaskProgressGetRequest setTargetSn(String targetSn) {
        this.targetSn = targetSn;
        return this;
    }

    public String getSn() {
        return sn;
    }

    public FlighttaskProgressGetRequest setSn(String sn) {
        this.sn = sn;
        return this;
    }
}
