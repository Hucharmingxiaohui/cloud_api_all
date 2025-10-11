package com.dji.sample.df.supControlDf.control;

import com.dji.sample.df.electricInspectionDf.model.PubWaylineJobPlanDfEntity;
import com.dji.sample.df.supControlDf.service.TaskcontrolService;
import com.dji.sample.df.thirdKmzDf.entity.pointResult.PointResult;
import com.dji.sdk.common.HttpResultResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
@RestController
@RequestMapping("pub/api/v1/supTask")
public class Taskcontrol {
    @Autowired
    private TaskcontrolService taskcontrolService;
    @PostMapping("/superiorCreatePlanJob")
    HttpResultResponse superiorCreatePlanJob(@RequestBody PubWaylineJobPlanDfEntity pubWaylineJobPlanDfEntity) throws SQLException {
        String result = taskcontrolService.SuperiorCreatePlanJob(pubWaylineJobPlanDfEntity);
        return HttpResultResponse.success().setMessage(result);
    }
    @GetMapping("/getPointResultByJobId")
    HttpResultResponse getPointResultByJobId(@RequestParam String job_id) throws Exception {
        PointResult pointResult= taskcontrolService.getResultByJobId(job_id);
        return   HttpResultResponse.success(pointResult);
    }

    /***
     *
     * @param job_id
     * @param type
     * @param command
     * @return
     */
    @PostMapping("/taskController")
    HttpResultResponse taskController(
            @RequestParam String job_id,
            @RequestParam String type,
            @RequestParam String  command){
        String  flag= taskcontrolService.superiorTaskHandling(job_id,type,command);
        return HttpResultResponse.success(flag);
    }
}
