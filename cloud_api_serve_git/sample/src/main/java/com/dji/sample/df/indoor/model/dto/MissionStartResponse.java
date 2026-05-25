package com.dji.sample.df.indoor.model.dto;

import lombok.Data;

@Data
public class MissionStartResponse {
    private String missionId;   // 注意：JSON中是 mission_id，需处理命名映射
    private String status;
}
