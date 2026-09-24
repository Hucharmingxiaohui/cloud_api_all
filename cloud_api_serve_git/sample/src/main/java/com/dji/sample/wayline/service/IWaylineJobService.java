package com.dji.sample.wayline.service;

import com.dji.sample.wayline.model.dto.WaylineJobDTO;
import com.dji.sample.wayline.model.enums.WaylineJobStatusEnum;
import com.dji.sample.wayline.model.param.CreateJobParam;
import com.dji.sdk.common.PaginationData;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author sean
 * @version 1.1
 * @date 2022/6/1
 */
public interface IWaylineJobService {

    /**
     * Create wayline job in the database.
     * @param param
     * @param workspaceId   user info
     * @param username      user info
     * @param beginTime     The time the job started.
     * @param endTime       The time the job ended.
     * @return
     */
    Optional<WaylineJobDTO> createWaylineJob(CreateJobParam param, String workspaceId, String username, Long beginTime, Long endTime);
//   空中下发航线任务创建
    Optional<WaylineJobDTO> createWaylineJob2(CreateJobParam param, String workspaceId, String username, Long beginTime, Long endTime);
    /**
     * Create a sub-task based on the information of the parent task.
     * @param workspaceId
     * @param parentId
     * @return
     */
    Optional<WaylineJobDTO> createWaylineJobByParent(String workspaceId, String parentId);

    /**
     * Query wayline jobs based on conditions.
     * @param workspaceId
     * @param jobIds
     * @param status
     * @return
     */
    List<WaylineJobDTO> getJobsByConditions(String workspaceId, Collection<String> jobIds, WaylineJobStatusEnum status);

    /**
     * Query job information based on job id.
     * @param workspaceId
     * @param jobId
     * @return job information
     */
    Optional<WaylineJobDTO> getJobByJobId(String workspaceId, String jobId);

    /**
     * Update job data.
     * @param dto
     * @return
     */
    Boolean updateJob(WaylineJobDTO dto);

    /**
     * Paginate through all jobs in this workspace.
     * @param workspaceId
     * @param page
     * @param pageSize
     * @return
     */
    PaginationData<WaylineJobDTO> getJobsByWorkspaceId(String workspaceId, long page, long pageSize, Map map);

    /**
     * Query the wayline execution status of the dock.
     * @param dockSn
     * @return
     */
    WaylineJobStatusEnum getWaylineState(String dockSn);

    /**
     * 更新任务的分析标记（断点续飞时重置为未分析，任务完成后重新保存并分析全部媒体）。
     * @param jobId 任务 id
     * @param isAnalyzed 分析标记
     */
    void updateJobIsAnalyzed(String jobId, Integer isAnalyzed);
}
