package com.dji.sample.df.accessControlDf.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dji.sample.df.accessControlDf.dao.PrivsTabDfMapper;
import com.dji.sample.df.accessControlDf.dao.RoleTabDfMapper;
import com.dji.sample.df.accessControlDf.dao.RpTabDfMapper;
import com.dji.sample.df.accessControlDf.dto.RpDTO;
import com.dji.sample.df.accessControlDf.dto.RpsDTO;
import com.dji.sample.df.accessControlDf.entity.PrivsTabDfEntity;
import com.dji.sample.df.accessControlDf.entity.RoleTabDfEntity;
import com.dji.sample.df.accessControlDf.entity.RpTabDfEntity;
import com.dji.sample.df.accessControlDf.service.RpTabDfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service//自动注入服务
@Transactional//创建事务，能回滚，保障数据库完整性
public class RpTabDfServiceImpl implements RpTabDfService {
    //权限角色关联
    @Autowired
    RpTabDfMapper rpTabDfMapper;
    //角色
    @Autowired
    RoleTabDfMapper roleTabDfMapper;
    //权限
    @Autowired
    PrivsTabDfMapper privsTabDfMapper;


    //绑定权限
    @Override
    public boolean createRp(RpTabDfEntity rpTabDfEntity) {
        rpTabDfEntity.setCreateTime(System.currentTimeMillis());
        rpTabDfEntity.setUpdateTime(System.currentTimeMillis());
        //查询有没有重复绑定
        RpTabDfEntity rp=rpTabDfMapper.selectOne(new LambdaQueryWrapper<RpTabDfEntity>()
                .eq(RpTabDfEntity::getRoleId,rpTabDfEntity.getRoleId()).
                eq(RpTabDfEntity::getPrivsId,rpTabDfEntity.getPrivsId()));
        if(rp==null)
        {
            rpTabDfMapper.insert(rpTabDfEntity);
            return true;
        }else {
            return false;
        }
    }


    //解除权限绑定
    @Override
    public boolean deleteRp(String role_id, String prvis_id) {
        //查询有没有关联
        RpTabDfEntity rp=rpTabDfMapper.selectOne(new LambdaQueryWrapper<RpTabDfEntity>()
                .eq(RpTabDfEntity::getRoleId,role_id).
                eq(RpTabDfEntity::getPrivsId,prvis_id));
        if(rp!=null)
        {
            rpTabDfMapper.deleteById(rp.getId());
            return true;
        }else {
            return false;
        }
    }
    //查询权限,查询所有的角色所拥有的权限
    @Override
    public  List<RpsDTO> getRolePrivs() {
        //查询所有的角色
        List<RoleTabDfEntity> roles=roleTabDfMapper.selectList(null);
        //查询所有的权限
        List<PrivsTabDfEntity> privs=privsTabDfMapper.selectList(null);
        List<RpsDTO> rpsList=new ArrayList<>();

        //查询每一项权限该角色是否拥有
        for(int i=0;i<roles.size();i++)
        {
            RpsDTO rpsDTO=new RpsDTO();
            RoleTabDfEntity role=roles.get(i);
            rpsDTO.setRoleId(role.getRoleId());
            rpsDTO.setRoleName(role.getRoleName());
            rpsDTO.setRoleDes(role.getRoleDes());
            //添加具体的权限列表
            List<RpDTO> rpDTOList=new ArrayList<>();
            for(int j=0;j<privs.size();j++)
            {
                PrivsTabDfEntity priv=privs.get(j);
                //看是否具有关联关系
                RpTabDfEntity rp=rpTabDfMapper.selectOne(new LambdaQueryWrapper<RpTabDfEntity>().
                        eq(RpTabDfEntity::getRoleId,role.getRoleId()).
                        eq(RpTabDfEntity::getPrivsId,priv.getPrivsId()));
                //是否具有权限角色关联性
                RpDTO rpDTO=new RpDTO();
                rpDTO.setPrivsId(priv.getPrivsId());
                rpDTO.setPrivsName(priv.getPrivsName());
                rpDTO.setPrivsDes(priv.getPrivsDes());
                if(rp!=null)
                {
                    rpDTO.setHasRp(true);
                }else
                {
                    rpDTO.setHasRp(false);
                }
                rpDTOList.add(rpDTO);

            }
            rpsDTO.setRpDTOList(rpDTOList);
            rpsList.add(rpsDTO);

        }
        return rpsList;
    }

}
