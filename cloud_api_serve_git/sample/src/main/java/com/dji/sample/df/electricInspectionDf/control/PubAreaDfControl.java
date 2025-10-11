package com.dji.sample.df.electricInspectionDf.control;

import com.dji.sample.df.electricInspectionDf.model.PubAreaDfEntity;
import com.dji.sample.df.electricInspectionDf.service.PubAreaDfService;
import com.dji.sdk.common.HttpResultResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("pub/api/v1/area")
public class PubAreaDfControl {
    @Autowired
    private PubAreaDfService pubAreaDfService;
    //添加区域
    @PostMapping("/addArea")
    HttpResultResponse addArea(@RequestBody PubAreaDfEntity pubAreaDfEntity){
        boolean flag = pubAreaDfService.addArea(pubAreaDfEntity);
        if(flag){
            return HttpResultResponse.success().setMessage("成功添加区域");
        }
        return HttpResultResponse.error("添加区域失败");
    }
    //根据场站id查询区域
    @GetMapping("/getAreasBySubCode")
    HttpResultResponse getAreasBySubCode(@RequestParam String sub_code){
        List<PubAreaDfEntity> pubAreaDfEntityList = pubAreaDfService.getAreaListBySubCode(sub_code);
        return HttpResultResponse.success(pubAreaDfEntityList).setMessage("查询区域成功");
    }
    //更新区域
    @PutMapping("/updateAreaById")
    HttpResultResponse updateById(@RequestBody PubAreaDfEntity pubAreaDfEntity){
        boolean flag = pubAreaDfService.updateAreaById(pubAreaDfEntity);
        if(flag){
            return HttpResultResponse.success().setMessage("成功更新区域信息");
        }else{
            return HttpResultResponse.error("更新区域信息失败");
        }
    }
    //删除区域
    @DeleteMapping("/deleteAreaById")
    HttpResultResponse deleteAreaById(@RequestParam Integer id){
        boolean flag = pubAreaDfService.deleteAreaById(id);
        if(flag){
            return HttpResultResponse.success().setMessage("成功删除区域");
        }else {
            return HttpResultResponse.error("删除区域失败");
        }
    }
}
