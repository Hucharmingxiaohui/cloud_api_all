package com.df.server.mapper.uni;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.df.server.entity.uni.UniRobotMappathEntity;
import com.df.server.vo.UniRobotMappath.UniRobotMappathVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 机器人巡检路线接口
 * <p>
 * Created by lianyc on 2025-05-26
 */
@Mapper
public interface UniRobotMappathMapper extends BaseMapper<UniRobotMappathEntity> {
    List<UniRobotMappathVO> listMapCoordinatePixel(String robotCode);

    void clearMap(String robotCode);
}
