package com.dji.sample.wayline.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 航线任务断点信息，来自 flighttask_progress 事件 ext.break_point，
 * 用于断点续飞时重新下发 flighttask_prepare + break_point。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WaylineBreakPointDTO {

    private String jobId;

    private String dockSn;

    @JsonProperty("file_id")
    private String fileId;

    /**
     * 断点航点序号
     */
    private Integer index;

    /**
     * 断点状态：0=航段中 1=航点上
     */
    private Integer state;

    /**
     * 当前航段进度 0~1
     */
    private Float progress;

    /**
     * 断点所在航线段 id（多航线任务）
     */
    @JsonProperty("wayline_id")
    private Integer waylineId;

    /**
     * 断开原因
     */
    @JsonProperty("break_reason")
    private Integer breakReason;

    private Float latitude;

    private Float longitude;

    private Float height;

    /**
     * 断点记录时间戳（毫秒）
     */
    @JsonProperty("create_time")
    private Long createTime;
}
