package com.dji.sample.df.manageDf.service;


import com.dji.sample.df.manageDf.model.dto.WorkspaceDTO;

import java.util.List;
import java.util.Optional;

public interface IWorkspaceServiceDf {

    /**
     * Query the information of a workspace based on its workspace id.
     * @param workspaceId
     * @return
     */
    Optional<WorkspaceDTO> getWorkspaceByWorkspaceId(String workspaceId);

    /**
     * Query the workspace of a workspace based on bind code.
     * @param bindCode
     * @return
     */
    Optional<WorkspaceDTO> getWorkspaceNameByBindCode(String bindCode);

    List<WorkspaceDTO> getAllWorkspaces();

    boolean saveWorkspace(WorkspaceDTO existingWorkspace);


    boolean addWorkspace(WorkspaceDTO workspaceDTO);

    List<WorkspaceDTO> getWorkspacesByName(String workspaceName);

    boolean deleteWorkspace(String workspaceId);
}
