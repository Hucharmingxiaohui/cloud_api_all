package com.dji.sample.df.testDf.service.impl;

import com.dji.sample.df.electricInspectionDf.model.PubSubstationDfEntity;
import com.dji.sample.df.testDf.dao.TestMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class TestServiceImpl implements TestService {

    @Resource
    TestMapper testMapper;
    @Override
    public List<PubSubstationDfEntity> query() {
        return testMapper.query();
    }
}
