package com.df.server.dto.uniPoint;

import lombok.Data;

import java.io.Serializable;

@Data
public class PointVideoPosDTO implements Serializable {
    private static final long serialVersionUID = 932348510518268863L;

    private String device_code = "";
    private String device_pos = "";
    private String robot_code = "";
    private String robot_pos = "";
    private String uav_code = "";
    private String uav_pos = "";
}
