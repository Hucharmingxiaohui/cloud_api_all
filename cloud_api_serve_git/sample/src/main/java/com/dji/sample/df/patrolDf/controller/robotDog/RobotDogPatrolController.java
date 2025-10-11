package com.dji.sample.df.patrolDf.controller.robotDog;

import com.df.framework.thread.CustomExecutorFactory;
import com.df.framework.utils.ParamsUtils;
import com.df.framework.vo.Result;
import com.df.server.dto.robotDog.RobotDogPointResultDTO;
import com.df.server.dto.uniPoint.BindNodeDTO;
import com.df.server.service.robotDog.RobotDogPatrolService;
import com.dji.sample.component.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author 平善闯
 * @date 2025-04-13 14:10
 */
@RestController
@RequestMapping("/partol")
@AuthInterceptor.IgnoreAuth
public class RobotDogPatrolController {
    @Autowired
    RobotDogPatrolService robotDogPatrolService;

    /**
     * 新建导航点
     *
     * @param param
     * @param request
     * @return
     */
    @PostMapping("/addNode")
    public Result addNode(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        return Result.success(robotDogPatrolService.addNode());
    }

    /**
     * 保存预置位
     *
     * @param param
     * @param request
     * @return
     */
    @PostMapping("/savePos")
    public Result<Map<String, Object>> savePos(@RequestBody Map<String, Object> param, HttpServletRequest request) {
        robotDogPatrolService.savePos();
        return Result.success();
    }

    /**
     * 点位绑定
     *
     * @param param
     * @param request
     * @return
     */
    @PostMapping("/bindNode")
    public Result<Map<String, Object>> bindNode(@RequestBody BindNodeDTO param, HttpServletRequest request) {
        ParamsUtils.isBlank(param, "subCode", "pointCode", "robotPos", "robotCode");
        robotDogPatrolService.bindNode(param);
        return Result.success();
    }

    /**
     * 接收机器狗抓拍
     *
     * @param request
     * @return
     */
    @PostMapping("/result")
    public Result pointResult(@RequestBody RobotDogPointResultDTO request) {
        CustomExecutorFactory.AnalyseHandlePool.execute(() -> {
            robotDogPatrolService.handleDogResult(request);
        });
        return Result.success();
    }

}
