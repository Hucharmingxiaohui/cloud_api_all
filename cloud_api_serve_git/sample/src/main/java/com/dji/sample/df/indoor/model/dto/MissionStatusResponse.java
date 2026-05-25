package com.dji.sample.df.indoor.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MissionStatusResponse {

    @JsonProperty("current_goal")
    private Integer currentGoal;

    private String message;

    @JsonProperty("mission_id")
    private String missionId;

    private String status;

    @JsonProperty("total_goals")
    private Integer totalGoals;
}
