package com.dji.sample.df.indoor.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("indoor_point_binding")
public class IndoorPointBinding {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;          // 唯一ID（UUID）

    private String name;        // 点位名称

    private Double x;           // X坐标
    private Double y;           // Y坐标
    private Double z;           // Z坐标
}
