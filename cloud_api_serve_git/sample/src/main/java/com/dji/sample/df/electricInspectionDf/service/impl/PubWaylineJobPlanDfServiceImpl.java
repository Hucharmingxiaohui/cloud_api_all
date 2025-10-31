package com.dji.sample.df.electricInspectionDf.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.df.server.dto.JobPlan.JobPlanItemPointDTO;
import com.df.server.mapper.uni.UniPointMapper;
import com.dji.sample.common.model.CustomClaim;
import com.dji.sample.df.electricInspectionDf.dao.PubWaylineJobPlanDfMapper;
import com.dji.sample.df.electricInspectionDf.model.PubWaylineJobPlanDfEntity;
import com.dji.sample.df.electricInspectionDf.service.PubWaylineJobPlanDfService;
import com.dji.sample.df.waylineDf.dao.IWaylineFileMapperDf;
import com.dji.sample.df.waylineDf.model.entity.WaylineFileEntity;
import com.dji.sample.media.dao.IFileMapper;
import com.dji.sample.media.model.MediaFileEntity;
import com.dji.sample.wayline.dao.IWaylineJobMapper;
import com.dji.sample.wayline.model.entity.WaylineJobEntity;
import com.dji.sample.wayline.model.param.CreateJobParam;
import com.dji.sample.wayline.service.IFlightTaskService;
import com.dji.sdk.cloudapi.wayline.OutOfControlActionEnum;
import com.dji.sdk.cloudapi.wayline.TaskTypeEnum;
import com.dji.sdk.cloudapi.wayline.WaylineTypeEnum;
import com.dji.sdk.common.HttpResultResponse;
import com.dji.sdk.common.Pagination;
import com.dji.sdk.common.PaginationData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PubWaylineJobPlanDfServiceImpl implements PubWaylineJobPlanDfService {
    @Autowired
    private PubWaylineJobPlanDfMapper pubWaylineJobPlanDfMapper;
    @Autowired
    private IFileMapper fileMapper;
    @Autowired
    private IWaylineJobMapper waylineJobMapper;
    //创建计划
    @Autowired
    private IFlightTaskService flightTaskService;
    @Autowired
    private UniPointMapper uniPointMapper;
    @Autowired
    private IWaylineFileMapperDf waylineFileMapperDf;

    //创建计划
    @Override
    public boolean createWaylineJObPlan(PubWaylineJobPlanDfEntity pubWaylineJobPlanDfEntity) {
        //创建计划接口
        pubWaylineJobPlanDfEntity.setPlanId(UUID.randomUUID().toString());
        // 获取当前系统时间戳（以毫秒为单位）
        long currentTimeMillis = System.currentTimeMillis();

        //如果是立即执行任务，添加begin_time
        if(pubWaylineJobPlanDfEntity.getTaskType()==0){
            pubWaylineJobPlanDfEntity.setBeginTime(currentTimeMillis);
        }
        pubWaylineJobPlanDfEntity.setCreateTime(currentTimeMillis);
        pubWaylineJobPlanDfEntity.setUpdateTime(currentTimeMillis);
        //校验paln_id是否重复
        PubWaylineJobPlanDfEntity entity = pubWaylineJobPlanDfMapper.selectOne(new LambdaQueryWrapper<PubWaylineJobPlanDfEntity>().
                eq(PubWaylineJobPlanDfEntity::getPlanId,pubWaylineJobPlanDfEntity.getPlanId()));
        if(entity!=null){//plan_id重复
            return false;
        }else{//plan_id不重复
            pubWaylineJobPlanDfMapper.insert(pubWaylineJobPlanDfEntity);
            return true;
        }
    }

    @Override
    public Map<Integer,String> getWaylineByPoint(Integer deviceLevel, String deviceListStr) {
        List<String> deviceList = Arrays.asList(deviceListStr.split(","));
        List<JobPlanItemPointDTO> jobPlanItemPointDTOS = uniPointMapper.listPlanPoint2(deviceLevel, deviceList);
        Map<Integer, List<JobPlanItemPointDTO>> collect = jobPlanItemPointDTOS.stream()
                .sorted(Comparator.comparing(JobPlanItemPointDTO::getWaylinePos))
                .collect(Collectors.groupingBy(JobPlanItemPointDTO::getWaylinePos));
        Map<Integer,String> map = new HashMap();
        for (Map.Entry<Integer, List<JobPlanItemPointDTO>> entry : collect.entrySet()) {
            List<JobPlanItemPointDTO> value = entry.getValue();
            String waylinePointPosStr ="";
            for (JobPlanItemPointDTO jobPlanItemPointDTO : value) {
                waylinePointPosStr +=jobPlanItemPointDTO.getWaylinePointPos();
                waylinePointPosStr +=",";
            }
            waylinePointPosStr = waylinePointPosStr.substring(0, waylinePointPosStr.length() - 1);
            map.put(entry.getKey(), waylinePointPosStr);
        }
        return map;
    }

    @Override
    public Map<String, String> getWaylineIdByPos(Map<Integer, String> waylineByPoint) {
        List<WaylineFileEntity> waylineFileEntities = waylineFileMapperDf.selectList(new LambdaQueryWrapper<WaylineFileEntity>()
                .in(WaylineFileEntity::getWaylinePos, waylineByPoint.keySet())
                .orderByAsc(WaylineFileEntity::getWaylinePos));
        List<String> waylineIdList = new ArrayList<>();
        for (WaylineFileEntity waylineFileEntity : waylineFileEntities) {
            waylineIdList.add(waylineFileEntity.getWaylineId());
        }
        List<Integer> sortedKeys = new ArrayList<>(waylineByPoint.keySet());
        Collections.sort(sortedKeys);
        Map<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i < waylineIdList.size(); i++) {
            String value = waylineByPoint.get(sortedKeys.get(i));
            result.put(waylineIdList.get(i), value);
        }
        return result;
    }

    //按场站id查询
