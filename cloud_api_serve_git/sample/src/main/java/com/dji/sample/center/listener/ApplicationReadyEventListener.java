package com.dji.sample.center.listener;

import com.dji.sample.center.config.SwitchConfig;
import com.dji.sample.center.thread.ExecutorFactory;
import com.dji.sample.center.v2022.handler.PatrolHostSocketClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ApplicationReadyEventListener extends Object implements ApplicationListener<ApplicationReadyEvent> {
    @Autowired
    private SwitchConfig switchConfig;
    @Autowired
    private ExecutorFactory executorFactory;
    @Autowired
    private PatrolHostSocketClient patrolHostClient;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (switchConfig.getCenterNormalTcpEnable().equals("true")) {
            log.info("******启动与上级TCP通讯监听 - 国网规范******");
            executorFactory.getExecutorService().submit(this.patrolHostClient);
        }

        if (switchConfig.getCenterOtherTcpEnable().equals("true")) {
            log.info("******启动与上级TCP通讯监听 - 第三方规范******");
//            executorFactory.getExecutorService().submit(this.audioProtocolServer);
        }

    }
}
