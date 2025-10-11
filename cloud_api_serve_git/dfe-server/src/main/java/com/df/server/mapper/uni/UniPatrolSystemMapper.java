package com.df.server.mapper.uni;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.df.server.dto.UniPatrolSystem.UniPatrolSystemDTO;
import com.df.server.dto.UniPatrolSystem.UniPatrolSystemPageDTO;
import com.df.server.entity.uni.UniPatrolSystemEntity;
import com.df.server.vo.UniPatrolSystem.UniPatrolSystemVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 巡检系统接口
 * <p>
 * Created by lianyc on 2023-11-09
 */
@Mapper
public interface UniPatrolSystemMapper extends BaseMapper<UniPatrolSystemEntity> {

    //自定义分页查询
    List<UniPatrolSystemVO> getPageList(UniPatrolSystemPageDTO pageDTO);

    Integer getPageTotal(UniPatrolSystemPageDTO pageDTO);

    /**
     * 获取下级巡视系统列表 针对平台是站端、区域、边缘（系统类型：edge/robot/uav）
     *
     * @param dto
     * @return
     */
    List<UniPatrolSystemVO> listLowerSystem(UniPatrolSystemDTO dto);

    /**
     * 获取视频系统编码
     *
     * @return
     */
    String getVideoSysCode();

    /**
     * 根据系统编码查询系统信息
     *
     * @param sysCode
     * @return
     */
    List<UniPatrolSystemVO> listPatrolSysBySysCode(String sysCode);

    /**
     * 获取点位应属巡视系统列表 （系统类型：video/edge/robot/uav）
     *
     * @param dto
     * @return
     */
    List<UniPatrolSystemVO> listPointSystem(UniPatrolSystemDTO dto);
}