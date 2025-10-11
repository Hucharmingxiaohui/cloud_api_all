package com.df.server.patrol.control.sendDto;

import com.alibaba.fastjson.JSON;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 请求体
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class RobotSyncPointDTO extends RobotAPIDTO {
    private List<Integer> point_ids;

    public RobotSyncPointDTO(List<Integer> pointIdsList) {
        this.point_ids = pointIdsList;
    }
}
