package com.df.server.patrol.control.sendDto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 请求体
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class RobotCameraAPIDTO extends RobotAPIDTO {
    private String camera;

    public RobotCameraAPIDTO(String camera) {
        this.camera = camera;
    }
}
