package com.dji.sample.df.electricInspectionDf.control;

import com.dji.sample.df.electricInspectionDf.model.PubWaylineFileDfEntity;
import com.dji.sample.df.electricInspectionDf.service.PubWaylineFileDfService;
import com.dji.sdk.common.HttpResultResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("wayline/api/v1/pubWayline")
public class PubWaylineFileDfControl {
    @Autowired
    private PubWaylineFileDfService pubWaylineFileDfService;
    //导入航线到变电站下的航线库
    @PostMapping("addWayline")
    public HttpResultResponse addWayline(@RequestBody PubWaylineFileDfEntity pubWaylineFileDfEntity){
        Integer flag = pubWaylineFileDfService.addPubWayline(pubWaylineFileDfEntity);
        if(flag==-1){
            return HttpResultResponse.error("导入航线到变电站失败，有可能重复导入");
        }else{
            return HttpResultResponse.success().setMessage("导入航线到变电站成功");
        }
    }
    @GetMapping("/getPubWaylineBySubCodeMajor")
    public HttpResultResponse getPubWaylineBySubCodeMajor(@RequestParam String sub_code,@RequestParam String major){
        List<PubWaylineFileDfEntity> pubWaylineFileDfEntityList = pubWaylineFileDfService.getPubWaylineBySubCodeMajor(sub_code,major);
        return HttpResultResponse.success(pubWaylineFileDfEntityList).setMessage("成功查询到航点关联信息");
    }
}
