package com.dji.sample.df.cqDockDf.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dji.sample.component.AuthInterceptor;
import com.dji.sample.center.entity.UniPoint;
import com.dji.sample.df.cqDockDf.config.CqDockProperties;
import com.dji.sample.df.cqDockDf.dao.CqDockTaskRecordMapper;
import com.dji.sample.df.cqDockDf.model.dto.CqAssignTaskRequest;
import com.dji.sample.df.cqDockDf.model.dto.CqTaskIdRequest;
import com.dji.sample.df.cqDockDf.model.entity.CqDockTaskRecordEntity;
import com.dji.sample.df.substationDf.dao.UniPointMapper2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地模拟下级无人机平台（EUA）接口，用于无外部环境时验证全流程：
 * 任务下发 -> 状态轮询（1执行中 -> 15秒后7完成）-> 图片列表（2可见光+2红外，pointId取自df_uni_point.point_code）
 * -> 图片下载（本地真实图片）-> 智能分析回调模拟（可见光）。
 */
@Slf4j
@RestController
@AuthInterceptor.IgnoreAuth
@RequestMapping("/machineNest/noauth/third")
public class CqDockMockController {

    /**
     * 状态推进：assign 后该毫秒数内返回 1（执行中），之后返回 7（完成）
     */
    private static final long RUNNING_DURATION_MS = 15_000L;

    /**
     * 每种图片类型返回的图片数
     */
    private static final int PICTURE_COUNT_PER_TYPE = 2;

    private static final ConcurrentHashMap<String, Long> ASSIGN_TIME = new ConcurrentHashMap<>();

    @Autowired
    private UniPointMapper2 uniPointMapper2;
    @Autowired
    private CqDockTaskRecordMapper cqDockTaskRecordMapper;
    @Autowired
    private CqDockProperties cqDockProperties;

    @PostMapping("/task/assign_task")
    public JSONObject assignTask(@RequestBody CqAssignTaskRequest request,
                                 @RequestHeader(value = "access_key", required = false) String accessKey,
                                 @RequestHeader(value = "access_secret", required = false) String accessSecret) {
        log.info("[cq-dock-mock] assign_task headers access_key={}, access_secret={}", accessKey, mask(accessSecret));
        log.info("[cq-dock-mock] assign_task body={}", request);

        String taskId = "mock-task-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        ASSIGN_TIME.put(taskId, System.currentTimeMillis());

        JSONObject data = new JSONObject();
        data.put("taskId", taskId);
        return success(data, "任务下发成功(mock)");
    }

    /**
     * 按任务映射的航线（route_id）查 df_uni_point，取 2 可见光 + 2 红外点位生成图片列表。
     * pointId 即点位编码 df_uni_point.point_code，供分析链路反查 pointAnalyseType / 红外坐标。
     */
    @PostMapping("/picture/list")
    public JSONObject pictureList(@RequestBody CqTaskIdRequest request,
                                  @RequestHeader(value = "access_key", required = false) String accessKey,
                                  @RequestHeader(value = "access_secret", required = false) String accessSecret) {
        log.info("[cq-dock-mock] picture/list headers access_key={}, access_secret={}", accessKey, mask(accessSecret));
        log.info("[cq-dock-mock] picture/list body={}", request);

        List<JSONObject> pictureList = buildMockPictures(request.getTaskId());
        JSONObject data = new JSONObject();
        data.put("taskId", request.getTaskId());
        data.put("pictureList", pictureList);
        return success(data, "图片列表获取成功(mock)");
    }

