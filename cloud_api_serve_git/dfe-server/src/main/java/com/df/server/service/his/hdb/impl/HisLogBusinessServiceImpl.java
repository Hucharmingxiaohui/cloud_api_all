package com.df.server.service.his.hdb.impl;


import com.df.framework.utils.PageUtils;
import com.df.server.entity.his.HisLogBusinessEntity;
import com.df.server.mapper.his.HisLogBusinessMapper;
import com.df.server.service.his.hdb.HisLogBusinessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service("hisLogBusinessService")
public class HisLogBusinessServiceImpl implements HisLogBusinessService {


    @Autowired
    private HisLogBusinessMapper baseMapper;


    @Override
    public void insertHisLogBusiness(HisLogBusinessEntity entity) {
        baseMapper.insertHisLogBusiness(entity);
    }


}
