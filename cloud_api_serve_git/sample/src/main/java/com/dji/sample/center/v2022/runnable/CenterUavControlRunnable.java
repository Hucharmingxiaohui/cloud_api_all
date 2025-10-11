package com.dji.sample.center.v2022.runnable;

import com.dji.sample.center.v2022.command.base.PatrolHostCommand;
import com.dji.sample.center.v2022.command.base.PatrolHostCommandItem;
import com.dji.sample.center.v2022.command.control.UavControlItem;
import com.dji.sample.center.v2022.data.CenterProtocolData;
import com.dji.sample.center.v2022.tool.BaseItem;
import com.dji.sample.center.v2022.tool.CenterXmlTool;
import com.dji.sample.df.supControlDf.service.DroneControlService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.util.List;

/**
 * 巡视上级无人机控制指令处理
 *
 * @author lyc
 * @date 2022/4/1
 */
@Slf4j
public class CenterUavControlRunnable extends CenterMessageBaseRunnable {
    @Resource
    private DroneControlService droneControlService;


    public CenterUavControlRunnable(CenterProtocolData protocolData) {
        super(protocolData);
    }

    @Override
    public void run() {
        try {
            PatrolHostCommand commandData = CenterXmlTool.deserialize(xmlMessage, UavControlItem.class);
            PatrolHostCommandItem commandItem = commandData.getItems();
            boolean isControl = true;
            if (commandItem != null) {
                List<BaseItem> items = commandItem.getItem();
                if (items != null && items.size() > 0) {
                    //巡视上级无人机控制处理
                    log.info("【接收巡视上级无人机控制指令】正在处理 ========> ");
                    for (BaseItem baseItem : items) {
                        UavControlItem robotControlItem = (UavControlItem) baseItem;
                        boolean controlOne = uavControlHandle(robotControlItem, commandData);
                        if (!controlOne) {
                            isControl = controlOne;
                        }
                    }
                    log.info("【接收巡视上级无人机控制指令】处理完毕 ========> ");
                }
            }
            //响应消息给中心端
            if (isControl) {
                this.responseMessage();
            } else {
                this.responseExceptionMessage();
            }
        } catch (Exception e) {
            log.error("【巡视上级无人机控制指令】处理异常，巡视上级IP：{}，异常信息：{}", this.centerIp, e.getMessage());
        }
    }

    /**
     * 处理Item里的信息
     */
    private boolean uavControlHandle(UavControlItem item, PatrolHostCommand commandData) throws Exception {
//        boolean isControl = xxxmethod.uavControl(commandData.getCommand(), commandData.getType(), commandData.getCode(), item.getValue());
        //20001无人机控制权
        if(commandData.getType().equals("20001"))
        {
            //控制权获取
            /***
             * code String 机巢编码
             * Type String 命令类型
             * Command String 命令
             * Item String 参数
             */
            if(commandData.getCommand().equals("6"))
            {
                String flag = droneControlService.droneControl(commandData.getCode(),
                        commandData.getType(),commandData.getCommand(),item.getValue());
                return flag.equals("true");
            }
            //启动一键返航
            if(commandData.getCommand().equals("3")&&item.getValue().equals("1"))
            {
                String flag = droneControlService.droneControl(commandData.getCode(),
                        commandData.getType(),commandData.getCommand(),item.getValue());
                return flag.equals("true");
            }
            //取消一键返航
            if(commandData.getCommand().equals("3")&&item.getValue().equals("2"))
            {
                String flag = droneControlService.droneControl(commandData.getCode(),
                        commandData.getType(),commandData.getCommand(),item.getValue());
                return flag.equals("true");
            }
            //启动自动降落
            if(commandData.getCommand().equals("4")&&item.getValue().equals("1"))
            {
                String flag = droneControlService.droneControl(commandData.getCode(),
                        commandData.getType(),commandData.getCommand(),item.getValue());
                return flag.equals("true");
            }


        }
        //20002无人机飞行控制指令
        if(commandData.getType().equals("20002"))
        {
            //急停
            /***
             * code String 机巢编码
             * Type String 命令类型
             * Command String 命令
             * Item String 参数
             */
            if(commandData.getCommand().equals("7")&&item.getValue().equals("1"))
            {
                String flag = droneControlService.droneControl(commandData.getCode(),
                        commandData.getType(),commandData.getCommand(),item.getValue());
                return flag.equals("true");
            }

        }
        //20003无人机云台控制指令
        if(commandData.getType().equals("20003"))
        {
            //云台重置，回中
            /***
             * code String 机巢编码
             * Type String 命令类型
             * Command String 命令
             * Item String 参数
             */
            if(commandData.getCommand().equals("5"))
            {
                String flag = droneControlService.droneControl(commandData.getCode(),
                        commandData.getType(),commandData.getCommand(),item.getValue());
                return flag.equals("true");
            }
        }
        //20005无人机机巢控制命令
        if(commandData.getType().equals("20005"))
        {
            //机巢重启
            if(commandData.getCommand().equals("1")&&item.getValue().equals("3"))
            {
                String flag = droneControlService.droneControl(commandData.getCode(),
                        commandData.getType(),commandData.getCommand(),item.getValue());
                return flag.equals("true");
            }
            //打开舱盖
            if(commandData.getCommand().equals("3")&&item.getValue().equals("1"))
            {
                String flag = droneControlService.droneControl(commandData.getCode(),
                        commandData.getType(),commandData.getCommand(),item.getValue());
                return flag.equals("true");
            }
            //关闭舱盖
            if(commandData.getCommand().equals("3")&&item.getValue().equals("2"))
            {
                String flag = droneControlService.droneControl(commandData.getCode(),
                        commandData.getType(),commandData.getCommand(),item.getValue());
                return flag.equals("true");
            }
        }
        return true;
    }
}
