package com.dji.sample.df.supControlDf.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dji.sample.center.utils.TaskUtils;
import com.dji.sample.center.v2022.command.upload.UavCoordinateItem;
import com.dji.sample.center.v2022.command.upload.UavDeviceAlarmItem;
import com.dji.sample.center.v2022.command.upload.UavPatrolLineItem;
import com.dji.sample.center.v2022.handler.CenterMsgPushHandler;
import com.dji.sample.common.model.CustomClaim;
import com.dji.sample.common.util.SpringBeanUtilsTest;
import com.dji.sample.df.electricInspectionDf.dao.CenterToUavPlanDfMapper;
import com.dji.sample.df.electricInspectionDf.dao.PubSubstationDfMapper;
import com.dji.sample.df.electricInspectionDf.model.PubSubstationDfEntity;
import com.dji.sample.df.electricInspectionDf.model.PubWaylineJobPlanDfEntity;
import com.dji.sample.df.electricInspectionDf.service.PubWaylineJobPlanDfService;
import com.dji.sample.df.mediaDf.model.MediaFileEntity;
import com.dji.sample.manage.dao.IDeviceHmsMapper;
import com.dji.sample.manage.dao.IDeviceMapper;
import com.dji.sample.manage.dao.IUserMapper;
import com.dji.sample.manage.model.entity.DeviceEntity;
import com.dji.sample.manage.model.entity.DeviceHmsEntity;
import com.dji.sample.manage.model.entity.UserEntity;
import com.dji.sample.manage.service.IDeviceRedisService;
import com.dji.sample.df.mediaDf.dao.IFileMapperDf;
import com.dji.sample.df.mediaDf.service.IFileServiceDf;
import com.dji.sample.df.supControlDf.entity.CenterToUavPlanDfEntity;
import com.dji.sample.df.supControlDf.service.TaskcontrolService;
import com.dji.sample.df.thirdKmzDf.entity.pointResult.PointCount.PointCount;
import com.dji.sample.df.thirdKmzDf.entity.pointResult.PointResult;
import com.dji.sample.df.thirdKmzDf.entity.wayline.Folder.Placemark.Placemark;
import com.dji.sample.df.thirdKmzDf.entity.wayline.Wayline;
import com.dji.sample.df.thirdKmzDf.service.WaylineKmzThirdService;
import com.dji.sample.wayline.dao.IWaylineJobMapper;
import com.dji.sample.wayline.model.entity.WaylineJobEntity;
import com.dji.sample.wayline.model.enums.WaylineTaskStatusEnum;
import com.dji.sample.wayline.model.param.CreateJobParam;
import com.dji.sample.wayline.model.param.UpdateJobParam;
import com.dji.sample.wayline.service.IFlightTaskService;
import com.dji.sample.wayline.service.IWaylineFileService;
import com.dji.sdk.cloudapi.device.OsdDockDrone;
import com.dji.sdk.cloudapi.wayline.OutOfControlActionEnum;
import com.dji.sdk.cloudapi.wayline.TaskTypeEnum;
import com.dji.sdk.cloudapi.wayline.WaylineTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
//任务控制，机场2
public class TaskcontrolServiceImpl implements TaskcontrolService {
    @Autowired
    private IWaylineJobMapper waylineJobMapper;
    @Autowired
    private IFlightTaskService flightTaskService;
    //上级创建计划并执行
    @Autowired
    private PubSubstationDfMapper pubSubstationDfMapper;
    @Autowired
    private IFileServiceDf fileService;
    @Autowired
    private IFileMapperDf mediaMapperDf;
    @Autowired
    private IDeviceMapper deviceMapper;
    @Autowired
    private PubWaylineJobPlanDfService pubWaylineJobPlanDfService;
    @Autowired
    private IDeviceRedisService deviceRedisService;
    @Autowired
    private IUserMapper iUserMapper;
    //任务状态上报
    @Autowired
    private CenterMsgPushHandler centerMsgPushHandler;
    //定时配置，
    //总的最大查询次数
    private static final int MAX_ATTEMPTS = 240; // 2 hours / 3 minutes = 40 attempts
    //线程bolean
    private AtomicBoolean found = new AtomicBoolean(false);

