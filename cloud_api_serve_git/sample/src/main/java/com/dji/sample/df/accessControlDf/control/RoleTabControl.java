package com.dji.sample.df.accessControlDf.control;

import com.dji.sample.df.accessControlDf.entity.RoleTabDfEntity;
import com.dji.sample.df.accessControlDf.service.RoleTabDfService;
import com.dji.sdk.common.HttpResultResponse;
import com.dji.sdk.common.PaginationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;



@RestController//注解在 Java 中主要用于构建 RESTful Web 服务
@RequestMapping("manage/api/v1/role")//注解在 Java 中主要用于将 HTTP 请求映射到控制器类中的具体处理方法上
public class RoleTabControl {
    //角色服务
    @Autowired
    private RoleTabDfService roleTabDfService;
    //添加一个角色
    /**
     *
     * @param roleTabDfEntity
     * 开发人员：ych
     * time:2025-2-21
     * @return
     */
    @PostMapping("/addRole")
    public HttpResultResponse addRole(@RequestBody RoleTabDfEntity roleTabDfEntity)
    {
        boolean result= roleTabDfService.addRole(roleTabDfEntity);
        if(result)
        {
            return HttpResultResponse.success().setMessage("添加角色成功");
        }else {
            return HttpResultResponse.error().setMessage("添加角色失败");
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
    @DeleteMapping("/deleteRoleByRoleId")
    public HttpResultResponse deleteRoleByRoleId(@RequestParam String role_id)
    {
        boolean result= roleTabDfService.deleteRole(role_id);
        if(result)
        {
            return HttpResultResponse.success().setMessage("删除角色成功");
        }else {
            return HttpResultResponse.error().setMessage("删除角色失败");
        }
    }
    //更新一个角色信息
    /**
     *
     * @param roleTabDfEntity
     * 开发人员：ych
     * time:2025-2-21
     * @return
     */
    @PutMapping("/updateRoleByRoleId")
    public HttpResultResponse updateRoleByRoleId(@RequestBody RoleTabDfEntity roleTabDfEntity)
    {
        boolean result= roleTabDfService.updateRole(roleTabDfEntity);
        if(result)
        {
            return HttpResultResponse.success().setMessage("更新角色信息成功");
        }else {
            return HttpResultResponse.error().setMessage("更新角色信息失败");
        }
    }
    //默认分页查询角色
    /**
     *
     * @param page
     * @param pageSize
     * 开发人员：ych
     * time:2025-2-21
     * @return
     */
    @GetMapping("/getRolesPages")
    public HttpResultResponse getRolesPages(@RequestParam(defaultValue = "1") Long page,
                                            //参数名称映射
                                            @RequestParam(defaultValue = "10",value = "page_size") Long pageSize){
        //分页查询数据返回
        PaginationData<RoleTabDfEntity> roleTab=roleTabDfService.getRolesByPage(page,pageSize);
        return HttpResultResponse.success(roleTab).setMessage("查询成功");
    }
    //按角色名称查询
    /**
     *
     * @param page
     * @param pageSize
     * @param roleName
     * 开发人员：ych
     * time:2025-2-21
     * @return
     */
    @GetMapping("/getRoleByName")
    public HttpResultResponse getRoleByName(@RequestParam(defaultValue = "1") Long page,
                                            //参数名称映射
                                            @RequestParam(defaultValue = "10",value = "page_size") Long pageSize,
                                            @RequestParam String roleName){
        PaginationData<RoleTabDfEntity> roleTab=roleTabDfService.getRoleByName(page,pageSize,roleName);
        return HttpResultResponse.success(roleTab).setMessage("查询成功");
    }

}
