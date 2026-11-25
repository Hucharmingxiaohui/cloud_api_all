package com.dji.sample.df.centerOrdinary;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.df.framework.vo.Result;
import com.dji.sample.common.model.CustomClaim;
import com.dji.sample.df.centerOrdinary.dao.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import javax.servlet.http.HttpServletRequest;
import static com.dji.sample.component.AuthInterceptor.TOKEN_CLAIM;

@RestController
@RequestMapping("/api/center-patrol")
public class CenterPatrolController {
    private final CenterPatrolPlanMapper plans;
    private final CenterOrdinaryTaskMapper tasks;
    private final CenterPatrolService service;
    public CenterPatrolController(CenterPatrolPlanMapper plans,CenterOrdinaryTaskMapper tasks,CenterPatrolService service) {
        this.plans=plans;this.tasks=tasks;this.service=service;
    }
    private String workspace(HttpServletRequest request) {
        CustomClaim c=(CustomClaim)request.getAttribute(TOKEN_CLAIM);
        if(c==null || !org.springframework.util.StringUtils.hasText(c.getWorkspaceId()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"请先登录工作空间");
        return c.getWorkspaceId();
    }
    @GetMapping("/page")
    public Result page(HttpServletRequest request,@RequestParam(defaultValue="1") int page,
                       @RequestParam(defaultValue="20") int size) {
        return Result.success(plans.selectPage(new Page<>(Math.max(1,page),Math.min(100,Math.max(1,size))),
                new QueryWrapper<CenterPatrolPlan>().select("id","center_code","sub_code","task_code","task_name","isenable","execution_id")
                        .eq("workspace_id",workspace(request)).orderByDesc("id")));
    }
    @PostMapping("/{id}/state/{state}")
    public Result state(HttpServletRequest request,@PathVariable long id,@PathVariable int state) {
        service.change(id,state,workspace(request)); return Result.success();
    }
    @GetMapping("/{id}/executions")
    public Result executions(HttpServletRequest request,@PathVariable long id,
            @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size) {
        String workspace=workspace(request);
        CenterPatrolPlan p=plans.selectById(id);
        if(p==null || !workspace.equals(p.getWorkspaceId())) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"计划不存在");
        return Result.success(tasks.selectPage(new Page<>(Math.max(1,page),Math.min(100,Math.max(1,size))),
                new QueryWrapper<CenterOrdinaryTaskEntity>().select("id","task_code","task_name","scheduled_time","fixed_start_time","plan_type","status","job_id","eua_record_id","eua_task_id","message")
                        .eq("upper_plan_id",id).eq("workspace_id",workspace).orderByDesc("id")));
    }
}
