package com.dji.sample.df.indoor.model.dto;

import lombok.Data;
import java.time.LocalDateTime;

// 响应体（可复用实体，但为清晰单独定义）
@Data
public class PointBindingResponse {

    private String id;
    private String name;
    private Double x;
    private Double y;
    private Double z;
}
