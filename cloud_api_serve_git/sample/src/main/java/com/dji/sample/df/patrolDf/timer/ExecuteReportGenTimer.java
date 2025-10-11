package com.dji.sample.df.patrolDf.timer;


import com.df.server.dto.HisUniTask.ConfirmHisTaskReportParams;
import com.df.server.mapper.his.HisUniTaskReportMapper;
import com.df.server.service.his.HisUniTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ExecuteReportGenTimer {

    private static final ConcurrentHashMap<Integer, ConfirmHisTaskReportParams> reportGenBucket = new ConcurrentHashMap<>();

    private boolean isGenRunning = false;

    @Autowired
    private HisUniTaskService hisUniTaskService;
    @Autowired
    private HisUniTaskReportMapper hisUniTaskReportMapper;


    /**
     * 增加
     *
     * @param key
     * @param value
     */
    public static synchronized void putMap(Integer key, ConfirmHisTaskReportParams value) {
        reportGenBucket.put(key, value);
    }

    /**
     * 获取数量
     */
    public static int getSize() {
        return reportGenBucket.size();
    }

    /**
     * 单线程逐个报告导出
     */
    @Scheduled(fixedDelay = 3 * 1000L)
    public void executeReportGen() {
        //没有正在执行生成时才去获取一个请求进入生成流程
        if (!isGenRunning) {
            for (Iterator<Map.Entry<Integer, ConfirmHisTaskReportParams>> iterator = reportGenBucket.entrySet().iterator(); iterator.hasNext(); ) {
                Map.Entry<Integer, ConfirmHisTaskReportParams> item = iterator.next();
                Integer reportId = item.getKey();
                ConfirmHisTaskReportParams params = item.getValue();
                Integer imageNeed = params.getImageNeed();
                // 导出文件类型 1word,2 excel
                String fileType = params.getFileType();
                isGenRunning = true;
                try {
                    log.error("生成报告开始，巡视任务ID：{} ", reportId);
                    hisUniTaskService.genPatrolTaskWordNew(reportId, imageNeed, fileType);
                } catch (Exception e) {
                    hisUniTaskReportMapper.deleteById(reportId);
                    log.error("生成报告失败，巡视任务ID：{}，原因 ", reportId, e);
                }
                iterator.remove();
                isGenRunning = false;
            }
        }
    }
}
