package com.dji.sample.df.cqDockDf.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class CqAssignTaskRequest {

    private String taskName;

    private String businessId;

    private String routeId;

    private List<DeviceItem> deviceIdList;

    @Data
    public static class DeviceItem {
        /** 电网资源编号 psrid */
        private String deviceId;
        /** 设备类型 1-杆塔 2-变电站 */
        private String deviceType;
    }
}
