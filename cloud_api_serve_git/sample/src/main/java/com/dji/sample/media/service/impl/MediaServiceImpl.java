package com.dji.sample.media.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.df.framework.redis.RedisUtils;
import com.dji.sample.component.oss.model.OssConfiguration;
import com.dji.sample.component.websocket.model.BizCodeEnum;
import com.dji.sample.component.websocket.service.IWebSocketMessageService;
import com.dji.sample.df.windDf.dao.FanWaylinePointsMapper;
import com.dji.sample.df.windDf.model.entity.FanWaylinePoints;
import com.dji.sample.manage.model.dto.DeviceDTO;
import com.dji.sample.manage.model.enums.UserTypeEnum;
import com.dji.sample.manage.service.IDeviceRedisService;
import com.dji.sample.manage.service.IDeviceService;
import com.dji.sample.media.model.MediaFileCountDTO;
import com.dji.sample.media.model.MediaFileDTO;
import com.dji.sample.media.service.IFileService;
import com.dji.sample.media.service.IMediaRedisService;
import com.dji.sample.media.service.IMediaService;
import com.dji.sample.storage.service.IStorageService;
import com.dji.sample.wayline.service.IWaylineJobService;
import com.dji.sdk.cloudapi.media.*;
import com.dji.sdk.cloudapi.media.api.AbstractMediaService;
import com.dji.sdk.cloudapi.storage.StsCredentialsResponse;
import com.dji.sdk.mqtt.MqttReply;
import com.dji.sdk.mqtt.events.TopicEventsRequest;
import com.dji.sdk.mqtt.events.TopicEventsResponse;
import com.dji.sdk.mqtt.requests.TopicRequestsRequest;
import com.dji.sdk.mqtt.requests.TopicRequestsResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author sean
 * @version 0.2
 * @date 2021/12/9
 */
@Service
@Slf4j
public class MediaServiceImpl extends AbstractMediaService implements IMediaService {

    @Autowired
    private IFileService fileService;

    @Autowired
    private IWaylineJobService waylineJobService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private IWebSocketMessageService webSocketMessageService;

    @Autowired
    private IDeviceRedisService deviceRedisService;

    @Autowired
    private IMediaRedisService mediaRedisService;

    @Autowired
    private IStorageService storageService;

    @Autowired
    FanWaylinePointsMapper fanWaylinePointsMapper;

    @Resource
    private RedisUtils redisUtils;

    @Override
    public Boolean fastUpload(String workspaceId, String fingerprint) {
        return fileService.checkExist(workspaceId, fingerprint);
    }

    @Override
    public Integer saveMediaFile(String workspaceId, MediaUploadCallbackRequest file) {
        return fileService.saveFile(workspaceId, file);
    }

