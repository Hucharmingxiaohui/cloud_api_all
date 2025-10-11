package com.dji.sample.center.v2022.runnable;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dji.sample.center.app.AppContext;
import com.dji.sample.center.utils.DateUtils;
import com.dji.sample.center.utils.StringUtils;
import com.dji.sample.center.v2022.command.base.PatrolHostCommand;
import com.dji.sample.center.v2022.command.base.PatrolHostCommandItem;
import com.dji.sample.center.v2022.command.control.CenterTaskCommandItem;
import com.dji.sample.center.v2022.data.CenterProtocolData;
import com.dji.sample.center.v2022.tool.BaseItem;
import com.dji.sample.center.v2022.tool.CenterXmlTool;
import com.dji.sample.df.electricInspectionDf.dao.PubWaylinePointDfMapper;
import com.dji.sample.df.manageDf.dao.IUniTaskPlanMapper;
import com.dji.sample.df.manageDf.model.entity.UniTaskPlanEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * 巡视上级任务下发指令处理
 */
@Slf4j
public class CenterTaskRunnable extends CenterMessageBaseRunnable {

    private IUniTaskPlanMapper iUniTaskPlanMapper = AppContext.getBean(IUniTaskPlanMapper.class);
    private PubWaylinePointDfMapper pubWaylinePointDfMapper = AppContext.getBean(PubWaylinePointDfMapper.class);

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

    /**
     * 处理任务下发，存储数据库
     */
    @Transactional(rollbackFor = Exception.class)
    public void taskHandle(CenterTaskCommandItem taskCommandItem, String sub_code) {
        UniTaskPlanEntity entity = new UniTaskPlanEntity();
        List<UniTaskPlanEntity> checkList = iUniTaskPlanMapper.selectList(
                new LambdaQueryWrapper<UniTaskPlanEntity>()
                .eq(UniTaskPlanEntity::getSubCode, sub_code)
                .eq(UniTaskPlanEntity::getPlanNo, taskCommandItem.getTask_code())
        );
        if (checkList != null && checkList.size() == 1) {
            iUniTaskPlanMapper.deleteById(checkList.get(0).getId());
        }


        entity.setSubCode(sub_code);
        entity.setPlanNo(taskCommandItem.getTask_code());
        entity.setPlanName(taskCommandItem.getTask_name());
        entity.setTaskType(Integer.parseInt(taskCommandItem.getType()));
        entity.setFixedStartTime(DateUtils.parseDate(taskCommandItem.getFixed_start_time()));
        entity.setDeviceLevel(taskCommandItem.getDevice_level());
        entity.setDeviceList(taskCommandItem.getDevice_list());
        entity.setCycleMonth(taskCommandItem.getCycle_month());
        entity.setCycleWeek(taskCommandItem.getCycle_week());
        entity.setCycleExecuteTime(DateUtils.parseDate(taskCommandItem.getCycle_execute_time(), "HH:mm:ss"));
        entity.setCycleStartTime(DateUtils.parseDate(taskCommandItem.getCycle_start_time()));
        entity.setCycleEndTime(DateUtils.parseDate(taskCommandItem.getCycle_end_time()));
        if (taskCommandItem.getCycle_execute_time() != null && !taskCommandItem.getCycle_execute_time().isEmpty()) {
            //普通周期
            if (taskCommandItem.getCycle_week() != null && !taskCommandItem.getCycle_week().isEmpty()) {
                entity.setCycleType(1);
            } else {
                entity.setCycleType(2);
            }
        }
        entity.setIntervalNumber(taskCommandItem.getInterval_number());
        entity.setIntervalType(taskCommandItem.getInterval_type());
        entity.setIntervalExecuteTime(DateUtils.parseDate(taskCommandItem.getInterval_execute_time(), "HH:mm:ss"));
        entity.setIntervalStartTime(DateUtils.parseDate(taskCommandItem.getInterval_start_time()));
        entity.setIntervalEndTime(DateUtils.parseDate(taskCommandItem.getInterval_end_time()));
        entity.setInvalidStartTime(DateUtils.parseDate(taskCommandItem.getInvalid_start_time()));
        entity.setInvalidEndTime(DateUtils.parseDate(taskCommandItem.getInvalid_end_time()));
        entity.setIsenable(Integer.parseInt(taskCommandItem.getIsenable()));
        entity.setCreator(taskCommandItem.getCreator());
        entity.setCreateTime(DateUtils.parseDate(taskCommandItem.getCreate_time()));
        if (taskCommandItem.getFixed_start_time() != null && !taskCommandItem.getFixed_start_time().isEmpty()) {
            entity.setExecuteType(2);
        } else {
            entity.setExecuteType(3);
        }
        entity.setPriority(Integer.valueOf(taskCommandItem.getPriority()));
        if (taskCommandItem.getDevice_level() == 3) {
            entity.setPointList(taskCommandItem.getDevice_list());
        } else {
            String[] split = taskCommandItem.getDevice_list().split(",");
            if (split != null && split.length > 0) {
                entity.setPointList(pubWaylinePointDfMapper.selectPoints(taskCommandItem.getDevice_level(), Arrays.asList(split)));
            }
        }
        if (StringUtils.isNotEmpty(entity.getPointList())) {
            String[] split = entity.getPointList().split(",");
            if (split != null && split.length > 0) {
                entity.setWaylineList(pubWaylinePointDfMapper.selectWaylines(Arrays.asList(split)));
            }
        }
        iUniTaskPlanMapper.insert(entity);
    }
}
