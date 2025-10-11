package com.dji.sample.df.supControlDf.service.impl;

import com.dji.sample.control.model.dto.JwtAclDTO;
import com.dji.sample.control.model.dto.LinkWorkMode;
import com.dji.sample.control.model.enums.DroneAuthorityEnum;
import com.dji.sample.control.model.enums.PayloadCommandsEnum;
import com.dji.sample.control.model.enums.RemoteDebugMethodEnum;
import com.dji.sample.control.model.param.DrcModeParam;
import com.dji.sample.control.model.param.DronePayloadParam;
import com.dji.sample.control.model.param.PayloadCommandsParam;
import com.dji.sample.control.model.param.RemoteDebugParam;
import com.dji.sample.control.service.IControlService;
import com.dji.sample.control.service.IDrcService;
import com.dji.sample.manage.service.IDeviceRedisService;
import com.dji.sample.df.supControlDf.service.DroneControlService;
import com.dji.sdk.cloudapi.control.CameraTypeEnum;
import com.dji.sdk.cloudapi.control.ControlMethodEnum;
import com.dji.sdk.cloudapi.control.GimbalResetModeEnum;
import com.dji.sdk.cloudapi.debug.DebugMethodEnum;
import com.dji.sdk.cloudapi.debug.SdrWorkmodeSwitchRequest;
import com.dji.sdk.cloudapi.device.CameraModeEnum;
import com.dji.sdk.cloudapi.device.LinkWorkModeEnum;
import com.dji.sdk.common.HttpResultResponse;
import com.dji.sdk.mqtt.drc.DrcDownPublish;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static com.dji.sdk.cloudapi.debug.DebugMethodEnum.SDR_WORKMODE_SWITCH;

@Service
@Slf4j
public class DroneControlServiceImpl implements DroneControlService {
    //注入控制类
    @Autowired
    private IControlService controlService;
    //获取无人机控制权，非drc指令

    //在线校验
    @Autowired
    private IDeviceRedisService deviceRedisService;
    @Autowired
    private IDrcService drcService;
    @Resource
    private DrcDownPublish drcDownPublish;
    /**
    *String sn 机场网关唯一标识
    * */
    @Override
    public String flightAuthorityGrab(String sn) {
        HttpResultResponse result = controlService.seizeAuthority(sn, DroneAuthorityEnum.FLIGHT, null);
        if(result.getCode()==0){
            return "true";
        }else{
            return "false";
        }
    }
    /**
     * String sn:机场网关唯一标识
     * **/
    @Override
    public String return_home(String sn) {
        //RemoteDebugParam param=new RemoteDebugParam();
        HttpResultResponse result=controlService.controlDockDebug(sn, RemoteDebugMethodEnum.RETURN_HOME,null);
        if(result.getCode()==0){
            return "true";
        }else{
            return "false";
        }
    }

    @Override
    public String return_home_cancel(String sn) {
        HttpResultResponse result=controlService.controlDockDebug(sn,RemoteDebugMethodEnum.RETURN_HOME_CANCEL,null);
        if(result.getCode()==0){
            return "true";
        }else{
            return "false";
        }
    }