    @Override
    public List<String> getAllTinyFingerprintsByWorkspaceId(String workspaceId) {
        return fileService.getAllFilesByWorkspaceId(workspaceId)
                .stream()
                .map(MediaFileDTO::getTinnyFingerprint)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getExistTinyFingerprints(String workspaceId, List<String> tinyFingerprints) {
        List<String> tinyFingerprintList = this.getAllTinyFingerprintsByWorkspaceId(workspaceId);
        return tinyFingerprints
                .stream()
                .filter(tinyFingerprintList::contains)
                .collect(Collectors.toList());

    }

    @Override
    public TopicRequestsResponse<MqttReply<StsCredentialsResponse>> storageConfigGet(TopicRequestsRequest<StorageConfigGet> request, MessageHeaders headers) {
        return new TopicRequestsResponse<MqttReply<StsCredentialsResponse>>()
                .setData(MqttReply.success(storageService.getSTSCredentials()));
    }

    @Override
    public TopicEventsResponse<MqttReply> fileUploadCallback(TopicEventsRequest<FileUploadCallback> request, MessageHeaders headers) {
        FileUploadCallback callback = request.getData();

        if (MqttReply.CODE_SUCCESS != callback.getResult()) {
            log.error("Media file upload failed!");
            return null;
        }

        String jobId = callback.getFile().getExt().getFlightId();

        Optional<DeviceDTO> deviceOpt = deviceRedisService.getDeviceOnline(request.getGateway());
        MediaFileCountDTO mediaFileCount = mediaRedisService.getMediaCount(request.getGateway(), jobId);
        // duplicate data
        if (deviceOpt.isEmpty()
                || (Objects.nonNull(mediaFileCount) && request.getBid().equals(mediaFileCount.getBid())
                && request.getTid().equals(mediaFileCount.getTid()))) {
            return new TopicEventsResponse<MqttReply>().setData(MqttReply.success());
        }

        DeviceDTO device = deviceOpt.get();
        boolean isSave = parseMediaFile(callback, device);
        if (!isSave) {
            log.error("Failed to save the file to the database, please check the data manually.");
            return null;
        }

        notifyUploadedCount(mediaFileCount, request, jobId, device);
        return new TopicEventsResponse<MqttReply>().setData(MqttReply.success());
    }

    @Override
    public TopicEventsResponse<MqttReply> highestPriorityUploadFlightTaskMedia(
            TopicEventsRequest<HighestPriorityUploadFlightTaskMedia> request, MessageHeaders headers) {
        String jobId = request.getData().getFlightId();
        if (!StringUtils.hasText(jobId)) {
            return null;
        }

        MediaFileCountDTO countDTO = mediaRedisService.getMediaHighestPriority(request.getGateway());
        if (Objects.nonNull(countDTO)) {
            if (jobId.equals(countDTO.getJobId())) {
                mediaRedisService.setMediaHighestPriority(request.getGateway(), countDTO);
                return new TopicEventsResponse<MqttReply>().setData(MqttReply.success());
            }
            countDTO.setPreJobId(countDTO.getJobId());
        } else {
            countDTO = new MediaFileCountDTO();
        }
        countDTO.setJobId(jobId);
        mediaRedisService.setMediaHighestPriority(request.getGateway(), countDTO);

        Optional<DeviceDTO> deviceOpt = deviceRedisService.getDeviceOnline(request.getGateway());
        if (deviceOpt.isEmpty()) {
            return null;
        }

        webSocketMessageService.sendBatch(deviceOpt.get().getWorkspaceId(), UserTypeEnum.WEB.getVal(),
                BizCodeEnum.HIGHEST_PRIORITY_UPLOAD_FLIGHT_TASK_MEDIA.getCode(), countDTO);

        return new TopicEventsResponse<MqttReply>().setData(MqttReply.success());
    }

    private Boolean parseMediaFile(FileUploadCallback callback, DeviceDTO device) {
        MediaUploadCallbackRequest file = convert2callbackRequest(callback.getFile());
        // Set the drone sn that shoots the media
        file.getExt().setSn(device.getChildDeviceSn());

        // set path
        String objectKey = file.getObjectKey();
        file.setPath(objectKey.substring(Optional.of(objectKey.indexOf(OssConfiguration.objectDirPrefix))
                .filter(index -> index > 0).map(index -> index++).orElse(0),
                objectKey.lastIndexOf("/")));

        return fileService.saveFile(device.getWorkspaceId(), file) > 0;
    }

    private void notifyUploadedCount(MediaFileCountDTO mediaFileCount, TopicEventsRequest<FileUploadCallback> request, String jobId, DeviceDTO dock) {
        // Do not notify when files that do not belong to the route are uploaded.
        if (Objects.isNull(mediaFileCount)) {
            return;
        }
        mediaFileCount.setBid(request.getBid());
        mediaFileCount.setTid(request.getTid());
        mediaFileCount.setUploadedCount(mediaFileCount.getUploadedCount() + 1);

//      如果是不停机巡检任务，已上传图片要加上
        int videoPointNum=0;
        FanWaylinePoints fanWaylinePoints = fanWaylinePointsMapper.selectOne(new LambdaQueryWrapper<FanWaylinePoints>()
                .eq(FanWaylinePoints::getJobId, redisUtils.get("jobId"))
                .orderByDesc(FanWaylinePoints::getId)
                .last("LIMIT 1"));
        if(fanWaylinePoints!=null){
            Integer jobType = fanWaylinePoints.getJobType();
            if (jobType == 1 && fanWaylinePoints.getVideoFanPoints()!=null) {
//                videoPoints  可能是null
                JSONArray videoPoints = JSON.parseArray(fanWaylinePoints.getVideoFanPoints());
                videoPointNum = videoPoints.size();
            }
        }

        // After all the files of the job are uploaded, delete the media file key.
        if (mediaFileCount.getUploadedCount()+videoPointNum >= mediaFileCount.getMediaCount()) {
            mediaRedisService.delMediaCount(request.getGateway(), jobId);

            // After uploading, delete the key with the highest priority.
            MediaFileCountDTO fileCount = mediaRedisService.getMediaHighestPriority(request.getGateway());
            if (Objects.nonNull(fileCount) && jobId.equals(fileCount.getJobId())) {
                mediaRedisService.delMediaHighestPriority(request.getGateway());
            }
        } else {
            mediaRedisService.setMediaCount(request.getGateway(), jobId, mediaFileCount);
        }

        webSocketMessageService.sendBatch(dock.getWorkspaceId(), UserTypeEnum.WEB.getVal(),
                BizCodeEnum.FILE_UPLOAD_CALLBACK.getCode(), mediaFileCount);
    }

    private MediaUploadCallbackRequest convert2callbackRequest(FileUploadCallbackFile file) {
        if (Objects.isNull(file)) {
            return null;
        }
        return new MediaUploadCallbackRequest()
                .setExt(Optional.ofNullable(file.getExt())
                        .map(ext -> new MediaFileExtension()
                                .setDroneModelKey(ext.getDroneModelKey())
                                .setFileGroupId(ext.getFlightId())
                                .setOriginal(ext.getOriginal())
                                .setPayloadModelKey(ext.getPayloadModelKey()))
                        .orElse(new MediaFileExtension()))
                .setMetadata(Optional.ofNullable(file.getMetadata())
                        .map(data -> new MediaFileMetadata()
                                .setAbsoluteAltitude(data.getAbsoluteAltitude())
                                .setGimbalYawDegree(data.getGimbalYawDegree())
                                .setRelativeAltitude(data.getRelativeAltitude())
                                .setShootPosition(data.getShootPosition())
                                .setCreatedTime(data.getCreatedTime()))
                        .get())
                .setName(file.getName())
                .setObjectKey(file.getObjectKey())
                .setPath(file.getPath());
    }
}
