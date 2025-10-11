package com.dji.sample.center.v2022.runnable;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dji.sample.center.app.AppContext;
import com.dji.sample.center.dao.IUniRepairAreaMapper;
import com.dji.sample.center.model.entity.UniRepairAreaEntity;
import com.dji.sample.center.utils.DateUtils;
import com.dji.sample.center.v2022.command.base.PatrolHostCommand;
import com.dji.sample.center.v2022.command.base.PatrolHostCommandItem;
import com.dji.sample.center.v2022.command.control.CenterOverhaulAreaItem;
import com.dji.sample.center.v2022.data.CenterProtocolData;
import com.dji.sample.center.v2022.tool.BaseItem;
import com.dji.sample.center.v2022.tool.CenterXmlTool;
import com.dji.sample.df.electricInspectionDf.dao.PubWaylinePointDfMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 巡视上级检修区域下发指令处理
 *
 * @author lyc
 * @date 2022/4/1
 */
@Slf4j
public class CenterOverhaulAreaRunnable extends CenterMessageBaseRunnable {

    private IUniRepairAreaMapper iUniRepairAreaMapper = AppContext.getBean(IUniRepairAreaMapper.class);
    private PubWaylinePointDfMapper pubWaylinePointDfMapper = AppContext.getBean(PubWaylinePointDfMapper.class);


    public CenterOverhaulAreaRunnable(CenterProtocolData protocolData) {
        super(protocolData);
    }

    @Override
    public void run() {
        try {
            PatrolHostCommand commandData = CenterXmlTool.deserialize(xmlMessage, PatrolHostCommand.class, CenterOverhaulAreaItem.class);
            PatrolHostCommandItem commandItem = commandData.getItems();
            if (commandItem != null) {
                List<BaseItem> items = commandItem.getItem();
                if (items != null && items.size() > 0) {
                    //处理检修区域下发
                    log.info("【接收巡视上级检修区域下发指令】正在处理 ========> ");
                    for (BaseItem item:items)
                    {
                        CenterOverhaulAreaItem centerOverhaulAreaItem = (CenterOverhaulAreaItem) item;
                        overhaulAreaHandle(centerOverhaulAreaItem);
                    }
                    log.info("【接收巡视上级检修区域下发指令】处理完毕 ========> ");
                }
            }
            //响应消息给中心端
            this.responseMessage();
        } catch (Exception e) {
            log.error("【巡视上级检修区域下发指令】处理异常，巡视上级IP：{}，异常信息：{}", this.centerIp, e.getMessage());
        }
    }

    /**
     * 处理检修区域下发
     */
    public void overhaulAreaHandle(CenterOverhaulAreaItem item) {
        List<UniRepairAreaEntity> list = iUniRepairAreaMapper.selectList(
                new LambdaQueryWrapper<UniRepairAreaEntity>()
                        .eq(UniRepairAreaEntity::getConfigCode, item.getConfig_code())
        );
        if (list != null && list.size() > 0) {
            iUniRepairAreaMapper.delete(
                    new LambdaQueryWrapper<UniRepairAreaEntity>()
                            .eq(UniRepairAreaEntity::getConfigCode, item.getConfig_code())
            );
        }

        UniRepairAreaEntity entity = new UniRepairAreaEntity();
        entity.setConfigCode(item.getConfig_code());
        entity.setEnable(item.getEnable());
        entity.setStartTime(DateUtils.parseDate(item.getStart_time()));
        entity.setEndTime(DateUtils.parseDate(item.getEnd_time()));
        entity.setDeviceLevel(item.getDevice_level());
        entity.setDeviceList(item.getDevice_list());
        entity.setCoordinatePixel(item.getCoordinate_pixel());

        List<String> deviceList = new ArrayList<>();
        if (entity.getDeviceList() != null) {
            deviceList = Arrays.asList(entity.getDeviceList().split(","));
        }
        if (deviceList != null && deviceList.size() > 0) {
            if (entity.getDeviceLevel() == 3) {
                entity.setPointList(item.getDevice_list());
            } else {
                entity.setPointList(pubWaylinePointDfMapper.selectPoints(entity.getDeviceLevel(), deviceList));
            }
        }

        iUniRepairAreaMapper.insert(entity);
    }
}
