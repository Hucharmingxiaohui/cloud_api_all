package com.dji.sample.center.thread;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

@Component
@Slf4j
public class ExecutorFactory {
    private ExecutorService executorService;
    private ScheduledExecutorService scheduledExecutorService;

    /**
     * 分析结果处理线程
     */
    public static final ThreadPoolExecutor AnalyseHandlePool = new ThreadPoolExecutor(
            10,
            1000,
            0L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(1024),
            Executors.defaultThreadFactory(),
            new ThreadPoolExecutor.AbortPolicy());

    /**
     * FTP 上传中心线程
     */
    public static final ThreadPoolExecutor UploadCenterFTPPool = new ThreadPoolExecutor(
            10,
            1000,
            0L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(1024),
            Executors.defaultThreadFactory(),
            new ThreadPoolExecutor.AbortPolicy());

    /**
     * 图片压缩线程
     */
    public static final ThreadPoolExecutor ScaleImagePool = new ThreadPoolExecutor(
            10,
            1000,
            0L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(1024),
            Executors.defaultThreadFactory(),
            new ThreadPoolExecutor.AbortPolicy());



    /**
     * 创建一个可缓存线程池，如果线程池长度超过处理需要，
     * 可灵活回收空闲线程，若无可回收，则新建线程。
     *
     * @return
     */
    public ExecutorService getExecutorService() {
        if (this.executorService == null) {
            this.executorService = Executors.newScheduledThreadPool(1000);
        }
        return this.executorService;
    }

    /**
     * 创建一个定长线程池，支持定时及周期性任务执行。
     *
     * @return
     */
    public ScheduledExecutorService getScheduledExecutorService() {
        if (this.scheduledExecutorService == null) {
            this.scheduledExecutorService = Executors.newScheduledThreadPool(10);
        }
        return this.scheduledExecutorService;
    }
}


