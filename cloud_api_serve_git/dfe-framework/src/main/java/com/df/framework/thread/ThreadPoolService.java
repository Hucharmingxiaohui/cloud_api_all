package com.df.framework.thread;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service("threadPoolService")
public class ThreadPoolService {

    @Async("taskExecutor")
    public void submit(Runnable runnable) {
        runnable.run();
    }
}