package com.dji.sample.df.patrolDf.controller.robotDog;

import com.df.framework.vo.Result;
import com.df.framework.websocket.WebSocketEndpoint;
import com.df.server.entity.uni.PubSubstationEntity;
import com.df.server.entity.uni.UniRobotEntity;
import com.df.server.mapper.uni.PubSubstationMapper;
import com.df.server.mapper.uni.UniRobotMapper;
import com.df.server.patrol.control.receiceDTO.DogReceiveDTO;
import com.dji.sample.component.AuthInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * @author 平善闯
 * @date 2025-04-18 13:48
 */
@Slf4j
@RestController
@RequestMapping("/run")
@AuthInterceptor.IgnoreAuth
public class RobotDogRunStateController {
    @Autowired
    private UniRobotMapper uniRobotMapper;
    @Autowired
    private PubSubstationMapper pubSubstationMapper;
    @Autowired
    private WebSocketEndpoint webSocketEndpoint;

    /**
     * 电池电量
     *
     * @param dogReceiveDTO
     * @param request
     * @return
     */
    @PostMapping("/battery")
    public Result battery(@RequestBody DogReceiveDTO dogReceiveDTO, HttpServletRequest request) {
        log.info("【收到机器狗电量信息，{}】", dogReceiveDTO.getBattery_soc());
        PubSubstationEntity pubSubstation = pubSubstationMapper.getOneStation();
        UniRobotEntity uni = uniRobotMapper.selectBySubCode(pubSubstation.getSubCode());
        dogReceiveDTO.setRobotCode(uni.getRobotCode());
        uniRobotMapper.updateReceiveDTO(dogReceiveDTO);
        webSocketEndpoint.sendMsg("robot", uniRobotMapper.selectBySubCode(pubSubstation.getSubCode()));
        return Result.success();
    }

    /**
     * 电机温度
     *
     * @param dogReceiveDTO
     * @param request
     * @return
     */
    @PostMapping("/motortemp")
    public Result motortemp(@RequestBody DogReceiveDTO dogReceiveDTO, HttpServletRequest request) {
        log.info("【收到机器狗电机温度信息，{}】", dogReceiveDTO.getMax_temperature());
        PubSubstationEntity pubSubstation = pubSubstationMapper.getOneStation();
        UniRobotEntity uni = uniRobotMapper.selectBySubCode(pubSubstation.getSubCode());
        dogReceiveDTO.setRobotCode(uni.getRobotCode());
        uniRobotMapper.updateReceiveDTO(dogReceiveDTO);
        webSocketEndpoint.sendMsg("robot", uniRobotMapper.selectBySubCode(pubSubstation.getSubCode()));
        return Result.success();
    }

    /**
     * 机器人图中实时像素坐标
     *
     * @param dogReceiveDTO
     * @param request
     * @return
     */
    @PostMapping("/frames")
    public Result frames(@RequestBody DogReceiveDTO dogReceiveDTO, HttpServletRequest request) {
        log.info("【收到机器狗坐标信息，x：{} y：{}】", dogReceiveDTO.getX(), dogReceiveDTO.getY());
        PubSubstationEntity pubSubstation = pubSubstationMapper.getOneStation();
        UniRobotEntity uni = uniRobotMapper.selectBySubCode(pubSubstation.getSubCode());
        Integer x = dogReceiveDTO.getX();
        Integer y = dogReceiveDTO.getY();
        String coordinate_pixel = x + "," + y + ",0" + ",0";
        uniRobotMapper.updateCoordinate(uni.getRobotCode(), coordinate_pixel);
        webSocketEndpoint.sendMsg("robot", uniRobotMapper.selectBySubCode(pubSubstation.getSubCode()));
        return Result.success();
    }

    /**
     * 执行任务状态
     *
     * @param dogReceiveDTO
     * @param request
     * @return
     */
    @PostMapping("/state")
    public Result state(@RequestBody DogReceiveDTO dogReceiveDTO, HttpServletRequest request) {
        log.info("【收到机器狗执行任务状态信息，{}】", dogReceiveDTO.getState());
        PubSubstationEntity pubSubstation = pubSubstationMapper.getOneStation();
        UniRobotEntity uni = uniRobotMapper.selectBySubCode(pubSubstation.getSubCode());
        dogReceiveDTO.setRobotCode(uni.getRobotCode());
        uniRobotMapper.updateReceiveDTO(dogReceiveDTO);
        webSocketEndpoint.sendMsg("robot", uniRobotMapper.selectBySubCode(pubSubstation.getSubCode()));
        return Result.success();
    }
}
