package com.dji.sample.df.supControlDf.service;

import com.dji.sample.control.model.param.PayloadCommandsParam;

//无人机控制接口，机场2
public interface DroneControlService {
    //获取无人机控制权、机场2 机场sn
    String  flightAuthorityGrab(String sn);
    //一键返航
    String return_home(String sn);
    //取消一键返航
    String return_home_cancel(String sn);
    //模式切换：进入drc模式
    String deviceDrcEnter(String sn);
    //模式切换：退出drc模式
    String deviceDrcExit(String sn);
    //急停
    String droneEmergencyStop(String sn);
    //自动降落,有障碍物会暂停
    String drcEmergencyLanding(String sn);
    //获取负载控制权
    String payload_authority_grab(String sn);
    //负载控制
    String payloadCommands(String sn, PayloadCommandsParam param) throws Exception;
    //集成一个接口在内部调用
    String droneControl(String sn,String  Type,String Command,String Item) throws Exception;

}
