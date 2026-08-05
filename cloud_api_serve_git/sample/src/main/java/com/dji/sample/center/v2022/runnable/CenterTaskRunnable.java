package com.dji.sample.center.v2022.runnable;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dji.sample.center.app.AppContext;
import com.dji.sample.center.config.SwitchConfig;
import com.dji.sample.center.utils.DateUtils;
import com.dji.sample.df.substationDf.dao.UniPointMapper2;
import com.dji.sample.center.v2022.command.base.PatrolHostCommand;
import com.dji.sample.center.v2022.command.base.PatrolHostCommandItem;
import com.dji.sample.center.v2022.command.control.CenterTaskCommandItem;
import com.dji.sample.center.v2022.data.CenterProtocolData;
import com.dji.sample.center.v2022.tool.BaseItem;
import com.dji.sample.center.v2022.tool.CenterXmlTool;
import com.dji.sample.df.electricInspectionDf.dao.PubWaylinePointDfMapper;
import com.dji.sample.df.manageDf.dao.IUniTaskPlanMapper;
import com.dji.sample.df.manageDf.model.entity.UniTaskPlanEntity;
import com.dji.sample.df.windDf.dao.WindTurbineMapper;
import com.dji.sample.df.windDf.model.entity.WindTurbine;
import com.dji.sample.df.uavCommonHandleDf.handler.CenterTaskHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;

/**
 * 巡视上级任务下发指令处理
 */
@Slf4j
public class CenterTaskRunnable extends CenterMessageBaseRunnable {

    private IUniTaskPlanMapper iUniTaskPlanMapper = AppContext.getBean(IUniTaskPlanMapper.class);
    private PubWaylinePointDfMapper pubWaylinePointDfMapper = AppContext.getBean(PubWaylinePointDfMapper.class);
    private WindTurbineMapper windTurbineMapper = AppContext.getBean(WindTurbineMapper.class);
    private CenterTaskHandler centerTaskHandler = AppContext.getBean(CenterTaskHandler.class);
    private UniPointMapper2 uniPointMapper2 = AppContext.getBean(UniPointMapper2.class);
    private SwitchConfig switchConfig = AppContext.getBean(SwitchConfig.class);

    public CenterTaskRunnable(CenterProtocolData protocolData) {
        super(protocolData);
    }

    @Override
    public void run() {
        try {
            PatrolHostCommand commandData = CenterXmlTool.deserialize(xmlMessage, PatrolHostCommand.class, CenterTaskCommandItem.class);
            PatrolHostCommandItem commandItem = commandData.getItems();
            String command = commandData.getCommand();
            String sub_code = commandData.getCode();
            if (commandItem != null && command.equals("1")) {
                List<BaseItem> items = commandItem.getItem();
                if (items != null && items.size() > 0) {
                    for (BaseItem baseItem : items) {
                        //巡视上级下发任务处理
                        log.info("【接收巡视上级无人机任务下发指令】正在处理 ========> ");
                        CenterTaskCommandItem taskCommandItem = (CenterTaskCommandItem) baseItem;
                        taskHandle(taskCommandItem, sub_code);
                        log.info("【接收巡视上级无人机任务下发指令】处理完毕 ========> ");
                    }
                }
            }
            //响应消息给中心端
            this.responseMessageOtherCommand();
        } catch (Exception e) {
            log.error("【巡视上级任务下发指令】处理异常，巡视上级IP：{}，异常信息：{}", this.centerIp, e.getMessage());
        }
    }


