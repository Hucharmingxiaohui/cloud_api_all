package com.dji.sample.df.manageDf.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dji.sample.df.manageDf.model.entity.UniTaskPlanEntity;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface IUniTaskPlanMapper extends BaseMapper<UniTaskPlanEntity> {

    String scheduleTaskSql = "<script> \n" +
            "select *\n" +
            "        from uni_task_plan_df\n" +
            "        where isenable = 0\n" +
            "          and (\n" +
            "                (\n" +
            "                        histask_insert_time is null\n" +
            "                        and now() >= fixed_start_time\n" +
            "                    )\n" +
            "                or\n" +
            "                (\n" +
            "                        (\n" +
            "                                invalid_start_time is null\n" +
            "                                or invalid_end_time is null\n" +
            "                                or\n" +
            "                                (\n" +
            "                                        invalid_start_time is not null\n" +
            "                                        and invalid_end_time is not null\n" +
            "                                        and (now() not between invalid_start_time and invalid_end_time)\n" +
            "                                    )\n" +
            "                            )\n" +
            "                        and\n" +
            "                        (\n" +
            "                                (\n" +
            "                                        cycle_type is not null\n" +
            "                                        and (now() between cycle_start_time and cycle_end_time)\n" +
            "                                        and curtime() >= cycle_execute_time\n" +
            "                                        and find_in_set(month(now()), cycle_month)\n" +
            "                                        and (histask_insert_time is null or\n" +
            "                                             timestampdiff(day,histask_insert_time, now()) >= 1)\n" +
            "                                        and\n" +
            "                                        (\n" +
            "                                                (cycle_type = 1 and find_in_set(weekday(now()) + 1, cycle_week))\n" +
            "                                                or\n" +
            "                                                (cycle_type = 2 and find_in_set(day(now()),cycle_day))\n" +
            "                                            )\n" +
            "                                    )\n" +
            "                                or\n" +
            "                                (\n" +
            "                                        cycle_type is null\n" +
            "                                        and interval_type is not null\n" +
            "                                        and (now() between interval_start_time and interval_end_time)\n" +
            "                                        and\n" +
            "                                        (\n" +
            "                                                (\n" +
            "                                                            interval_type = 1 and curtime() >= interval_execute_time\n" +
            "                                                        and\n" +
            "                                                            (\n" +
            "                                                                    (histask_insert_time IS NULL)\n" +
            "                                                                    or\n" +
            "                                                                    (\n" +
            "                                                                            (histask_insert_time is not null) and\n" +
            "                                                                            (timestampdiff(hour,histask_insert_time, NOW()) >= interval_number)\n" +
            "                                                                        )\n" +
            "                                                                )\n" +
            "                                                    )\n" +
            "                                                or\n" +
            "                                                (\n" +
            "                                                            interval_type = 2 and curtime() >= interval_execute_time\n" +
            "                                                        and\n" +
            "                                                            (\n" +
            "                                                                    (histask_insert_time is null)\n" +
            "                                                                    or\n" +
            "                                                                    ((histask_insert_time is not null) and\n" +
            "                                                                     (timestampdiff(day,histask_insert_time, now()) >= interval_number))\n" +
            "                                                                )\n" +
            "                                                    )\n" +
            "                                            )\n" +
            "                                    )\n" +
            "                            )\n" +
            "                    )\n" +
            "            )\n" +
            "\torder by priority desc";

    @Select(scheduleTaskSql + "</script>")
    List<UniTaskPlanEntity> getScheduledPlan();
}
