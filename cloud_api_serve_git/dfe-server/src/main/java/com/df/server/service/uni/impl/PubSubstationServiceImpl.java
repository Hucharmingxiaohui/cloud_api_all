package com.df.server.service.uni.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.df.framework.vo.Tree;
import com.df.server.entity.uni.PubSubstationEntity;
import com.df.server.mapper.uni.PubSubstationMapper;
import com.df.server.service.uni.PubSubstationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service("pubSubstationService")
public class PubSubstationServiceImpl extends ServiceImpl<PubSubstationMapper, PubSubstationEntity> implements PubSubstationService {


    @Override
    public List<Tree> pubSubstationTree() {
        return baseMapper.pubSubstationTree();
    }

    @Override
    public PubSubstationEntity getStationBySubCode(String subCode) {
        return this.lambdaQuery().eq(PubSubstationEntity::getSubCode, subCode).one();
    }

    @Override
    public List<String> listAllSubCode() {
        return this.list().stream().map(PubSubstationEntity::getSubCode).collect(Collectors.toList());
    }
}
