package com.df.server.mapper.sys;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.df.server.entity.sys.SysConfigEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 参数配置接口
 * <p>
 * Created by lianyc on 2025-02-11
 */
@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfigEntity> {
    String getConfigValueByKey(String configKey);
}