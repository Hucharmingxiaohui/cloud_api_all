package com.df.server.mapper.his;


import com.df.server.entity.his.HisUniTaskItemFileEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 机器人巡检结果文件Mapper接口
 *
 * @author ruoyi
 * @date 2021-01-08
 */
@Mapper
public interface HisUniTaskItemFileMapper  {

    HisUniTaskItemFileEntity getByRequestId(String requestId);

    void updateById(HisUniTaskItemFileEntity fileEntity);

    int insert(HisUniTaskItemFileEntity fileEntity);
}
