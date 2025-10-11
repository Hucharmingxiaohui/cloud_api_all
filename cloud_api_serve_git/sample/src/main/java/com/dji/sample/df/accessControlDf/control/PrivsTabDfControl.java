package com.dji.sample.df.accessControlDf.control;

import com.dji.sample.df.accessControlDf.entity.PrivsTabDfEntity;
import com.dji.sample.df.accessControlDf.service.PrivsTabDfservice;
import com.dji.sdk.common.HttpResultResponse;
import com.dji.sdk.common.PaginationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController//注解在 Java 中主要用于构建 RESTful Web 服务
@RequestMapping("manage/api/v1/privs")//注解在 Java 中主要用于将 HTTP 请求映射到控制器类中的具体处理方法上
public class PrivsTabDfControl {
    //权限服务
    @Autowired
    private PrivsTabDfservice privsTabDfservice;
    //新增加一条权限
    /**
     *
     * @param privsTabDfEntity
     * @开发人员：ych
     * @time:2025-2-13
     */
    @PostMapping("/addPrivs")
    public HttpResultResponse addPrivs(@RequestBody PrivsTabDfEntity privsTabDfEntity){
        //插入数据
        boolean result= privsTabDfservice.addPrivs(privsTabDfEntity);
        if(result)
        {
            return HttpResultResponse.success().setMessage("添加权限成功");
        }else{
            return HttpResultResponse.error().setMessage("添加权限失败，权限名称重复");
        }
    }
    //删除一条权限
    /**
     *
     * @param privs_id
     * @开发人员ych:2025-2-14
     */
    @DeleteMapping("/deletePrivsByPrivsId")
    public HttpResultResponse deletePrivsByPrivsId(@RequestParam String privs_id)
    {
        //删除权限
         if(privsTabDfservice.deletePrivs(privs_id))
         {
             return HttpResultResponse.success().setMessage("删除权限成功");
         }else{
             return HttpResultResponse.error().setMessage("删除权限失败");
         }
    }
    //修改一条权限的信息
    /**
     *
     * @param privsTabDfEntity
     * @开发人员:ych
     * time:2025-2-14
     */
    @PutMapping("/updatePrivsById")
    public HttpResultResponse updatePrivsById(@RequestBody PrivsTabDfEntity privsTabDfEntity)
    {
        if(privsTabDfservice.updatePrivs(privsTabDfEntity))
        {
            return HttpResultResponse.success().setMessage("更新权限成功");
        }else{
            return HttpResultResponse.error().setMessage("更新权限失败");
        }
    }
    //默认分页查询所有的权限
    @GetMapping("/getPrivsPages")
    public HttpResultResponse getPrivsPage(@RequestParam(defaultValue = "1") Long page,
                                           //参数名称映射
                                           @RequestParam(defaultValue = "10",value = "page_size") Long pageSize)
    {   //分页查询数据返回
        PaginationData<PrivsTabDfEntity> prvisPage=privsTabDfservice.getPrivsByPage(page,pageSize);
        return HttpResultResponse.success(prvisPage).setMessage("查询成功");
    }
    //根据权限名称查询权限
    @GetMapping("getPrivsByPrivsName")
    public HttpResultResponse getPrivsByPrivsName(@RequestParam(defaultValue = "1") Long page,
                                                  //参数名称映射
                                                  @RequestParam(defaultValue = "10",value = "page_size") Long pageSize,
                                                  @RequestParam String privsName)
    {//分页查询数据返回
        PaginationData<PrivsTabDfEntity> prvisPage=privsTabDfservice.getPrivsByName(page,pageSize,privsName);
        return HttpResultResponse.success(prvisPage).setMessage("查询成功");
    }
}
