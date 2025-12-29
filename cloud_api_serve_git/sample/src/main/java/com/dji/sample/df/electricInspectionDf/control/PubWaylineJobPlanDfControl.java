package com.dji.sample.df.electricInspectionDf.control;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.df.framework.redis.RedisUtils;
import com.dji.sample.center.utils.StringUtils;
import com.dji.sample.common.model.CustomClaim;
import com.dji.sample.df.electricInspectionDf.model.PubWaylineJobPlanDfEntity;
import com.dji.sample.df.electricInspectionDf.service.PubWaylineJobPlanDfService;
import com.dji.sample.wayline.dao.IWaylineFileMapper;
import com.dji.sample.wayline.dao.IWaylineJobMapper;
import com.dji.sample.wayline.model.entity.WaylineFileEntity;
import com.dji.sample.wayline.model.entity.WaylineJobEntity;
import com.dji.sdk.common.HttpResultResponse;
import com.dji.sdk.common.PaginationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.dji.sample.component.AuthInterceptor.TOKEN_CLAIM;

@RestController
@RequestMapping("pub/api/v1/waylinePlan")
public class PubWaylineJobPlanDfControl {
    @Autowired
    private PubWaylineJobPlanDfService pubWaylineJobPlanDfService;

    @Resource
    private RedisUtils redisUtils;

    @Autowired
    private IWaylineJobMapper waylineJobMapper;

    @Autowired
    IWaylineFileMapper waylineFileMapper;

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
//            List<PubWaylineJobPlanDfEntity> planDfEntityList = (List<PubWaylineJobPlanDfEntity>) waylineJObPlan.get("plans");
////          定时任务则立即执行,要根据实际情况加上间隔(暂定1个小时）
//            // 获取第一个对象的beginTime作为基准时间
//            long baseTime = planDfEntityList.get(0).getBeginTime();
//            for (int i = 0; i < planDfEntityList.size(); i++) {
//                PubWaylineJobPlanDfEntity planDfEntity = planDfEntityList.get(i);
//                // 为每个对象设置新的beginTime：基准时间 + i * 1小时
//                long newBeginTime = baseTime + i * 3600000L; // 1小时 = 3600000毫秒
//                planDfEntity.setBeginTime(newBeginTime);
//                if (planDfEntity.getTaskType() == 1) {
//                    pubWaylineJobPlanDfService.expressPlan(customClaim, planDfEntity);
//                }
//            }
            PubWaylineJobPlanDfEntity planDfEntity = (PubWaylineJobPlanDfEntity) waylineJObPlan.get("plan");
//          定时任务则立即执行
            if(planDfEntity.getTaskType()==1) {
                pubWaylineJobPlanDfService.expressPlan(customClaim, planDfEntity);
            }
            return HttpResultResponse.success(waylineJObPlan).setMessage("创建飞行计划成功");
        }else{
           return HttpResultResponse.error("创建飞行计划失败，计划id有可能重复");
        }
    }
