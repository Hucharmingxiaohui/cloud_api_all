package com.df.server.service.robotDog;

import com.df.server.dto.robotDog.RobotDogPointResultDTO;
import com.df.server.dto.uniPoint.BindNodeDTO;

import java.util.Map;

/**
 * @author 平善闯
 * @date 2025-04-13 14:30
 */
public interface RobotDogPatrolService {
    /**
     * 新建点位
     *
     * @return
     */
    Map<String, Object> addNode();

    /**
     * 点位绑定
     *
     * @param param
     */
    void bindNode(BindNodeDTO param);

    /**
     * 处理巡视抓拍结果
     *
     * @param robotDogPointResultDTO
     */
    void handleDogResult(RobotDogPointResultDTO robotDogPointResultDTO);

    /**
     * 保存预置位
     *
     * @return
     */
    Map<String, Object> savePos();

}
