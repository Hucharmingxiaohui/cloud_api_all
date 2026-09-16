package com.dji.sample.df.frogJumpDf.model.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class FrogJumpExecuteParam {

    @NotBlank
    private String flightId;

    private String droneSn;

    @NotBlank
    private String takeoffDockSn;

    @NotBlank
    private String landingDockSn;
}
