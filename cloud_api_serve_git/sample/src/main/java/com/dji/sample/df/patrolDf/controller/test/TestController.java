package com.dji.sample.df.patrolDf.controller.test;


import com.alibaba.fastjson.JSONObject;
import com.df.framework.thread.CustomExecutorFactory;
import com.df.framework.utils.HttpUtils;
import com.df.framework.utils.file.FileUtils;
import com.df.framework.vo.Result;
import com.df.server.dto.robotDog.RobotDogPointResultDTO;
import com.df.server.mapper.uni.UniPointMapper;
import com.dji.sample.component.AuthInterceptor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Component("patrolTestController")
@AuthInterceptor.IgnoreAuth
public class TestController {
    @Autowired
    private UniPointMapper uniPointMapper;
    @Autowired
    private HttpUtils httpUtils;

    /**
     * 机器狗控制
     *
     * @return
     */
    @PostMapping("/1")
    public Result controlDogByParams() {
        List<String> list = uniPointMapper.test1();
        return Result.success(StringUtils.join(list, ","));
    }

    /**
     * 机器狗控制
     *
     * @return
     */
    @PostMapping("/2")
    public Result test2() throws InterruptedException {
        String base64txt = "static/excel/base64.txt";
        String readBase64 = "";
        try (InputStream in = FileUtils.class.getClassLoader().getResourceAsStream(base64txt)) {
            if (in != null) {
                // 使用StringBuilder来高效地构建字符串
                StringBuilder content = new StringBuilder();
                byte[] b = new byte[1024];
                int length;
                while ((length = in.read(b)) > 0) {
                    content.append(new String(b, 0, length, StandardCharsets.UTF_8));
                }
                readBase64 = content.toString();
            } else {
                System.err.println("文件未找到: " + base64txt);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        RobotDogPointResultDTO robotDogPointResultDTO = new RobotDogPointResultDTO();
        String finalReadBase6 = readBase64;
        CustomExecutorFactory.AnalyseHandlePool.execute(() -> {
            for (int i = 0; i < 1200; i++) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {

                }
                robotDogPointResultDTO.setId(i + "");
                robotDogPointResultDTO.setImages(finalReadBase6);
                httpUtils.sendPostJson("http://localhost:19500/partol/result", JSONObject.toJSONString(robotDogPointResultDTO));
            }
        });
        return Result.success(robotDogPointResultDTO);
    }

    @PostMapping("/uni_go2")
    public Result uni_go2(@RequestBody Map<String, Object> dogPatrolVO) {
        // System.out.println(dogPatrolVO);
        return Result.success();
    }

    @PostMapping("/camera")
    public Result camera(@RequestBody Map<String, Object> dogPatrolVO) {
        //  System.out.println(dogPatrolVO);
        return Result.success();
    }

    @PostMapping("/command")
    public Result command(@RequestBody Map<String, Object> dogPatrolVO) {
        //  System.out.println(dogPatrolVO);
        return Result.success();
    }

    @GetMapping("/node")
    public Result node(Map<String, Object> dogPatrolVO) {
        //  System.out.println(dogPatrolVO);
        return Result.success();
    }

    @PostMapping("/save_point")
    public Result save_point(@RequestBody Map<String, Object> dogPatrolVO) {
        //  System.out.println(dogPatrolVO);
        return Result.success();
    }
}
