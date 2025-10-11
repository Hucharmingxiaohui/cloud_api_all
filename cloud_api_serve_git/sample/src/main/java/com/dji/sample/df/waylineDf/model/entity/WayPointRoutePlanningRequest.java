package com.dji.sample.df.waylineDf.model.entity;

import java.util.List;

public class WayPointRoutePlanningRequest {
    private List<List<Double>> coordinates;

    // Getter and Setter methods
    public List<List<Double>> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(List<List<Double>> coordinates) {
        this.coordinates = coordinates;
    }
}
