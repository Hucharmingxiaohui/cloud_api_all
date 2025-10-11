package com.df.server.service.uni.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.df.framework.utils.PageUtils;
import com.df.framework.vo.PageVO;
import com.df.server.dto.UniPatrolSystem.UniPatrolSystemDTO;
import com.df.server.dto.UniPatrolSystem.UniPatrolSystemPageDTO;
import com.df.server.entity.uni.UniPatrolSystemEntity;
import com.df.server.mapper.uni.UniPatrolSystemMapper;
import com.df.server.service.uni.UniPatrolSystemService;
import com.df.server.vo.UniPatrolSystem.UniPatrolSystemVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("uniPatrolSystemService")
public class UniPatrolSystemServiceImpl extends ServiceImpl<UniPatrolSystemMapper, UniPatrolSystemEntity> implements UniPatrolSystemService {

    // 自定义分页查询
    @Override
    public PageVO<UniPatrolSystemVO> customPage(UniPatrolSystemPageDTO pageDTO) {
        List<UniPatrolSystemVO> list = baseMapper.getPageList(pageDTO);
        Integer total = baseMapper.getPageTotal(pageDTO);
        return PageUtils.returnPageVOExtend(pageDTO, list, total);
    }

    @Override
    public List<UniPatrolSystemVO> listLowerSystem(UniPatrolSystemDTO dto) {
        return baseMapper.listLowerSystem(dto);
    }

    @Override
    public String getVideoSysCode() {
        return baseMapper.getVideoSysCode();
    }

    @Override
    public List<UniPatrolSystemVO> listPatrolSysBySysCode(String sysCode) {
        return baseMapper.listPatrolSysBySysCode(sysCode);
    }

    @Override
    public List<UniPatrolSystemVO> listPointSystem(UniPatrolSystemDTO dto) {
        return baseMapper.listPointSystem(dto);
    }
}
