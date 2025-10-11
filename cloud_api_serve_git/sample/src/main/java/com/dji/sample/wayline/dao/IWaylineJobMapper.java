package com.dji.sample.wayline.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dji.sample.wayline.model.entity.WaylineJobEntity;
import org.apache.ibatis.annotations.Param;

/**
 * @author sean
 * @version 1.1
 * @date 2022/6/1
 */
public interface IWaylineJobMapper extends BaseMapper<WaylineJobEntity> {
    void updatePointNum(@Param("warnNum") Integer warnNum,
                                   @Param("failNum") Integer failNum,
                                   @Param("normalNum") Integer normalNum,
                                   @Param("exceptionNum") Integer exceptionNum,
                                   @Param("id") Integer id);
}
