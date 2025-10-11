package com.df.server.service.his.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.df.framework.utils.PageUtils;
import com.df.server.dto.HisUniTask.ConfirmHisTaskReportParams;
import com.df.server.entity.his.HisUniTaskReportEntity;
import com.df.server.mapper.his.HisUniTaskReportMapper;
import com.df.server.service.his.HisUniTaskReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Slf4j
@Service("hisUniTaskReportService")
public class HisUniTaskReportServiceImpl implements HisUniTaskReportService {

    @Autowired
    private HisUniTaskReportMapper baseMapper;

    @Override
    public Integer createNewReport(ConfirmHisTaskReportParams params) {
        baseMapper.deleteByTaskPatrolledId(params.getTaskPatrolledId());
        HisUniTaskReportEntity reportEntity = new HisUniTaskReportEntity();
        reportEntity.setTaskPatrolledId(params.getTaskPatrolledId());
        reportEntity.setReportStatus(2);
        reportEntity.setReportStartTime(new Date());
        baseMapper.insert(reportEntity);
        return reportEntity.getId();
    }

    @Override
    public HisUniTaskReportEntity getByTaskId(String taskPatrolledId) {
        return baseMapper.getByTaskId(taskPatrolledId);
    }
}
