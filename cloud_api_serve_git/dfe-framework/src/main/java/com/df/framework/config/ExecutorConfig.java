package com.df.framework.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置
 */
@Slf4j
@Configuration
@EnableAsync
public class ExecutorConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        /**
         * 核心线程数（默认是1）：若是IO密集型，cpu核心数*2，若是cpu密集型，cpu核心数
         * 核心线程会一直存活，及时没有任务需要执行
         * 设置allowCoreThreadTimeout=true（默认false）时，核心线程会超时关闭
         * 注意：当线程数小于核心线程数时，即使有线程空闲，线程池也会优先创建新线程处理
         */
        taskExecutor.setCorePoolSize(8);
        /**
         * 最大可创建的线程数，系统默认Integer.MAX_VALUE
         */
        taskExecutor.setMaxPoolSize(300);
        /**
         * 缓冲队列大小，系统默认Integer.MAX_VALUE
         * 注意：这个值肯定要改小，不然任务陡增时，都堆在队列中（队列值大），
         * 核心线程数就那几个，无法很快执行队列中的任务，
         * 就会延长响应时间，阻塞任务
         */
        taskExecutor.setQueueCapacity(10);
        /**
         * 允许线程空闲时间（单位：默认为秒，默认60S）
         * 当线程空闲时间达到keepAliveTime时，线程会退出，直到线程数量=corePoolSize
         * 如果allowCoreThreadTimeout=true，则会直到线程数量=0
         */
        taskExecutor.setKeepAliveSeconds(60);
        /**
         * 该方法用来设置线程池关闭的时候，等待所有任务都完成后，再继续销毁其他的Bean
         * 这样这些异步任务的销毁,就会先于数据库连接池对象的销毁。
         */
        taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
        /**
         * 任务的等待时间 如果超过这个时间还没有销毁就强制销毁，以确保应用最后能够被关闭，而不是阻塞住。
         */
        taskExecutor.setAwaitTerminationSeconds(2 * 60);
        /**
         * 设置任务拒绝策略(4种)
         * ThreadPoolExecutor类有几个内部实现类来处理这类情况：
         - AbortPolicy 丢弃任务，抛RejectedExecutionException
         - CallerRunsPolicy 由该线程调用线程运行。直接调用Runnable的run方法运行。
         - DiscardPolicy  抛弃策略，直接丢弃这个新提交的任务
         - DiscardOldestPolicy 抛弃旧任务策略，从队列中踢出最先进入队列（最后一个执行）的任务
         * 实现RejectedExecutionHandler接口，可自定义处理器
         */
        taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        /**
         * 线程前缀名称
         */
        taskExecutor.setThreadNamePrefix("taskExecutor-");
        return taskExecutor;
    }
}