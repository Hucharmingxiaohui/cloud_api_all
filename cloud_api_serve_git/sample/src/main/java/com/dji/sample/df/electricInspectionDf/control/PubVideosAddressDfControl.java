package com.dji.sample.df.electricInspectionDf.control;

/**
 * 视频流地址的管理
 */

import com.dji.sample.df.electricInspectionDf.model.PubVideosAddressDfEntity;
import com.dji.sample.df.electricInspectionDf.service.PubVideosAddressDfService;
import com.dji.sdk.common.HttpResultResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.security.SecureRandom;
@RestController
@RequestMapping("pub/api/v1/videosAddress")
public class PubVideosAddressDfControl {
    @Autowired
    private PubVideosAddressDfService pubVideosAddressDfService;

    //查询所有的视频地址
    @GetMapping("/getAllVideosAddress")
    HttpResultResponse getAllVideosAddress(){
        List<PubVideosAddressDfEntity> pubVideosAddressDfEntityList = pubVideosAddressDfService.getVideosList();
        return HttpResultResponse.success(pubVideosAddressDfEntityList).setMessage("成功查询所有的视频流地址");
    }

    //新增视频流地址
    @PostMapping("/addVideoAddress")
    HttpResultResponse addVideoAddress(@RequestBody PubVideosAddressDfEntity pubVideosAddressDfEntity){
        //生成18位编码
        SecureRandom secureRandom = new SecureRandom();
        StringBuilder code = new StringBuilder(18);
        for (int i = 0; i < 18; i++) {
            code.append(secureRandom.nextInt(10)); // 生成0-9之间的随机数
        }
        pubVideosAddressDfEntity.setCode18(code.toString());
        boolean flag = pubVideosAddressDfService.addVideosAddress(pubVideosAddressDfEntity);
        if(flag){
            return HttpResultResponse.success().setMessage("新增视频地址成功");

        }else{
            return HttpResultResponse.error("新增视频地址失败，18位编码有可能重复");
        }
    }

    //通过场站id查询视频地址
    @GetMapping("/getVideoListBySubCode")
    HttpResultResponse getVideoListBySubCode(@RequestParam String sub_code){
        List<PubVideosAddressDfEntity> pubVideosAddressDfEntityList = pubVideosAddressDfService.getVideosBySubCode(sub_code);
        return HttpResultResponse.success(pubVideosAddressDfEntityList).setMessage("成功查询到该场站视频地址");
    }

    //更新视频地址
    @PutMapping("/updateVideoAddressById")
    HttpResultResponse updateVideoAddressById(@RequestBody PubVideosAddressDfEntity pubVideosAddressDfEntity){
        System.out.println(pubVideosAddressDfEntity);
        boolean flag = pubVideosAddressDfService.updateVideosAddress(pubVideosAddressDfEntity);
        if(flag){
           return HttpResultResponse.success().setMessage("更新视频成功");
        }else {
           return HttpResultResponse.error("更新视频失败");
        }
    }

    //按id删除视频地址
    @DeleteMapping("/deleteVideoAddressById")
    HttpResultResponse deleteVideoAddressById(@RequestParam Integer id){
        boolean flag = pubVideosAddressDfService.deleteVideosById(id);
        if(flag){
            return HttpResultResponse.success().setMessage("成功删除视频地址配置");
        }else{
            return HttpResultResponse.error("删除视频地址失败");
        }
    }

    //通过设备sn查询视频地址信息
    @GetMapping("/getVideoByDeviceSn")
    HttpResultResponse getVideoAddressByDeviceSn(@RequestParam String device_sn){
        PubVideosAddressDfEntity entity=pubVideosAddressDfService.getVideoAddressByDeviceSn(device_sn);
        return HttpResultResponse.success(entity);
    }
}