    @Override
    public String deviceDrcEnter(String sn) {
        String flag= this.flightAuthorityGrab(sn);
        if(flag.equals("true")){
            DrcModeParam drcModeParam=new DrcModeParam();
            drcModeParam.setClientId(UUID.randomUUID().toString());
            drcModeParam.setDockSn(sn);
            long currentTimeMillis = System.currentTimeMillis();
            LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(currentTimeMillis), ZoneId.systemDefault());
            LocalDateTime dateTimePlusOneDay = dateTime.plus(1, ChronoUnit.DAYS);
            long newTimeMillis = dateTimePlusOneDay.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            drcModeParam.setExpireSec(newTimeMillis);
            drcService.deviceDrcEnter("e3dea0f5-37f2-4d79-ae58-490af3228069",drcModeParam);
            return "true";
        }
        return "false";
    }

    @Override
    public String deviceDrcExit(String sn) {
        String flag= this.flightAuthorityGrab(sn);
        if(flag.equals("true")){
            DrcModeParam drcModeParam=new DrcModeParam();
            drcModeParam.setClientId(UUID.randomUUID().toString());
            drcModeParam.setDockSn(sn);
//            是否有必要去掉
//            long currentTimeMillis = System.currentTimeMillis();
//            LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(currentTimeMillis), ZoneId.systemDefault());
//            LocalDateTime dateTimePlusOneDay = dateTime.plus(1, ChronoUnit.DAYS);
//            long newTimeMillis = dateTimePlusOneDay.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
//            drcModeParam.setExpireSec(newTimeMillis);
            drcService.deviceDrcEnter("e3dea0f5-37f2-4d79-ae58-490af3228069",drcModeParam);
            return "true";
        }
        return "false";
    }

    /**
     *
     * @param sn 机场网关
     */
    //急停
    @Override
    public String droneEmergencyStop(String sn) {
        //0.抢夺飞行控制权
        String flag= this.flightAuthorityGrab(sn);
        if(flag.equals("true")){
            //1.进入drc模式
            DrcModeParam drcModeParam=new DrcModeParam();
            drcModeParam.setClientId(UUID.randomUUID().toString());
            drcModeParam.setDockSn(sn);
            // 获取当前系统时间戳（以毫秒为单位）
            long currentTimeMillis = System.currentTimeMillis();

            // 将时间戳转换为LocalDateTime对象
            LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(currentTimeMillis), ZoneId.systemDefault());

            // 在当前时间上加一天
            LocalDateTime dateTimePlusOneDay = dateTime.plus(1, ChronoUnit.DAYS);

            // 将加一天后的LocalDateTime对象转换回时间戳
            long newTimeMillis = dateTimePlusOneDay.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            drcModeParam.setExpireSec(newTimeMillis);
            drcService.deviceDrcEnter("e3dea0f5-37f2-4d79-ae58-490af3228069",drcModeParam);
            //2.急停
            drcDownPublish.publish(sn, ControlMethodEnum.DRONE_EMERGENCY_STOP.getMethod());
            return "true";
        }
        return "false";
    }

    /***
     *
     * @param sn 机场网关
     * @return
     */
    //自动降落
    @Override
    public String drcEmergencyLanding(String sn) {
        //1.检查飞机是否在线
        //2.进行飞行控制权抢夺
        String flag= this.flightAuthorityGrab(sn);
        //3.进入drc控制模式
        if(flag.equals("true")){
            //1.进入drc模式
            DrcModeParam drcModeParam=new DrcModeParam();
            drcModeParam.setClientId(UUID.randomUUID().toString());
            drcModeParam.setDockSn(sn);
            // 获取当前系统时间戳（以毫秒为单位）
            long currentTimeMillis = System.currentTimeMillis();

            // 将时间戳转换为LocalDateTime对象
            LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(currentTimeMillis), ZoneId.systemDefault());

            // 在当前时间上加一天
            LocalDateTime dateTimePlusOneDay = dateTime.plus(1, ChronoUnit.DAYS);

            // 将加一天后的LocalDateTime对象转换回时间戳
            long newTimeMillis = dateTimePlusOneDay.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            drcModeParam.setExpireSec(newTimeMillis);
            drcService.deviceDrcEnter("e3dea0f5-37f2-4d79-ae58-490af3228069",drcModeParam);
            //紧急降落
            drcDownPublish.publish(sn,"drone_emergency_stop");
            return "true";
        }
        //4.自动降落
        return "false";
    }

    @Override
    public String payload_authority_grab(String sn) {
        //1.检查无人机是否在线
        boolean flag = deviceRedisService.checkDeviceOnline(sn);
        //2.抢夺
        if(flag){
            DronePayloadParam param =new DronePayloadParam();
            param.setPayloadIndex("81-0-0");
            param.setCameraType(CameraTypeEnum.WIDE);
            float zoomFactor =10;
            param.setZoomFactor(zoomFactor);
            param.setCameraMode(CameraModeEnum.PHOTO);
            param.setLocked(true);
            param.setPitchSpeed(0.5);
            param.setYawSpeed(0.5);
            param.setX(0.5);
            param.setY(0.5);
            param.setResetMode(GimbalResetModeEnum.RECENTER);
            controlService.seizeAuthority(sn, DroneAuthorityEnum.PAYLOAD,param);
            return "true";
        }

        return "false";
    }
    //负载控制
    @Override
    public String payloadCommands(String sn, PayloadCommandsParam param) throws Exception {
        //先抢夺控制权
        String flag= this.payload_authority_grab(sn);
        //再控制
        if(flag.equals("true")){
            param.setSn(sn);
            controlService.payloadCommands(param);
            return "true";
        }
        return "false";
    }

    /***
     *
     * @param sn:机场网关
     * @param Type:命令类型
     * @param Command:指令
     * @param Item:参数
     */
    @Override
    public String droneControl(String sn, String Type, String Command, String Item) throws Exception {
        //检查无人机是否在线
        boolean isOnline= deviceRedisService.checkDeviceOnline(sn);
        if(!isOnline){
            return "false";
        }
        //控制权获取
        if(Type.equals("20001")&&Command.equals("6")){
            //负载
            this.payload_authority_grab(sn);
            //飞行
            return this.flightAuthorityGrab(sn);
        }
        //一键返航启动
        if(Type.equals("20001")&&Command.equals("3")&&Item.equals("1"))
        {
             return this.return_home(sn);
        }
        //取消一键返航
        if(Type.equals("20001")&&Command.equals("3")&&Item.equals("2"))
        {
            return this.return_home_cancel(sn);
        }
        //自动降落，有障碍物会停止
        if(Type.equals("20001")&&Command.equals("4")&&Item.equals("1"))
        {
            return this.drcEmergencyLanding(sn);
        }
        //控制模式切换：进入drc模式
        if(Type.equals("20001")&&Command.equals("5")&&Item.equals("1"))
        {
            return this.deviceDrcEnter(sn);
        }
        //控制模式切换：退出drc模式
        if(Type.equals("20001")&&Command.equals("5")&&Item.equals("2")){
            return this.deviceDrcExit(sn);
        }
        //云台控制，云台重置回中
        if(Type.equals("20003")&&Command.equals("5")){
            PayloadCommandsParam param= new PayloadCommandsParam();
            param.setSn(sn);
            DronePayloadParam data=new DronePayloadParam();
            data.setPayloadIndex("99-0-0");
            data.setResetMode(GimbalResetModeEnum.RECENTER);
            param.setData(data);
            param.setCmd(PayloadCommandsEnum.GIMBAL_RESET);
            return this.payloadCommands(sn,param);
        }
        //相机控制，切换相机模式
        if(Type.equals("20004")&&Command.equals("2")){
            PayloadCommandsParam param= new PayloadCommandsParam();
            param.setSn(sn);
            DronePayloadParam data=new DronePayloadParam();
            data.setPayloadIndex("99-0-0");
            switch (Item){
                case "0":
                    data.setCameraMode(CameraModeEnum.PHOTO);break;
                case "1":
                    data.setCameraMode(CameraModeEnum.VIDEO);break;
                case "2":
                    data.setCameraMode(CameraModeEnum.LOW_LIGHT_INTELLIGENCE);break;
                case "3":
                    data.setCameraMode(CameraModeEnum.PANORAMA);break;
            }
            param.setData(data);
            param.setCmd(PayloadCommandsEnum.CAMERA_MODE_SWitCH);
            return this.payloadCommands(sn,param);
        }
        //相机控制，拍照控制
        if(Type.equals("20004")&&Command.equals("3")){
            PayloadCommandsParam param= new PayloadCommandsParam();
            param.setSn(sn);
            DronePayloadParam data=new DronePayloadParam();
            data.setPayloadIndex("99-0-0");
            param.setData(data);
            param.setCmd(PayloadCommandsEnum.CAMERA_PHOTO_TAKE);
            return this.payloadCommands(sn,param);
        }

        return "false";
    }


}
