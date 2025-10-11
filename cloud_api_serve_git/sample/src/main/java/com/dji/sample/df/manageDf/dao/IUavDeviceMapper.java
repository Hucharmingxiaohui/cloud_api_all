package com.dji.sample.df.manageDf.dao;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.dji.sample.df.manageDf.model.entity.UavDeviceEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 *
 * @author sean.zhou
 * @date 2021/11/10
 * @version 0.1
 */
public interface IUavDeviceMapper extends BaseMapper<UavDeviceEntity> {

    String sql = "<script> \n" +
            "select * from pub_uav_device_df\n" +
            "${ew.customSqlSegment} \n";

    @Select(sql + " </script>")
    List<UavDeviceEntity> selectByCondition(@Param(Constants.WRAPPER)Wrapper<UavDeviceEntity> wrapper);

}