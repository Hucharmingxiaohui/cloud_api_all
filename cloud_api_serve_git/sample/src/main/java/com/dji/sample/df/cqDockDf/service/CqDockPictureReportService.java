package com.dji.sample.df.cqDockDf.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dji.sample.center.config.CenterNormalConfig;
import com.dji.sample.center.utils.DateUtils;
import com.dji.sample.center.utils.ftp.FtpUtils;
import com.dji.sample.center.v2022.command.base.PatrolHostCommand;
import com.dji.sample.center.v2022.command.upload.PatrolResultItem;
import com.dji.sample.center.v2022.handler.PatrolHostSocketClient;
import com.dji.sample.df.cqDockDf.config.CqDockProperties;
import com.dji.sample.df.cqDockDf.dao.CqDockTaskPictureMapper;
import com.dji.sample.df.cqDockDf.dao.CqDockTaskRecordMapper;
import com.dji.sample.df.cqDockDf.model.dto.CqApiResponse;
import com.dji.sample.df.cqDockDf.model.entity.CqDockTaskPictureEntity;
import com.dji.sample.df.cqDockDf.model.entity.CqDockTaskRecordEntity;
import com.dji.sample.df.wind.utils.FileNameUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class CqDockPictureReportService {

    @Autowired
    private CqDockApiService cqDockApiService;
    @Autowired
    private CqDockTaskPictureMapper cqDockTaskPictureMapper;
    @Autowired
    private CqDockTaskRecordMapper cqDockTaskRecordMapper;
    @Autowired
    private PatrolHostSocketClient patrolHostSocketClient;
    @Autowired
    private CenterNormalConfig centerConfig;
    @Autowired
    private CqDockProperties cqDockProperties;

    /**
     * 保留原手动测试链路：HTTP查图 -> 本地保存 -> FTP上传 -> 上级Type=61上报。
     */
    public void fetchSaveAndReport(String taskCode, String taskName, String euaTaskId) {
        fetchAndSavePictures(taskCode, taskName, euaTaskId, true);
    }

    /**
     * MQTT主动推送链路：只入库和下载到本地，不上传FTP、不上报上级。
     */
    public void saveMqttPicture(String topic, JSONObject picture) {
        String euaTaskId = firstText(picture, "taskId", "euaTaskId", "eua_task_id");
        if (!StringUtils.hasText(euaTaskId)) {
            log.warn("EUA MQTT图片推送缺少taskId，忽略: topic={}, payload={}", topic, picture.toJSONString());
            return;
        }

        CqDockTaskRecordEntity record = findTaskRecord(euaTaskId);
        String taskCode = record == null ? firstText(picture, "businessId", "taskCode", "task_code") : record.getBusinessId();
        if (!StringUtils.hasText(taskCode)) {
            taskCode = euaTaskId;
        }
        String taskName = record == null ? firstText(picture, "taskName", "task_name") : record.getTaskName();

        savePicture(taskCode, taskName, euaTaskId, picture, false, "mqtt");
        log.info("EUA MQTT图片已入库/本地保存: topic={}, taskCode={}, euaTaskId={}, pictureName={}, totalNum={}",
                topic, taskCode, euaTaskId, picture.getString("pictureName"), picture.getString("totalNum"));
    }

    private int fetchAndSavePictures(String taskCode, String taskName, String euaTaskId, boolean reportToCenter) {
        CqApiResponse response = cqDockApiService.pictureList(euaTaskId);
        if (response == null || !(Boolean.TRUE.equals(response.getSuccess()) || Objects.equals(response.getCode(), 200))) {
            log.warn("EUA图片列表获取失败: taskCode={}, euaTaskId={}, msg={}", taskCode, euaTaskId, response == null ? null : response.getMsg());
            return 0;
        }
        List<JSONObject> pictures = extractPictures(response.getData());
        if (pictures.isEmpty()) {
            log.info("EUA图片列表为空: taskCode={}, euaTaskId={}", taskCode, euaTaskId);
            return 0;
        }
        int count = 0;
        for (JSONObject picture : pictures) {
            if (savePicture(taskCode, taskName, euaTaskId, picture, reportToCenter, "http") != null) {
                count++;
            }
        }
        return count;
    }

    private List<JSONObject> extractPictures(JSONObject data) {
        if (data == null) {
            return Collections.emptyList();
        }
        JSONArray array = data.getJSONArray("pictureList");
        if (array == null) {
            array = data.getJSONArray("list");
        }
        if (array == null) {
            array = data.getJSONArray("records");
        }
        if (array == null) {
            return Collections.emptyList();
        }
        List<JSONObject> result = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            JSONObject item = array.getJSONObject(i);
            if (item != null) {
                result.add(item);
            }
        }
        return result;
    }

    private CqDockTaskPictureEntity savePicture(String taskCode, String taskName, String euaTaskId,
                                                JSONObject picture, boolean reportToCenter, String source) {
        String pictureId = firstText(picture, "pictureId", "photoId","id");
        String pictureUrl = firstText(picture, "pictureUrl", "url", "fileUrl", "path");
        String pictureName = firstText(picture, "pictureName", "fileName", "name");
        if (!StringUtils.hasText(pictureName)) {
            pictureName = buildPictureName(pictureUrl, pictureId);
        }

        CqDockTaskPictureEntity entity = findPicture(taskCode, euaTaskId, pictureName, pictureId, pictureUrl);
        Date now = new Date();
        boolean isNew = false;

        if (entity == null) {
            entity = new CqDockTaskPictureEntity();
            entity.setBusinessId(taskCode);
            entity.setEuaTaskId(euaTaskId);
            entity.setReportStatus(0);
            entity.setCreateTime(now);
            isNew = true;
        }
        if (StringUtils.hasText(taskName)) {
            entity.setTaskName(taskName);
        }
        if (StringUtils.hasText(pictureId)) {
            entity.setPictureId(pictureId);
        }
        if (StringUtils.hasText(pictureName)) {
            entity.setPictureName(pictureName);
        }
        if (StringUtils.hasText(pictureUrl)) {
            entity.setPictureUrl(pictureUrl);
        }
//      todo mqtt主动上报图片消息需确定怎么获取点位id
        String pointId = firstText(picture, "pointId", "deviceId", "photoPointId");
        String pointName = firstText(picture, "pointName", "deviceName", "photoPointName");
        if (StringUtils.hasText(pointId)) {
            entity.setPointId(pointId);
        }
        if (StringUtils.hasText(pointName)) {
            entity.setPointName(pointName);
        }
        entity.setRawData(picture.toJSONString());
        entity.setUpdateTime(now);

        if (isNew) {
            cqDockTaskPictureMapper.insert(entity);
        } else {
            cqDockTaskPictureMapper.updateById(entity);
        }

        try {
            if (!StringUtils.hasText(entity.getLocalPath()) || !new File(entity.getLocalPath()).exists()) {
                entity.setLocalPath(downloadPicture(pictureUrl, taskCode, pictureName));
            }
            if (reportToCenter) {
                entity.setFtpPath(uploadPicture(taskCode, entity.getLocalPath()));
                sendPictureResult(taskCode, taskName, euaTaskId, entity);
                entity.setReportStatus(1);
                entity.setReportMsg("上报成功");
            } else if (Objects.equals(entity.getReportStatus(), 1)) {
                entity.setReportMsg(source + "已入库并保存本地，已上报状态保持不变");
            } else {
                entity.setReportStatus(0);
                entity.setReportMsg(source + "已入库并保存本地，未上报");
            }

        } catch (Exception e) {
            entity.setReportStatus(2);
            entity.setReportMsg(e.getMessage());
            log.error("EUA图片处理失败: source={}, taskCode={}, euaTaskId={}, pictureName={}, pictureUrl={}",
                    source, taskCode, euaTaskId, pictureName, pictureUrl, e);
        }
        entity.setUpdateTime(new Date());
        cqDockTaskPictureMapper.updateById(entity);
        return entity;
    }

    private CqDockTaskPictureEntity findPicture(String taskCode, String euaTaskId, String pictureName, String pictureId, String pictureUrl) {
        LambdaQueryWrapper<CqDockTaskPictureEntity> wrapper = new LambdaQueryWrapper<CqDockTaskPictureEntity>()
                .eq(CqDockTaskPictureEntity::getBusinessId, taskCode)
                .eq(CqDockTaskPictureEntity::getEuaTaskId, euaTaskId);
        if (StringUtils.hasText(pictureName)) {
            wrapper.eq(CqDockTaskPictureEntity::getPictureName, pictureName);
        } else if (StringUtils.hasText(pictureId)) {
            wrapper.eq(CqDockTaskPictureEntity::getPictureId, pictureId);
        } else {
            wrapper.eq(CqDockTaskPictureEntity::getPictureUrl, pictureUrl);
        }
        return cqDockTaskPictureMapper.selectOne(wrapper.last("LIMIT 1"));
    }

    private CqDockTaskRecordEntity findTaskRecord(String euaTaskId) {
        if (!StringUtils.hasText(euaTaskId)) {
            return null;
        }
        return cqDockTaskRecordMapper.selectOne(new LambdaQueryWrapper<CqDockTaskRecordEntity>()
                .eq(CqDockTaskRecordEntity::getEuaTaskId, euaTaskId)
                .last("LIMIT 1"));
    }

    private String downloadPicture(String pictureUrl, String taskCode, String pictureName) throws Exception {
        if (!StringUtils.hasText(pictureUrl)) {
            throw new IllegalArgumentException("EUA图片地址为空");
        }
        Path dir = Paths.get(cqDockProperties.getPictureLocalPath(), taskCode);
        Files.createDirectories(dir);
        String safeName = FileNameUtils.convertChineseToPinyinInitials(pictureName);
        Path target = dir.resolve(safeName);
        try (InputStream inputStream = new URL(pictureUrl).openStream();
             FileOutputStream outputStream = new FileOutputStream(target.toFile())) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
        }
        log.info("EUA图片下载完成: url={}, localPath={}, size={}", pictureUrl, target.toAbsolutePath(), Files.size(target));
        return target.toAbsolutePath().toString();
    }

    private String uploadPicture(String taskCode, String localPath) {
        String destDir = "/" + taskCode;
        File localFile = new File(localPath);
        if (!localFile.exists()) {
            throw new IllegalStateException("EUA图片本地文件不存在: " + localPath);
        }
        String destName = FileNameUtils.convertChineseToPinyinInitials(localFile.getName());
        Boolean uploaded = FtpUtils.getInstance().uploadFileToCenterNormal(localFile, destDir, destName);
        if (!Boolean.TRUE.equals(uploaded)) {
            throw new IllegalStateException("EUA图片FTP上传失败: " + localPath);
        }
        return String.format("%s/%s", destDir, destName);
    }

    private void sendPictureResult(String taskCode, String taskName, String euaTaskId, CqDockTaskPictureEntity entity) {
        PatrolHostCommand commandData = patrolHostSocketClient.getBaseCommand("61", "", centerConfig.getStationCode());
        PatrolResultItem item = new PatrolResultItem();
        item.setPatroldevice_name("EUA平台无人机");
        item.setPatroldevice_code("--");
        item.setTask_name(taskName);
        item.setTask_code(taskCode);
//      todo 后续确定点位id及名称
        item.setDevice_name(defaultText(entity.getPointName()));
        item.setDevice_id(defaultText(entity.getPointId()));
        item.setValue("");
        item.setUnit("");
        item.setValue_unit("");
        item.setTime(DateUtils.getNowDateTimeStr());
        item.setRecognition_type("");
        item.setFile_path(entity.getFtpPath());
        item.setFile_type("2");
        item.setRectangle("");
        item.setTask_patrolled_id(euaTaskId);
        item.setObj_id("---");
        item.setValid("1");
        commandData.addItem(item);
        patrolHostSocketClient.sendCommand(commandData, PatrolResultItem.class);
        log.info("上报EUA巡视图片: taskCode={}, euaTaskId={}, pictureId={}, filePath={}",
                taskCode, euaTaskId, entity.getPictureId(), entity.getFtpPath());
    }

    private String firstText(JSONObject object, String... keys) {
        if (object == null) {
            return null;
        }
        for (String key : keys) {
            String value = object.getString(key);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private String defaultText(String value) {
        return value == null ? "" : value;
    }

    private String buildPictureName(String pictureUrl, String pictureId) {
        if (StringUtils.hasText(pictureUrl)) {
            String path = pictureUrl.split("\\?", 2)[0];
            int index = path.lastIndexOf('/');
            if (index >= 0 && index < path.length() - 1) {
                return path.substring(index + 1);
            }
        }
        return StringUtils.hasText(pictureId) ? pictureId + ".jpg" : System.currentTimeMillis() + ".jpg";
    }
}
