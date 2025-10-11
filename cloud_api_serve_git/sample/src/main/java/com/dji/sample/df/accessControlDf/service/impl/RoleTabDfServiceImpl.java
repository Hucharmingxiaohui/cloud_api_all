package com.dji.sample.df.accessControlDf.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dji.sample.df.accessControlDf.dao.RoleTabDfMapper;
import com.dji.sample.df.accessControlDf.entity.RoleTabDfEntity;
import com.dji.sample.df.accessControlDf.service.RoleTabDfService;
import com.dji.sdk.common.Pagination;
import com.dji.sdk.common.PaginationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service//自动注入服务
@Transactional//创建事务，能回滚，保障数据库完整性
public class RoleTabDfServiceImpl implements RoleTabDfService {
    //角色mapper
    @Autowired
    private RoleTabDfMapper roleTabDfMapper;
    //添加一个角色

    /**
     *
     * @param roleTabDfEntity
     * 开发人员：ych
     * time:2025-2-21
     * @return
     */
    @Override
    public boolean addRole(RoleTabDfEntity roleTabDfEntity) {
        //设置角色id
        roleTabDfEntity.setRoleId(UUID.randomUUID().toString());
        //设置创建时间
        roleTabDfEntity.setCreateTime(System.currentTimeMillis());
        //设置更新时间
        roleTabDfEntity.setUpdateTime(System.currentTimeMillis());
        //校验角色名称，角色名称不能重复
        //查询重名的权限
        RoleTabDfEntity reNameRole=roleTabDfMapper.selectOne(new LambdaQueryWrapper<RoleTabDfEntity>()
                .eq(RoleTabDfEntity::getRoleName,roleTabDfEntity.getRoleName()));
        if(reNameRole==null)
        {
            roleTabDfMapper.insert(roleTabDfEntity);
            return true;
        }else {
            return false;
        }
    }
    //删除一个角色

    /**
     *
     * @param role_id
     * 开发人员：ych
     * time:2025-2-21
     * @return
     */
    @Override
    public boolean deleteRole(String role_id) {
        //先查询是否有这个角色，再执行删除操作
        RoleTabDfEntity roleTabDfEntity=roleTabDfMapper.selectOne(new LambdaQueryWrapper<RoleTabDfEntity>().
                eq(RoleTabDfEntity::getRoleId,role_id));
        if(roleTabDfEntity!=null)
        {
            roleTabDfMapper.delete(new LambdaQueryWrapper<RoleTabDfEntity>()
                    .eq(RoleTabDfEntity::getRoleId,role_id));
            return true;
        }else {
            return false;
        }
    }
    //更新一条角色信息

    /**
     *
     * @param roleTabDfEntity
     * 开发人员：ych
     * time:2025-2-21
     * @return
     */
    @Override
    public boolean updateRole(RoleTabDfEntity roleTabDfEntity) {
        //更新时间
        roleTabDfEntity.setUpdateTime(System.currentTimeMillis());
        //校验存在与否
        RoleTabDfEntity valiEntity=roleTabDfMapper.selectOne(new LambdaQueryWrapper<RoleTabDfEntity>()
                .eq(RoleTabDfEntity::getRoleId,roleTabDfEntity.getRoleId()));
        if(valiEntity!=null)
        {
            roleTabDfMapper.updateById(roleTabDfEntity);
            return true;
        }else {
            return false;
        }
    }
    //分页查询所有的角色

    /**
     *
     * @param page
     * @param pageSize
     * 开发人员：ych
     * time:2025-2-21
     * @return
     */
    @Override
    public PaginationData<RoleTabDfEntity> getRolesByPage(Long page, Long pageSize) {
        //查询分页信息
        Page<RoleTabDfEntity> rolesTab=roleTabDfMapper.selectPage(new Page<>(page,pageSize),null);
        //一页的角色信息
        List<RoleTabDfEntity> rolesList=rolesTab.getRecords();
        //提取信息进行返回
        return new PaginationData<RoleTabDfEntity>(rolesList,
                new Pagination(rolesTab.getCurrent(),rolesTab.getSize(),rolesTab.getTotal()));
    }
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
    @Override
    public PaginationData<RoleTabDfEntity> getRoleByName(Long page, Long pageSize, String roleName) {
        //查询分页信息
        Page<RoleTabDfEntity> rolesTab=roleTabDfMapper.selectPage(new Page<>(page,pageSize),
                new LambdaQueryWrapper<RoleTabDfEntity>().like(RoleTabDfEntity::getRoleName,roleName));
        //一页的角色信息
        List<RoleTabDfEntity> rolesList=rolesTab.getRecords();
        //提取信息进行返回
        return new PaginationData<RoleTabDfEntity>(rolesList,
                new Pagination(rolesTab.getCurrent(),rolesTab.getSize(),rolesTab.getTotal()));
    }
}
