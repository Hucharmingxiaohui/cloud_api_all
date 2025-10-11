package com.df.server.mapper.his;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.df.server.entity.his.HisUniTaskReportEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Created by lianyc on 2025-05-23
 */
@Mapper
public interface HisUniTaskReportMapper {
    void insert(HisUniTaskReportEntity reportEntity);

    HisUniTaskReportEntity getById(Integer reportId);

    void updateFile(HisUniTaskReportEntity reportEntity);

    void deleteByTaskPatrolledId(String taskPatrolledId);

    HisUniTaskReportEntity getByTaskId(String taskPatrolledId);

    void deleteById(Integer reportId);
}
