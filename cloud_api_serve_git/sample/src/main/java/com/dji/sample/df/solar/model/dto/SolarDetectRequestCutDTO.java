package com.dji.sample.df.solar.model.dto;

import lombok.Data;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 光伏检测请求DTO（像素坐标）
 */
@Data
public class SolarDetectRequestCutDTO {

    @NotEmpty(message = "光伏区域名称不能为空")
    private String solar_area_name;

    @NotEmpty(message = "检测区域不能为空")
    @Valid
    private List<DetectAreaDTO> detect_areas;

    @Data
    public static class DetectAreaDTO {

        @NotEmpty(message = "检测区域名称不能为空")
        private String area_name;

        @NotEmpty(message = "角点坐标不能为空")
        @Size(min = 4, max = 4, message = "必须包含4个角点坐标")
        @Valid
        private List<CornerPixelDTO> corners_pixels;
    }

    @Data
    public static class CornerPixelDTO {

        @NotNull(message = "行坐标不能为空")
        private Integer row;

        @NotNull(message = "列坐标不能为空")
        private Integer col;
    }
}
