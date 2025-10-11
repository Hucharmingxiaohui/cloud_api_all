package com.dji.sample.df.patrolDf.controller.robotDog;

import com.alibaba.fastjson.JSONObject;
import com.df.framework.config.FileConfig;
import com.df.framework.exception.FastException;
import com.df.framework.vo.DogPatrolVO;
import com.df.framework.vo.Result;
import com.df.server.dto.dogControl.DogControlDTO;
import com.df.server.patrol.control.RobotControlService;
import com.df.server.patrol.control.sendDto.RobotAPIDTO;
import com.df.server.patrol.control.sendDto.RobotCameraAPIDTO;
import com.df.server.patrol.control.sendDto.RobotCommandAPIDTO;
import com.df.server.patrol.control.sendDto.RobotGoAPIDTO;
import com.df.server.service.uni.UniRobotMappathService;
import com.df.server.service.uni.UniRobotService;
import com.df.server.vo.UniRobot.UniRobotVO;
import com.df.server.vo.UniRobotMappath.UniRobotMappathVO;
import com.dji.sample.component.AuthInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;


/**
 * 机器狗业务
 */
@Slf4j
@RestController
@RequestMapping("/dog/control")
@AuthInterceptor.IgnoreAuth
public class RobotDogController {

    @Autowired
    private RobotControlService robotControlService;
    @Autowired
    private UniRobotService uniRobotService;
    @Autowired
    private FileConfig fileConfig;
    @Autowired
    private UniRobotMappathService uniRobotMappathService;

    /**
     * 机器狗控制
     *
     * @return
     */
    @PostMapping("/controlParams")
    public Result<DogPatrolVO> controlDogByParams(@RequestBody DogControlDTO dogControlDTO) {
        RobotAPIDTO robotAPIDTO = null;
        String s = "";
        // 1为机器狗本体控制，2为云台控制，3为任务控制
        try {
            if (dogControlDTO.getType() == 1) {
                robotAPIDTO = new RobotGoAPIDTO(dogControlDTO.getKey());
                s = robotControlService.sendCustomGo(robotAPIDTO);
            }
            if (dogControlDTO.getType() == 2) {
                robotAPIDTO = new RobotCameraAPIDTO(dogControlDTO.getCommand());
                s = robotControlService.sendCustomCamera(robotAPIDTO);
            }
            if (dogControlDTO.getType() == 3) {
                robotAPIDTO = new RobotCommandAPIDTO(dogControlDTO.getCommand());
                s = robotControlService.sendCustomCommand(robotAPIDTO);
            }
        } catch (Exception e) {
            log.error("机器人服务不在线 {}", e.getMessage());
            throw new FastException("机器人服务不在线");
        }
        DogPatrolVO dogPatrolVO = JSONObject.parseObject(s, DogPatrolVO.class);
        return Result.success(dogPatrolVO);
    }

    /**
     * 获取机器狗的详情
     *
     * @param dogControlDTO
     * @return
     */
    @PostMapping("/getDogInfo")
    public Result<UniRobotVO> getDogInfo(@RequestBody DogControlDTO dogControlDTO) {
        UniRobotVO vo = uniRobotService.getDogInfo();
        vo.setFilePathUrl(fileConfig.getFileVisitUrl(vo.getFilePath()));
        return Result.success(vo);
    }

    /**
     * 获取机器狗的机器人路径点位
     *
     * @param dogControlDTO
     * @return
     */
    @PostMapping("/getDogMapCoordinatePixel")
    public Result<List<UniRobotMappathVO>> getDogMapCoordinatePixel(@RequestBody DogControlDTO dogControlDTO) {
        UniRobotVO vo = uniRobotService.getDogInfo();
        List<UniRobotMappathVO> list = uniRobotMappathService.listMapCoordinatePixel(vo.getRobotCode());
        return Result.success(list);
    }

    /**
     * 上传机器狗的地图文件
     *
     * @param file
     * @return
     */
    @PostMapping("/uploadRobotFile")
    public void uploadRobotFile(@RequestParam MultipartFile file) throws IOException {
        uniRobotService.uploadImage(file);
    }
}
