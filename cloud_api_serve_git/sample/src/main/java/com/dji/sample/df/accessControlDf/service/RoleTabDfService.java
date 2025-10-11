package com.dji.sample.df.accessControlDf.service;

import com.dji.sample.df.accessControlDf.entity.RoleTabDfEntity;
import com.dji.sdk.common.PaginationData;

public interface RoleTabDfService {
    //添加一个角色

    /**
     *
     * @param roleTabDfEntity
     * 开发人员：ych
     * time:2025-2-21
     * @return
     */
    boolean addRole(RoleTabDfEntity roleTabDfEntity);
    //删除一个角色

    /**
     *
     * @param role_id
     * 开发人员：ych
     * time:2025-2-21
     * @return
     */
    boolean deleteRole(String role_id);
    //更新一条角色信息

    /**
     *
     * @param roleTabDfEntity
     * 开发人员：ych
     * time:2025-2-21
     * @return
     */
    boolean updateRole(RoleTabDfEntity roleTabDfEntity);
    //分页查询所有的角色

    /**
     *
     * @param page
     * @param pageSize
     * 开发人员：ych
     * time:2025-2-21
     * @return
     */
    PaginationData<RoleTabDfEntity> getRolesByPage(Long page,Long pageSize);
    //根据角色名称查询权限

    /**
     *
     * @param page
     * @param pageSize
     * @param roleName
     * 开发人员：ych
     * time:2025-2-21
     * @return
     */
    PaginationData<RoleTabDfEntity> getRoleByName(Long page,Long pageSize,String roleName);
}
