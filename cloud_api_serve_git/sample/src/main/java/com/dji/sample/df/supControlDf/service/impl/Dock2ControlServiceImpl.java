package com.dji.sample.df.supControlDf.service.impl;

import com.dji.sample.control.model.enums.RemoteDebugMethodEnum;
import com.dji.sample.control.service.IControlService;
import com.dji.sample.df.supControlDf.service.Dock2ConTrolService;
import com.dji.sdk.common.HttpResultResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class Dock2ControlServiceImpl implements Dock2ConTrolService {
    //注入控制类
    @Autowired
    private IControlService controlService;
    /***
     *
     * @param sn 机场网关
     *
     */
    @Override
    public String dockControl(String sn,String Type,String Command,String Item) {
        //1.机巢开启
        //2.机巢关闭
        //3.机巢重置（重启）
        if(Type.equals("20005")&&Command.equals("1")&&Item.equals("3")){
            HttpResultResponse result1 =controlService.controlDockDebug(sn, RemoteDebugMethodEnum.DEBUG_MODE_OPEN,null);
            HttpResultResponse result2=controlService.controlDockDebug(sn,RemoteDebugMethodEnum.DEVICE_REBOOT,null);
            if(result1.getCode()==0&&result2.getCode()==0){
                controlService.controlDockDebug(sn,RemoteDebugMethodEnum.DEBUG_MODE_CLOSE,null);
                return "true";
            }
        }
        //机巢开盖
        if(Type.equals("20005")&&Command.equals("3")&&Item.equals("1")){
            HttpResultResponse result1 =controlService.controlDockDebug(sn,RemoteDebugMethodEnum.DEBUG_MODE_OPEN,null);
            HttpResultResponse result2=controlService.controlDockDebug(sn,RemoteDebugMethodEnum.COVER_OPEN,null);
            if(result1.getCode()==0&&result2.getCode()==0){
                controlService.controlDockDebug(sn,RemoteDebugMethodEnum.DEBUG_MODE_CLOSE,null);
                return "true";
            }
        }
        //关闭舱盖
        if(Type.equals("20005")&&Command.equals("3")&&Item.equals("2")){
            HttpResultResponse result1 =controlService.controlDockDebug(sn,RemoteDebugMethodEnum.DEBUG_MODE_OPEN,null);
            HttpResultResponse result2=controlService.controlDockDebug(sn,RemoteDebugMethodEnum.COVER_CLOSE,null);
            if(result1.getCode()==0&&result2.getCode()==0){
                controlService.controlDockDebug(sn,RemoteDebugMethodEnum.DEBUG_MODE_CLOSE,null);
                return "true";
            }
        }
        return "false";
    }
}
