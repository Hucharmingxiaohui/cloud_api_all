package com.df.server.service.uni.impl;


import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.df.server.entity.uni.UniRobotMappathEntity;
import com.df.server.mapper.uni.UniRobotMappathMapper;
import com.df.server.mapper.uni.UniRobotMapper;
import com.df.server.patrol.control.RobotControlService;
import com.df.server.patrol.control.receiceDTO.DogReceiveDTO;
import com.df.server.patrol.control.sendDto.RobotCommandAPIDTO;
import com.df.server.service.uni.UniRobotMappathService;
import com.df.server.vo.UniRobot.UniRobotVO;
import com.df.server.vo.UniRobotMappath.UniRobotMappathVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service("uniRobotMappathService")
public class UniRobotMappathServiceImpl extends ServiceImpl<UniRobotMappathMapper, UniRobotMappathEntity> implements UniRobotMappathService {

    @Autowired
    private UniRobotMapper uniRobotMapper;
    @Autowired
    private RobotControlService robotControlService;


    @Override
    public List<UniRobotMappathVO> listMapCoordinatePixel(String robotCode) {
        return baseMapper.listMapCoordinatePixel(robotCode);
    }

    @Override
    public void syncRobotMapCoordinatePixel() {
        UniRobotVO dogInfo = uniRobotMapper.getDogInfo();
        Integer onlineState = dogInfo.getOnlineState();
        if (onlineState != null && onlineState == 1) {
            try {
                String s = robotControlService.sendCustomNode(new RobotCommandAPIDTO(""));
                List<DogReceiveDTO> points = JSON.parseArray(s, DogReceiveDTO.class);
                baseMapper.clearMap(dogInfo.getRobotCode());
                for (int i = 0; i < points.size(); i++) {
                    DogReceiveDTO dogReceiveDTO = points.get(i);
                    Integer x = dogReceiveDTO.getX();
                    Integer y = dogReceiveDTO.getY();
                    String coordinatePixel = x + "," + y + ",0" + ",0";

                    UniRobotMappathEntity uniRobotMappathEntity = new UniRobotMappathEntity();
                    uniRobotMappathEntity.setRobotCode(dogInfo.getRobotCode());
                    uniRobotMappathEntity.setRobotName(dogInfo.getRobotName());
                    uniRobotMappathEntity.setNodeNo(i + 1);
                    uniRobotMappathEntity.setCoordinatePixel(coordinatePixel);
                    this.save(uniRobotMappathEntity);
                }
            } catch (Exception e) {
                log.error("同步机器巡视路径异常，{}", e.getMessage());
            }
        }
    }
}