    @Transactional(rollbackFor = Exception.class)
    public void taskHandle(CenterTaskCommandItem taskCommandItem, String sub_code) {
       log.info("开始处理下发任务");
//      需要区分普通任务和风机任务
//      暂定风机任务设备层级设备id匹配风机；普通任务间隔层级间隔id匹配航线
//      todo 对接第三方无人机平台下发任务，暂定先只能安间隔下发，根据配置开关位区分和普通航线任务的逻辑
        String taskCode = taskCommandItem.getTask_code();
        String taskName = taskCommandItem.getTask_name();
        int deviceLevel = taskCommandItem.getDevice_level();
        String deviceList = taskCommandItem.getDevice_list();
        String fixedStartTime = taskCommandItem.getFixed_start_time();
//      上级下发定时任务存表，方便上级立即执行去查询
        saveOrUpdateTaskPlan(taskCommandItem, sub_code);
        try {
            // 1. 检查设备层级和列表
            if (deviceLevel == 2) {
                // 2. 检查设备列表是否只有一个ID
                String[] deviceIds = deviceList.split(",");
                if (deviceIds.length == 1) {
                    String singleDeviceId = deviceIds[0].trim();

                    // 3. 查询所有风机
                    List<WindTurbine> windTurbines = windTurbineMapper.selectList(new HashMap<>());

                    // 4. 检查是否能匹配上风机ID
                    WindTurbine matchedTurbine = windTurbines.stream()
                            .filter(wt -> singleDeviceId.equals(wt.getId()) ||
                                    singleDeviceId.equals(wt.getId()))
                            .findFirst()
                            .orElse(null);
                    if (matchedTurbine!=null) {

                        // 存入定时任务
                        centerTaskHandler.addScheduledTask(1,taskCode, fixedStartTime,
                                singleDeviceId, taskName);

                    }
                }
            }else if (deviceLevel == 1) {
//              普通航线：规定选一个间隔
                String[] deviceIds = deviceList.split(",");
                if (deviceIds.length == 1) {
                    String bayId = deviceIds[0].trim();
                    if (isCqDockTaskEnabled()) {
                        // EUA平台任务同样复用原有Redis定时任务，到fixedStartTime后再调用下级下发接口。
                        centerTaskHandler.addScheduledTask(5, taskCode, fixedStartTime, bayId, taskName);
                        return;
                    }
                    // 存入定时任务
                    centerTaskHandler.addScheduledTask(0,taskCode, fixedStartTime,
                            bayId, taskName);
                }
            }

        } catch (Exception e) {
            log.error("任务处理失败", e);
        }
    }



    private void saveOrUpdateTaskPlan(CenterTaskCommandItem taskCommandItem, String subCode) {
        UniTaskPlanEntity entity = iUniTaskPlanMapper.selectOne(
                new LambdaQueryWrapper<UniTaskPlanEntity>()
                        .eq(UniTaskPlanEntity::getSubCode, subCode)
                        .eq(UniTaskPlanEntity::getPlanNo, taskCommandItem.getTask_code())
        );
        if (entity == null) {
            entity = new UniTaskPlanEntity();
            entity.setSubCode(subCode);
            entity.setPlanNo(taskCommandItem.getTask_code());
        }
        entity.setPlanName(taskCommandItem.getTask_name());
        entity.setTaskType(parseInteger(taskCommandItem.getType()));
        entity.setFixedStartTime(DateUtils.parseDate(taskCommandItem.getFixed_start_time()));
        entity.setDeviceLevel(taskCommandItem.getDevice_level());
        entity.setDeviceList(taskCommandItem.getDevice_list());
        entity.setIsenable(parseInteger(taskCommandItem.getIsenable(), 0));
        entity.setCreator(taskCommandItem.getCreator());
        entity.setCreateTime(DateUtils.parseDate(taskCommandItem.getCreate_time()));
        entity.setExecuteType(taskCommandItem.getFixed_start_time() == null || taskCommandItem.getFixed_start_time().isEmpty() ? 3 : 2);
        entity.setPriority(parseInteger(taskCommandItem.getPriority()));
        if (entity.getId() == null) {
            iUniTaskPlanMapper.insert(entity);
        } else {
            iUniTaskPlanMapper.updateById(entity);
        }
        log.info("上级任务下发已保存任务方案: taskCode={}, taskName={}, subCode={}, deviceLevel={}, deviceList={}, fixedStartTime={}",
                taskCommandItem.getTask_code(), taskCommandItem.getTask_name(), subCode,
                taskCommandItem.getDevice_level(), taskCommandItem.getDevice_list(), taskCommandItem.getFixed_start_time());
    }

    private Integer parseInteger(String value) {
        return parseInteger(value, null);
    }

    private Integer parseInteger(String value, Integer defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        return Integer.valueOf(value);
    }

    /**
     * 是否开启上级间隔任务转重庆EUA平台；支持true/1两种配置值。
     */
    private boolean isCqDockTaskEnabled() {
        String enable = switchConfig.getCenterCqDockTaskEnable();
        return "true".equalsIgnoreCase(enable) || "1".equals(enable);
    }

