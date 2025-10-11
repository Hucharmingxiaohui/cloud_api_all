package com.df.server.mapper.uni;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.df.server.entity.uni.UniRobotEntity;
import com.df.server.patrol.control.receiceDTO.DogReceiveDTO;
import com.df.server.vo.UniRobot.UniRobotVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

@Mapper
public interface UniRobotMapper extends BaseMapper<UniRobotEntity> {

    UniRobotEntity selectBySubCode(@Param("subCode") String subCode);

    List<UniRobotEntity> listRobotByCodeSet(Set<String> robotCodeList);

    int countUnRunningRobot(List<String> waiteRobotCodeList);

    void updateReceiveDTO(DogReceiveDTO dogReceiveDTO);

    void updateCoordinate(@Param("robotCode") String robotCode, @Param("coordinatePixel") String coordinatePixel);

    UniRobotVO getDogInfo();

    void updateMap(@Param("robotCode") String robotCode, @Param("mapPath") String mapPath);

    void updateRobotOnlineState(@Param("commState") int commState,
                                @Param("online") int online);
}