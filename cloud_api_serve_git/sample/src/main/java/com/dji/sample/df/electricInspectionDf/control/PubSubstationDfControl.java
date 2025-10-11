package com.dji.sample.df.electricInspectionDf.control;

import com.df.framework.vo.Result;
import com.df.server.entity.uni.PubSubstationEntity;
import com.df.server.service.uni.PubSubstationService;
import com.dji.sample.df.electricInspectionDf.model.PubSubstationDfEntity;
import com.dji.sample.df.electricInspectionDf.service.PubSubstationDfService;
import com.dji.sdk.common.HttpResultResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("pub/api/v1/pubStation")
public class PubSubstationDfControl {
    @Autowired
    private PubSubstationDfService pubSubstationDfService;

    @Autowired
    private PubSubstationService pubSubstationService;
    //查询所有的场站
    @GetMapping("/getSubStations")
    HttpResultResponse getSubStations(){
        List<PubSubstationDfEntity> pubSubstationDfEntityList;
        pubSubstationDfEntityList = pubSubstationDfService.getPubStationList();
        return HttpResultResponse.success(pubSubstationDfEntityList).setMessage("查询所有场站数据成功");
    }

    @PostMapping("/info")
    public Result<PubSubstationEntity> info(HttpServletRequest request) {
        PubSubstationEntity one = pubSubstationService.lambdaQuery().last("limit 1").one();
        return Result.success(one);
    }
    //添加一个场站
    @PostMapping("addSubStation")
    HttpResultResponse addSubStation(@RequestBody PubSubstationDfEntity pubSubstationDfEntity){
        pubSubstationDfEntity.setSubCode(UUID.randomUUID().toString());
        int flag = pubSubstationDfService.addPubStation(pubSubstationDfEntity);
        if(flag==-1){
           return HttpResultResponse.error("场站名称重复");
        }else{
           return HttpResultResponse.success().setMessage("成功添加场站");
        }
    }
    //根据运维部、工作空间查询场站
    @GetMapping("getSubStationByWorkspaceId")
    HttpResultResponse getSubStationByWorkspaceId(@RequestParam  String workspace_id){
        List<PubSubstationDfEntity> pubSubstationDfEntities=pubSubstationDfService.getPubStationByWorkspaceId(workspace_id);
        return HttpResultResponse.success(pubSubstationDfEntities).setMessage("成功查询到场站信息");
    }
    //删除场站
    @DeleteMapping("/deleteSubStationById")
    HttpResultResponse deleteSubStationById(@RequestParam Integer id){
        boolean flag = pubSubstationDfService.deleteSubStationById(id);
        if (flag){
            return HttpResultResponse.success().setMessage("删除场站成功");
        }
        return HttpResultResponse.error("删除场站失败");
    }
    @PutMapping("/updateSubStationById")
    HttpResultResponse updateSubStationById(@RequestBody PubSubstationDfEntity pubSubstationDfEntity){
        boolean flag=pubSubstationDfService.updateSubSationById(pubSubstationDfEntity);
        if(flag){
            return HttpResultResponse.success().setMessage("更新场站信息成功");
        }
        return HttpResultResponse.error("更新场站失败");
    }

    //条件查询所有的场站
    @GetMapping("/getSubstationsByParam")
    HttpResultResponse getSubstationsByParam(@RequestParam Map<String,Object> map){
        Map substationsByParam = pubSubstationDfService.getSubstationsByParam(map);
        return HttpResultResponse.success(substationsByParam).setMessage("查询所有场站数据成功");
    }
}
