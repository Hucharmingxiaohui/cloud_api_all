package com.dji.sample.df.mediaDf.service;

import com.df.server.entity.uni.UniPointEntity;
import com.dji.sample.df.mediaDf.model.MediaFileDTO;
import com.dji.sample.df.thirdKmzDf.entity.pointResult.PointResult;
import com.dji.sdk.cloudapi.media.MediaUploadCallbackRequest;
import com.dji.sdk.common.PaginationData;

import java.net.URL;
import java.util.List;

/**
 * @author sean
 * @version 0.2
 * @date 2021/12/9
 */
public interface IFileServiceDf {

    /**
     * Query if the file already exists based on the workspace id and the fingerprint of the file.
     * @param workspaceId
     * @param fingerprint
     * @return
     */
    Boolean checkExist(String workspaceId, String fingerprint);

    /**
     * Save the basic information of the file to the database.
     * @param workspaceId
     * @param file
     * @return
     */
    Integer saveFile(String workspaceId, MediaUploadCallbackRequest file);

    /**
     * Query information about all files in this workspace based on the workspace id.
     * @param workspaceId
     * @return
     */
    List<MediaFileDTO> getAllFilesByWorkspaceId(String workspaceId);

    /**
     * Paginate through all media files in this workspace.
     * @param workspaceId
     * @param page
     * @param pageSize
     * @return
     */
    PaginationData<MediaFileDTO> getMediaFilesPaginationByWorkspaceId(String workspaceId, long page, long pageSize);

    /**
     * Get the download address of the file.
     * @param workspaceId
     * @param fileId
     * @return
     */
    URL getObjectUrl(String workspaceId, String fileId);

    /**
     * Query all media files of a job.
     * @param jobId
     * @return
     */
    List<MediaFileDTO> getFilesByJobId( String jobId);
//
    List<MediaFileDTO> getUniqueFilesByJobId( String jobId);

    PaginationData<MediaFileDTO> getMediaFilesPaginationByFileName(String workspaceId, long page, long pageSize, String fileName);

    boolean deleteMedia(String workspaceId, String fileId);

    PaginationData<MediaFileDTO> getMediaFilesPaginationByJobId(String workspaceId, Long page, Long pageSize, String jobId, Long startTime, Long endTime);

    List<MediaFileDTO> getJobIdList(String workspaceId);
    //根据job_id获取详细信息
    PointResult getMediaDileByJobId(String job_id,String workspace_id,String wayline_id) throws Exception;
    //根据job_id获取详细信息（最新版，不解析航点）
    List<MediaFileDTO> getMediaDileByJobId3(String job_id,String workspace_id) throws Exception;
//  获取指定航点的结果照片
    PointResult getMediaDileByJobId2(String job_id,String workspace_id,String wayline_id) throws Exception;

    List<UniPointEntity> getPointByJobId(String job_id) throws Exception;

    PointResult getMediaFileByPoint(String job_id,Integer point_pos, String workspace_id, String wayline_id) throws Exception;
    //缩略图
    boolean getThumbnailByJobId(String workspaceId, String fileId);
    /**
     * Get the download address of the thumbnail.
     * @param workspaceId
     * @param fileId
     * @return
     */
    URL getThumbnailUrl(String workspaceId, String fileId);
}
