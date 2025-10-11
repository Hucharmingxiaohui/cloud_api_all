package com.df.server.patrol.control.sendDto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 请求体
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class RobotCommandAPIDTO extends RobotAPIDTO {
    private String command;

    public RobotCommandAPIDTO(String command) {
        this.command = command;
    }
}
