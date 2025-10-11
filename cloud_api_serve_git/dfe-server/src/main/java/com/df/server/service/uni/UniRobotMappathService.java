package com.df.server.service.uni;


import com.baomidou.mybatisplus.extension.service.IService;
import com.df.server.entity.uni.UniRobotMappathEntity;
import com.df.server.vo.UniRobotMappath.UniRobotMappathVO;

import java.util.List;

/**
 * 机器人巡检路线接口
 * <p>
 * Created by lianyc on 2025-05-26
 */
public interface UniRobotMappathService extends IService<UniRobotMappathEntity> {


    List<UniRobotMappathVO> listMapCoordinatePixel(String robotCode);

    void syncRobotMapCoordinatePixel();

}

