package com.dji.sample;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dji.sample.center.thread.ExecutorFactory;
import com.dji.sample.center.v2022.handler.PatrolHostSocketClient;
import com.dji.sample.control.model.enums.TestEnum;
import com.dji.sample.wayline.dao.IWaylineJobMapper;
import com.dji.sample.wayline.model.entity.WaylineJobEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = CloudApiSampleApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class Test {

    @Autowired
    private PatrolHostSocketClient patrolHostSocketClient;

    @Autowired
    private ExecutorFactory executorFactory;

    @Autowired
    private IWaylineJobMapper waylineJobMapper;

    @org.junit.jupiter.api.Test
    void test(){

//        executorFactory.getExecutorService().submit(this.patrolHostSocketClient);
        System.out.println(TestEnum.find("A").getDescription());
        System.out.println(TestEnum.valueOf("AAA").getNumber());
        String name = waylineJobMapper.selectOne(new LambdaQueryWrapper<WaylineJobEntity>()
                .eq(WaylineJobEntity::getJobId, "316fad36-1387-49b2-ab7e-34f95347cdca")).getName();
        System.out.println(name+"------------------------------");
    }
}
