package com.dji.sample.df.manageDf.controller;

import com.dji.sample.df.manageDf.model.dto.WorkspaceDTO;
import com.dji.sample.df.manageDf.service.IWorkspaceServiceDf;
import com.dji.sdk.common.HttpResultResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author sean.zhou
 * @version 0.1
 * @date 2021/11/23
 */
@RestController
@RequestMapping("${url.manage.prefix}${url.manage.version}/workspaces")
public class WorkspaceControllerDf {

    @Autowired
    private IWorkspaceServiceDf workspaceService;


    //查询所有的工作空间
    @GetMapping("/all")
    public HttpResultResponse getAllWorkspaces() {
        List<WorkspaceDTO> workspaces = workspaceService.getAllWorkspaces();

        return workspaces.isEmpty() ? HttpResultResponse.error() : HttpResultResponse.success(workspaces);
    }

    /**
     * Updates the workspace with the given WorkspaceId.
     * @param workspaceId
     * @param workspaceDTO
     * @return
     */
    //更新当前工作空间数据
    @PutMapping("/{workspaceId}")
    public HttpResultResponse updateWorkspace(@PathVariable("workspaceId") String workspaceId, @RequestBody WorkspaceDTO workspaceDTO) {
        Optional<WorkspaceDTO> workspaceOpt = workspaceService.getWorkspaceByWorkspaceId(workspaceId);
        if (workspaceOpt.isEmpty()) {
            return HttpResultResponse.error("Workspace not found");
        }

        //判断绑定吗、工作空间id是否重复
        WorkspaceDTO existingWorkspace = workspaceOpt.get();
        // 更新工作空间的属性
        existingWorkspace.setPlatformName(workspaceDTO.getPlatformName());
        existingWorkspace.setWorkspaceDesc(workspaceDTO.getWorkspaceDesc());
        existingWorkspace.setWorkspaceName(workspaceDTO.getWorkspaceName());
        existingWorkspace.setBindCode(workspaceDTO.getBindCode());

        // 调用service层方法保存更新后的工作空间
        boolean updated = workspaceService.saveWorkspace(existingWorkspace);

        return updated ? HttpResultResponse.success(existingWorkspace) : HttpResultResponse.error("Failed to update workspace");
    }
    //生成唯一工作空间id

    // 新增一个工作空间

    @PostMapping("/add")
    public HttpResultResponse addWorkspace(@RequestBody WorkspaceDTO workspaceDTO){
        System.out.println(workspaceDTO);
        UUID uuid = UUID.randomUUID();
        workspaceDTO.setWorkspaceId(uuid.toString());
        Optional<WorkspaceDTO> workspaceOpt = workspaceService.getWorkspaceByWorkspaceId(workspaceDTO.getWorkspaceId());
        if(workspaceOpt.isEmpty())
        {
            //找不到就创建该工作空间
            boolean created = workspaceService.addWorkspace(workspaceDTO);
            return created ? HttpResultResponse.success("添加成功") : HttpResultResponse.error("添加失败：绑定码重复或其它错误");
        }else{
            //找到该工作空间就不创建
            return HttpResultResponse.error("该工作空间已经存在");
        }
    }

    //通过名称查询
    @GetMapping("/findByName")
    public HttpResultResponse getWorkspaceByName(@RequestParam("workspaceName") String workspaceName) {
        System.out.println(1+workspaceName);
        List<WorkspaceDTO> workspaces = workspaceService.getWorkspacesByName(workspaceName);

        return workspaces.isEmpty() ? HttpResultResponse.error("No workspaces found") : HttpResultResponse.success(workspaces);
    }

    // 删除工作空间
    @DeleteMapping("/{workspaceId}")
    public HttpResultResponse deleteWorkspace(@PathVariable("workspaceId") String workspaceId) {
        Optional<WorkspaceDTO> workspaceOpt = workspaceService.getWorkspaceByWorkspaceId(workspaceId);

        if (workspaceOpt.isEmpty()) {
            return HttpResultResponse.error("Workspace not found");
        }

        boolean deleted = workspaceService.deleteWorkspace(workspaceId);

        return deleted ? HttpResultResponse.success("Workspace deleted") : HttpResultResponse.error("Failed to delete workspace");
    }



}