//    多机巢计划，和上述逻辑一样，只不过传多计划，一个机场对应一个计划
//    @PostMapping("/createWaylinePlan")
//    HttpResultResponse createWaylinePlan(HttpServletRequest request,@RequestBody PubWaylineJobPlanDfEntity pubWaylineJobPlanDfEntity) throws SQLException {
//        CustomClaim customClaim = (CustomClaim) request.getAttribute(TOKEN_CLAIM);
//        String workspaceId = customClaim.getWorkspaceId();
//        String creator = customClaim.getUsername();
////      为了后续风机航线创建任务
//        redisUtils.set("workspaceId", workspaceId);
//        redisUtils.set("creator", creator);
//        Map<String, Object> waylineJObPlan = pubWaylineJobPlanDfService.createWaylineJObPlan(pubWaylineJobPlanDfEntity);
//        boolean result = (boolean) waylineJObPlan.get("result");
//        if(result){
//            List<PubWaylineJobPlanDfEntity> planDfEntityList = (List<PubWaylineJobPlanDfEntity>) waylineJObPlan.get("plans");
////          定时任务则立即执行,要根据实际情况加上间隔
//            for (PubWaylineJobPlanDfEntity planDfEntity : planDfEntityList) {
//                if(planDfEntity.getTaskType()==1){
//                    pubWaylineJobPlanDfService.expressPlan(customClaim,planDfEntity);
//                }
//            }
//            return HttpResultResponse.success().setMessage("创建飞行计划成功");
//        }else{
//            return HttpResultResponse.error("创建飞行计划失败，计划id有可能重复");
//        }
//    }

    @GetMapping("/plan_type/{plan_type}/getPlanByPlantype")
    public HttpResultResponse<PaginationData<PubWaylineJobPlanDfEntity>> getPlanByPlantype(@RequestParam(defaultValue = "1") Long page,
                                                                                          @RequestParam(name = "page_size", defaultValue = "10") Long pageSize,
                                                                                          @PathVariable(name = "plan_type") String plan_type,@RequestParam Map map) {
        PaginationData<PubWaylineJobPlanDfEntity> data = pubWaylineJobPlanDfService.getPlanByPlantype(plan_type, page, pageSize,map);
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

    //批量删除飞行计划
    @PostMapping("/batchDeletePlanByIds")
    HttpResultResponse batchDeletePlanByIds(@RequestBody List<Integer> ids){
        boolean flag= pubWaylineJobPlanDfService.batchDeletePlanByIds(ids);
        if(flag){
            return HttpResultResponse.success().setMessage("批量删除计划成功");
        }else {
            return HttpResultResponse.error("批量删除计划失败");
        }
    }
//  删除飞行任务
    @DeleteMapping("/deleteJobByJobId")
    HttpResultResponse deleteJobByJobId(@RequestParam String job_id){
        boolean flag= pubWaylineJobPlanDfService.deleteJobByBobId(job_id);
        if (flag){
            return HttpResultResponse.success().setMessage("删除任务成功");
        }else {
            return HttpResultResponse.error("删除任务失败");
        }
    }
//  批量删除飞行任务，暂时不用，因为任务里准备中的任务是调取消任务的接口，应该分两类进行处理,也要改成post
    @PostMapping("/batchDeleteJobByJobIds")
    HttpResultResponse batchDeleteJobByJobIds(@RequestBody List<String> jobIds){
        boolean flag = true;
        for(String jobId:jobIds){
            boolean flag1= pubWaylineJobPlanDfService.deleteJobByBobId(jobId);
            flag = flag1 && flag ;
        }
        if (flag){
            return HttpResultResponse.success().setMessage("批量删除任务成功");
        }else {
            return HttpResultResponse.error("批量删除任务失败");
        }
    }

    //  批量删除飞行任务，暂时不用，因为任务里准备中的任务是调取消任务的接口，应该分两类进行处理,也要改成post
    @GetMapping("/getNowTask")
    HttpResultResponse getNowTask(){
        Map map = new HashMap();
        String jobId = redisUtils.get("jobId").toString();
        if(StringUtils.isNotEmpty(jobId)){
            WaylineJobEntity waylineJobEntity = waylineJobMapper.selectOne(new LambdaQueryWrapper<WaylineJobEntity>()
                    .eq(WaylineJobEntity::getJobId, jobId));
            String jobEntityName = waylineJobEntity.getName();
            WaylineFileEntity waylineFileEntity = waylineFileMapper.selectOne(new LambdaQueryWrapper<WaylineFileEntity>().
                    eq(WaylineFileEntity::getWaylineId, waylineJobEntity.getFileId()));
            String waylineName = waylineFileEntity.getName();
            String waylineId = waylineFileEntity.getWaylineId();
            Long executeTime = waylineJobEntity.getExecuteTime();
            Date date = new Date(executeTime);
            System.out.println(date); // 直接输出
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String formattedDate = sdf.format(date);
            map.put("jobEntityName",jobEntityName);
            map.put("executeTime",formattedDate);
            map.put("waylineName",waylineName);
            map.put("waylineId",waylineId);
            return HttpResultResponse.success().setData(map);
        }
        return HttpResultResponse.success().setData(map);
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
