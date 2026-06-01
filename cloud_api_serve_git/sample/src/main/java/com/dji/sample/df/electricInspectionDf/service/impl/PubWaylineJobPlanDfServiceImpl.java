package com.dji.sample.df.electricInspectionDf.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.df.server.dto.JobPlan.JobPlanItemPointDTO;
import com.df.server.mapper.uni.UniPointMapper;
import com.dji.sample.center.utils.StringUtils;
import com.dji.sample.common.model.CustomClaim;
import com.dji.sample.component.oss.model.OssConfiguration;
import com.dji.sample.component.oss.service.impl.OssServiceContext;
import com.dji.sample.component.redis.RedisConst;
import com.dji.sample.component.redis.RedisOpsUtils;
import com.dji.sample.df.electricInspectionDf.dao.PubWaylineJobPlanDfMapper;
import com.dji.sample.df.electricInspectionDf.model.PubWaylineJobPlanDfEntity;
import com.dji.sample.df.electricInspectionDf.service.PubWaylineJobPlanDfService;
import com.dji.sample.df.waylineDf.dao.IWaylineFileMapperDf;
import com.dji.sample.df.waylineDf.model.entity.WaylineFileEntity;
import com.dji.sample.df.wind.config.FjFileConfig;
import com.dji.sample.df.wind.dao.DefectEntityMapper;
import com.dji.sample.df.wind.model.entity.DefectEntity;
import com.dji.sample.df.wind.service.RoutePlanService;
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

