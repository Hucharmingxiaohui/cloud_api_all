package com.dji.sample.df.cqDockDf.model.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

import java.util.List;

@Data
public class CqAssignTaskRequest {

    @JsonAlias({"taskName", "task_name"})
    private String taskName;

    @JsonAlias({"businessId", "business_id"})
    private String businessId;

    @JsonAlias({"routeId", "route_id"})
    private String routeId;

    @JsonAlias({"deviceIdList", "device_id_list"})
    private List<DeviceItem> deviceIdList;

    @Data
    public static class DeviceItem {
        /** 电网资源编号 psrid */
        @JsonAlias({"deviceId", "device_id"})
        private String deviceId;
        /** 设备类型 1-杆塔 2-变电站 */
        @JsonAlias({"deviceType", "device_type"})
        private String deviceType;
    }
}
