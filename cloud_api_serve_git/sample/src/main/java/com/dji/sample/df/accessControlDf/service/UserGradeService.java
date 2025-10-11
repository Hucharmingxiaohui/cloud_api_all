package com.dji.sample.df.accessControlDf.service;

import com.dji.sample.df.accessControlDf.entity.UserGradeControlDfEntity;
import com.dji.sample.manage.model.dto.WorkspaceDTO;

import java.util.List;
import java.util.Optional;

public interface UserGradeService {
    //查询所有用户等级
    public UserGradeControlDfEntity  getUserGradeByUserId(String user_id);

    //查询所有的工作空间变电站
    List<WorkspaceDTO> getAllWorkspaces();
    //更新工作空间
    boolean saveWorkspace(WorkspaceDTO existingWorkspace);

    //新建工作空间
    boolean addWorkspace(WorkspaceDTO workspaceDTO);
    //根据工作空间名称查询
    List<WorkspaceDTO> getWorkspacesByName(String workspaceName);
    //删除工作空间
    boolean deleteWorkspace(String workspaceId);
    Optional<WorkspaceDTO> getWorkspaceByWorkspaceId(String workspaceId);
}
