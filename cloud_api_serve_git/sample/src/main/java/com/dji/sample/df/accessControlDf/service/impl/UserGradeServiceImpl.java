package com.dji.sample.df.accessControlDf.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dji.sample.df.accessControlDf.dao.UserGradeMapper;
import com.dji.sample.df.accessControlDf.entity.UserGradeControlDfEntity;
import com.dji.sample.df.accessControlDf.service.UserGradeService;
import com.dji.sample.manage.dao.IWorkspaceMapper;
import com.dji.sample.manage.model.dto.WorkspaceDTO;
import com.dji.sample.manage.model.entity.WorkspaceEntity;
import com.dji.sample.manage.service.IWorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserGradeServiceImpl implements UserGradeService {
    @Autowired
    private UserGradeMapper userGradeMapper;
    @Autowired
    private IWorkspaceService workspaceService;
    @Autowired
    private IWorkspaceMapper mapper;

    @Override
    public UserGradeControlDfEntity getUserGradeByUserId(String user_id) {//按用户id查询用户等级
        return userGradeMapper.selectOne(new LambdaQueryWrapper<UserGradeControlDfEntity>().eq(UserGradeControlDfEntity::getUserId, user_id));
    }

    @Override
    public List<WorkspaceDTO> getAllWorkspaces() {//查询所有的工作空间、变电站
        List<WorkspaceEntity> entities = mapper.selectList(null);
        List<WorkspaceDTO> dtos = entities.stream()
                .map(this::entityConvertToDto)
                .collect(Collectors.toList());
        return dtos;
    }

    @Override
    public boolean saveWorkspace(WorkspaceDTO existingWorkspace) {//更新工作空间
        WorkspaceEntity entity = mapper.selectOne(
                new LambdaQueryWrapper<WorkspaceEntity>()
                        .eq(WorkspaceEntity::getWorkspaceId, existingWorkspace.getWorkspaceId()));

        if (entity == null) {
            return false;
        }
        // 获取当前系统时间戳
        long currentTimestamp = System.currentTimeMillis();
        entity.setPlatformName(existingWorkspace.getPlatformName());
        entity.setWorkspaceDesc(existingWorkspace.getWorkspaceDesc());
        entity.setWorkspaceName(existingWorkspace.getWorkspaceName());
        entity.setBindCode(existingWorkspace.getBindCode());
        entity.setUpdateTime(currentTimestamp);//更新时间
        int rowsAffected = mapper.updateById(entity);
        return rowsAffected > 0;
    }

    @Override
    public boolean addWorkspace(WorkspaceDTO workspaceDTO) {//添加工作空间
        WorkspaceEntity entity = mapper.selectOne(
                new LambdaQueryWrapper<WorkspaceEntity>()
                        .eq(WorkspaceEntity::getWorkspaceId, workspaceDTO.getWorkspaceId()));

        if (entity != null) {
            return false; // 工作空间已经存在，添加失败
        }
        WorkspaceEntity entity1 = mapper.selectOne(
                new LambdaQueryWrapper<WorkspaceEntity>().eq(WorkspaceEntity::getBindCode,workspaceDTO.getBindCode())
        );
        if (entity1 != null) {
            return false; // 绑定码工作空间已经存在，添加失败
        }
        WorkspaceEntity newEntity = new WorkspaceEntity();
        newEntity.setWorkspaceId(workspaceDTO.getWorkspaceId());
        newEntity.setPlatformName(workspaceDTO.getPlatformName());
        newEntity.setWorkspaceDesc(workspaceDTO.getWorkspaceDesc());
        newEntity.setWorkspaceName(workspaceDTO.getWorkspaceName());
        newEntity.setBindCode(workspaceDTO.getBindCode());


        int rowsAffected = mapper.insert(newEntity);
        return rowsAffected > 0;
    }

    @Override
    public List<WorkspaceDTO> getWorkspacesByName(String workspaceName) {//根据名称模糊查询
        List<WorkspaceEntity> entities = mapper.selectList(
                new LambdaQueryWrapper<WorkspaceEntity>()
                        .like(WorkspaceEntity::getWorkspaceName, "%" + workspaceName + "%"));

        List<WorkspaceDTO> dtos = entities.stream()
                .map(this::entityConvertToDto)
                .collect(Collectors.toList());

        return dtos;
    }

    @Override
    public boolean deleteWorkspace(String workspaceId) {//删除工作空间
        int rowsAffected = mapper.delete(
                new LambdaQueryWrapper<WorkspaceEntity>()
                        .eq(WorkspaceEntity::getWorkspaceId, workspaceId));

        return rowsAffected > 0;
    }

    @Override
    public Optional<WorkspaceDTO> getWorkspaceByWorkspaceId(String workspaceId) {
        return Optional.ofNullable(entityConvertToDto(
                mapper.selectOne(
                        new LambdaQueryWrapper<WorkspaceEntity>()
                                .eq(WorkspaceEntity::getWorkspaceId, workspaceId))));
    }
    private WorkspaceDTO entityConvertToDto(WorkspaceEntity entity) {
        if (entity == null) {
            return null;
        }
        return WorkspaceDTO.builder()
                .id(entity.getId())
                .workspaceId(entity.getWorkspaceId())
                .platformName(entity.getPlatformName())
                .workspaceDesc(entity.getWorkspaceDesc())
                .workspaceName(entity.getWorkspaceName())
                .bindCode(entity.getBindCode())
                .build();
    }
}
