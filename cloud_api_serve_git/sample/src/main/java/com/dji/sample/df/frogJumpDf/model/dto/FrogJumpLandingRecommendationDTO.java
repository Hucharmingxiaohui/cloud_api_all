package com.dji.sample.df.frogJumpDf.model.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class FrogJumpLandingRecommendationDTO {
    private Endpoint endpoint;
    private List<Candidate> candidates = new ArrayList<>();
    private String recommendedDockSn;
    private String message;

    @Data
    @AllArgsConstructor
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Endpoint {
        private double longitude;
        private double latitude;
        private int waylineId;
        private int waypointIndex;
    }

    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Candidate {
        private String dockSn;
        private String name;
        private Double distanceMeters;
        private boolean online;
        private boolean selectable;
        private String reason;
        private boolean recommended;
    }
}
