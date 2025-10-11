package com.dji.sample.df.electricInspectionDf.control;

import com.dji.sample.df.electricInspectionDf.model.PubDeviceDfEntity;
import com.dji.sample.df.electricInspectionDf.service.PubDeviceDfService;
import com.dji.sdk.common.HttpResultResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("pub/api/v1/device")
public class PubDeviceDfControl {
    @Autowired
    private PubDeviceDfService pubDeviceDfService;

    // 添加设备信息
    @PostMapping("/addDevice")
    HttpResultResponse addDevice(@RequestBody PubDeviceDfEntity pubDeviceDfEntity) {
        boolean flag = pubDeviceDfService.addDevice(pubDeviceDfEntity);
        if (flag) {
            return HttpResultResponse.success().setMessage("成功添加设备信息");
        }
        return HttpResultResponse.error("添加设备信息失败");
    }

    // 根据设备ID查询设备信息
    @GetMapping("/getDevicesByBayId")
    HttpResultResponse getDeviceById(@RequestParam String bay_id) {
        List<PubDeviceDfEntity> pubDeviceDfEntityList = pubDeviceDfService.getDeviceListByBayId(bay_id);
        if (pubDeviceDfEntityList != null) {
            return HttpResultResponse.success(pubDeviceDfEntityList).setMessage("查询设备信息成功");
        } else {
            return HttpResultResponse.error("未找到设备信息");
        }
    }

    // 更新设备信息
    @PutMapping("/updateDeviceById")
    HttpResultResponse updateDeviceById(@RequestBody PubDeviceDfEntity pubDeviceDfEntity) {
        boolean flag = pubDeviceDfService.updateDeviceById(pubDeviceDfEntity);
        if (flag) {
            return HttpResultResponse.success().setMessage("成功更新设备信息");
        } else {
            return HttpResultResponse.error("更新设备信息失败");
        }
    }

    // 删除设备信息
    @DeleteMapping("/deleteDeviceById")
    HttpResultResponse deleteDeviceById(@RequestParam Integer id) {
        boolean flag = pubDeviceDfService.deleteDeviceById(id);
        if (flag) {
            return HttpResultResponse.success().setMessage("成功删除设备信息");
        } else {
            return HttpResultResponse.error("删除设备信息失败");
        }
    }
}
