package com.dji.sample.df.centerOrdinary.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dji.sample.df.centerOrdinary.CenterPatrolPlan;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface CenterPatrolPlanMapper extends BaseMapper<CenterPatrolPlan> {
    @Insert("INSERT INTO df_center_patrol_plan(center_code,sub_code,task_code,task_name,isenable) VALUES(#{center},#{sub},#{code},#{code},1) ON DUPLICATE KEY UPDATE id=id")
    int ensure(@Param("center") String center, @Param("sub") String sub, @Param("code") String code);
    @Select("SELECT * FROM df_center_patrol_plan WHERE center_code=#{center} AND sub_code=#{sub} AND task_code=#{code} FOR UPDATE")
    CenterPatrolPlan lockIdentity(@Param("center") String center, @Param("sub") String sub, @Param("code") String code);
    @Select("SELECT * FROM df_center_patrol_plan WHERE id=#{id} FOR UPDATE")
    CenterPatrolPlan lock(@Param("id") long id);
    @Select("SELECT * FROM df_center_patrol_plan WHERE center_code=#{center} AND task_code=#{code} LIMIT 2")
    List<CenterPatrolPlan> controlCandidates(@Param("center") String center, @Param("code") String code);
}