    /**
     * 提供本地真实图片：visible1.jpg/visible2.jpg（可见光）、ir1.jpg/ir2.jpg（红外），
     * 目录由 cq-dock.mock-picture-path 配置。
     */
    @GetMapping(value = "/mock/picture/{fileName:.+}", produces = MediaType.IMAGE_JPEG_VALUE)
    public ResponseEntity<byte[]> mockPicture(@PathVariable String fileName) {
        File file = resolveMockFile(fileName);
        if (file == null) {
            log.warn("[cq-dock-mock] mock picture不存在: {}", fileName);
            return ResponseEntity.notFound().build();
        }
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            log.info("[cq-dock-mock] mock picture返回: {} size={}", file.getAbsolutePath(), bytes.length);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(bytes);
        } catch (Exception e) {
            log.error("[cq-dock-mock] mock picture读取失败: {}", file.getAbsolutePath(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 状态推进：assign 后 15 秒内返回 1（执行中），之后返回 7（完成），用于验证 Type=41 的两段上报。
     */
    @PostMapping("/task/status")
    public JSONObject taskStatus(@RequestBody CqTaskIdRequest request,
                                 @RequestHeader(value = "access_key", required = false) String accessKey,
                                 @RequestHeader(value = "access_secret", required = false) String accessSecret) {
        log.info("[cq-dock-mock] task/status headers access_key={}, access_secret={}", accessKey, mask(accessSecret));
        log.info("[cq-dock-mock] task/status body={}", request);

        String status = resolveStatus(request.getTaskId());
        JSONObject data = new JSONObject();
        data.put("taskId", request.getTaskId());
        data.put("status", status);
        log.info("[cq-dock-mock] task/status 返回 status={}", status);
        return success(data, "任务状态查询成功(mock)");
    }

    private List<JSONObject> buildMockPictures(String taskId) {
        List<JSONObject> pictureList = new ArrayList<>();
        String routeId = resolveRouteId(taskId);
        if (!StringUtils.hasText(routeId)) {
            log.warn("[cq-dock-mock] 未找到任务映射航线，返回空图片列表: taskId={}", taskId);
            return pictureList;
        }
        List<UniPoint> visiblePoints = uniPointMapper2.selectList(new LambdaQueryWrapper<UniPoint>()
                .eq(UniPoint::getWaylineId, routeId)
                .eq(UniPoint::getPicType, 0)
                .last("LIMIT " + PICTURE_COUNT_PER_TYPE));
        List<UniPoint> infraredPoints = uniPointMapper2.selectList(new LambdaQueryWrapper<UniPoint>()
                .eq(UniPoint::getWaylineId, routeId)
                .eq(UniPoint::getPicType, 1)
                .last("LIMIT " + PICTURE_COUNT_PER_TYPE));

        int seq = 1;
        int irSeq = 1;
        for (UniPoint point : visiblePoints) {
            pictureList.add(buildPicture(seq, point, 0, "visible" + seq + ".jpg"));
            seq++;
        }
        for (UniPoint point : infraredPoints) {
            pictureList.add(buildPicture(seq, point, 1, "ir" + irSeq + ".jpg"));
            seq++;
            irSeq++;
        }
        log.info("[cq-dock-mock] 生成mock图片列表: taskId={}, routeId={}, 可见光={}张, 红外={}张",
                taskId, routeId, visiblePoints.size(), infraredPoints.size());
        return pictureList;
    }

    private JSONObject buildPicture(int seq, UniPoint point, int pointType, String fileName) {
        JSONObject picture = new JSONObject();
        picture.put("id", "mock-pic-" + seq);
        picture.put("pictureId", "mock-pic-" + seq);
        picture.put("pictureUrl", "http://127.0.0.1:6789/machineNest/noauth/third/mock/picture/" + fileName);
        picture.put("pictureName", fileName);
        picture.put("pointId", point.getPointCode());
        picture.put("pointName", point.getPointName());
        picture.put("pointType", pointType);
        return picture;
    }

    private String resolveRouteId(String taskId) {
        if (!StringUtils.hasText(taskId)) {
            return null;
        }
        CqDockTaskRecordEntity record = cqDockTaskRecordMapper.selectOne(new LambdaQueryWrapper<CqDockTaskRecordEntity>()
                .eq(CqDockTaskRecordEntity::getEuaTaskId, taskId)
                .last("LIMIT 1"));
        return record == null ? null : record.getRouteId();
    }

    private File resolveMockFile(String fileName) {
        File dir = new File(cqDockProperties.getMockPicturePath());
        File direct = new File(dir, fileName);
        if (direct.exists()) {
            return direct;
        }
        // 下载保存名是拼音缩写，回调/下载按原文件名找不到时按包含匹配兜底
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().toLowerCase().contains(fileName.toLowerCase().replace(".jpg", ""))) {
                    return file;
                }
            }
        }
        return null;
    }

    private String resolveStatus(String taskId) {
        Long assignTime = ASSIGN_TIME.get(taskId);
        if (assignTime == null) {
            return "7";
        }
        return System.currentTimeMillis() - assignTime > RUNNING_DURATION_MS ? "7" : "1";
    }

    private JSONObject success(JSONObject data, String msg) {
        JSONObject result = new JSONObject();
        result.put("code", 200);
        result.put("data", data);
        result.put("msg", msg);
        result.put("success", true);
        return result;
    }

    private String mask(String value) {
        if (value == null || value.length() <= 4) {
            return value;
        }
        return value.substring(0, 2) + "***" + value.substring(value.length() - 2);
    }
}
