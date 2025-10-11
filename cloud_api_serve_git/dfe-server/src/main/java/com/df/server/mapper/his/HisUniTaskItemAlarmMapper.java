package com.df.server.mapper.his;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.df.server.dto.HisUniTaskItemAlarm.HisUniTaskItemAlarmPageDTO;
import com.df.server.dto.HisUniTaskItemPoints.HisPointsHandleDTO;
import com.df.server.entity.his.HisUniTaskItemAlarmEntity;
import com.df.server.vo.HisUniTaskItemAlarm.HisUniTaskItemAlarmVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 机器人巡检告警接口
 * <p>
 * Created by lianyc on 2025-05-23
 */
@Mapper
public interface HisUniTaskItemAlarmMapper {
    //自定义分页查询
    List<HisUniTaskItemAlarmVO> getPageList(HisUniTaskItemAlarmPageDTO pageDTO);

    Integer getPageTotal(HisUniTaskItemAlarmPageDTO pageDTO);

    void insertDfHisUniTaskItemAlarm(HisUniTaskItemAlarmEntity dfHisUniTaskItemAlarm);

    /**
     * 一键确认时，告警确认
     *
     * @param taskPatrolledId
     */
    void confirmAll(String taskPatrolledId);

    /**
     * 告警确认"taskPatrolledId", "pointCode", "confirmResult"， "alarmConfirmComment";
     *
     * @param params
     */
    void confirmAlarm(HisPointsHandleDTO params);
}
