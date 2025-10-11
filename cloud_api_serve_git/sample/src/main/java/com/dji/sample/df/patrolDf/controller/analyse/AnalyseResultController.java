package com.dji.sample.df.patrolDf.controller.analyse;

import com.df.framework.thread.CustomExecutorFactory;
import com.df.framework.vo.Result;
import com.df.server.dto.robotDog.AnalyseParamsRecReq;
import com.df.server.service.his.HisExePointAnalyseService;
import com.dji.sample.component.AuthInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 接收智能分析结果接口
 */
@Slf4j
@RestController
@RequestMapping("")
@AuthInterceptor.IgnoreAuth
public class AnalyseResultController {

    @Autowired
    private HisExePointAnalyseService hisExePointAnalyseService;

    private static ConcurrentLinkedQueue<AnalyseParamsRecReq> AnalyseResultQueue = new ConcurrentLinkedQueue<>();


    /**
     * 接收智能分析结果接口
     *
     * @param params
     * @return
     */
    @PostMapping("/picAnalyseRetNotify")
    public Result picAnalyseRetNotify(@RequestBody AnalyseParamsRecReq params) {
        AnalyseResultQueue.add(params);
        return Result.success();
    }

    @Scheduled(fixedDelay = 50)
    public void handleAnalyse() {
        AnalyseParamsRecReq params = AnalyseResultQueue.poll();
        if (params != null) {
            CustomExecutorFactory.AnalyseHandlePool.execute(() -> {
                        try {
                            hisExePointAnalyseService.analyseFinish(params);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
            );
        }
    }

}
