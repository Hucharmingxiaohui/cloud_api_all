package com.dji.sample.df.manageDf.controller;

import com.dji.sample.df.manageDf.model.dto.UserListDTO;
import com.dji.sample.df.manageDf.service.IUserServiceDf;
import com.dji.sdk.common.HttpResultResponse;
import com.dji.sdk.common.PaginationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("${url.manage.prefix}${url.manage.version}/users")
public class UserControllerDf {

    @Autowired
    private IUserServiceDf userService;

    //按名称模糊查询用户
    @GetMapping("/{workspace_id}/users/{username}")
    public HttpResultResponse<PaginationData<UserListDTO>>  getWorkspaceUserbyName(
                                               @RequestParam(defaultValue = "1") Long page,
                                               @RequestParam(value = "page_size", defaultValue = "50") Long pageSize,
                                               @PathVariable("workspace_id") String workspaceId,
                                               @PathVariable("username") String userName){
        PaginationData<UserListDTO> paginationData = userService.getWorkspaceUserByname(page, pageSize,workspaceId, userName);
        return HttpResultResponse.success(paginationData);
    }
    //添加一个用户
    @PostMapping("/{workspace_id}/users")
    public HttpResultResponse addUser(@RequestBody UserListDTO user,
                                      @PathVariable("workspace_id") String workspaceId) {

        userService.addUser(workspaceId, user);
        return HttpResultResponse.success();
    }
    //删除当前工作空间的指定用户
    /**
     * Delete a user in the current workspace by ID.
     * @param workspaceId
     * @param userId
     * @return
     */
    @DeleteMapping("/{workspace_id}/users/{user_id}")
    public HttpResultResponse deleteUser(@PathVariable("workspace_id") String workspaceId,
                                         @PathVariable("user_id") String userId) {

        // 调用userService的删除用户方法
        boolean deleted = userService.deleteUser(workspaceId, userId);
        if (deleted) {
            return HttpResultResponse.success();
        } else {
            return HttpResultResponse.error("Failed to delete user.");
        }
    }


}
