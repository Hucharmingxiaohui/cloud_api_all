package com.dji.sample.df.electricInspectionDf.control;

import com.dji.sample.df.electricInspectionDf.model.PubBayDfEntity;
import com.dji.sample.df.electricInspectionDf.service.PubBayDfService;
import com.dji.sdk.common.HttpResultResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("pub/api/v1/bay")
public class PubBayDfControl {
    @Autowired
    private PubBayDfService pubBayDfService;

    //添加间隔
    @PostMapping("/addBay")
    HttpResultResponse addBay(@RequestBody PubBayDfEntity pubBayDfEntity) {
        boolean flag = pubBayDfService.addBay(pubBayDfEntity);
        if (flag) {
            return HttpResultResponse.success().setMessage("成功添加间隔");
        }
        return HttpResultResponse.error("添加间隔失败");
    }

    //根据场站id查询间隔
    @GetMapping("/getBaysByAreaId")
    HttpResultResponse getBaysByAreaId(@RequestParam String area_id) {
        List<PubBayDfEntity> pubBayDfEntityList = pubBayDfService.getBayListByAreaId(area_id);
        return HttpResultResponse.success(pubBayDfEntityList).setMessage("查询间隔成功");
    }

    //更新间隔
    @PutMapping("/updateBayById")
    HttpResultResponse updateById(@RequestBody PubBayDfEntity pubBayDfEntity) {
        boolean flag = pubBayDfService.updateBayById(pubBayDfEntity);
        if (flag) {
            return HttpResultResponse.success().setMessage("成功更新间隔信息");
        } else {
            return HttpResultResponse.error("更新间隔信息失败");
        }
    }

    //删除间隔
    @DeleteMapping("/deleteBayById")
    HttpResultResponse deleteBayById(@RequestParam Integer id) {
        boolean flag = pubBayDfService.deleteBayById(id);
        if (flag) {
            return HttpResultResponse.success().setMessage("成功删除间隔");
        } else {
            return HttpResultResponse.error("删除间隔失败");
        }
    }
}
