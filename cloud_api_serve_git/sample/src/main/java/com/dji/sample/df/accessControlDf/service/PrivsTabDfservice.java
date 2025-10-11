package com.dji.sample.df.accessControlDf.service;

import com.dji.sample.df.accessControlDf.entity.PrivsTabDfEntity;
import com.dji.sdk.common.PaginationData;

public interface PrivsTabDfservice {
    //新增加一条权限
    /**
     *
     * @param privsTabDfEntity
     * @开发人员：ych
     * @time:2025-2-13
     */
    boolean addPrivs(PrivsTabDfEntity privsTabDfEntity);
    //删除一条权限
    /**
     *
     * @param privs_id
     * @开发人员:ych
     * time:2025-2-14
     */
    boolean deletePrivs(String privs_id);
    //修改一条权限的信息
    /**
     *
     * @param privsTabDfEntity
     * @开发人员:ych
     * time:2025-2-14
     */
    boolean updatePrivs(PrivsTabDfEntity privsTabDfEntity);
    //分页查询所有的权限
    /**
     *
     * @param page
     * @param pageSize
     * @开发人员:ych
     * time:2025-2-14
     */
    PaginationData<PrivsTabDfEntity> getPrivsByPage(Long page,Long pageSize);
    //根据权限名称查询权限
    /**
     *
     * @param page
     * @param pageSize
     * @param privsName
     * @开发人员:ych
     * time:2025-2-14
     */
    PaginationData<PrivsTabDfEntity> getPrivsByName(Long page,Long pageSize,String privsName);

}
