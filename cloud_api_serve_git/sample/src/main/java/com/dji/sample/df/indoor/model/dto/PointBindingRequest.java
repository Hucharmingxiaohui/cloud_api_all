package com.dji.sample.df.indoor.model.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

// 新增/修改点位的请求体
@Data
public class PointBindingRequest {
    @NotBlank(message = "名称不能为空")
    private String name;

    @NotNull(message = "X坐标不能为空")
    private Double x;

    @NotNull(message = "Y坐标不能为空")
    private Double y;

    @NotNull(message = "Z坐标不能为空")
    private Double z;
}
