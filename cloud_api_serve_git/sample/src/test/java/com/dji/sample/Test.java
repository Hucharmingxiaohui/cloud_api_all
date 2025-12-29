package com.dji.sample;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.df.framework.ftp.FtpUtils;
import com.df.framework.redis.RedisUtils;
import com.dji.sample.center.thread.ExecutorFactory;

import com.dji.sample.center.v2022.handler.PatrolHostSocketClient;
import com.dji.sample.component.oss.model.OssConfiguration;
import com.dji.sample.component.oss.service.IOssService;
import com.dji.sample.component.oss.service.impl.OssServiceContext;
import com.dji.sample.control.model.enums.TestEnum;
import com.dji.sample.df.mediaDf.dao.IFileMapperDf;
import com.dji.sample.df.mediaDf.model.MediaFileEntity;
import com.dji.sample.df.wind.config.FjFileConfig;
import com.dji.sample.df.wind.config.WaylineUrlConfig;
import com.dji.sample.df.wind.controller.FjReportController;
import com.dji.sample.df.wind.dao.DefectEntityMapper;
import com.dji.sample.df.wind.dao.FanWaylinePointsMapper;
import com.dji.sample.df.wind.model.entity.AnalysisRequest;
import com.dji.sample.df.wind.model.entity.AnalysisResponse;
import com.dji.sample.df.wind.model.entity.DefectEntity;
import com.dji.sample.df.wind.model.entity.FanWaylinePoints;
import com.dji.sample.df.wind.service.FjReportService;

import com.dji.sample.df.wind.timer.DeleteWaylineFileTimer;
import com.dji.sample.wayline.dao.IWaylineJobMapper;
import com.dji.sample.wayline.model.entity.WaylineJobEntity;
import com.dji.sdk.mqtt.CommonTopicRequest;
import com.dji.sdk.mqtt.MqttGatewayPublish;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.dji.sample.wayline.service.impl.SDKWaylineService.convert;
import static org.jacoco.agent.rt.internal_43f5073.core.runtime.AgentOptions.OutputMode.file;

@Slf4j
@SpringBootTest(classes = CloudApiSampleApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class Test {

    @Autowired
    FanWaylinePointsMapper fanWaylinePointsMapper;

    @Autowired
    DeleteWaylineFileTimer deleteWaylineFileTimer;

    @Autowired
    private OssServiceContext ossService;

    @Autowired
    MqttGatewayPublish mqttGatewayPublish;

    @org.junit.jupiter.api.Test
    void test() throws IOException {

//        try {
//            MultipartFile file = convert(new File("C:\\Users\\90828\\Desktop\\git提交.txt"));
//            String job_id="113";
//            String ObjectKey=OssConfiguration.objectDirPrefix +  "/" + job_id + "/" +file.getOriginalFilename();
//            ossService.putObject(OssConfiguration.bucket, ObjectKey, file.getInputStream());
////            MediaFileEntity mediaFileEntity = new MediaFileEntity();
////
////            fileMapperDf.insert(mediaFileEntity)
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }

//        deleteWaylineFileTimer.deleteKmzFiles();
//        throw new UnsupportedOperationException("airsenseWarning not implemented");
        String localFile = "C:/Users/90828/Desktop/mqtt.txt";
        String destName = new File(localFile).getName();
        String destDir = "/df1560/mqtt.txt";
        FtpUtils instance = FtpUtils.getInstance();
        instance.downloadFromFtps(destDir,localFile);
    }

    @org.junit.jupiter.api.Test
    void test2(){

    }

}
