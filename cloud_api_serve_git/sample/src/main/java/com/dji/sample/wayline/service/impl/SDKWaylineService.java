package com.dji.sample.wayline.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.df.framework.redis.RedisUtils;
import com.dji.sample.common.error.CommonErrorEnum;
import com.dji.sample.component.mqtt.model.EventsReceiver;
import com.dji.sample.component.websocket.model.BizCodeEnum;
import com.dji.sample.component.websocket.service.IWebSocketMessageService;
import com.dji.sample.df.wind.FlyToFront;
import com.dji.sample.manage.model.dto.DeviceDTO;
import com.dji.sample.manage.model.enums.UserTypeEnum;
import com.dji.sample.manage.service.IDeviceRedisService;
import com.dji.sample.media.model.MediaFileCountDTO;
import com.dji.sample.media.service.IMediaRedisService;
import com.dji.sample.wayline.dao.IWaylineJobMapper;
import com.dji.sample.wayline.model.dto.WaylineJobDTO;
import com.dji.sample.wayline.model.entity.WaylineJobEntity;
import com.dji.sample.wayline.model.enums.WaylineJobStatusEnum;
import com.dji.sample.wayline.service.IWaylineFileService;
import com.dji.sample.wayline.service.IWaylineJobService;
import com.dji.sample.wayline.service.IWaylineRedisService;
import com.dji.sdk.cloudapi.wayline.*;
import com.dji.sdk.cloudapi.wayline.api.AbstractWaylineService;
import com.dji.sdk.mqtt.MqttReply;
import com.dji.sdk.mqtt.events.EventsDataRequest;
import com.dji.sdk.mqtt.events.TopicEventsRequest;
import com.dji.sdk.mqtt.events.TopicEventsResponse;
import com.dji.sdk.mqtt.requests.TopicRequestsRequest;
import com.dji.sdk.mqtt.requests.TopicRequestsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * @author sean
 * @version 1.7
 * @date 2023/7/7
 */
@Service
@Slf4j
public class SDKWaylineService extends AbstractWaylineService {

    @Autowired
    private IDeviceRedisService deviceRedisService;

    @Autowired
    private IWaylineRedisService waylineRedisService;

    @Autowired
    private IMediaRedisService mediaRedisService;

    @Autowired
    private IWebSocketMessageService webSocketMessageService;

    @Autowired
    private IWaylineJobService waylineJobService;

    @Autowired
    private IWaylineFileService waylineFileService;

    @Autowired
    private IWaylineJobMapper waylineJobMapper;

    @Resource
    FlyToFront flyToFront;

    @Resource
    RedisUtils redisUtils;

    @Override
    public TopicEventsResponse<MqttReply> deviceExitHomingNotify(TopicEventsRequest<DeviceExitHomingNotify> request, MessageHeaders headers) {
        return super.deviceExitHomingNotify(request, headers);
    }

