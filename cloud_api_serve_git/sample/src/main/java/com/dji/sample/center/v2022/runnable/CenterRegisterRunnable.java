package com.dji.sample.center.v2022.runnable;

import com.dji.sample.center.app.AppContext;
import com.dji.sample.center.thread.ExecutorFactory;
import com.dji.sample.center.utils.StringUtil;
import com.dji.sample.center.v2022.command.base.PatrolHostCommand;
import com.dji.sample.center.v2022.command.base.PatrolHostCommandItem;
import com.dji.sample.center.v2022.command.system.CenterRegisterItem;
import com.dji.sample.center.v2022.data.CenterProtocolData;
import com.dji.sample.center.v2022.data.IntervalProtocolData;
import com.dji.sample.center.v2022.handler.PatrolHostSocketClient;
import com.dji.sample.center.v2022.tool.BaseItem;
import com.dji.sample.center.v2022.tool.CenterXmlTool;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

/**
 * 巡视上级注册指令的响应处理
 *
 * @author lyc
 * @date 2022/4/1
 */
@Slf4j
public class CenterRegisterRunnable extends CenterMessageBaseRunnable {

    private ExecutorFactory executorFactory = AppContext.getBean(ExecutorFactory.class);

    public CenterRegisterRunnable(CenterProtocolData protocolData) {
        super(protocolData);
    }

    @Override
    public void run() {
        try {
            //构建间隔上报协议数据
            IntervalProtocolData protocolData = new IntervalProtocolData();
            protocolData.setPatrolHostSocketClient(patrolHostSocketClient);
            protocolData.setCenterIp(centerIp);
            protocolData.setPatrolHostCode(patrolHostCode);
            protocolData.setCenterCode(centerCode);

            PatrolHostCommand commandData = CenterXmlTool.deserialize(xmlMessage, CenterRegisterItem.class);
            PatrolHostCommandItem commandItem = commandData.getItems();
            if (commandItem != null) {
                List<BaseItem> items = commandItem.getItem();
                if (items != null && items.size() > 0) {
                    //注册巡视上级的响应报文处理
                    centerRegisterResponseHandle(items, protocolData);
                }
            }
        } catch (Exception e) {
            log.error("【巡视上级注册指令的响应】处理异常，巡视上级IP：{}，异常信息：{}", this.centerIp, e.getMessage());
        }
    }


    /**
     * 处理Item里的信息
     *
     * @param items
     */
    private void centerRegisterResponseHandle(List<BaseItem> items, IntervalProtocolData protocolData) {
        CenterRegisterItem registerItem = (CenterRegisterItem) items.get(0);
        if (!StringUtil.isEmpty(registerItem.getHeart_beat_interval())) {
            int rate = Integer.parseInt(registerItem.getHeart_beat_interval());
            protocolData.setIntervalSeconds(rate);

            PatrolHostHeartRunnable patrolHostHeartRunnable = new PatrolHostHeartRunnable(protocolData);
            executorFactory.getExecutorService().submit(patrolHostHeartRunnable);

            String uuid = UUID.randomUUID().toString().replaceAll("-", "");
            PatrolHostSocketClient.V2022PatrolHostRunnableBucket.put(uuid, patrolHostHeartRunnable);
        } else {
            log.error("【巡视上级注册指令的响应】未返回心跳间隔，不能启动上报服务");
        }

        if (!StringUtil.isEmpty(registerItem.getPatroldevice_run_interval())) {
            // 巡视设备运行数据定时上送
            int rate = Integer.parseInt(registerItem.getPatroldevice_run_interval());
            protocolData.setIntervalSeconds(rate);

            UavHostRunRunnable uavHostRunRunnable = new UavHostRunRunnable(protocolData);
            executorFactory.getExecutorService().submit(uavHostRunRunnable);

            String uuid = UUID.randomUUID().toString().replaceAll("-", "");
            PatrolHostSocketClient.V2022PatrolHostRunnableBucket.put(uuid, uavHostRunRunnable);

            // 巡视设备状态数据定时上送
            UavHostStatusRunnable uavHostStatusRunnable = new UavHostStatusRunnable(protocolData);
            executorFactory.getExecutorService().submit(uavHostStatusRunnable);

            uuid = UUID.randomUUID().toString().replaceAll("-", "");
            PatrolHostSocketClient.V2022PatrolHostRunnableBucket.put(uuid, uavHostStatusRunnable);
        } else {
            log.error("【巡视上级注册指令的响应】未返回巡视设备运行数据间隔，不能启动上报服务");
        }

        if (!StringUtil.isEmpty(registerItem.getNest_run_interval())) {
            // 无人机机巢运行数据定时上送
            int rate = Integer.parseInt(registerItem.getNest_run_interval());
            protocolData.setIntervalSeconds(rate);

            UavNestRunRunnable uavNestRunRunnable = new UavNestRunRunnable(protocolData);
            executorFactory.getExecutorService().submit(uavNestRunRunnable);

            String uuid = UUID.randomUUID().toString().replaceAll("-", "");
            PatrolHostSocketClient.V2022PatrolHostRunnableBucket.put(uuid, uavNestRunRunnable);

            // 无人机机巢状态数据定时上送
            UavNestStatusRunnable uavNestStatusRunnable = new UavNestStatusRunnable(protocolData);
            executorFactory.getExecutorService().submit(uavNestStatusRunnable);

            uuid = UUID.randomUUID().toString().replaceAll("-", "");
            PatrolHostSocketClient.V2022PatrolHostRunnableBucket.put(uuid, uavNestStatusRunnable);
        } else {
            log.error("【巡视上级注册指令的响应】未返回无人机机巢运行数据间隔，不能启动上报服务");
        }

        if (!StringUtil.isEmpty(registerItem.getEnv_interval())) {
            // 环境数据定时上送
            int rate = Integer.parseInt(registerItem.getEnv_interval());
            protocolData.setIntervalSeconds(rate);

            UavHostEnvRunnable uavEnvRunnable = new UavHostEnvRunnable(protocolData);
            executorFactory.getExecutorService().submit(uavEnvRunnable);

            String uuid = UUID.randomUUID().toString().replaceAll("-", "");
            PatrolHostSocketClient.V2022PatrolHostRunnableBucket.put(uuid, uavEnvRunnable);

        } else {
            log.error("【巡视上级注册指令的响应】未返回环境数据间隔间隔，不能启动上报服务");
        }
    }
}
