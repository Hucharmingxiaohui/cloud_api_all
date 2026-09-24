package com.dji.sample.wayline.service;

import com.dji.sample.component.mqtt.model.EventsReceiver;
import com.dji.sample.wayline.model.dto.ConditionalWaylineJobKey;
import com.dji.sample.wayline.model.dto.WaylineBreakPointDTO;
import com.dji.sample.wayline.model.dto.WaylineJobDTO;
import com.dji.sdk.cloudapi.wayline.FlighttaskProgress;

import java.util.Optional;

/**
 * @author sean
 * @version 1.4
 * @date 2023/3/24
 */
public interface IWaylineRedisService {

    /**
     * Save the status of the wayline job performed by the dock into redis.
     * @param dockSn
     * @param data
     */
    void setRunningWaylineJob(String dockSn, EventsReceiver<FlighttaskProgress> data);

    /**
     * Query the status of wayline job performed by the dock in redis.
     * @param dockSn
     * @return
     */
    Optional<EventsReceiver<FlighttaskProgress>> getRunningWaylineJob(String dockSn);

    /**
     * Delete the wayline job status of the dock operation in redis.
     * @param dockSn
     * @return
     */
    Boolean delRunningWaylineJob(String dockSn);

    /**
     * Save the wayline job suspended by the dock to redis.
     * @param dockSn
     * @param jobId
     */
    void setPausedWaylineJob(String dockSn, String jobId);

    /**
     * Query the wayline job id suspended by the dock in redis.
     * @param dockSn
     * @return
     */
    String getPausedWaylineJobId(String dockSn);

    /**
     * Delete the wayline job suspended by the dock in redis.
     * @param dockSn
     * @return
     */
    Boolean delPausedWaylineJob(String dockSn);

    /**
     * Save the wayline job blocked by the dock to redis.
     * @param dockSn
     * @param jobId
     */
    void setBlockedWaylineJob(String dockSn, String jobId);

    /**
     * Query the wayline job id blocked by the dock in redis.
     * @param dockSn
     * @return
     */
    String getBlockedWaylineJobId(String dockSn);

    /**
     * Save the conditional wayline job by the dock to redis.
     * @param waylineJob
     */
    void setConditionalWaylineJob(WaylineJobDTO waylineJob);

    /**
     * Query the conditional wayline job id by the dock in redis.
     * @param jobId
     * @return
     */
    Optional<WaylineJobDTO> getConditionalWaylineJob(String jobId);

    /**
     * Delete the conditional wayline job by the dock in redis.
     * @param jobId
     * @return
     */
    Boolean delConditionalWaylineJob(String jobId);

    Boolean addPrepareConditionalWaylineJob(WaylineJobDTO waylineJob);

    Optional<ConditionalWaylineJobKey> getNearestConditionalWaylineJob();

    Double getConditionalWaylineJobTime(ConditionalWaylineJobKey jobKey);

    Boolean removePrepareConditionalWaylineJob(ConditionalWaylineJobKey jobKey);

    /**
     * 保存航线任务断点信息（断点续飞用）。
     * @param dto 断点信息
     */
    void setWaylineJobBreakpoint(WaylineBreakPointDTO dto);

    /**
     * 按任务 id 查询断点信息。
     * @param jobId 任务 id
     */
    Optional<WaylineBreakPointDTO> getWaylineJobBreakpoint(String jobId);

    /**
     * 删除任务断点信息。
     * @param jobId 任务 id
     */
    Boolean delWaylineJobBreakpoint(String jobId);

    /**
     * 记录断点续飞前任务的媒体总数（续飞 OK 时累加，避免总数被单趟计数覆盖）。
     * @param jobId 任务 id
     * @param mediaCount 续飞前媒体总数
     */
    void setWaylineJobMediaBase(String jobId, Integer mediaCount);

    /**
     * 查询断点续飞前的媒体总数基数。
     * @param jobId 任务 id
     */
    Optional<Integer> getWaylineJobMediaBase(String jobId);

    /**
     * 删除媒体总数基数（累加后消费）。
     * @param jobId 任务 id
     */
    Boolean delWaylineJobMediaBase(String jobId);
}
