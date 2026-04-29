package com.dji.sample.df.solar.model.dto;

import lombok.Data;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 光伏检测区域DTO
 * 用于接收前端正射图绘制的检测区域参数
 */
@Data
public class SolarDetectRequestDTO {

    /**
     * 正射图id
     */
    @NotEmpty(message = "正射图id不能为空")
    private Integer orthophoto_id;

    /**
     * 光伏区域名称
     */
    @NotEmpty(message = "光伏区域名称不能为空")
    private String solar_area_name;

    /**
     * 检测区域列表
     */
    @NotEmpty(message = "检测区域不能为空")
    @Valid
    private List<DetectAreaDTO> detect_areas;

    /**
     * 航线高度（米）
     */
    @NotNull(message = "飞行高度不能为空")
    private Double flight_altitude;

    /**
     * 光伏板倾角（度）
     */
    @NotNull(message = "倾角不能为空")
    private Double tilt_angle;

    /**
     * 横向航线数
     */
    @NotNull(message = "横向航线数不能为空")
    private Integer horizontal_lines;

    /**
     * 光伏区域海拔
     */
    @NotNull(message = "区域海拔不能为空")
    private Double area_height;

    /**
     * 光伏架设高度
     */
    @NotNull(message = "光伏板高度不能为空")
    private Double panel_height;

    /**
     * 光伏板朝向
     */
    @NotNull(message = "光伏板朝向不能为空")
    private Double panel_heading;

    /**
     * 区域边距
     */
    @NotNull(message = "区域边距不能为空")
    private Double margin;

    /**
     * 航线内点数
     */
    @NotNull(message = "航线内点数不能为空")
    private Integer points_per_line;

    /**
     * 检测区域内部类
     */
    @Data
    public static class DetectAreaDTO {

        /**
         * 检测区域名称
         */
        @NotEmpty(message = "检测区域名称不能为空")
        private String area_name;

        /**
         * 像素坐标角点列表
         * 必须包含4个点（矩形的4个角）
         */
        @NotEmpty(message = "角点坐标不能为空")
        @Size(min = 4, max = 4, message = "必须包含4个角点坐标")
        @Valid
        private List<CornerPixelDTO> corners_pixels;
    }

    /**
     * 像素坐标内部类
     */
    @Data
    public static class CornerPixelDTO {

        /**
         * 行坐标（像素）
         */
        @NotNull(message = "行坐标不能为空")
        private Integer row;

        /**
         * 列坐标（像素）
         */
        @NotNull(message = "列坐标不能为空")
        private Integer col;
    }
}
