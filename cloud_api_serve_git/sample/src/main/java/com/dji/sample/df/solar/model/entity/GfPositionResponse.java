package com.dji.sample.df.solar.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * 光伏缺陷位置信息响应DTO
 */
@Data
public class GfPositionResponse implements Serializable {

    private Integer code;
    private String message;
    private Data data;

    /**
     * 内部类 - 数据主体
     */
    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    public static class Data implements Serializable {
        @JsonProperty("inspection_id")
        private String inspectionId;

        @JsonProperty("orthophoto_name")
        private String orthophotoName;

        @JsonProperty("annotated_image")
        private String annotatedImage;

        private List<ResultItem> results;
    }

    /**
     * 内部类 - 结果项
     */
    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    public static class ResultItem implements Serializable {
        @JsonProperty("defect_id")
        private String defectId;

        @JsonProperty("original_name")
        private String originalName;

        @JsonProperty("positioned_name")
        private String positionedName;

        @JsonProperty("solar_panel_name")
        private String solarPanelName;

        @JsonProperty("has_defect")
        private Boolean hasDefect;

        private List<Defect> defects;
    }

    /**
     * 内部类 - 缺陷信息
     */
    @Getter
    @Setter
    @ToString
    @EqualsAndHashCode
    public static class Defect implements Serializable {
        @JsonProperty("defect_type")
        private String defectType;

        private Double lon;
        private Double lat;

        @JsonProperty("solar_panel_name")
        private String solarPanelName;

        @JsonProperty("solar_panel_component_name")
        private String solarPanelComponentName;
    }
}