//    @Override
//    public List<PubWaylineJobPlanDfEntity> getPlanBySubCode(String sub_code) {
//        List<PubWaylineJobPlanDfEntity> pubWaylineJobPlanDfEntityList = pubWaylineJobPlanDfMapper.selectList(new LambdaQueryWrapper<PubWaylineJobPlanDfEntity>()
//                .eq(PubWaylineJobPlanDfEntity::getSubCode,sub_code));
//
//        return pubWaylineJobPlanDfEntityList;
//    }
    @Override
    public PaginationData<PubWaylineJobPlanDfEntity> getPlanBySubCode(String sub_code, long page, long pageSize) {
        Page<PubWaylineJobPlanDfEntity> pageData = pubWaylineJobPlanDfMapper.selectPage(
                new Page<PubWaylineJobPlanDfEntity>(page, pageSize),
                new LambdaQueryWrapper<PubWaylineJobPlanDfEntity>()
                        .eq(PubWaylineJobPlanDfEntity::getSubCode, sub_code)
                        .orderByDesc(PubWaylineJobPlanDfEntity::getId));
        List<PubWaylineJobPlanDfEntity> records = pageData.getRecords();
        return new PaginationData<PubWaylineJobPlanDfEntity>(records, new Pagination(pageData.getCurrent(), pageData.getSize(), pageData.getTotal()));
    }
    //执行任务
    @Override
    public HttpResultResponse expressPlan(CustomClaim customClaim,  PubWaylineJobPlanDfEntity pubWaylineJobPlanDfEntity) throws SQLException {
        CreateJobParam param =new CreateJobParam();

        //设置param参数
        //任务名称
        param.setName(pubWaylineJobPlanDfEntity.getName());
        //航线id
        param.setFileId(pubWaylineJobPlanDfEntity.getFileId());
        //机场sn
        param.setDockSn(pubWaylineJobPlanDfEntity.getDockSn());
        //航线类型,初始化为航点航线
        WaylineTypeEnum waylineTypeEnum=WaylineTypeEnum.WAYPOINT;
        if(pubWaylineJobPlanDfEntity.getWaylineType()==0){
            waylineTypeEnum=WaylineTypeEnum.WAYPOINT;
        } else if (pubWaylineJobPlanDfEntity.getWaylineType()==1) {
            waylineTypeEnum=WaylineTypeEnum.MAPPING_2D;
        } else if (pubWaylineJobPlanDfEntity.getWaylineType()==2) {
            waylineTypeEnum=WaylineTypeEnum.MAPPING_3D;
        } else if (pubWaylineJobPlanDfEntity.getWaylineType()==3) {
            waylineTypeEnum=WaylineTypeEnum.MAPPING_STRIP;
        }
        param.setWaylineType(waylineTypeEnum);

        //任务类型
        TaskTypeEnum taskType = TaskTypeEnum.IMMEDIATE;
        if(pubWaylineJobPlanDfEntity.getTaskType()==0){
            taskType=TaskTypeEnum.IMMEDIATE;
        } else if (pubWaylineJobPlanDfEntity.getTaskType()==1) {
            taskType=TaskTypeEnum.TIMED;
        } else if (pubWaylineJobPlanDfEntity.getTaskType()==2) {
            taskType=TaskTypeEnum.CONDITIONAL;
        }
        param.setTaskType(taskType);

        //返航高度
        param.setRthAltitude(pubWaylineJobPlanDfEntity.getRthAltitude());
        //失控动作
        OutOfControlActionEnum outOfControlAction=OutOfControlActionEnum.HOVERING;
        if(pubWaylineJobPlanDfEntity.getOutOfControl()==0){
            outOfControlAction=OutOfControlActionEnum.RETURN_TO_HOME;
        } else if (pubWaylineJobPlanDfEntity.getOutOfControl()==1) {
            outOfControlAction=OutOfControlActionEnum.HOVERING;
        } else if (pubWaylineJobPlanDfEntity.getOutOfControl()==2) {
            outOfControlAction=OutOfControlActionEnum.LANDING;
        }
        param.setOutOfControlAction(outOfControlAction);
        //条件任务特殊处理
        if(pubWaylineJobPlanDfEntity.getTaskType()==2)
        {
            param.setMinBatteryCapacity(pubWaylineJobPlanDfEntity.getMinBatteryCapacity());//执行任务的最小电量
            param.setMinStorageCapacity(pubWaylineJobPlanDfEntity.getMinStorageCapacity());//执行任务的最小存储量
        }else {
            //最小电量
            param.setMinBatteryCapacity(50);
            //最小存储量
            param.setMinStorageCapacity(null);
        }
        //执行的日期
        List<Long> task_days=new ArrayList<>();//存储执行日期

        if(pubWaylineJobPlanDfEntity.getTaskType()!=0)
        {
            //取出出时间段
            String days= pubWaylineJobPlanDfEntity.getTaskDays();//长度为21
            Long day1=Long.parseLong(days.substring(0,9));
            Long day2=Long.parseLong(days.substring(11,20));
            if(day1.equals(day2))
            {
                task_days.add(day1);
            }else {
                for(Long i=day1;i<=day2;)
                {
                    task_days.add(i);
                    i=i+86400;
                }
            }
        }
          param.setTaskDays(task_days);
        //执行的时间段
        List<List<Long>> task_periods=new ArrayList<>();//存储执行时间段
        if(pubWaylineJobPlanDfEntity.getTaskType()!=0)
        {   String taskPeriods= pubWaylineJobPlanDfEntity.getTaskPeriods();
            for(int i=0;i<taskPeriods.length();)
            {
                Long time1=Long.parseLong(taskPeriods.substring(0+i,9+i));//开始时间
                Long time2=Long.parseLong(taskPeriods.substring(11+i,20+i));//结束时间
                List<Long> list=new ArrayList<>();//存储时间段
                if(time1.equals(time2))
                {
                    list.add(time1);
                    task_periods.add(list);
                }else {
                    list.add(time1);
                    list.add(time2);
                    task_periods.add(list);
                }
                i=i+22;
            }

        }
        param.setTaskPeriods(task_periods);
        param.setPlanId(pubWaylineJobPlanDfEntity.getPlanId());
        param.setFanName(pubWaylineJobPlanDfEntity.getFanName());

        System.out.println(param);
        System.out.println("kkkkk");
        //更新状态
        pubWaylineJobPlanDfMapper.updateById(pubWaylineJobPlanDfEntity);
        return flightTaskService.publishFlightTask(param,customClaim, pubWaylineJobPlanDfEntity);
    }

    @Override
    public HttpResultResponse cancelPlan(CustomClaim customClaim, WaylineJobEntity waylineJobEntity) throws SQLException {
        String workspaceId = waylineJobEntity.getWorkspaceId();
        String jobId = waylineJobEntity.getJobId();
        List list=new ArrayList();
        list.add(jobId);
        return flightTaskService.cancelFlightTask(workspaceId,list);
    }

    @Override
    public boolean deletePlanById(Integer id) {
        int flag = pubWaylineJobPlanDfMapper.deleteById(id);
        if(flag>0){
            return true;
        }
        return false;
    }

    @Override
    public boolean deleteJobByBobId(String job_id) {
        //1.删除图片
        List<MediaFileEntity> mediaFileEntities = fileMapper.selectList(new LambdaQueryWrapper<MediaFileEntity>()
                .eq(MediaFileEntity::getJobId,job_id));
        if(mediaFileEntities.size()>0){
            fileMapper.delete(new LambdaQueryWrapper<MediaFileEntity>()
                    .eq(MediaFileEntity::getJobId,job_id));
        }
        //2.删除任务
        int flag = waylineJobMapper.delete(new LambdaQueryWrapper<WaylineJobEntity>()
                .eq(WaylineJobEntity::getJobId,job_id));
        if(flag>0){
            return true;
        }
        return false;
    }


}
