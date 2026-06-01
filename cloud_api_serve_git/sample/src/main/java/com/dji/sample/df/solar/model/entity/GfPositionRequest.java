package com.dji.sample.df.solar.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GfPositionRequest {

    @JsonProperty("inspection_id")
    private String inspectionId;

    private List<Image> images;

    @JsonProperty("area_height")
    private Double areaHeight;

    @JsonProperty("panel_height")
    private Double panelHeight;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Image {

        @JsonProperty("defect_id")
        private Integer defectId;

        @JsonProperty("image_name")
        private String imageName;

        @JsonProperty("image_type")
        private String imageType;

        @JsonProperty("has_defect")
        private Boolean hasDefect;

        @JsonProperty("defects")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private List<Defect> defects;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Defect {
        // ---- 可见光 (visible) 的字段 ----
        @JsonProperty("defect_type")
        private String defectType;

        private Integer col;
        private Integer row;

        // ---- 红外 (ir) 的字段 ----
        @JsonProperty("center_col")
        private Integer centerCol;

        @JsonProperty("center_row")
        private Integer centerRow;

        private Integer width;
        private Integer height;
    }
}