import javax.annotation.Resource;
import java.io.File;
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
    @Resource
    RoutePlanService routePlanService;
    @Autowired
    private OssServiceContext ossService;
    @Autowired
    private FjFileConfig fileConfig;
    @Autowired
    private DefectEntityMapper defectEntityMapper;


    //创建计划
    @Override
    public Map<String,Object> createWaylineJObPlan(PubWaylineJobPlanDfEntity pubWaylineJobPlanDfEntity) {
        Map map=new HashMap();
        Integer planType = pubWaylineJobPlanDfEntity.getPlanType();
        if(planType==1){
//          风机计划
            return routePlanService.buildFanWayline(pubWaylineJobPlanDfEntity);
        }else if(planType==2){
//          风机兴趣点环绕计划
            return routePlanService.buildInterestPointWayline(pubWaylineJobPlanDfEntity);
        }else if(planType==4) {
//          光伏计划（目前只是光伏区域）
            return routePlanService.buildSolarPanelWayline(pubWaylineJobPlanDfEntity);
        }else {
//          航点航线计划planType==0和普通航线计划planType==3
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
            pubWaylineJobPlanDfEntity.setWaylineType(0);
            //校验paln_id是否重复
            PubWaylineJobPlanDfEntity entity = pubWaylineJobPlanDfMapper.selectOne(new LambdaQueryWrapper<PubWaylineJobPlanDfEntity>().
                    eq(PubWaylineJobPlanDfEntity::getPlanId,pubWaylineJobPlanDfEntity.getPlanId()));
            if(entity!=null){//plan_id重复
                map.put("result",false);
                return map;
            }else{//plan_id不重复
                pubWaylineJobPlanDfMapper.insert(pubWaylineJobPlanDfEntity);
                map.put("result",true);
                map.put("plan",pubWaylineJobPlanDfEntity);
                return map;
            }
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

    @Override
    public PaginationData<PubWaylineJobPlanDfEntity> getPlanByPlantype(String plan_type, long page, long pageSize,Map map) {
        // 构建查询条件
        LambdaQueryWrapper<PubWaylineJobPlanDfEntity> queryWrapper = new LambdaQueryWrapper<PubWaylineJobPlanDfEntity>()
                .eq(PubWaylineJobPlanDfEntity::getPlanType, plan_type);

        // 动态添加 planId 条件
        if (map != null && map.containsKey("planId")) {
            String planId = map.get("planId").toString();
            if (StringUtils.isNotBlank(planId)) {
                queryWrapper.eq(PubWaylineJobPlanDfEntity::getPlanId, planId);
            }
        }

        // 动态添加 name 条件（模糊查询）
        if (map != null && map.containsKey("name")) {
            String name = map.get("name").toString();
            if (StringUtils.isNotBlank(name)) {
                queryWrapper.like(PubWaylineJobPlanDfEntity::getName, name);
            }
        }

        // 动态添加执行方式
        if (map != null && map.containsKey("taskType")) {
            String taskType = map.get("taskType").toString();
            if (StringUtils.isNotBlank(taskType)) {
                queryWrapper.eq(PubWaylineJobPlanDfEntity::getTaskType, taskType);
            }
        }

        // 排序
        queryWrapper.orderByDesc(PubWaylineJobPlanDfEntity::getId);

        // 执行分页查询
        Page<PubWaylineJobPlanDfEntity> pageData = pubWaylineJobPlanDfMapper.selectPage(
                new Page<PubWaylineJobPlanDfEntity>(page, pageSize),
                queryWrapper);

        List<PubWaylineJobPlanDfEntity> records = pageData.getRecords();
        return new PaginationData<PubWaylineJobPlanDfEntity>(records,
                new Pagination(pageData.getCurrent(), pageData.getSize(), pageData.getTotal()));
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

//        if(pubWaylineJobPlanDfEntity.getTaskType()!=0)
//        {
//            //取出出时间段
//            String days= pubWaylineJobPlanDfEntity.getTaskDays();//长度为21
//            Long day1=Long.parseLong(days.substring(0,9));
//            Long day2=Long.parseLong(days.substring(11,20));
//            if(day1.equals(day2))
//            {
//                task_days.add(day1);
//            }else {
//                for(Long i=day1;i<=day2;)
//                {
//                    task_days.add(i);
//                    i=i+86400;
//                }
//            }
//        }
//          param.setTaskDays(task_days);
//        //执行的时间段
//        List<List<Long>> task_periods=new ArrayList<>();//存储执行时间段
//        if(pubWaylineJobPlanDfEntity.getTaskType()!=0)
//        {   String taskPeriods= pubWaylineJobPlanDfEntity.getTaskPeriods();
//            for(int i=0;i<taskPeriods.length();)
//            {
//                Long time1=Long.parseLong(taskPeriods.substring(0+i,9+i));//开始时间
//                Long time2=Long.parseLong(taskPeriods.substring(11+i,20+i));//结束时间
//                List<Long> list=new ArrayList<>();//存储时间段
//                if(time1.equals(time2))
//                {
//                    list.add(time1);
//                    task_periods.add(list);
//                }else {
//                    list.add(time1);
//                    list.add(time2);
//                    task_periods.add(list);
//                }
//                i=i+22;
//            }
//
//        }
//        param.setTaskPeriods(task_periods);
        param.setPlanId(pubWaylineJobPlanDfEntity.getPlanId());
        param.setFanName(pubWaylineJobPlanDfEntity.getFanName());
        param.setBeginTime(pubWaylineJobPlanDfEntity.getBeginTime());
        System.out.println(param);

        //更新状态
        pubWaylineJobPlanDfMapper.updateById(pubWaylineJobPlanDfEntity);
        HttpResultResponse httpResultResponse = flightTaskService.publishFlightTask(param, customClaim, pubWaylineJobPlanDfEntity);
        return httpResultResponse;
    }

    @Override
    public HttpResultResponse cancelPlan(CustomClaim customClaim, WaylineJobEntity waylineJobEntity) throws SQLException {
        String workspaceId = waylineJobEntity.getWorkspaceId();
        String jobId = waylineJobEntity.getJobId();
        List list=new ArrayList();
        list.add(jobId);
        return flightTaskService.cancelFlightTask(workspaceId,list);
    }
//  删除此计划下所有任务的redis定时数据
    @Override
    public boolean deletePlanById(Integer id) {
        PubWaylineJobPlanDfEntity pubWaylineJobPlanDfEntity = pubWaylineJobPlanDfMapper.selectById(id);
        List<WaylineJobEntity> waylineJobEntities = waylineJobMapper.selectList(new LambdaQueryWrapper<WaylineJobEntity>().
                eq(WaylineJobEntity::getPlanId, pubWaylineJobPlanDfEntity.getPlanId()));
        if(!waylineJobEntities.isEmpty()){
            for (WaylineJobEntity waylineJob : waylineJobEntities) {
                RedisOpsUtils.zRemove(RedisConst.WAYLINE_JOB_TIMED_EXECUTE,RedisConst.WAYLINE_JOB_TIMED_EXECUTE,
                        waylineJob.getWorkspaceId() + RedisConst.DELIMITER + waylineJob.getDockSn() + RedisConst.DELIMITER + waylineJob.getJobId());
            }
        }
        int flag = pubWaylineJobPlanDfMapper.deleteById(id);
        if(flag>0){
            return true;
        }
        return false;
    }

    @Override
    public boolean batchDeletePlanByIds(List<Integer> ids) {
        for (Integer id : ids) {
            PubWaylineJobPlanDfEntity pubWaylineJobPlanDfEntity = pubWaylineJobPlanDfMapper.selectById(id);
            List<WaylineJobEntity> waylineJobEntities = waylineJobMapper.selectList(new LambdaQueryWrapper<WaylineJobEntity>().
                    eq(WaylineJobEntity::getPlanId, pubWaylineJobPlanDfEntity.getPlanId()));
            if(!waylineJobEntities.isEmpty()){
                for (WaylineJobEntity waylineJob : waylineJobEntities) {
                    RedisOpsUtils.zRemove(RedisConst.WAYLINE_JOB_TIMED_EXECUTE,RedisConst.WAYLINE_JOB_TIMED_EXECUTE,
                            waylineJob.getWorkspaceId() + RedisConst.DELIMITER + waylineJob.getDockSn() + RedisConst.DELIMITER + waylineJob.getJobId());
                }
            }
        }
        int flag = pubWaylineJobPlanDfMapper.deleteBatchIds(ids);
        if(flag>0){
            return true;
        }
        return false;
    }

    @Override
    public boolean deleteJobByBobId(String job_id) {
//      1.删除图片（数据库和minio)
        List<MediaFileEntity> mediaFileEntities = fileMapper.selectList(new LambdaQueryWrapper<MediaFileEntity>()
                .eq(MediaFileEntity::getJobId,job_id));
        if(mediaFileEntities.size()>0){
            fileMapper.delete(new LambdaQueryWrapper<MediaFileEntity>()
                    .eq(MediaFileEntity::getJobId,job_id));
//      删除minio数据
            for(MediaFileEntity mediaFileEntity : mediaFileEntities){
                ossService.deleteObject(OssConfiguration.bucket, mediaFileEntity.getObjectKey());
            }
        }
//      2.删除图片文件夹（服务器）
        WaylineJobEntity waylineJobEntity = waylineJobMapper.selectOne(
                new LambdaQueryWrapper<WaylineJobEntity>()
                        .eq(WaylineJobEntity::getJobId, job_id)
        );
//      3.删除报告（服务器）
        String filePictrueUrl = fileConfig.getFilePictrueUrl() + job_id;
        boolean isPicDeleted = deletePicFolder(filePictrueUrl);
        if(isPicDeleted){
            log.info("已成功删除图片");
        }
        String reportPath = fileConfig.getFileReportPath() + "/"+ waylineJobEntity.getName() +".docx";
        File reportFile = new File(reportPath);
        boolean isFileDeleted = deleteReportFile(reportFile, waylineJobEntity.getName());
        if(isFileDeleted){
            log.info("已成功删除报告");
        }
//      4.删除缺陷数据
        defectEntityMapper.delete(new LambdaQueryWrapper<DefectEntity>()
                .eq(DefectEntity::getJobId,job_id));
//      5.删除任务
        int flag = waylineJobMapper.delete(new LambdaQueryWrapper<WaylineJobEntity>()
                .eq(WaylineJobEntity::getJobId,job_id));
        if(flag>0){
            return true;
        }
        return false;
    }

    /**
     * 删除报告文件
     */
    private boolean deleteReportFile(File reportFile, String fileName) {
        if (reportFile.exists()) {
            if (reportFile.delete()) {
                log.info("报告文件删除成功: {}", fileName);
                return true;
            } else {
                log.error("报告文件删除失败: {}", fileName);
                return false;
            }
        } else {
            log.warn("报告文件不存在: {}", fileName);
            return true; // 文件不存在也算删除成功
        }
    }

    /**
     * 删除图片文件夹及其所有内容
     * @param reportPath 文件夹路径
     * @return 删除成功返回true，否则返回false
     */
    public static boolean deletePicFolder(String reportPath) {
        if (reportPath == null || reportPath.trim().isEmpty()) {
            return false;
        }

        File folder = new File(reportPath);

        // 如果文件夹不存在，直接返回true
        if (!folder.exists()) {
            return true;
        }

        // 检查是否是文件夹
        if (!folder.isDirectory()) {
            System.err.println("指定的路径不是文件夹: " + reportPath);
            return false;
        }

        try {
            return deleteFolderRecursive(folder);
        } catch (Exception e) {
            System.err.println("删除文件夹失败: " + reportPath);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 递归删除文件夹
     */
    private static boolean deleteFolderRecursive(File folder) {
        if (folder == null) {
            return false;
        }

        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // 递归删除子目录
                    deleteFolderRecursive(file);
                } else {
                    // 删除文件
                    boolean deleted = file.delete();
                    if (!deleted) {
                        System.err.println("删除文件失败: " + file.getAbsolutePath());
                    }
                }
            }
        }

        // 删除空文件夹
        return folder.delete();
    }

}
