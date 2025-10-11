package com.dji.sample.df.manageDf.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dji.sample.manage.dao.IDeviceMapper;
import com.dji.sample.manage.model.entity.DeviceEntity;
import com.dji.sample.df.manageDf.dao.IUserMapperDf;
import com.dji.sample.df.manageDf.dao.IWorkspaceMapperDf;
import com.dji.sample.df.manageDf.model.dto.WorkspaceDTO;
import com.dji.sample.df.manageDf.model.entity.UserEntity;
import com.dji.sample.df.manageDf.model.entity.WorkspaceEntity;
import com.dji.sample.df.manageDf.service.IWorkspaceServiceDf;
import com.dji.sample.media.dao.IFileMapper;
import com.dji.sample.media.model.MediaFileEntity;
import com.dji.sample.wayline.dao.IWaylineFileMapper;
import com.dji.sample.wayline.model.entity.WaylineFileEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class WorkspaceServiceImplDf implements IWorkspaceServiceDf {

    @Autowired
    private IWorkspaceMapperDf mapper;

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private IFileMapper mediaFileMapper;
    @Autowired
    private IWaylineFileMapper iWaylineFileMapper;
    @Autowired
    private IUserMapperDf userMapperDf;
    @Autowired
    private IDeviceMapper deviceMapper;

    @Override
    public Optional<WorkspaceDTO> getWorkspaceByWorkspaceId(String workspaceId) {
        return Optional.ofNullable(entityConvertToDto(
                mapper.selectOne(
                        new LambdaQueryWrapper<WorkspaceEntity>()
                                .eq(WorkspaceEntity::getWorkspaceId, workspaceId))));
    }

    @Override
    public Optional<WorkspaceDTO> getWorkspaceNameByBindCode(String bindCode) {
        return Optional.ofNullable(entityConvertToDto(
                mapper.selectOne(new LambdaQueryWrapper<WorkspaceEntity>().eq(WorkspaceEntity::getBindCode, bindCode))));
    }
//查询所有的数据
    @Override
    public List<WorkspaceDTO> getAllWorkspaces() {
        List<WorkspaceEntity> entities = mapper.selectList(null);
        List<WorkspaceDTO> dtos = entities.stream()
                .map(this::entityConvertToDto)
                .collect(Collectors.toList());
        return dtos;
    }
//更新当前工作空间的数据
    @Override
    public boolean saveWorkspace(WorkspaceDTO existingWorkspace) {
        WorkspaceEntity entity = mapper.selectOne(
                new LambdaQueryWrapper<WorkspaceEntity>()
                        .eq(WorkspaceEntity::getWorkspaceId, existingWorkspace.getWorkspaceId()));

        if (entity == null) {
            return false;
        }

        entity.setPlatformName(existingWorkspace.getPlatformName());
        entity.setWorkspaceDesc(existingWorkspace.getWorkspaceDesc());
        entity.setWorkspaceName(existingWorkspace.getWorkspaceName());
        entity.setBindCode(existingWorkspace.getBindCode());

        int rowsAffected = mapper.updateById(entity);
        return rowsAffected > 0;
    }
//添加一个工作空间
    @Override
    public boolean addWorkspace(WorkspaceDTO workspaceDTO) {
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
//通过名称模糊匹配
    @Override
    public List<WorkspaceDTO> getWorkspacesByName(String workspaceName) {
        List<WorkspaceEntity> entities = mapper.selectList(
                new LambdaQueryWrapper<WorkspaceEntity>()
                        .like(WorkspaceEntity::getWorkspaceName, "%" + workspaceName + "%"));

        List<WorkspaceDTO> dtos = entities.stream()
                .map(this::entityConvertToDto)
                .collect(Collectors.toList());

        return dtos;
    }

    //按工作空间删除
    @Override
    public boolean deleteWorkspace(String workspaceId) {
        //1.删除图片
        //先查询
        List<MediaFileEntity> mediaFileEntities=mediaFileMapper.selectList(new LambdaQueryWrapper<MediaFileEntity>()
                .eq(MediaFileEntity::getWorkspaceId,workspaceId));
        if(mediaFileEntities.size()>0){
            mediaFileMapper.delete(new LambdaQueryWrapper<MediaFileEntity>()
                    .eq(MediaFileEntity::getWorkspaceId,workspaceId));
        }
        //2.删除航线
        //先查询
        List<WaylineFileEntity> waylineFileEntities=iWaylineFileMapper.selectList(new LambdaQueryWrapper<WaylineFileEntity>()
                .eq(WaylineFileEntity::getWorkspaceId,workspaceId));
        if(waylineFileEntities.size()>0){
            iWaylineFileMapper.delete(new LambdaQueryWrapper<WaylineFileEntity>()
                    .eq(WaylineFileEntity::getWorkspaceId,workspaceId));
        }
        //3.删除用户
        List<UserEntity> userEntities= userMapperDf.selectList(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getWorkspaceId,workspaceId));
        if(userEntities.size()>0){
            userMapperDf.delete(new LambdaQueryWrapper<UserEntity>()
                    .eq(UserEntity::getWorkspaceId,workspaceId));
        }
        //4.删除设备
        List<DeviceEntity> deviceEntities=deviceMapper.selectList(new LambdaQueryWrapper<DeviceEntity>()
                .eq(DeviceEntity::getWorkspaceId,workspaceId));
        if (deviceEntities.size()>0){
            deviceMapper.delete(new LambdaQueryWrapper<DeviceEntity>()
                    .eq(DeviceEntity::getWorkspaceId,workspaceId));
        }
        //5.删除工作空间
        int rowsAffected = mapper.delete(
                new LambdaQueryWrapper<WorkspaceEntity>()
                        .eq(WorkspaceEntity::getWorkspaceId, workspaceId));

        return rowsAffected > 0;
    }


    /**
     * Convert database entity objects into workspace data transfer object.
     * @param entity
     * @return
     */
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
