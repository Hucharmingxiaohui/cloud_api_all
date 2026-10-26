package com.dji.sample.df.substationDf.model.entity;

import lombok.Data;
import java.util.List;

@Data
@com.fasterxml.jackson.databind.annotation.JsonNaming(com.fasterxml.jackson.databind.PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public class BindEuaRouteRequest {
    private List<Long> ids;
    private String euaRouteName;
}
