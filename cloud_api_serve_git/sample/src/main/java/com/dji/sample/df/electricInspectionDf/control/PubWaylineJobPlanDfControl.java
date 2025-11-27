package com.dji.sample.df.electricInspectionDf.control;

import com.df.framework.redis.RedisUtils;
import com.dji.sample.common.model.CustomClaim;
import com.dji.sample.df.electricInspectionDf.model.PubWaylineJobPlanDfEntity;
import com.dji.sample.df.electricInspectionDf.service.PubWaylineJobPlanDfService;
import com.dji.sample.wayline.model.entity.WaylineJobEntity;
import com.dji.sdk.common.HttpResultResponse;
import com.dji.sdk.common.PaginationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.util.Map;

import static com.dji.sample.component.AuthInterceptor.TOKEN_CLAIM;

@RestController
@RequestMapping("pub/api/v1/waylinePlan")
public class PubWaylineJobPlanDfControl {
    @Autowired
    private PubWaylineJobPlanDfService pubWaylineJobPlanDfService;

    @Resource
    private RedisUtils redisUtils;

    @PostMapping("/createWaylinePlan")
    HttpResultResponse createWaylinePlan(HttpServletRequest request,@RequestBody PubWaylineJobPlanDfEntity pubWaylineJobPlanDfEntity) throws SQLException {
        CustomClaim customClaim = (CustomClaim) request.getAttribute(TOKEN_CLAIM);
        String workspaceId = customClaim.getWorkspaceId();
        String creator = customClaim.getUsername();
//      为了后续风机航线创建任务
        redisUtils.set("workspaceId", workspaceId);
        redisUtils.set("creator", creator);
        Map<String, Object> waylineJObPlan = pubWaylineJobPlanDfService.createWaylineJObPlan(pubWaylineJobPlanDfEntity);
        boolean result = (boolean) waylineJObPlan.get("result");
        if(result){
            PubWaylineJobPlanDfEntity plan = (PubWaylineJobPlanDfEntity)waylineJObPlan.get("plan");
//          定时任务则立即执行
            if(plan.getTaskType()==1){
                pubWaylineJobPlanDfService.expressPlan(customClaim,plan);
            }
            return HttpResultResponse.success().setMessage("创建飞行计划成功");
        }else{
           return HttpResultResponse.error("创建飞行计划失败，计划id有可能重复");
        }
    }

    @GetMapping("/plan_type/{plan_type}/getPlanByPlantype")
    public HttpResultResponse<PaginationData<PubWaylineJobPlanDfEntity>> getPlanByPlantype(@RequestParam(defaultValue = "1") Long page,
                                                                                          @RequestParam(name = "page_size", defaultValue = "10") Long pageSize,
                                                                                          @PathVariable(name = "plan_type") String plan_type) {
        PaginationData<PubWaylineJobPlanDfEntity> data = pubWaylineJobPlanDfService.getPlanByPlantype(plan_type, page, pageSize);
        return HttpResultResponse.success(data).setMessage("成功查询到飞行计划信息");
    }


//  按场站查询计划
    @GetMapping("/sub_code/{sub_code}/getPlanBySubCode")
    public HttpResultResponse<PaginationData<PubWaylineJobPlanDfEntity>> getPlanBySubCode(@RequestParam(defaultValue = "1") Long page,
                                                                                          @RequestParam(name = "page_size", defaultValue = "10") Long pageSize,
                                                                                          @PathVariable(name = "sub_code") String sub_code) {
        PaginationData<PubWaylineJobPlanDfEntity> data = pubWaylineJobPlanDfService.getPlanBySubCode(sub_code, page, pageSize);
        return HttpResultResponse.success(data).setMessage("成功查询到场站飞行计划信息");
    }

    //执行任务
    @PostMapping("/expressPlan")
    HttpResultResponse expressPlan(HttpServletRequest request,@RequestBody PubWaylineJobPlanDfEntity pubWaylineJobPlanDfEntity) throws SQLException {
        CustomClaim customClaim = (CustomClaim)request.getAttribute(TOKEN_CLAIM);
        //计划变成任务,更新计划状态
        return pubWaylineJobPlanDfService.expressPlan(customClaim,pubWaylineJobPlanDfEntity);
    }

    //取消任务
    @PostMapping("/cancelPlan")
    HttpResultResponse cancelPlan(HttpServletRequest request, @RequestBody WaylineJobEntity waylineJobEntity) throws SQLException {
        CustomClaim customClaim = (CustomClaim)request.getAttribute(TOKEN_CLAIM);
        //计划变成任务,更新计划状态
        return pubWaylineJobPlanDfService.cancelPlan(customClaim,waylineJobEntity);
    }
    //删除飞行计划
    @DeleteMapping("/deletePlanById")
    HttpResultResponse deletePlanById(@RequestParam Integer id){
       boolean flag= pubWaylineJobPlanDfService.deletePlanById(id);
       if(flag){
           return HttpResultResponse.success().setMessage("成功删除计划");
       }else {
           return HttpResultResponse.error("删除计划失败");
       }
    }
    @DeleteMapping("/deleteJobByJobId")
    HttpResultResponse deleteJobByJobId(@RequestParam String job_id){
        boolean flag= pubWaylineJobPlanDfService.deleteJobByBobId(job_id);
        if (flag){
            return HttpResultResponse.success().setMessage("删除任务成功");
        }else {
            return HttpResultResponse.error("删除任务失败");
        }
    }


    // 初级版本（启用）
//    @PostMapping("/createWaylinePlan2")
//    HttpResultResponse createWaylinePlan2(@RequestBody PubWaylineJobPlanDfEntity pubWaylineJobPlanDfEntity)
//    {
//        Map<Integer, String> waylineByPoint = pubWaylineJobPlanDfService.
//                getWaylineByPoint(pubWaylineJobPlanDfEntity.getDeviceLevel(), pubWaylineJobPlanDfEntity.getDeviceList());
//        Map<String, String> waylineIdByPos = pubWaylineJobPlanDfService.getWaylineIdByPos(waylineByPoint);
//        boolean flag = true;
//        for (Map.Entry<String, String> entry : waylineIdByPos.entrySet()) {
//            pubWaylineJobPlanDfEntity.setFileId(entry.getKey());
//            pubWaylineJobPlanDfEntity.setWaylinePointPos(entry.getValue());
//            boolean waylineJObPlan = pubWaylineJobPlanDfService.createWaylineJObPlan(pubWaylineJobPlanDfEntity);
//            flag = flag && waylineJObPlan;
//        }
//        if(flag){
//            return HttpResultResponse.success().setMessage("创建飞行计划成功");
//        }else{
//            return HttpResultResponse.error("创建飞行计划失败，计划id有可能重复");
//        }
//    }


    public static void main(String[] args) {

    }

}
