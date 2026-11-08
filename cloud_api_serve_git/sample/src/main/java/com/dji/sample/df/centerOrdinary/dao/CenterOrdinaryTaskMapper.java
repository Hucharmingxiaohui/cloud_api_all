package com.dji.sample.df.centerOrdinary.dao;

import com.dji.sample.df.centerOrdinary.CenterOrdinaryTaskEntity;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface CenterOrdinaryTaskMapper extends BaseMapper<CenterOrdinaryTaskEntity> {
    @Select("SELECT * FROM df_center_ordinary_task WHERE status IN ('COMPLETED','FAILED','CANCELLED','EXPIRED','UNKNOWN','INCOMPLETE','REPORT_UNKNOWN') AND (status_report_key IS NULL OR status_report_key<>status) ORDER BY COALESCE(status_report_time,0),id LIMIT 100")
    List<CenterOrdinaryTaskEntity> pendingStatusReports();

    @Update("UPDATE df_center_ordinary_task SET status_report_time=#{now} WHERE id=#{id} AND (status_report_key IS NULL OR status_report_key<>#{key}) AND (status_report_time IS NULL OR status_report_time<=#{before})")
    int reserveStatusReport(@Param("id") long id, @Param("key") String key, @Param("now") long now, @Param("before") long before);

    @Update("UPDATE df_center_ordinary_task SET status_report_key=#{key} WHERE id=#{id} AND status_report_time=#{now}")
    int statusReported(@Param("id") long id, @Param("key") String key, @Param("now") long now);

    @Select("SELECT * FROM df_center_ordinary_task WHERE sub_code=#{sub} AND task_code=#{task} AND scheduled_time=#{time}")
    CenterOrdinaryTaskEntity findIdentity(@Param("sub") String sub, @Param("task") String task, @Param("time") long time);

    @Select("SELECT * FROM df_center_ordinary_task WHERE job_id=#{job}")
    CenterOrdinaryTaskEntity findJob(@Param("job") String job);

    @Select("SELECT * FROM df_center_ordinary_task WHERE status='SCHEDULED' AND scheduled_time<=#{now} ORDER BY scheduled_time,id LIMIT 100")
    List<CenterOrdinaryTaskEntity> due(@Param("now") long now);

    @Select("SELECT * FROM df_center_ordinary_task WHERE status='DISPATCHED' AND job_id IS NOT NULL ORDER BY id LIMIT 1000")
    List<CenterOrdinaryTaskEntity> monitoring();

    @Update("UPDATE df_center_ordinary_task SET status='DISPATCHING',claimed_time=#{now} WHERE id=#{id} AND status='SCHEDULED' AND scheduled_time<=#{now}")
    int claim(@Param("id") long id, @Param("now") long now);

    @Update("UPDATE df_center_ordinary_task SET job_id=#{job} WHERE id=#{id} AND status='DISPATCHING' AND job_id IS NULL")
    int bindJob(@Param("id") long id, @Param("job") String job);

    @Update("UPDATE df_center_ordinary_task SET status='DISPATCHED',job_id=#{job} WHERE id=#{id} AND status='DISPATCHING' AND (job_id IS NULL OR job_id=#{job})")
    int dispatched(@Param("id") long id, @Param("job") String job);

    @Update("UPDATE df_center_ordinary_task SET status=#{next},message=#{message} WHERE id=#{id} AND status=#{expected}")
    int finish(@Param("id") long id, @Param("expected") String expected, @Param("next") String next, @Param("message") String message);

    @Update("UPDATE df_center_ordinary_task SET status='UNKNOWN',message='执行进程中断，禁止自动重飞' WHERE status='DISPATCHING' AND claimed_time<#{before}")
    int quarantine(@Param("before") long before);

    @Update("UPDATE df_center_ordinary_task SET status='REPORT_UNKNOWN',message='媒体上报进程中断，需人工核验，禁止自动重发' WHERE status='REPORTING' AND claimed_time<#{before}")
    int quarantineReports(@Param("before") long before);

    @Update("UPDATE df_center_ordinary_task SET result_snapshot=#{json} WHERE id=#{id} AND status='REPORTING'")
    int saveResults(@Param("id") long id, @Param("json") String json);
}