    @Override
    public TopicEventsResponse<MqttReply> flighttaskProgress(TopicEventsRequest<EventsDataRequest<FlighttaskProgress>> response, MessageHeaders headers) {
        EventsReceiver<FlighttaskProgress> eventsReceiver = new EventsReceiver<>();
        eventsReceiver.setResult(response.getData().getResult());
        eventsReceiver.setOutput(response.getData().getOutput());
        eventsReceiver.setBid(response.getBid());
        eventsReceiver.setSn(response.getGateway());

        FlighttaskProgress output = eventsReceiver.getOutput();
        log.info("Task progress: {}", output.getProgress().toString());
        if (!eventsReceiver.getResult().isSuccess()) {
            log.error("Task progress ===> Error: " + eventsReceiver.getResult());
        }

        Optional<DeviceDTO> deviceOpt = deviceRedisService.getDeviceOnline(response.getGateway());
        if (deviceOpt.isEmpty()) {
            return new TopicEventsResponse<>();
        }

        FlighttaskStatusEnum statusEnum = output.getStatus();
        Integer currentWaypointIndex = response.getData().getOutput().getExt().getCurrentWaypointIndex();
        System.out.println("当前航点号为--------"+currentWaypointIndex+"号");

        Optional<EventsReceiver<FlighttaskProgress>> runningWaylineJob = waylineRedisService.getRunningWaylineJob(response.getGateway());
        if(runningWaylineJob != null && runningWaylineJob.isPresent() && runningWaylineJob.get().getOutput()!=null) {
            if(runningWaylineJob.get().getOutput().getExt().getCurrentWaypointIndex().equals(currentWaypointIndex)){
                System.out.println("无需处理---------");
                String flightId = runningWaylineJob.get().getOutput().getExt().getFlightId();
                String name = waylineJobMapper.selectOne(new LambdaQueryWrapper<WaylineJobEntity>()
                        .eq(WaylineJobEntity::getJobId, flightId)).getName();
                System.out.println("航点名-------"+flightId+"<UNK>");
                // 当前航点索引相同，无需处理
            }else {
                // 写发送逻辑
                String url = "http://172.20.63.157:5002/state";
                JSONObject jsonObject = new JSONObject();
                String flightId = runningWaylineJob.get().getOutput().getExt().getFlightId();
                String name = waylineJobMapper.selectOne(new LambdaQueryWrapper<WaylineJobEntity>()
                        .eq(WaylineJobEntity::getJobId, flightId)).getName();
                jsonObject.put("waylineType", "flightTask");
                jsonObject.put("status", statusEnum);
                jsonObject.put("wayPoint", currentWaypointIndex);
                jsonObject.put("flightName", name);
                String jsonInput = jsonObject.toString();
                try {
                    URL obj = new URL(url);
                    HttpURLConnection con = (HttpURLConnection) obj.openConnection();
                    // 设置请求方法
                    con.setRequestMethod("POST");
                    con.setRequestProperty("Content-Type", "application/json");
                    con.setDoOutput(true);
                    // 发送请求
                    try(OutputStream os = con.getOutputStream()) {
                        byte[] input = jsonInput.getBytes("utf-8");
                        os.write(input, 0, input.length);
                    }
                    // 获取响应
                    int responseCode = con.getResponseCode();
                    System.out.println("Response Code: " + responseCode);

                    try(BufferedReader br = new BufferedReader(
                            new InputStreamReader(con.getInputStream(), "utf-8"))) {
                        StringBuilder response1 = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response1.append(responseLine.trim());
                        }
                        JSONObject jsonResponse = JSONObject.parseObject(response1.toString());
//                      ypcx不为空代表为顶端航线，会返回偏航角，下发指令飞行至中心点
                        if(jsonResponse.getString("ypcx")!=null&&jsonResponse.getString("ypcx")!=""){
                            String turbineName = jsonResponse.getString("turbine_name");
                            String yaw = jsonResponse.getString("ypcx");
                            double value = Double.parseDouble(yaw);
                            flyToFront.flyToFront(turbineName,value);
//                          存redis,为当前任务的风机名
                            redisUtils.set("turbineName",turbineName);
//                      否则就是不停机巡检，返回图片用以存储和前端显示
                        }else {

                        }
                        System.out.println("Response: " + response.toString());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.out.println("发送当前航点号为--------"+currentWaypointIndex+"号");
            }
        }else {
            // Redis中不存在数据时的处理逻辑
            waylineRedisService.setRunningWaylineJob(response.getGateway(), eventsReceiver);
            System.out.println("首次存储航点号到Redis: " + currentWaypointIndex + "号");
        }

        waylineRedisService.setRunningWaylineJob(response.getGateway(), eventsReceiver);

        if (statusEnum.isEnd()) {
            WaylineJobDTO job = WaylineJobDTO.builder()
                    .jobId(response.getBid())
                    .status(WaylineJobStatusEnum.SUCCESS.getVal())
                    .completedTime(LocalDateTime.now())
                    .mediaCount(output.getExt().getMediaCount())
                    .build();

            // record the update of the media count.
            if (Objects.nonNull(job.getMediaCount()) && job.getMediaCount() != 0) {
                mediaRedisService.setMediaCount(response.getGateway(), job.getJobId(),
                        MediaFileCountDTO.builder().deviceSn(deviceOpt.get().getChildDeviceSn())
                                .jobId(response.getBid()).mediaCount(job.getMediaCount()).uploadedCount(0).build());
            }

            if (FlighttaskStatusEnum.OK != statusEnum) {
                job.setCode(eventsReceiver.getResult().getCode());
                job.setStatus(WaylineJobStatusEnum.FAILED.getVal());
            }
            waylineJobService.updateJob(job);
            waylineRedisService.delRunningWaylineJob(response.getGateway());
            waylineRedisService.delPausedWaylineJob(response.getBid());
        }

        webSocketMessageService.sendBatch(deviceOpt.get().getWorkspaceId(), UserTypeEnum.WEB.getVal(),
                BizCodeEnum.FLIGHT_TASK_PROGRESS.getCode(), eventsReceiver);

        System.out.println("执行---------------");
        return new TopicEventsResponse<>();
    }

    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    @Override
    public TopicRequestsResponse<MqttReply<FlighttaskResourceGetResponse>> flighttaskResourceGet(TopicRequestsRequest<FlighttaskResourceGetRequest> response, MessageHeaders headers) {
        String jobId = response.getData().getFlightId();

        Optional<DeviceDTO> deviceOpt = deviceRedisService.getDeviceOnline(response.getGateway());
        if (deviceOpt.isEmpty()) {
            log.error("The device is offline, please try again later.");
            return new TopicRequestsResponse().setData(MqttReply.error(CommonErrorEnum.DEVICE_OFFLINE));
        }
        Optional<WaylineJobDTO> waylineJobOpt = waylineJobService.getJobByJobId(deviceOpt.get().getWorkspaceId(), jobId);
        if (waylineJobOpt.isEmpty()) {
            log.error("The wayline job does not exist.");
            return new TopicRequestsResponse().setData(MqttReply.error(CommonErrorEnum.ILLEGAL_ARGUMENT));
        }

        WaylineJobDTO waylineJob = waylineJobOpt.get();

        // get wayline file
        Optional<GetWaylineListResponse> waylineFile = waylineFileService.getWaylineByWaylineId(waylineJob.getWorkspaceId(), waylineJob.getFileId());
        if (waylineFile.isEmpty()) {
            log.error("The wayline file does not exist.");
            return new TopicRequestsResponse().setData(MqttReply.error(CommonErrorEnum.ILLEGAL_ARGUMENT));
        }
        // get file url
        try {
            URL url = waylineFileService.getObjectUrl(waylineJob.getWorkspaceId(), waylineFile.get().getId());
            return new TopicRequestsResponse<MqttReply<FlighttaskResourceGetResponse>>().setData(
                    MqttReply.success(new FlighttaskResourceGetResponse()
                            .setFile(new FlighttaskFile()
                                    .setUrl(url.toString())
                                    .setFingerprint(waylineFile.get().getSign()))));
        } catch (SQLException | NullPointerException e) {
            e.printStackTrace();
            return new TopicRequestsResponse().setData(MqttReply.error(CommonErrorEnum.SYSTEM_ERROR));
        }
    }
}
