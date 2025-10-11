package com.df.server.service.uni;


import com.baomidou.mybatisplus.extension.service.IService;
import com.df.framework.vo.PageVO;
import com.df.server.dto.UniPatrolSystem.UniPatrolSystemDTO;
import com.df.server.dto.UniPatrolSystem.UniPatrolSystemPageDTO;
import com.df.server.entity.uni.UniPatrolSystemEntity;
import com.df.server.vo.UniPatrolSystem.UniPatrolSystemVO;

import java.util.List;

/**
 * 巡检系统接口
 * <p>
 * Created by lianyc on 2023-11-09
 */
public interface UniPatrolSystemService extends IService<UniPatrolSystemEntity> {


    //自定义分页查询（可写SQL自己关联表）
    PageVO<UniPatrolSystemVO> customPage(UniPatrolSystemPageDTO pageDTO);

    /**
     * 获取下级巡视系统列表 针对平台是站端、区域、边缘（系统类型：edge\\robot\\uav）
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
     */
    List<UniPatrolSystemVO> listPatrolSysBySysCode(String sysCode);

    /**
     * * 获取点位应属巡视系统列表 （系统类型：video/edge/robot/uav）
     *
     * @param dto
     * @return
     */
    List<UniPatrolSystemVO> listPointSystem(UniPatrolSystemDTO dto);
}

