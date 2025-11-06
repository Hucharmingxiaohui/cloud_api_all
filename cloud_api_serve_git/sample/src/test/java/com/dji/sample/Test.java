package com.dji.sample;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.df.framework.redis.RedisUtils;
import com.dji.sample.center.thread.ExecutorFactory;
import com.dji.sample.center.v2022.handler.PatrolHostSocketClient;
import com.dji.sample.component.oss.model.OssConfiguration;
import com.dji.sample.component.oss.service.impl.OssServiceContext;
import com.dji.sample.control.model.enums.TestEnum;
import com.dji.sample.df.mediaDf.dao.IFileMapperDf;
import com.dji.sample.df.mediaDf.model.MediaFileEntity;
import com.dji.sample.df.wind.config.FjFileConfig;
import com.dji.sample.df.wind.controller.FjReportController;
import com.dji.sample.df.wind.dao.DefectEntityMapper;
import com.dji.sample.df.wind.model.entity.AnalysisRequest;
import com.dji.sample.df.wind.model.entity.AnalysisResponse;
import com.dji.sample.df.wind.model.entity.DefectEntity;
import com.dji.sample.df.wind.service.FjReportService;
import com.dji.sample.wayline.dao.IWaylineJobMapper;
import com.dji.sample.wayline.model.entity.WaylineJobEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.jacoco.agent.rt.internal_43f5073.core.runtime.AgentOptions.OutputMode.file;

@Slf4j
@SpringBootTest(classes = CloudApiSampleApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class Test {

    @Autowired
    private PatrolHostSocketClient patrolHostSocketClient;

    @Autowired
    private ExecutorFactory executorFactory;

    @Autowired
    private IWaylineJobMapper waylineJobMapper;

    @Autowired
    private OssServiceContext ossService;

    @Resource
    RedisUtils redisUtils;

    @Resource
    IFileMapperDf fileMapperDf;

    @Autowired
    private FjReportService fjReportService;

    @Autowired
    DefectEntityMapper defectEntityMapper;

    @Autowired
    private FjFileConfig fileConfig;

    @Resource
    IFileMapperDf iFileMapperDf;

    @Resource
    FjReportController fjReportController;

    @org.junit.jupiter.api.Test
    void test(){

//        executorFactory.getExecutorService().submit(this.patrolHostSocketClient);
//        System.out.println(TestEnum.find("A").getDescription());
//        System.out.println(TestEnum.valueOf("AAA").getNumber());
//        String name = waylineJobMapper.selectOne(new LambdaQueryWrapper<WaylineJobEntity>()
//                .eq(WaylineJobEntity::getJobId, "316fad36-1387-49b2-ab7e-34f95347cdca")).getName();
//        System.out.println(name+"------------------------------");

        try {
            MultipartFile file = convert(new File("C:\\Users\\90828\\Desktop\\风机参数.txt"));
            String job_id="111";
            String ObjectKey=OssConfiguration.objectDirPrefix +  "/" + job_id + "/" +file.getOriginalFilename();
            ossService.putObject(OssConfiguration.bucket, ObjectKey, file.getInputStream());
//            MediaFileEntity mediaFileEntity = new MediaFileEntity();
//
//            fileMapperDf.insert(mediaFileEntity)
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        redisUtils.set("creator", "adminPC");
    }

    @org.junit.jupiter.api.Test
    void test2(){
//        AnalysisResponse mockResponse = createMockResponse();
//        String job_id="8db78490-90b1-44a8-ab3b-d0a5ee063b16";
//        fjReportService.processAndAddDefects(mockResponse,job_id);
        String job_id="8db78490-90b1-44a8-ab3b-d0a5ee063b16";
        AnalysisRequest request = new AnalysisRequest();
        request.setFunction("defect_fjxj");
        request.setFile_path(fileConfig.getFilePictrueUrl()+job_id);
        // 动态生成文件名列表
        List<MediaFileEntity> mediaFileEntities = iFileMapperDf.selectList(new LambdaQueryWrapper<MediaFileEntity>().
                eq(MediaFileEntity::getJobId,job_id ));
        // 从Redis获取图片命名规则
        String fanPointsJson = redisUtils.get("fanPoints").toString();
        JSONArray points = JSON.parseArray(fanPointsJson);
        List<String> fileNames = fjReportController.generateFileNames(mediaFileEntities, points);
        request.setFile_name(fileNames);
        System.out.println("request"+request.toString());
        AnalysisResponse response = fjReportController.sendAnalysisRequest(request);
        if (response != null) {
            System.out.println("分析结果: " + response);
        }
        fjReportService.processAndAddDefects(response, job_id);


//        List<DefectEntity> defectList = defectEntityMapper.selectList(new LambdaQueryWrapper<DefectEntity>()
//                .eq(DefectEntity::getJobId,"8db78490-90b1-44a8-ab3b-d0a5ee063b16"));
////      缺陷数
//        long count = defectList.stream()
//                .filter(defect -> !defect.getDefectType().contains("无缺陷"))
//                .filter(defect -> !defect.getDefectType().contains("无结果"))
//                .count();
//
//        System.out.println(count+"11111111");

    }



    private AnalysisResponse createMockResponse() {
        List<AnalysisResponse.ResultItem> resultsList = new ArrayList<>();

        // 添加无缺陷结果
        resultsList.add(new AnalysisResponse.ResultItem(
                "2000", 0.9999, "无缺陷/无结果",
                "defect_fjxj",
                "/home/uav_server/defect_images/defect_out/A叶片-迎风面-1_result0.jpg",
                "0"
        ));

        // 添加有缺陷结果
        resultsList.add(new AnalysisResponse.ResultItem(
                "2000", 0.9999, Arrays.asList("LE腐蚀", "LE腐蚀", "LE腐蚀"),
                "defect_fjxj",
                "/home/uav_server/defect_images/defect_out/A叶片-迎风面-2_result0.jpg",
                "0"
        ));

        resultsList.add(new AnalysisResponse.ResultItem(
                "2000", 0.9999, Arrays.asList("漏油", "掉漆", "掉漆", "掉漆"),
                "defect_fjxj",
                "/home/uav_server/defect_images/defect_out/A叶片-迎风面-3_result0.jpg",
                "0"
        ));

        return new AnalysisResponse(resultsList);
    }




    public static MultipartFile convert(File file) throws IOException {
        return new CustomMultipartFile(
                file.getName(), file
        );
    }

    static class CustomMultipartFile implements MultipartFile {
        private final String name;
        private final File file;
        private FileInputStream fis;

        public CustomMultipartFile(String name, File file) throws IOException {
            this.name = name;
            this.file = file;
            this.fis = new FileInputStream(file);
        }

        @Override public String getName() { return name; }
        @Override public String getOriginalFilename() { return name; }
        @Override public String getContentType() { return null; }
        @Override public boolean isEmpty() { return file.length() == 0; }
        @Override public long getSize() { return file.length(); }

        @Override
        public byte[] getBytes() throws IOException {
            return new byte[0];
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return fis; // 直接返回文件流
        }

        @Override
        public void transferTo(File dest) throws IOException {
            Files.copy(fis, dest.toPath());
        }
    }
}
