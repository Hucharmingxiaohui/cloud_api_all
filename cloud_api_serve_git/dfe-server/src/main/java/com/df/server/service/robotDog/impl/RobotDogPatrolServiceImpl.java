package com.df.server.service.robotDog.impl;

import com.alibaba.fastjson.JSONArray;
import com.df.framework.config.FileConfig;
import com.df.framework.exception.FastException;
import com.df.framework.utils.HttpUtils;
import com.df.framework.utils.CustomStringUtils;
import com.df.server.dto.robotDog.AnalyseImageInfo;
import com.df.server.dto.robotDog.AnalyseParamsReq;
import com.df.server.dto.robotDog.RobotDogPointResultDTO;
import com.df.server.dto.robotDog.TaskInfo;
import com.df.server.dto.uniPoint.BindNodeDTO;
import com.df.server.dto.uniPoint.PointVideoPosDTO;
import com.df.server.entity.his.HisUniTaskEntity;
import com.df.server.entity.his.HisUniTaskItemFileEntity;
import com.df.server.entity.his.HisUniTaskItemPointsEntity;
import com.df.server.entity.uni.PubSubstationEntity;
import com.df.server.entity.uni.UniPointEntity;
import com.df.server.mapper.his.HisUniTaskItemFileMapper;
import com.df.server.mapper.his.HisUniTaskItemPointsMapper;
import com.df.server.mapper.his.HisUniTaskMapper;
import com.df.server.mapper.sys.SysConfigMapper;
import com.df.server.mapper.uni.PubSubstationMapper;
import com.df.server.mapper.uni.UniPointMapper;
import com.df.server.patrol.control.RobotControlService;
import com.df.server.patrol.control.sendDto.RobotCameraAPIDTO;
import com.df.server.patrol.control.sendDto.RobotCommandAPIDTO;
import com.df.server.patrol.control.sendDto.RobotParamsConstant;
import com.df.server.service.his.HisExePointAnalyseService;
import com.df.server.service.robotDog.RobotDogPatrolService;
import com.df.server.vo.his.HisUniTaskVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * @author 平善闯
 * @date 2025-04-13 14:26
 */
@Slf4j
@Service
public class RobotDogPatrolServiceImpl implements RobotDogPatrolService {
    @Autowired
    PubSubstationMapper pubSubstationMapper;
    @Autowired
    UniPointMapper uniPointMapper;
    @Autowired
    FileConfig fileConfig;
    @Autowired
    HisUniTaskMapper hisUniTaskMapper;
    @Autowired
    HisUniTaskItemFileMapper hisUniTaskItemFileMapper;
    @Autowired
    HisUniTaskItemPointsMapper hisUniTaskItemPointsMapper;
    @Autowired
    HisExePointAnalyseService hisExePointAnalyseService;
    @Autowired
    private RobotControlService robotControlService;

