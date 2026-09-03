package com.dji.sdk.cloudapi.wayline;

import com.dji.sdk.common.BaseModel;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

public class FlighttaskStopRequest extends BaseModel {

    @NotNull
    @Pattern(regexp = "^[^<>:\"/|?*._\\\\]+$")
    private String flightId;

    @NotNull
    private Integer reason;

    public FlighttaskStopRequest() {
    }

    public String getFlightId() {
        return flightId;
    }

    public FlighttaskStopRequest setFlightId(String flightId) {
        this.flightId = flightId;
        return this;
    }

    public Integer getReason() {
        return reason;
    }

    public FlighttaskStopRequest setReason(Integer reason) {
        this.reason = reason;
        return this;
    }
}
