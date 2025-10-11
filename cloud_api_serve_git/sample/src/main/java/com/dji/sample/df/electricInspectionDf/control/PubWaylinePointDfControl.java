package com.dji.sample.df.electricInspectionDf.control;

import com.dji.sample.df.electricInspectionDf.model.PubWaylinePointDfEntity;
import com.dji.sample.df.electricInspectionDf.service.PubWaylinePointDfService;
import com.dji.sdk.common.HttpResultResponse;
import com.dji.sdk.common.PaginationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("point/api/v1/points")
public class PubWaylinePointDfControl {
    @Autowired
    private PubWaylinePointDfService pubWaylinePointDfService;


    //将点位台帐存入数据库
    @PostMapping("/addPoints")
    public HttpResultResponse addPoints(@RequestParam String sub_code,
                                        @RequestBody List<PubWaylinePointDfEntity> pointsList) {
        List<PubWaylinePointDfEntity> repeatList= pubWaylinePointDfService.savePubWaylinePointDfEntity(sub_code,pointsList);
        if(repeatList !=null && !repeatList.isEmpty()){
            return HttpResultResponse.success(repeatList).setMessage("导入的点位台账有重复数据");
        }else{
            return HttpResultResponse.success(repeatList).setMessage("成功将点位台帐存入到数据库");
        }


    }
    // 根据ID获取点位信息
    @GetMapping("getPubWaylinePointDfEntityById")
    public HttpResultResponse getPubWaylinePointDfEntityById(@RequestParam Integer id) {
        PubWaylinePointDfEntity entity = pubWaylinePointDfService.getPubWaylinePointDfEntityById(id);
        if (entity != null) {
            return HttpResultResponse.success(entity);
        } else {
            return HttpResultResponse.error("未找到对应的点位信息");
        }
    }

    // 更新点位信息
    @PutMapping("/update")
    public HttpResultResponse updatePubWaylinePointDfEntity(@RequestBody PubWaylinePointDfEntity pubWaylinePointDfEntity) {
        int result = pubWaylinePointDfService.updatePubWaylinePointDfEntity(pubWaylinePointDfEntity);
        if (result > 0) {
            return HttpResultResponse.success("更新成功");
        } else {
            return HttpResultResponse.error("更新失败,该点位可能已经被其它航点关联");
        }
    }

    // 删除点位信息
    @DeleteMapping("/deletePointById")
    public HttpResultResponse deletePubWaylinePointDfEntityById(@RequestParam Integer id) {
        int result = pubWaylinePointDfService.deletePubWaylinePointDfEntityById(id);
        if (result > 0) {
            return HttpResultResponse.success("删除成功");
        } else {
            return HttpResultResponse.error("删除失败");
        }
    }

    // 获取所有点位信息
    @GetMapping("/all")
    public HttpResultResponse<PaginationData<PubWaylinePointDfEntity>> getAllPubWaylinePointDfEntities(@RequestParam(defaultValue = "1") Long page,
                                                                                                       @RequestParam(name = "page_size", defaultValue = "10") Long pageSize) {
        PaginationData<PubWaylinePointDfEntity> data = pubWaylinePointDfService.getAllPubWaylinePointDfEntities(page, pageSize);
        return HttpResultResponse.success(data).setMessage("成功查询到点位信息");
        //        List<PubWaylinePointDfEntity> list = pubWaylinePointDfService.getAllPubWaylinePointDfEntities();
//        if (list != null && !list.isEmpty()) {
//            return HttpResultResponse.success(list);
//        } else {
//            return HttpResultResponse.error("未找到任何点位信息");
//        }
    }
    //按变电站id查询
    @GetMapping("/getPointBySubCode")
    public HttpResultResponse getPointBySubCode(@RequestParam String sub_code){

        return HttpResultResponse.success(pubWaylinePointDfService.getPointBySubCode(sub_code));
    }
    //根据部件id查询点位
    @GetMapping("/getPointsByComponentId")
    public HttpResultResponse get1(@RequestParam String component_id){
        List<PubWaylinePointDfEntity> entities = pubWaylinePointDfService.getPointsByComponentId(component_id);
        return HttpResultResponse.success(entities).setMessage("成功查询到点位信息");
    }

    //条件查询点位
    @GetMapping("/getPointsByParam")
    public HttpResultResponse getPointsByParam(@RequestParam Map map){
        Map pointsByParam = pubWaylinePointDfService.getPointsByParam(map);
        return HttpResultResponse.success(pointsByParam).setMessage("成功查询到点位信息");
    }

}
