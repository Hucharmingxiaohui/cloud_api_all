package com.dji.sample.df.electricInspectionDf.control;

import com.df.framework.thread.CustomExecutorFactory;
import com.df.framework.vo.Result;
import com.df.server.dto.robotDog.AnalyseParamsRecReq;
import com.dji.sample.df.electricInspectionDf.service.ResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.ConcurrentLinkedQueue;


@RestController
@RequestMapping("result/api/v1/files")
public class ResultController {

    private static ConcurrentLinkedQueue<AnalyseParamsRecReq> AnalyseResultQueue = new ConcurrentLinkedQueue<>();

    @Autowired
    private ResultService resultService;
    //  参考智能分析，返回图片结果转为base64，发送智能请求，进行联调
    @GetMapping("/{workspace_id}/analyse")
    public void getFileUrl(@PathVariable(name = "workspace_id") String workspaceId,String jobId, HttpServletResponse response) {

        CustomExecutorFactory.AnalyseHandlePool.execute(() -> {
            try {
                resultService.handleUavResult(workspaceId,jobId,response);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

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
                            resultService.analyseFinish(params);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
            );
        }
    }
}
