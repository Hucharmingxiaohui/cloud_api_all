package com.dji.sample.df.accessControlDf.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dji.sample.df.accessControlDf.dao.PrivsTabDfMapper;
import com.dji.sample.df.accessControlDf.entity.PrivsTabDfEntity;
import com.dji.sample.df.accessControlDf.service.PrivsTabDfservice;
import com.dji.sdk.common.Pagination;
import com.dji.sdk.common.PaginationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service//自动注入服务
@Transactional//创建事务，能回滚，保障数据库完整性
public class PrivsTabDfserviceImpl implements PrivsTabDfservice {
    //引入权限mapper
    @Autowired
    private PrivsTabDfMapper privsTabDfMapper;
    //新增加一条权限

    /**
     *
     * @param privsTabDfEntity
     * @开发人员：ych
     * @time:2025-2-13
     */
    @Override
    public boolean addPrivs(PrivsTabDfEntity privsTabDfEntity) {
        //设置权限id
        privsTabDfEntity.setPrivsId(UUID.randomUUID().toString());
        //设置创建时间
        privsTabDfEntity.setCreateTime(System.currentTimeMillis());
        //设置更新时间
        privsTabDfEntity.setUpdateTime(System.currentTimeMillis());
        //校验权限名称，权限名称不能重复
        //查询重名的权限
        PrivsTabDfEntity reNamePrivs=privsTabDfMapper.selectOne(new LambdaQueryWrapper<PrivsTabDfEntity>()
                .eq(PrivsTabDfEntity::getPrivsName,privsTabDfEntity.getPrivsName()));
        //如果为空则插入
        if(reNamePrivs==null){
            //插入成功，返回true
            privsTabDfMapper.insert(privsTabDfEntity);
            return true;
        }else {
            //插入失败返回false
            return false;
        }
    }
    //删除一条权限
    /**
     *
     * @param privs_id
     * @开发人员ych:2025-2-14
     */
    @Override
    public boolean deletePrivs(String privs_id) {
        //先查询是否有这条权限在执行删除操作
        PrivsTabDfEntity privsTabDfEntity=privsTabDfMapper.selectOne(new LambdaQueryWrapper<PrivsTabDfEntity>()
                .eq(PrivsTabDfEntity::getPrivsId,privs_id));
        if(privsTabDfEntity!=null)
        {
            //删除这条权限
            privsTabDfMapper.delete(new LambdaQueryWrapper<PrivsTabDfEntity>()
                    .eq(PrivsTabDfEntity::getPrivsId,privs_id));
            //返回true
            return true;
        }else {
            //如果参数不对，不删除
            return false;
        }
    }
    //修改一条权限的信息
    /**
     *
     * @param privsTabDfEntity
     * @开发人员:ych
     * time:2025-2-14
     */
    @Override
    public boolean updatePrivs(PrivsTabDfEntity privsTabDfEntity) {
        //设置修改的时间为当前的时间
        privsTabDfEntity.setUpdateTime(System.currentTimeMillis());
        //校验有没有这条权限
        PrivsTabDfEntity ValiEntity =privsTabDfMapper.selectOne(new LambdaQueryWrapper<PrivsTabDfEntity>()
                .eq(PrivsTabDfEntity::getPrivsId,privsTabDfEntity.getPrivsId()));
        //如果有就进行修改
        if(ValiEntity!=null)
        {
            //更新权限
            privsTabDfMapper.updateById(privsTabDfEntity);
            //返回true
            return true;
        }else{
            //更新失败，返回false
            return false;
        }
    }
    //分页查询所有的权限
    /**
     *
     * @param page
     * @param pageSize
     * @开发人员:ych
     * time:2025-2-14
     */
    @Override
    public PaginationData<PrivsTabDfEntity> getPrivsByPage(Long page, Long pageSize) {
        //分页查询出信息
        Page<PrivsTabDfEntity> privsPage=privsTabDfMapper.selectPage(new Page<>(page,pageSize),
                null);
        //一页的信息权限
        List<PrivsTabDfEntity> privsList=privsPage.getRecords();
        //提取信息进行返回
        return new PaginationData<PrivsTabDfEntity>(privsList,
                new Pagination(privsPage.getCurrent(),
                privsPage.getSize(),privsPage.getTotal()));
    }
    //根据权限名称查询权限
    /**
     *
     * @param page
     * @param pageSize
     * @param privsName
     * @开发人员:ych
     * time:2025-2-14
     */
    @Override
    public PaginationData<PrivsTabDfEntity> getPrivsByName(Long page, Long pageSize, String privsName) {
        //分页查询出信息
        Page<PrivsTabDfEntity> privsPage=privsTabDfMapper.selectPage(new Page<>(page,pageSize),
                new LambdaQueryWrapper<PrivsTabDfEntity>().like(PrivsTabDfEntity::getPrivsName,privsName));
        //一页的信息权限
        List<PrivsTabDfEntity> privsList=privsPage.getRecords();
        //提取信息进行返回
        return new PaginationData<PrivsTabDfEntity>(privsList,
                new Pagination(privsPage.getCurrent(),
                        privsPage.getSize(),privsPage.getTotal()));
    }
}
