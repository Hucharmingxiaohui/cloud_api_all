package com.df.server.mapper.sys;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.df.server.entity.sys.SysLogSystemEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统日志记录接口
 * <p>
 * Created by lianyc on 2023-11-02
 */
@Mapper
public interface SysLogSystemMapper extends BaseMapper<SysLogSystemEntity> {

}