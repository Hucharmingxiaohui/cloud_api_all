package com.df.server.dto.JobPlan;


import lombok.Data;

import java.io.Serializable;


@Data
public class JobPlanItemPointDTO implements Serializable {

    private String subCode;

    private Integer dataType;

    private String pointCode;

    private Integer robotPos;

    private String robotCode;

    private Integer waylinePos;

    private Integer waylinePointPos;

}
