package com.dji.sample.df.wind.timer;


import com.df.server.dto.HisUniTask.ConfirmHisTaskReportParams;
import com.df.server.mapper.his.HisUniTaskReportMapper;
import com.df.server.service.his.HisUniTaskService;
import com.dji.sample.df.wind.service.FjReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ExecuteFJReportGenTimer {

    private static final ConcurrentHashMap<String, String> reportGenBucket = new ConcurrentHashMap<>();

    private boolean isGenRunning = false;

    @Autowired
    private FjReportService fjReportService;
    @Autowired
    private HisUniTaskReportMapper hisUniTaskReportMapper;

    /**
     * 增加
     *
     * @param key
     * @param value
     */
    public static synchronized void putMap(String key, String value) {
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
            for (Iterator<Map.Entry<String, String>> iterator = reportGenBucket.entrySet().iterator(); iterator.hasNext(); ) {
                Map.Entry<String, String> item = iterator.next();
                String reportId = item.getKey();
                String jobId = item.getValue();
                isGenRunning = true;
                try {
                    log.error("生成报告开始，巡视任务ID：{},报告ID：{} ", jobId,reportId);
                    fjReportService.genPatrolTaskWordNew(reportId,jobId);
                } catch (Exception e) {
                    log.error("生成报告失败，巡视任务ID：{}，原因 ", jobId, e);
                }
                iterator.remove();
                isGenRunning = false;
            }
        }
    }
}
