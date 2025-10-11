package com.df.server.service.his.impl;


import com.df.framework.config.FileConfig;
import com.df.framework.utils.PageUtils;
import com.df.framework.vo.PageVO;
import com.df.server.dto.HisUniTaskItemAlarm.HisUniTaskItemAlarmPageDTO;
import com.df.server.dto.HisUniTaskItemPoints.HisPointsHandleDTO;
import com.df.server.entity.his.HisUniTaskItemAlarmEntity;
import com.df.server.entity.his.HisUniTaskItemPointsEntity;
import com.df.server.mapper.his.HisUniTaskItemAlarmMapper;
import com.df.server.service.his.HisUniTaskItemAlarmService;
import com.df.server.vo.HisUniTaskItemAlarm.HisUniTaskItemAlarmVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Slf4j
@Service("hisUniTaskItemAlarmService")
public class HisUniTaskItemAlarmServiceImpl implements HisUniTaskItemAlarmService {

    @Autowired
    private HisUniTaskItemAlarmMapper baseMapper;
    @Autowired
    private FileConfig fileConfig;

    // 自定义分页查询
    @Override
    public PageVO<HisUniTaskItemAlarmVO> customPage(HisUniTaskItemAlarmPageDTO pageDTO) {
        List<HisUniTaskItemAlarmVO> list = baseMapper.getPageList(pageDTO);
        for (HisUniTaskItemAlarmVO vo : list) {
            vo.setFilePathUrl(fileConfig.getFileVisitUrl(vo.getFilePathUrl()));
            vo.setRecgFilePathUrl(fileConfig.getFileVisitUrl(vo.getRecgFilePathUrl()));
        }
        Integer total = baseMapper.getPageTotal(pageDTO);
        return PageUtils.returnPageVOExtend(pageDTO, list, total);
    }

    @Override
    public void createAlarm(HisUniTaskItemPointsEntity itemPoint, Integer alarmLevel) {
        HisUniTaskItemAlarmEntity dfHisUniTaskItemAlarm = new HisUniTaskItemAlarmEntity();
        dfHisUniTaskItemAlarm.setSubCode(itemPoint.getSubCode());
        dfHisUniTaskItemAlarm.setRequestId(itemPoint.getRequestId());
        dfHisUniTaskItemAlarm.setTaskPatrolledId(itemPoint.getTaskPatrolledId());
        dfHisUniTaskItemAlarm.setPointCode(itemPoint.getPointCode());
        dfHisUniTaskItemAlarm.setPatroldeviceCode(itemPoint.getPatroldeviceCode());
        dfHisUniTaskItemAlarm.setAlarmTime(new Date());
        dfHisUniTaskItemAlarm.setAlarmLevel(alarmLevel);

        Integer recognitionType = itemPoint.getRecognitionType();
        if (recognitionType == 1) {//表计读取
            dfHisUniTaskItemAlarm.setAlarmType(7);
        } else if (recognitionType == 2) {//位置状态识别
            dfHisUniTaskItemAlarm.setAlarmType(10);
        } else if (recognitionType == 3) {//设备外观查看
            dfHisUniTaskItemAlarm.setAlarmType(6);
        } else if (recognitionType == 4) {//红外测温
            dfHisUniTaskItemAlarm.setAlarmType(1);
        } else if (recognitionType == 5) {//声音检测
            dfHisUniTaskItemAlarm.setAlarmType(5);
        } else if (recognitionType == 6) {//闪烁
            dfHisUniTaskItemAlarm.setAlarmType(10);
        } else {
            //其他对应不上,针对国网测试不便给空
            dfHisUniTaskItemAlarm.setAlarmType(7);
        }
        dfHisUniTaskItemAlarm.setRecognitionType(itemPoint.getRecognitionType());
        dfHisUniTaskItemAlarm.setValue(itemPoint.getPointVal());
        dfHisUniTaskItemAlarm.setValueUnit(itemPoint.getPointValUnit());
        dfHisUniTaskItemAlarm.setUnit(itemPoint.getPointUnit());
        dfHisUniTaskItemAlarm.setPlanNo(itemPoint.getTaskCode());
        dfHisUniTaskItemAlarm.setTaskCode(itemPoint.getTaskCode());
        baseMapper.insertDfHisUniTaskItemAlarm(dfHisUniTaskItemAlarm);
    }

    @Override
    public void confirmAll(String taskPatrolledId) {
        baseMapper.confirmAll(taskPatrolledId);
    }

    /**
     * 告警确认"taskPatrolledId", "pointCode", "confirmResult"， "alarmConfirmComment";
     *
     * @param params
     */
    @Override
    public void confirmAlarm(HisPointsHandleDTO params) {
        baseMapper.confirmAlarm(params);
    }
}
