package com.dji.sample.df.electricInspectionDf.control;

import com.dji.sample.df.electricInspectionDf.model.PubComponentDfEntity;
import com.dji.sample.df.electricInspectionDf.service.PubComponentDfService;
import com.dji.sdk.common.HttpResultResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("pub/api/v1/component")
public class PubComponentDfConTrol {
    @Autowired
    private PubComponentDfService pubComponentDfService;

    // 添加部件信息
    @PostMapping("/addComponent")
    HttpResultResponse addComponent(@RequestBody PubComponentDfEntity pubComponentDfEntity) {
        boolean flag = pubComponentDfService.addComponent(pubComponentDfEntity);
        if (flag) {
            return HttpResultResponse.success().setMessage("成功添加部件信息");
        }
        return HttpResultResponse.error("添加部件信息失败");
    }

    // 根据设备ID查询部件列表
    @GetMapping("/getComponentsByDeviceId")
    HttpResultResponse getComponentsByDeviceId(@RequestParam String device_id) {
        List<PubComponentDfEntity> componentList = pubComponentDfService.getComponentListByDeviceId(device_id);
        return HttpResultResponse.success(componentList).setMessage("查询部件信息成功");
    }

    // 更新部件信息
    @PutMapping("/updateComponentById")
    HttpResultResponse updateComponentById(@RequestBody PubComponentDfEntity pubComponentDfEntity) {
        boolean flag = pubComponentDfService.updateComponentById(pubComponentDfEntity);
        if (flag) {
            return HttpResultResponse.success().setMessage("成功更新部件信息");
        } else {
            return HttpResultResponse.error("更新部件信息失败");
        }
    }

    // 删除部件信息
    @DeleteMapping("/deleteComponentById")
    HttpResultResponse deleteComponentById(@RequestParam Integer id) {
        boolean flag = pubComponentDfService.deleteComponentById(id);
        if (flag) {
            return HttpResultResponse.success().setMessage("成功删除部件信息");
        } else {
            return HttpResultResponse.error("删除部件信息失败");
        }
    }
}
