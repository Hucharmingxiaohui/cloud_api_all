package com.dji.sample.df.solar.model.dto;

import lombok.Data;
import java.util.List;

/**
 * 光伏检测响应DTO（地理坐标）
 */
@Data
public class SolarDetectResponseDTO {

    private Integer code;

    private String message;

    private ResponseData data;

    @Data
    public static class ResponseData {

        private String solar_area_name;

        private List<DetectAreaResponseDTO> detect_areas;
    }

    @Data
    public static class DetectAreaResponseDTO {

        private String area_name;

        private List<GeoCoordinateDTO> corners_geo;
    }

    @Data
    public static class GeoCoordinateDTO {

        private Double lat;

        private Double lon;
    }
}