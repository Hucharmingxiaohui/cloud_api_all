package com.df.server.service.his;


import com.df.framework.vo.PageVO;
import com.df.server.dto.HisUniTaskItemAlarm.HisUniTaskItemAlarmPageDTO;
import com.df.server.dto.HisUniTaskItemPoints.HisPointsHandleDTO;
import com.df.server.entity.his.HisUniTaskItemPointsEntity;
import com.df.server.vo.HisUniTaskItemAlarm.HisUniTaskItemAlarmVO;

/**
 * 机器人巡检告警接口
 * <p>
 * Created by lianyc on 2025-05-23
 */
public interface HisUniTaskItemAlarmService {


    //自定义分页查询（可写SQL自己关联表）
    PageVO<HisUniTaskItemAlarmVO> customPage(HisUniTaskItemAlarmPageDTO pageDTO);

    void createAlarm(HisUniTaskItemPointsEntity hisPoint, Integer alarmLevel);

    /**
     * 一键确认时，告警确认所有告警
     *
     * @param taskPatrolledId
     */
    void confirmAll(String taskPatrolledId);

    /**
     * 单个告警确认，需要有pointCode
     *
     * @param params
     */
    void confirmAlarm(HisPointsHandleDTO params);
}

