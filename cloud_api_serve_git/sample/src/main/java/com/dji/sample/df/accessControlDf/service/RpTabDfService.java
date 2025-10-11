package com.dji.sample.df.accessControlDf.service;

import com.dji.sample.df.accessControlDf.dto.RpsDTO;
import com.dji.sample.df.accessControlDf.entity.RpTabDfEntity;

import java.util.List;

public interface RpTabDfService {
    //绑定权限
    boolean createRp(RpTabDfEntity rpTabDfEntity);
    //解除权限绑定
    //查询权限,查询所有的角色所拥有的权限
    boolean deleteRp(String role_id,String prvis_id);
    List<RpsDTO> getRolePrivs();
}