    @Override
    public Map<String, Object> addNode() {
        String result = null;
        try {
            result = robotControlService.sendCustomCommand(new RobotCommandAPIDTO(RobotParamsConstant.COMMAND_XINJIAN_DAOHANGDIAN));
        } catch (Exception e) {
            log.error("机器人服务不在线 {}", e.getMessage());
            throw new FastException("机器人服务不在线");
        }
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> map = null;
        try {
            map = mapper.readValue(result, Map.class);
            if (map.get("data") == null) {
                throw new FastException("获取预置位号失败");
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        PubSubstationEntity pubSubstation = pubSubstationMapper.getOneStation();
        map.put("subCode", pubSubstation.getSubCode());
        map.put("subName", pubSubstation.getSubName());
        return map;
    }

    @Override
    public Map<String, Object> savePos() {
        String result = null;
        try {
            result = robotControlService.sendCustomCamera(new RobotCameraAPIDTO(RobotParamsConstant.CAMERA_YUZHIWEIBAOCUN));
        } catch (Exception e) {
            log.error("机器人服务不在线 {}", e.getMessage());
            throw new FastException("机器人服务不在线");
        }
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> map = null;
        try {
            map = mapper.readValue(result, Map.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
       /* PubSubstationEntity pubSubstation = pubSubstationMapper.getOneStation();
        map.put("subCode", pubSubstation.getSubCode());
        map.put("subName", pubSubstation.getSubName());*/
        return map;
    }

    @Override
    public void bindNode(BindNodeDTO param) {
        String robotPos = param.getRobotPos();
        String robotCode = param.getRobotCode();
        String subCode = param.getSubCode();
        String pointCode = param.getPointCode();
        PointVideoPosDTO videoPosDTO = new PointVideoPosDTO();
        videoPosDTO.setRobot_code(robotCode);
        videoPosDTO.setDevice_pos(robotPos);
        List<PointVideoPosDTO> pointVideoPosList = new ArrayList();
        pointVideoPosList.add(videoPosDTO);
        String videoPos = JSONArray.toJSONString(pointVideoPosList);
        uniPointMapper.clearRobotPos(robotPos, robotCode);
        uniPointMapper.updatePointRobotPos(subCode, pointCode, videoPos, robotPos, robotCode);

    }

    @Override
    public void handleDogResult(RobotDogPointResultDTO robotDogPointResultDTO) {
        log.info("【收到机器狗点位结果，消息内容为，{}】", robotDogPointResultDTO.getId());
        if (CustomStringUtils.isEmpty(robotDogPointResultDTO.getImages())) {
            return;
        }
        HisUniTaskVO hisUniTask = hisUniTaskMapper.getRunningTask();
        if (hisUniTask == null) {
            log.info("【当前没有执行/暂停中的任务】");
            return;
        }
        String taskPatrolledId = hisUniTask.getTaskPatrolledId();
        String robotPos = robotDogPointResultDTO.getId();
        //找到对应的巡视点位
        HisUniTaskItemPointsEntity hisUniTaskItemPoint =
                hisUniTaskItemPointsMapper.getHisPointByRobotResult(taskPatrolledId, robotPos);
        if (hisUniTaskItemPoint == null) {
            log.error("【收到机器狗点位结果，但没有找到巡视任务点位，消息内容为，{}】", robotDogPointResultDTO.getId());
            return;
        }
        String subCode = hisUniTaskItemPoint.getSubCode();
        String pointCode = hisUniTaskItemPoint.getPointCode();
        String requestId = hisUniTaskItemPoint.getRequestId();
        //更新抓拍时间
        hisUniTaskItemPointsMapper.updateRunTime(taskPatrolledId, robotPos);

        //然后处理item_file表
        HisUniTaskItemFileEntity fileEntity = hisUniTaskItemFileMapper.getByRequestId(requestId);
        if (fileEntity == null) {
            log.error("【收到机器狗点位结果，但没有找到巡视任务点位文件信息，消息内容为，requestId：{}】", requestId);
            return;
        }
        UniPointEntity uniPoint = uniPointMapper.getPointEntityByCode(subCode, pointCode);
        if (uniPoint == null) {
            log.error("【收到机器狗点位结果，但没有找到点位信息，消息内容为，subCode：{}, pointCode：{}】", subCode, pointCode);
            return;
        }
        //开始解析图片并存储
        String filePath = handleImage(robotDogPointResultDTO.getImages(), fileConfig.getFileSavePath(), subCode, pointCode);
        fileEntity.setFileType(uniPoint.getSaveTypeList());
        fileEntity.setFilePath(filePath);
        hisUniTaskItemFileMapper.updateById(fileEntity);

        //发送智能分析
        AnalyseParamsReq analyseParamsReq = new AnalyseParamsReq();
        //和杨哥有个终生的约定，机器人二次分析的请求，objectId传递，点位编码+"_1"
        analyseParamsReq.setObjectId(pointCode + "_1");
        analyseParamsReq.setImageUrlList(Lists.newArrayList(filePath));
        //typeList
        String pointAnalyseType = uniPoint.getPointAnalyseType();
        if (CustomStringUtils.isNotEmpty(pointAnalyseType)) {
            List<String> typeList = Arrays.asList(pointAnalyseType.split(","));
            analyseParamsReq.setTypeList(typeList);
        }

        List<AnalyseParamsReq> analyseParamsReqList = new ArrayList<>();
        analyseParamsReqList.add(analyseParamsReq);
        AnalyseImageInfo analyseImageInfo = new AnalyseImageInfo();
        analyseImageInfo.setRequestId(requestId);
        analyseImageInfo.setAnalyseParamsReqList(analyseParamsReqList);
        analyseImageInfo.setTaskInfo(new TaskInfo(subCode, taskPatrolledId, "", "", null));
        hisExePointAnalyseService.sendAnalyse(analyseImageInfo);
    }

    public String handleImage(String base64, String savePath, String subCode, String pointCode) {
        try {
            String suffix = "jpg"; // 默认 jpg
            if (base64.contains("image/png")) {
                suffix = "png";
            } else if (base64.contains("image/jpeg")) {
                suffix = "jpg";
            }
            // 获取当前年月日
            LocalDate today = LocalDate.now();
            String year = String.valueOf(today.getYear());
            String month = String.format("%02d", today.getMonthValue());
            String day = String.format("%02d", today.getDayOfMonth());
            // 构建完整路径
            String relativePath = Paths.get(subCode, year, month, day).toString();
            String dirPath = Paths.get(savePath, relativePath).toString();
            // 创建目录
            File dir = new File(dirPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            LocalDateTime now = LocalDateTime.now();
            String formatted = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            // 生成唯一文件名
            String fileName = formatted + "_" + pointCode + "." + suffix;
            String fullPath = Paths.get(dirPath, fileName).toString();
            log.info("【机器狗图片文件存储，存储路径，{}】", fullPath);
            // 解码Base64字符串为字节数组
            // 解码并保存
            byte[] imageBytes = Base64.getDecoder().decode(base64);
            // 将字节数组写入到文件
            Files.write(Paths.get(fullPath), imageBytes);
            return Paths.get(relativePath, fileName).toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "获取图片失败";
        }
    }

}