    //获取文件夹路径
    @Value("${singlePointUrl}")
    private String singlePointUrl;
    @Autowired
    private WaylineKmzThirdService waylineKmzService;//kmz服务
    @Autowired
    private IWaylineFileService waylineFileService;
    @Autowired
    private IDeviceHmsMapper iDeviceHmsMapper;
    @Autowired
    private CenterToUavPlanDfMapper centerToUavPlanDfMapper;
    //上级创建飞行计划并执行
    /*
    * 逻辑：上级下发计划后开始执行，生成任务，最终结果有失败、成功两种
    * */
    @Override
    public String SuperiorCreatePlanJob(PubWaylineJobPlanDfEntity pubWaylineJobPlanDfEntity) throws SQLException {
        //center计划表信息
        CenterToUavPlanDfEntity centerToUavPlanDfEntity=new CenterToUavPlanDfEntity();
        centerToUavPlanDfEntity.setSubCode(pubWaylineJobPlanDfEntity.getSubCode());
        centerToUavPlanDfEntity.setCenterPlanCode(pubWaylineJobPlanDfEntity.getPlanId());
        String centerTaskPatrolledId= TaskUtils.genTaskPatrolledId(pubWaylineJobPlanDfEntity.getPlanId());
        centerToUavPlanDfEntity.setCenterTaskPatrolledId(centerTaskPatrolledId);
        centerToUavPlanDfEntity.setWayline(pubWaylineJobPlanDfEntity.getFileId());
        // 获取当前日期和时间
        LocalDateTime currentDateTime = LocalDateTime.now();
        centerToUavPlanDfEntity.setStartTime(currentDateTime);

        //查询变电站给上级系统匹配执行任务的无人机，只设置了一个，没有直接返回false
        PubSubstationDfEntity pubSubstationDfEntity=pubSubstationDfMapper.selectOne(new LambdaQueryWrapper<PubSubstationDfEntity>()
                .eq(PubSubstationDfEntity::getSubCode,pubWaylineJobPlanDfEntity.getSubCode()));
        if(pubWaylineJobPlanDfEntity==null){
            //没有设置机场不执行，直接返回字符串"false"
            //调用任务执行状态上报，失败 0%
            UUID uuid=UUID.randomUUID();//随机生成一个uuid当做一个失败的任务id
            centerToUavPlanDfEntity.setUavPlanCode(uuid.toString());
            centerToUavPlanDfEntity.setStatus(4);
            //存入center计划表
            centerToUavPlanDfMapper.insert(centerToUavPlanDfEntity);
            //上报状态
            centerMsgPushHandler.pushPatrolStatusOther(uuid.toString(),
                    pubWaylineJobPlanDfEntity.getName(),
                    "4",
                    "0");
            return "false";
        }
        //设置巡检的机场
        pubWaylineJobPlanDfEntity.setDockSn(pubWaylineJobPlanDfEntity.getDockSn());
        //  查询是否在线

        //没有在线的设备
        if(! deviceRedisService.checkDeviceOnline(pubWaylineJobPlanDfEntity.getDockSn())){
            //没有在线的设备不执行
            UUID uuid=UUID.randomUUID();//随机生成一个uuid当做一个失败的任务id
            centerToUavPlanDfEntity.setUavPlanCode(uuid.toString());
            centerToUavPlanDfEntity.setStatus(4);
            //存入center计划表
            centerToUavPlanDfMapper.insert(centerToUavPlanDfEntity);
            //上报状态
            centerMsgPushHandler.pushPatrolStatusOther(uuid.toString(),
                    pubWaylineJobPlanDfEntity.getName(),
                    "4",
                    "0");
            return "false";
        }
        //有在线的设备
        //专业:场景后续调整
        pubWaylineJobPlanDfEntity.setMajor("变电");
        //来源
        pubWaylineJobPlanDfEntity.setPlanSource("上级下发");
        //工作空间id
        PubSubstationDfEntity pubSubstationDf=pubSubstationDfMapper.selectOne(new LambdaQueryWrapper<PubSubstationDfEntity>()
                .eq(PubSubstationDfEntity::getSubCode,pubWaylineJobPlanDfEntity.getSubCode()));
        pubWaylineJobPlanDfEntity.setWorkspaceId(pubSubstationDf.getWorkspaceId());
        //航线类型 航点航线
        pubWaylineJobPlanDfEntity.setWaylineType(0);
        //任务类型 立即执行
        pubWaylineJobPlanDfEntity.setTaskType(0);
        //返航高度 30m
        pubWaylineJobPlanDfEntity.setRthAltitude(100);
        //失控动作 1悬停
        pubWaylineJobPlanDfEntity.setOutOfControl(1);
        //启用状态 默认启用
        pubWaylineJobPlanDfEntity.setEnableStatus(1);
        //设置优先级 4最高
        pubWaylineJobPlanDfEntity.setPlanPriority(4);
        // 获取当前系统时间戳（以毫秒为单位）
        long currentTimeMillis = System.currentTimeMillis();
        //如果是立即执行任务，添加begin_time
        if(pubWaylineJobPlanDfEntity.getTaskType()==0){
            pubWaylineJobPlanDfEntity.setBeginTime(currentTimeMillis);
        }
        pubWaylineJobPlanDfEntity.setCreateTime(currentTimeMillis);
        pubWaylineJobPlanDfEntity.setUpdateTime(currentTimeMillis);

        //存入计划表
        pubWaylineJobPlanDfService.createWaylineJObPlan(pubWaylineJobPlanDfEntity);

        //下发任务
        //创建用户信息
        CreateJobParam param =new CreateJobParam();
        CustomClaim customClaim = new CustomClaim();
        customClaim.setId(pubSubstationDf.getUserId());
        //获取用户名称
        UserEntity userEntity=iUserMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUserId,pubSubstationDfEntity.getUserId()));
        if(userEntity!=null)
        {
            customClaim.setUsername(userEntity.getUsername());
        }

        customClaim.setWorkspaceId(pubSubstationDf.getWorkspaceId());
        customClaim.setUserType(1);

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
        //执行任务的日期
        if(pubWaylineJobPlanDfEntity.getTaskType()==0){
            //如果是立即执行任务
            //最小电量
            param.setMinBatteryCapacity(50);
            //最小存储量
            param.setMinStorageCapacity(null);
            //执行的日期
            List<Long> longList = new ArrayList<>();
            longList.add(System.currentTimeMillis());
            param.setTaskDays(longList);
            //执行的时间段
            List<List<Long>> taskPeriods =new ArrayList<>();
            param.setTaskPeriods(taskPeriods);
        }
        //更新计划状态
        pubWaylineJobPlanDfService.expressPlan(customClaim,pubWaylineJobPlanDfEntity);
        //计划变成任务
        String job_id =flightTaskService.publishFlightTask(param,customClaim,pubWaylineJobPlanDfEntity).getMessage();
        if (job_id.length()==36){
            //启动定时上报程序
            this.timedQueryByJobId(job_id,pubSubstationDf.getWorkspaceId(), pubWaylineJobPlanDfEntity.getFileId(), pubWaylineJobPlanDfEntity.getSubCode());
            //上报路线
            DeviceEntity device=deviceMapper.selectOne(new LambdaQueryWrapper<DeviceEntity>()
                    .eq(DeviceEntity::getDeviceSn,pubSubstationDfEntity.getDroneSn()));
            try {
                //尝试解析路线并上报
                upLoadUavLine(device.getDeviceName(),pubSubstationDfEntity.getDroneSn(),
                        pubWaylineJobPlanDfEntity.getFileId(),
                        pubSubstationDf.getWorkspaceId());
            }catch (Exception e){

            }
            //存入center计划表
            centerToUavPlanDfEntity.setUavPlanCode(job_id);
            centerToUavPlanDfEntity.setStatus(4);
            centerToUavPlanDfMapper.insert(centerToUavPlanDfEntity);
            //任务正在执行
            centerMsgPushHandler.pushPatrolStatusOther(pubWaylineJobPlanDfEntity.getPlanId(),
                    pubWaylineJobPlanDfEntity.getName(),
                    "2",
                    "5");
            return job_id;
        }
        //没有生成任务id,任务执行失败
        UUID uuid=UUID.randomUUID();//随机生成一个uuid当做一个失败的任务id
        //存入center计划表
        centerToUavPlanDfEntity.setUavPlanCode(uuid.toString());
        centerToUavPlanDfEntity.setStatus(4);
        centerToUavPlanDfMapper.insert(centerToUavPlanDfEntity);
        //上报状态
        centerMsgPushHandler.pushPatrolStatusOther(uuid.toString(),
                pubWaylineJobPlanDfEntity.getName(),
                "4",
                "5");
        return "false";
    }

    /***
     *
     * @param job_id
     *
     */
    @Override
    public PointResult getResultByJobId(String job_id) throws Exception {
        WaylineJobEntity waylineJobEntity=waylineJobMapper.selectOne(new LambdaQueryWrapper<WaylineJobEntity>()
                .eq(WaylineJobEntity::getJobId,job_id));
        if(waylineJobEntity!=null){
            PointResult pointResult = fileService.getMediaDileByJobId(job_id,waylineJobEntity.getWorkspaceId(),waylineJobEntity.getFileId());
            return pointResult;
        }

        return null;
    }
    //定时查询结果上报给上级
    @Override
    public String timedQueryByJobId(String job_id,String workspace_id,String wayline_id,String sub_code) {
        // 创建一个单线程的ScheduledExecutorService
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        Long startTime = System.currentTimeMillis();
        //定时任务
        Runnable task = new Runnable() {
            int attempt = 0;

            @Override
            public void run() {
                if (attempt < MAX_ATTEMPTS && !found.get()) {
                    // 在这里执行查询任务的逻辑
                    //1.查wayline_job里的任务表看任务状态与图片数量
                    WaylineJobEntity waylineJobEntity=waylineJobMapper.selectOne(new LambdaQueryWrapper<WaylineJobEntity>()
                            .eq(WaylineJobEntity::getJobId,job_id));
                    //2.查媒体文件图片表
                    List<MediaFileEntity> mediaFileEntities= mediaMapperDf.selectList(new LambdaQueryWrapper<MediaFileEntity>()
                            .eq(MediaFileEntity::getJobId,job_id));

                    //3.数量对的上上报结果
                    if (waylineJobEntity.getStatus()==3) {
                        //比对数量
                        if(waylineJobEntity.getMediaCount()==mediaFileEntities.size()){
                            //对的上上报结果
                            try {
                                PointResult pointResult=fileService.getMediaDileByJobId(job_id,workspace_id,wayline_id);
                                List<PointCount> pointCountList=pointResult.getPointCountList();
                                //返回结果
                                //查询设备名称
                                DeviceEntity device=deviceMapper.selectOne(new LambdaQueryWrapper<DeviceEntity>()
                                        .eq(DeviceEntity::getDeviceSn,waylineJobEntity.getDockSn()));
                                //调用线程上报图片
                                executeTasks(pointCountList,job_id,sub_code,
                                        device.getDeviceName(),device.getDeviceSn(),waylineJobEntity.getName());
                                //返回状态
                                //pushPatrolStatusOther(String uavTaskPatrolledId, String planName, String state, String taskProgress)
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                            found.set(true);
                            scheduler.shutdown();
                        }
                    } else if (waylineJobEntity.getStatus()==4||waylineJobEntity.getStatus()==5) {
                        found.set(true);
                        scheduler.shutdown();
                        System.out.println("任务执行失败");
                        //失败或者取消
                        centerMsgPushHandler.pushPatrolStatusOther(job_id,
                                waylineJobEntity.getName(),
                                "4",
                                "50");
                    } else {
                        //没有找到时间继续加1
                        attempt++;
                        //System.out.printf("第"+ attempt+"次执行任务");
                        //上报无人机异常情况;
                       List< DeviceHmsEntity> deviceHmsEntitys=iDeviceHmsMapper.
                               selectList(new LambdaQueryWrapper<DeviceHmsEntity>().
                               eq(DeviceHmsEntity::getSn,waylineJobEntity.getDockSn()).
                                       gt(DeviceHmsEntity::getCreateTime,startTime));
                        DeviceHmsEntity maxHms=deviceHmsEntitys.get(0);
                       List<UavDeviceAlarmItem> alarmLists=new ArrayList<>();
                       DeviceEntity device=deviceMapper.selectOne(new LambdaQueryWrapper<DeviceEntity>().
                               eq(DeviceEntity::getDeviceSn,waylineJobEntity.getDockSn()));

                       for(int i=0;i<deviceHmsEntitys.size();i++)
                       {
                          if(deviceHmsEntitys.get(i).getCreateTime()>maxHms.getCreateTime())
                          {
                              maxHms=deviceHmsEntitys.get(i);
                          }
                       }
                        UavDeviceAlarmItem uavDeviceAlarmItem=new UavDeviceAlarmItem();
                       //设备名称
                        uavDeviceAlarmItem.setPatroldevice_name(device.getDeviceName());
                        //设备编码
                        uavDeviceAlarmItem.setPatroldevice_code(device.getDeviceSn());
                        //时间
                        uavDeviceAlarmItem.setTime(Long.toString(maxHms.getCreateTime()));
                        //描述
                        uavDeviceAlarmItem.setContent(maxHms.getMessageZh());
                        alarmLists.add(uavDeviceAlarmItem);
                       centerMsgPushHandler.pushDeviceAlarm(alarmLists);
                        //上报设备坐标
                        Optional<OsdDockDrone> deviceOpt = SpringBeanUtilsTest.getBean(IDeviceRedisService.class)
                                .getDeviceOsd(device.getDeviceSn(), OsdDockDrone.class);

                        //上报设备信息
                        String device_position;//无人机坐标
                        if(deviceOpt.isPresent())
                        {
                            OsdDockDrone osdDockDrone = deviceOpt.get();
                            List<UavCoordinateItem> uavPositionList=new ArrayList<>();
                            UavCoordinateItem uavCoordinateItem=new UavCoordinateItem();
                            //无人机名称
                            uavCoordinateItem.setPatroldevice_name(device.getDeviceName());
                            //无人机编码
                            uavCoordinateItem.setPatroldevice_code(device.getDeviceSn());
                            //时间
                            uavCoordinateItem.setTime(Long.toString(System.currentTimeMillis()));
                            //坐标
                            device_position=osdDockDrone.getLongitude()+","+osdDockDrone.getLatitude();
                            uavCoordinateItem.setCoordinate_geography(device_position);
                            uavPositionList.add(uavCoordinateItem);
                            centerMsgPushHandler.pushCoordinate(uavPositionList,device.getDeviceSn());
                        }
                    }
                } else {
                    //1.查wayline_job里的任务表看任务状态与图片数量
                    WaylineJobEntity waylineJobEntity=waylineJobMapper.selectOne(new LambdaQueryWrapper<WaylineJobEntity>()
                            .eq(WaylineJobEntity::getJobId,job_id));
                    //超时间结束
                    scheduler.shutdown();
                    //System.out.println("任务超时未上报");
                    centerMsgPushHandler.pushPatrolStatusOther(job_id,
                            waylineJobEntity.getName(),
                            "6",
                            "90");
                }
            }
        };

        // 安排任务在延迟0秒后开始执行，之后每隔3分钟执行一次
        scheduler.scheduleAtFixedRate(task, 0, 10, TimeUnit.SECONDS);
        return "";

    }
    //多线程上传图片
    private void executeTasks( List<PointCount> pointCountList,String job_id, String sub_code, String deviceName, String deviceSn,String planName) {
        int lengthSize=pointCountList.size();
        //用ExecutorService来管理线程池
        // 创建一个和点位数量一致大小的线程池
        ExecutorService executorService = Executors.newFixedThreadPool(lengthSize);
        // 提交任务到线程池
        Future<?>[] futures = new Future<?>[pointCountList.size()];
        for (int i = 0; i < lengthSize; i++) {
            final int index = i;
            PointCount pointCount = pointCountList.get(i);
            if (pointCount.getCount() > 0) {
                futures[index] = executorService.submit(() -> {
                    try {
                        // 创建一个新的列表来存储当前的图片信息
                        List<PointCount> list = new ArrayList<>();
                        list.add(pointCount);
                        // 调用上传方法上传图片
                        centerMsgPushHandler.pushPatrolResult(list, job_id, sub_code, deviceName, deviceSn);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }
        // 等待所有任务完成或超时
        int completedTasks = 0;
        boolean hasTimeoutFailure = false;
        for (int i = 0; i < lengthSize; i++) {
            try {
                if (futures[i] != null) {
                    // 等待每个任务最多60秒
                    futures[i].get(60, TimeUnit.SECONDS);
                    completedTasks++;
                }
            } catch (TimeoutException e) {
                System.out.println("任务 " + i + " 没有在60秒内完成");
                // 取消任务
                futures[i].cancel(true);
                hasTimeoutFailure = true;
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        // 关闭线程池,停止接收新任务
        executorService.shutdown();
        try {
            // 等待线程池在指定时间内终止
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                // 如果未能正常终止，则强制关闭线程池
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            // 恢复中断状态
            Thread.currentThread().interrupt();
        }
        // 上报任务状态
        double progress = (double) completedTasks / lengthSize * 100;
        if (progress >= 1 && hasTimeoutFailure) {
            //部分完成
            centerMsgPushHandler.pushPatrolStatusOther(job_id,planName,"4","50");
        } else if (progress == 100) {
            //全部完成
            centerMsgPushHandler.pushPatrolStatusOther(job_id,planName,"4","100");
        } else {
            //全部超时
            centerMsgPushHandler.pushPatrolStatusOther(job_id,planName,"4","0");
        }
    }
    //上级任务2.暂停、3.继续、4.取消
    @Override
    public String superiorTaskHandling( String job_id,String type,String command) {
        //先查询任务是否正在进行
        WaylineJobEntity waylineJobEntity=waylineJobMapper.selectOne(new LambdaQueryWrapper<WaylineJobEntity>()
                .eq(WaylineJobEntity::getJobId,job_id));
        if(waylineJobEntity.getStatus()!=2){
            //返回状态，正在处理中
            centerMsgPushHandler.pushPatrolStatusOther(job_id,waylineJobEntity.getName(),"2","20");
            return "false";
        }
        //2.任务暂停
        if(type.equals("41")&&command.equals(2)){
            UpdateJobParam updateJobParam=new UpdateJobParam();
            WaylineTaskStatusEnum status=WaylineTaskStatusEnum.PAUSE;
            updateJobParam.setStatus(status);
            flightTaskService.updateJobStatus(waylineJobEntity.getWorkspaceId(),job_id,updateJobParam);
            //返回状态
            centerMsgPushHandler.pushPatrolStatusOther(job_id,waylineJobEntity.getName(),"3","20");
            return "true";
        }

        //3.任务继续
        if(type.equals("41")&&command.equals("3")){
            UpdateJobParam updateJobParam=new UpdateJobParam();
            WaylineTaskStatusEnum status=WaylineTaskStatusEnum.RESUME;
            updateJobParam.setStatus(status);
            flightTaskService.updateJobStatus(waylineJobEntity.getWorkspaceId(), job_id,updateJobParam);
            //返回状态,任务继续
            centerMsgPushHandler.pushPatrolStatusOther(job_id,waylineJobEntity.getName(),"2","25");
            return "true";
        }
        //4.任务停止
        if (type.equals(41)&&command.equals(4)){
            Collection<String> jobIds=new ArrayList<String>();
            jobIds.add(job_id);
            flightTaskService.cancelFlightTask(waylineJobEntity.getWorkspaceId(),jobIds);
            //返回状态，任务认为停止
            centerMsgPushHandler.pushPatrolStatusOther(job_id,waylineJobEntity.getName(),"4","30");
            return "true";
        }
        return "false";
    }
    //任务下发时上传巡视路线
    private void upLoadUavLine(String deviceName, String deviceCode,String wayline_id,String workspace_id) throws Exception {
        //获取下载路径
        URL url = waylineFileService.getObjectUrl(workspace_id, wayline_id);
        Wayline wayline=waylineKmzService.GetKmzWaypointWayline(workspace_id,wayline_id,url,singlePointUrl);
        List<Placemark> placeMarks=wayline.getFolder().getPlaceMarks();//航点列表
        List<UavPatrolLineItem> list=new ArrayList<>();
        for(int i=0;i<placeMarks.size();i++)
        {  Placemark placemark= placeMarks.get(i);
            UavPatrolLineItem uavPatrolLineItem=new UavPatrolLineItem();
            //无人机名称
            uavPatrolLineItem.setPatroldevice_name(deviceName);
            //无人机编码
            uavPatrolLineItem.setPatroldevice_code(deviceCode);
            // 获取当前系统时间戳（以毫秒为单位）
            long currentTimeMillis = System.currentTimeMillis();
            uavPatrolLineItem.setTime(Long.toString(currentTimeMillis));
            //经纬度
            uavPatrolLineItem.setCoordinate_geography( placemark.getPoint().getCoordinates());
            list.add(uavPatrolLineItem);
        }
        centerMsgPushHandler.pushRoute(list,deviceCode);

    }
}
