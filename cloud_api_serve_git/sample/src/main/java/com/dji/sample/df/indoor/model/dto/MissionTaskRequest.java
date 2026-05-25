package com.dji.sample.df.indoor.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Positive;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MissionTaskRequest {

    @NotEmpty(message = "goals cannot be empty")
    private List<List<Double>> goals;  // 每个子列表为 [x, y, z, yaw_deg]

    @Positive(message = "dwell_time must be positive")
    @JsonProperty("dwell_time")
    private Double dwellTime;
}
