package com.dji.sample.df.manageDf.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dji.sample.df.manageDf.model.entity.WaylinePointEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 *
 * @author sean.zhou
 * @date 2021/11/15
 * @version 0.1
 */
public interface IWaylinePointMapper extends BaseMapper<WaylinePointEntity> {

    String sql = "<script> \n" +
            "select * from pub_wayline_point_df \n" +
            "<where> \n" +
            "<if test='subCode != null and subCode != \"\"'> \n" +
            "   and sub_code = #{subCode} \n" +
            "</if> \n" +
            "</where> \n";

    @Select(sql + " </script>")
    List<WaylinePointEntity> selectAllData(@Param("subCode") String subCode);

}
