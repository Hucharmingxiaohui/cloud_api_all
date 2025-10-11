package com.dji.sample.df.accessControlDf.control;

import com.dji.sample.df.accessControlDf.entity.UserGradeControlDfEntity;
import com.dji.sample.df.accessControlDf.service.UserGradeService;
import com.dji.sample.manage.model.dto.WorkspaceDTO;
import com.dji.sdk.common.HttpResultResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("manage/api/v1/grade")
public class UserGradeControl {
    @Autowired
    private UserGradeService userGradeService;

    //根据用户id查询用户权限等级
    @GetMapping("/getUserGradeByUserId")
    public HttpResultResponse GetUserGradeByUserId(@RequestParam("user_id") String user_id){
        UserGradeControlDfEntity userGradeControlDfEntity = userGradeService.getUserGradeByUserId(user_id);
        //判断是否查询到值
        if(userGradeControlDfEntity==null){
            return HttpResultResponse.success().setMessage("暂未对该用户进行赋权");
        }else {
            return HttpResultResponse.success(userGradeControlDfEntity);
        }

    }
    //查询所有的工作空间（变电站）
    @GetMapping("/getAllWorkspaces")
    public HttpResultResponse getAllWorkspaces(@RequestParam("user_id")  String user_id) {
        UserGradeControlDfEntity userGradeControlDfEntity = userGradeService.getUserGradeByUserId(user_id);
        if("1".equals(userGradeControlDfEntity.getUserGrade())){
            List<WorkspaceDTO> workspaces = userGradeService.getAllWorkspaces();
            return workspaces.isEmpty() ? HttpResultResponse.error() : HttpResultResponse.success(workspaces);
        }else{
            return HttpResultResponse.success().setMessage("用户权限不够");
        }



    }

    //更新当前工作空间数据
    @PutMapping("/updateWorkspace")
    public HttpResultResponse updateWorkspace(@RequestParam("workspace_id") String workspace_id,@RequestParam("user_id")  String user_id,@RequestBody WorkspaceDTO workspaceDTO) {
        UserGradeControlDfEntity userGradeControlDfEntity = userGradeService.getUserGradeByUserId(user_id);
        if("1".equals(userGradeControlDfEntity.getUserGrade())){
            Optional<WorkspaceDTO> workspaceOpt = userGradeService.getWorkspaceByWorkspaceId(workspace_id);
            if (workspaceOpt.isEmpty()) {
                return HttpResultResponse.error("未查询到工作空间");
            }

            //判断绑定吗、工作空间id是否重复
            WorkspaceDTO existingWorkspace = workspaceOpt.get();
            // 更新工作空间的属性
            existingWorkspace.setPlatformName(workspaceDTO.getPlatformName());
            existingWorkspace.setWorkspaceDesc(workspaceDTO.getWorkspaceDesc());
            existingWorkspace.setWorkspaceName(workspaceDTO.getWorkspaceName());
            existingWorkspace.setBindCode(workspaceDTO.getBindCode());

            // 调用service层方法保存更新后的工作空间
            boolean updated = userGradeService.saveWorkspace(existingWorkspace);

            return updated ? HttpResultResponse.success("更新成功") : HttpResultResponse.error("更新工作空间失败");
        }else {
            return HttpResultResponse.success().setMessage("用户权限不够");
        }

    }

    // 新增一个工作空间
    @PostMapping("/addWorkspace")
    public HttpResultResponse addWorkspace(@RequestParam("user_id")  String user_id,@RequestBody WorkspaceDTO workspaceDTO){
        UserGradeControlDfEntity userGradeControlDfEntity = userGradeService.getUserGradeByUserId(user_id);
        if("1".equals(userGradeControlDfEntity.getUserGrade())){
            UUID uuid = UUID.randomUUID();
            workspaceDTO.setWorkspaceId(uuid.toString());
            Optional<WorkspaceDTO> workspaceOpt = userGradeService.getWorkspaceByWorkspaceId(workspaceDTO.getWorkspaceId());
            if(workspaceOpt.isEmpty())
            {
                //找不到就创建该工作空间
                boolean created = userGradeService.addWorkspace(workspaceDTO);
                return created ? HttpResultResponse.success("添加成功") : HttpResultResponse.error("添加失败：绑定码重复或其它错误");
            }else{
                //找到该工作空间就不创建
                return HttpResultResponse.error("该工作空间已经存在");
            }
        }else{
            return HttpResultResponse.success().setMessage("用户权限不够");
        }

    }

    //通过名称查询
    @GetMapping("/findByWorkspaceName")
    public HttpResultResponse getWorkspaceByName(@RequestParam("workspace_name") String workspace_name,@RequestParam("user_id") String user_id) {
        UserGradeControlDfEntity userGradeControlDfEntity = userGradeService.getUserGradeByUserId(user_id);
        if("1".equals(userGradeControlDfEntity.getUserGrade())){
            List<WorkspaceDTO> workspaces = userGradeService.getWorkspacesByName(workspace_name);

            return workspaces.isEmpty() ? HttpResultResponse.error("没有查询到工作空间") : HttpResultResponse.success(workspaces);
        }else{
            return HttpResultResponse.success().setMessage("用户权限不够");
        }

    }

    // 删除工作空间
    @DeleteMapping("/deleteWorkspace")
    public HttpResultResponse deleteWorkspace(@RequestParam("workspace_id") String workspace_id,@RequestParam("user_id") String user_id) {
        UserGradeControlDfEntity userGradeControlDfEntity = userGradeService.getUserGradeByUserId(user_id);
        if("1".equals(userGradeControlDfEntity.getUserGrade())){
            Optional<WorkspaceDTO> workspaceOpt = userGradeService.getWorkspaceByWorkspaceId(workspace_id);

            if (workspaceOpt.isEmpty()) {
                return HttpResultResponse.error("未查询到工作空间");
            }
            boolean deleted = userGradeService.deleteWorkspace(workspace_id);
            return deleted ? HttpResultResponse.success("删除成功") : HttpResultResponse.error("删除失败");
        }else{
            return HttpResultResponse.success().setMessage("用户权限不够");
        }

    }
}
