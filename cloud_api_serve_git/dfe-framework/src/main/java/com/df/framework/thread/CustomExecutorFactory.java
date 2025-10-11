package com.df.framework.thread;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class CustomExecutorFactory {

    /**
     *
     */
    public static final ThreadPoolExecutor AnalyseHandlePool = new ThreadPoolExecutor(
            10,
            20,
            0L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(1024),
            new CustomizableThreadFactory("AnalyseHandlePool-"),
            new ThreadPoolExecutor.AbortPolicy());

    public static final ThreadPoolExecutor dbHandlepool = new ThreadPoolExecutor(
            10,
            100,
            0L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(1024),
            new CustomizableThreadFactory("dbHandlePool-"),
            new ThreadPoolExecutor.AbortPolicy());

    public static final ThreadPoolExecutor ScaleImagePool = new ThreadPoolExecutor(
            10,
            100,
            0L,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(1024),
            new CustomizableThreadFactory("scalePool-"),
            new ThreadPoolExecutor.AbortPolicy());


}


