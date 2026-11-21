package com.dji.sample.media.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dji.sample.media.model.MediaFileEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * @author sean
 * @version 0.2
 * @date 2021/12/9
 */
public interface IFileMapper extends BaseMapper<MediaFileEntity> {
    @Select("SELECT * FROM media_file WHERE BINARY workspace_id = BINARY #{workspaceId} "
            + "AND BINARY job_id <=> BINARY #{jobId} AND BINARY object_key = BINARY #{objectKey} ORDER BY id LIMIT 1")
    MediaFileEntity findUploadedFile(@Param("workspaceId") String workspaceId,
                                     @Param("jobId") String jobId, @Param("objectKey") String objectKey);

    @Select("SELECT COUNT(DISTINCT CASE WHEN TRIM(object_key) <> '' THEN BINARY object_key END) "
            + "+ COALESCE(SUM(CASE WHEN object_key IS NULL OR TRIM(object_key) = '' THEN 1 ELSE 0 END), 0) "
            + "FROM media_file WHERE BINARY workspace_id = BINARY #{workspaceId} AND BINARY job_id = BINARY #{jobId}")
    int countUploadedFiles(@Param("workspaceId") String workspaceId, @Param("jobId") String jobId);
}
