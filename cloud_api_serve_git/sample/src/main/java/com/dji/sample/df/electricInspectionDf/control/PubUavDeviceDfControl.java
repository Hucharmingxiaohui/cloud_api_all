package com.dji.sample.df.electricInspectionDf.control;

import com.dji.sample.df.electricInspectionDf.model.PubUavDeviceDfEntity;
import com.dji.sample.df.electricInspectionDf.service.PubUavDeviceDfService;
import com.dji.sdk.common.HttpResultResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 将无人机设备与厂站建立关系
 */
@RestController
@RequestMapping("pub/api/v1/uavDevice")
public class PubUavDeviceDfControl {
    @Autowired
    private PubUavDeviceDfService pubUavDeviceDfService;

    //添加无人机相关设备到场站
    @PostMapping("/addUavDevice")
    HttpResultResponse addUavDevice(@RequestBody PubUavDeviceDfEntity pubUavDeviceDfEntity){
        boolean flag = pubUavDeviceDfService.addDeviceToSub(pubUavDeviceDfEntity);
        if (flag){
            return HttpResultResponse.success().setMessage("成功添加设备到场站");
        }else {
            return HttpResultResponse.error("添加设备失败");
        }
    }

    //通过场站id查询
    @GetMapping("/getUavDeviceBySubCode")
    HttpResultResponse getUavDeviceBySubCode(@RequestParam String sub_code){
        List<PubUavDeviceDfEntity> pubUavDeviceDfEntityList= pubUavDeviceDfService.getUavDeviceBySubCode(sub_code);
        return HttpResultResponse.success(pubUavDeviceDfEntityList).setMessage("成功查询到设备信息");
    }

    //根据id更新无人机设备
    @PutMapping("/updateUavDeviceById")
    public HttpResultResponse updateUavDeviceById(@RequestBody PubUavDeviceDfEntity pubUavDeviceDfEntity){
        boolean flag = pubUavDeviceDfService.updateUavDeviceById(pubUavDeviceDfEntity);
        if (flag){
           return HttpResultResponse.success().setMessage("更新成功");
        }else{
           return HttpResultResponse.error("更新失败");
        }
    }

    @DeleteMapping("/deleteUavDeviceById")
    public HttpResultResponse deleteUavDeviceById(@RequestParam Integer id){
        boolean flag = pubUavDeviceDfService.deleteUavDeviceById(id);
        if (flag){
            return HttpResultResponse.success().setMessage("删除成功");
        }else{
            return HttpResultResponse.error("删除失败");
        }
    }
}
