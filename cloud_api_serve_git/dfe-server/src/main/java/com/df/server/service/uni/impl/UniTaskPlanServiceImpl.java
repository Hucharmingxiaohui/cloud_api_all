package com.df.server.service.uni.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.df.framework.utils.DateUtils;
import com.df.framework.utils.PageUtils;
import com.df.framework.vo.PageVO;
import com.df.server.dto.UniTaskPlan.UniTaskPlanPageDTO;
import com.df.server.dto.UniTaskPlan.UniTaskPlanParamsDTO;
import com.df.server.entity.his.HisUniTaskEntity;
import com.df.server.entity.uni.UniTaskPlanEntity;
import com.df.server.mapper.uni.UniTaskPlanMapper;
import com.df.server.service.uni.UniPatrolSystemService;
import com.df.server.service.uni.UniTaskPlanItemPointService;
import com.df.server.service.uni.UniTaskPlanService;
import com.df.server.vo.UniTaskPlan.UniTaskPlanVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.List;

@Slf4j
@Service("uniTaskPlanService")
public class UniTaskPlanServiceImpl extends ServiceImpl<UniTaskPlanMapper, UniTaskPlanEntity> implements UniTaskPlanService {

    @Autowired
    private UniTaskPlanItemPointService uniTaskPlanItemPointService;
    @Autowired
    private UniPatrolSystemService uniPatrolSystemService;


    // 自定义分页查询
    @Override
    public PageVO<UniTaskPlanVO> customPage(UniTaskPlanPageDTO pageDTO) {
        List<UniTaskPlanVO> list = baseMapper.getPageList(pageDTO);
        for (UniTaskPlanVO item : list) {
            setDescription(item);
        }
        Integer total = baseMapper.getPageTotal(pageDTO);
        return PageUtils.returnPageVOExtend(pageDTO, list, total);
    }

    private void setDescription(UniTaskPlanVO item) {
        StringBuilder description = new StringBuilder();
        if (item.getFixedStartTime() != null) {
            String fixedStartTime = DateUtils.parseDateToStr(item.getFixedStartTime());
            if (fixedStartTime.endsWith(".0")) {
                fixedStartTime = fixedStartTime.replace(".0", "");
            }
            description.append(fixedStartTime);
        } else if (StringUtils.isNotBlank(item.getCycleMonth())) {
            description.append("每");
            description.append(item.getCycleMonth());
            description.append("月 ");
            if (StringUtils.isNotBlank(item.getCycleWeek())) {
                description.append("周");
                description.append(item.getCycleWeek());
            } else if (item.getCycleDay() != null) {
                description.append("日");
                description.append(item.getCycleDay());
            }
            if (item.getCycleExecuteTime() != null) {
                description.append(" ");
                description.append(DateUtils.parseDateToStr(item.getCycleExecuteTime(), DateUtils.T_TIME));
            }
        }
        if (StringUtils.isNotBlank(item.getIntervalType())) {
            description = new StringBuilder("每间隔");
            if (StringUtils.isNotBlank(item.getIntervalNumber())) {
                description.append(item.getIntervalNumber());
            }
            int intervalType = Integer.valueOf(item.getIntervalType());
            if (intervalType == 1) {
                description.append("小时 ");
            } else if (intervalType == 2) {
                description.append("天 ");
            }
            if (item.getIntervalExecuteTime() != null) {
                description.append(DateUtils.parseDateToStr(item.getIntervalExecuteTime(), DateUtils.T_TIME));
            }
        }
        item.setDescription(description.toString());
    }

    @Override
    public void updatePlan(UniTaskPlanEntity entity) {
        String subCode = entity.getSubCode();
        String planNo = entity.getPlanNo();
        UniTaskPlanEntity plan = this.getById(entity.getId());
        this.removeById(plan.getId());
        baseMapper.insert(entity);
        Integer deviceLevel = entity.getDeviceLevel();
        String deviceList = entity.getDeviceList();
        uniTaskPlanItemPointService.saveNewPlanPoints(subCode, planNo, deviceLevel, deviceList);
    }

    @Override
    public UniTaskPlanVO getInfoByParams(UniTaskPlanParamsDTO paramsDTO) {
        UniTaskPlanVO uniTaskPlanVO = baseMapper.getInfoByParams(paramsDTO);
        setDescription(uniTaskPlanVO);
        return uniTaskPlanVO;
    }

    @Override
    public void deleteByParams(UniTaskPlanParamsDTO paramsDTO) {
        baseMapper.deleteByParams(paramsDTO);
        uniTaskPlanItemPointService.deleteByParams(paramsDTO);
    }

    @Override
    public void enableByPlanNo(UniTaskPlanParamsDTO paramsDTO) {
        baseMapper.enableByPlanNo(paramsDTO);
    }

    @Override
    public void auditByPlanNo(UniTaskPlanParamsDTO paramsDTO) {
        baseMapper.auditByPlanNo(paramsDTO);
    }

    @Override
    public List<UniTaskPlanEntity> getScheduledPlan() {
        return baseMapper.getScheduledPlan();
    }

    @Override
    public void updatePlanLastExecuteTime(String planNo, Date planStartTime) {
        baseMapper.updatePlanLastExecuteTime(planNo, planStartTime);
    }

    @Override
    public void saveNewPlan(UniTaskPlanEntity entity) {
        String subCode = entity.getSubCode();
        String planNo = subCode + "_" + DateUtils.getNowDateTimeStrMil();
        entity.setSysCode(uniPatrolSystemService.getVideoSysCode());
        entity.setPlanNo(planNo);
        entity.setPlanType("4");
        baseMapper.insert(entity);
        Integer deviceLevel = entity.getDeviceLevel();
        String deviceList = entity.getDeviceList();
        uniTaskPlanItemPointService.saveNewPlanPoints(subCode, planNo, deviceLevel, deviceList);
    }
}
