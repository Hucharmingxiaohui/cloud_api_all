package com.dji.sample.df.cqDockDf.model.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
@com.fasterxml.jackson.databind.annotation.JsonNaming(com.fasterxml.jackson.databind.PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public class CqEuaPointOption {
    private String subCode;
    private String routeId;
    private String routeName;
    private List<Point> points = new ArrayList<>();

    @Data
    @com.fasterxml.jackson.databind.annotation.JsonNaming(com.fasterxml.jackson.databind.PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public static class Point {
        private Long id;
        private String pointCode;
        private String pointName;
    }
}
