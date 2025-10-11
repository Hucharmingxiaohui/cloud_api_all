package com.df.server.mapper.uni;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.df.server.entity.uni.UniPointThresholdEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 机器人点位告警阈值接口
 * <p>
 * Created by lianyc on 2025-05-19
 */
@Mapper
public interface UniPointThresholdMapper extends BaseMapper<UniPointThresholdEntity> {
}