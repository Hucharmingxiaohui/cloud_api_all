package com.dji.sample.df.accessControlDf.service;

import com.dji.sample.df.accessControlDf.entity.UrTabDfEntity;

public interface UrTabDfService {
    //绑定角色、
    boolean createUr(UrTabDfEntity urTabDfEntity);
    //解除绑定
    boolean deleteUr(String user_id,String role_id);
    //根据用户信息查询角色
    UrTabDfEntity getUrByUserId(String user_id);
}