    /**
     * 处理任务下发，存储数据库（以前，弃用）
     */
//    @Transactional(rollbackFor = Exception.class)
//    public void taskHandle(CenterTaskCommandItem taskCommandItem, String sub_code) {
//        UniTaskPlanEntity entity = new UniTaskPlanEntity();
//        List<UniTaskPlanEntity> checkList = iUniTaskPlanMapper.selectList(
//                new LambdaQueryWrapper<UniTaskPlanEntity>()
//                .eq(UniTaskPlanEntity::getSubCode, sub_code)
//                .eq(UniTaskPlanEntity::getPlanNo, taskCommandItem.getTask_code())
//        );
//        if (checkList != null && checkList.size() == 1) {
//            iUniTaskPlanMapper.deleteById(checkList.get(0).getId());
//        }
//
//
//        entity.setSubCode(sub_code);
//        entity.setPlanNo(taskCommandItem.getTask_code());
//        entity.setPlanName(taskCommandItem.getTask_name());
//        entity.setTaskType(Integer.parseInt(taskCommandItem.getType()));
//        entity.setFixedStartTime(DateUtils.parseDate(taskCommandItem.getFixed_start_time()));
//        entity.setDeviceLevel(taskCommandItem.getDevice_level());
//        entity.setDeviceList(taskCommandItem.getDevice_list());
//        entity.setCycleMonth(taskCommandItem.getCycle_month());
//        entity.setCycleWeek(taskCommandItem.getCycle_week());
//        entity.setCycleExecuteTime(DateUtils.parseDate(taskCommandItem.getCycle_execute_time(), "HH:mm:ss"));
//        entity.setCycleStartTime(DateUtils.parseDate(taskCommandItem.getCycle_start_time()));
//        entity.setCycleEndTime(DateUtils.parseDate(taskCommandItem.getCycle_end_time()));
//        if (taskCommandItem.getCycle_execute_time() != null && !taskCommandItem.getCycle_execute_time().isEmpty()) {
//            //普通周期
//            if (taskCommandItem.getCycle_week() != null && !taskCommandItem.getCycle_week().isEmpty()) {
//                entity.setCycleType(1);
//            } else {
//                entity.setCycleType(2);
//            }
//        }
//        entity.setIntervalNumber(taskCommandItem.getInterval_number());
//        entity.setIntervalType(taskCommandItem.getInterval_type());
//        entity.setIntervalExecuteTime(DateUtils.parseDate(taskCommandItem.getInterval_execute_time(), "HH:mm:ss"));
//        entity.setIntervalStartTime(DateUtils.parseDate(taskCommandItem.getInterval_start_time()));
//        entity.setIntervalEndTime(DateUtils.parseDate(taskCommandItem.getInterval_end_time()));
//        entity.setInvalidStartTime(DateUtils.parseDate(taskCommandItem.getInvalid_start_time()));
//        entity.setInvalidEndTime(DateUtils.parseDate(taskCommandItem.getInvalid_end_time()));
//        entity.setIsenable(Integer.parseInt(taskCommandItem.getIsenable()));
//        entity.setCreator(taskCommandItem.getCreator());
//        entity.setCreateTime(DateUtils.parseDate(taskCommandItem.getCreate_time()));
//        if (taskCommandItem.getFixed_start_time() != null && !taskCommandItem.getFixed_start_time().isEmpty()) {
//            entity.setExecuteType(2);
//        } else {
//            entity.setExecuteType(3);
//        }
//        entity.setPriority(Integer.valueOf(taskCommandItem.getPriority()));
//        if (taskCommandItem.getDevice_level() == 3) {
//            entity.setPointList(taskCommandItem.getDevice_list());
//        } else {
//            String[] split = taskCommandItem.getDevice_list().split(",");
//            if (split != null && split.length > 0) {
//                entity.setPointList(pubWaylinePointDfMapper.selectPoints(taskCommandItem.getDevice_level(), Arrays.asList(split)));
//            }
//        }
//        if (StringUtils.isNotEmpty(entity.getPointList())) {
//            String[] split = entity.getPointList().split(",");
//            if (split != null && split.length > 0) {
//                entity.setWaylineList(pubWaylinePointDfMapper.selectWaylines(Arrays.asList(split)));
//            }
//        }
//        iUniTaskPlanMapper.insert(entity);
//    }

    /**
     * 处理任务下发，（真正执行）
     */
}
