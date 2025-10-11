package com.dji.sample.df.accessControlDf.control;

import com.dji.sample.df.accessControlDf.entity.UrTabDfEntity;
import com.dji.sample.df.accessControlDf.service.UrTabDfService;
import com.dji.sdk.common.HttpResultResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController//注解在 Java 中主要用于构建 RESTful Web 服务
@RequestMapping("manage/api/v1/ur")//注解在 Java 中主要用于将 HTTP 请求映射到控制器类中的具体处理方法上
public class UrTabControl {
    @Autowired
    private UrTabDfService urTabDfService;
    @PostMapping("/createUr")
    public HttpResultResponse createUr(@RequestParam UrTabDfEntity urTabDfEntity)
    {
        boolean result= urTabDfService.createUr(urTabDfEntity);
        if(result)
        {
            return HttpResultResponse.success().setMessage("绑定成功");
        }else {
            return HttpResultResponse.error().setMessage("绑定失败");
        }
    }
    @DeleteMapping("/deleteUr")
    public HttpResultResponse deleteUr(@RequestParam String user_id,
                                       @RequestParam String role_id)
    {
        boolean result= urTabDfService.deleteUr(user_id,role_id);
        if(result)
        {
            return HttpResultResponse.success().setMessage("解绑成功");
        }else {
            return HttpResultResponse.error().setMessage("解绑失败");
        }
    }
    @GetMapping("/getUrByUserId")
    public HttpResultResponse getUrByUserId(@RequestParam String user_id)
    {
        UrTabDfEntity ur=urTabDfService.getUrByUserId(user_id);
        return HttpResultResponse.success(ur).setMessage("查询成功");

    }

}
