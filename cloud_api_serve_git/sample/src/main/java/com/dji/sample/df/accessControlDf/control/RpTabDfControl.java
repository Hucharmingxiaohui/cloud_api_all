package com.dji.sample.df.accessControlDf.control;

import com.dji.sample.df.accessControlDf.dto.RpsDTO;
import com.dji.sample.df.accessControlDf.entity.RpTabDfEntity;
import com.dji.sample.df.accessControlDf.service.RpTabDfService;
import com.dji.sdk.common.HttpResultResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController//注解在 Java 中主要用于构建 RESTful Web 服务
@RequestMapping("manage/api/v1/rp")//注解在 Java 中主要用于将 HTTP 请求映射到控制器类中的具体处理方法上
public class RpTabDfControl {
    @Autowired
    private RpTabDfService rpTabDfService;
    //新增一条关联关系
    @PostMapping("/addRp")
    public HttpResultResponse addRp(@RequestBody RpTabDfEntity rpTabDfEntity)
    {
        //新增一条权限角色关联关系
        boolean result=rpTabDfService.createRp(rpTabDfEntity);
        if(result)
        {
            return HttpResultResponse.success().setMessage("新增成功");
        }else {
            return HttpResultResponse.error().setMessage("新增失败，重复绑定");
        }
    }
    //解绑关联关系
    @DeleteMapping("/deleteRp")
    public HttpResultResponse deleteRp(@RequestParam String role_id,
                                       @RequestParam String privs_id)
    {
        //解除绑定
        boolean result=rpTabDfService.deleteRp(role_id,privs_id);
        if(result)
        {
            return HttpResultResponse.success().setMessage("解绑成功");
        }else {
            return HttpResultResponse.error().setMessage("解绑失败");
        }
    }
    //查询所有的关联关系
    @GetMapping("/getRps")
    HttpResultResponse getRps()
    {
        List<RpsDTO> rpsDTOList=rpTabDfService.getRolePrivs();
        return HttpResultResponse.success(rpsDTOList).setMessage("查询成功");
    }
}


