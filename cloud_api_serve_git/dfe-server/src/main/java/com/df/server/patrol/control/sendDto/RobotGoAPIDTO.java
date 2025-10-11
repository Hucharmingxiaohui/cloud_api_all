package com.df.server.patrol.control.sendDto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 请求体
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class RobotGoAPIDTO extends RobotAPIDTO {
    private String key;

    public RobotGoAPIDTO(String key) {
        this.key = key;
    }
}
