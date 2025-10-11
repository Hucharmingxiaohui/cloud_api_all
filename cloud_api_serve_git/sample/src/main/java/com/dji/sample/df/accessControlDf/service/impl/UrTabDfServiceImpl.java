package com.dji.sample.df.accessControlDf.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dji.sample.df.accessControlDf.dao.UrTabDfMapper;
import com.dji.sample.df.accessControlDf.entity.UrTabDfEntity;
import com.dji.sample.df.accessControlDf.service.UrTabDfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service//自动注入服务
@Transactional//创建事务，能回滚，保障数据库完整性
public class UrTabDfServiceImpl implements UrTabDfService {
    @Autowired
    private UrTabDfMapper urTabDfMapper;
    //绑定角色、
    @Override
    public boolean createUr(UrTabDfEntity urTabDfEntity) {
        //校验存在
        UrTabDfEntity ur=urTabDfMapper.selectOne(new LambdaQueryWrapper<UrTabDfEntity>()
                .eq(UrTabDfEntity::getUserId,urTabDfEntity.getUserId())
                .eq(UrTabDfEntity::getRoleId,urTabDfEntity.getRoleId()));
        if(ur!=null)
        {
            urTabDfMapper.insert(urTabDfEntity);
            return true;
        }else {
            return false;
        }

    }
    //解除绑定
    @Override
    public boolean deleteUr(String user_id, String role_id) {
        //校验存在
        UrTabDfEntity ur=urTabDfMapper.selectOne(new LambdaQueryWrapper<UrTabDfEntity>()
                .eq(UrTabDfEntity::getUserId,user_id)
                .eq(UrTabDfEntity::getRoleId,role_id));

        if(ur!=null)
        {
            urTabDfMapper.deleteById(ur.getId());
            return true;
        }else {
            return false;
        }
    }

    @Override
    public UrTabDfEntity getUrByUserId(String user_id) {
        UrTabDfEntity urTabDfEntity=urTabDfMapper.selectOne(new
                LambdaQueryWrapper<UrTabDfEntity>().
                eq(UrTabDfEntity::getUserId,user_id));

        return urTabDfEntity;
    }
}
