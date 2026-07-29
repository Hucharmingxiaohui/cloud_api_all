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
import com.dji.sample.df.cqDockDf.model.dto.CqApiResponse;
import com.dji.sample.df.cqDockDf.model.entity.CqDockTaskPictureEntity;
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
    private PatrolHostSocketClient patrolHostSocketClient;
    @Autowired
    private CenterNormalConfig centerConfig;
    @Autowired
    private CqDockProperties cqDockProperties;

    public void fetchSaveAndReport(String taskCode, String taskName, String euaTaskId) {
        CqApiResponse response = cqDockApiService.pictureList(euaTaskId);
        if (response == null || !(Boolean.TRUE.equals(response.getSuccess()) || Objects.equals(response.getCode(), 200))) {
            log.warn("EUA图片列表获取失败: taskCode={}, euaTaskId={}, msg={}", taskCode, euaTaskId, response == null ? null : response.getMsg());
            return;
        }
        List<JSONObject> pictures = extractPictures(response.getData());
        if (pictures.isEmpty()) {
            log.info("EUA图片列表为空: taskCode={}, euaTaskId={}", taskCode, euaTaskId);
            return;
        }
        for (JSONObject picture : pictures) {
            saveAndReportPicture(taskCode, taskName, euaTaskId, picture);
        }
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

    private void saveAndReportPicture(String taskCode, String taskName, String euaTaskId, JSONObject picture) {
        String pictureId = firstText(picture, "id", "pictureId", "photoId");
        String pictureUrl = firstText(picture, "pictureUrl", "url", "fileUrl", "path");
        String pictureName = firstText(picture, "pictureName", "fileName", "name");
        if (!StringUtils.hasText(pictureName)) {
            pictureName = buildPictureName(pictureUrl, pictureId);
        }
//      存数据库
        CqDockTaskPictureEntity entity = findPicture(taskCode, euaTaskId, pictureId, pictureUrl);
        if (entity == null) {
            entity = new CqDockTaskPictureEntity();
            entity.setBusinessId(taskCode);
            entity.setTaskName(taskName);
            entity.setEuaTaskId(euaTaskId);
            entity.setPictureId(pictureId);
            entity.setPictureName(pictureName);
            entity.setPictureUrl(pictureUrl);
//          todo 后续需确定点位id从哪获取，接口补充字段？或者图片名称为点位名称再进行对应
            entity.setPointId(firstText(picture, "pointId", "deviceId", "photoPointId"));
            entity.setPointName(firstText(picture, "pointName", "deviceName", "photoPointName"));
            entity.setRawData(picture.toJSONString());
            entity.setReportStatus(0);
            Date now = new Date();
            entity.setCreateTime(now);
            entity.setUpdateTime(now);
            cqDockTaskPictureMapper.insert(entity);
        }

        try {
            if (!StringUtils.hasText(entity.getLocalPath()) || !new File(entity.getLocalPath()).exists()) {
//              保存图片到本地（如果这个pictureId对应的图片之前没存过）
                entity.setLocalPath(downloadPicture(pictureUrl, taskCode, pictureName));
            }
//          利用ftp上传图片到上级巡视
            entity.setFtpPath(uploadPicture(taskCode, entity.getLocalPath()));
//          进行图片巡视上级上报
            sendPictureResult(taskCode, taskName, euaTaskId, entity);
            entity.setReportStatus(1);
            entity.setReportMsg("上报成功");
        } catch (Exception e) {
            entity.setReportStatus(2);
            entity.setReportMsg(e.getMessage());
            log.error("EUA图片上报失败: taskCode={}, euaTaskId={}, pictureUrl={}", taskCode, euaTaskId, pictureUrl, e);
        }
        entity.setUpdateTime(new Date());
        cqDockTaskPictureMapper.updateById(entity);
    }

    private CqDockTaskPictureEntity findPicture(String taskCode, String euaTaskId, String pictureId, String pictureUrl) {
        LambdaQueryWrapper<CqDockTaskPictureEntity> wrapper = new LambdaQueryWrapper<CqDockTaskPictureEntity>()
                .eq(CqDockTaskPictureEntity::getBusinessId, taskCode)
                .eq(CqDockTaskPictureEntity::getEuaTaskId, euaTaskId);
        if (StringUtils.hasText(pictureId)) {
            wrapper.eq(CqDockTaskPictureEntity::getPictureId, pictureId);
        } else {
            wrapper.eq(CqDockTaskPictureEntity::getPictureUrl, pictureUrl);
        }
        return cqDockTaskPictureMapper.selectOne(wrapper.last("LIMIT 1"));
    }

    private String downloadPicture(String pictureUrl, String taskCode, String pictureName) throws Exception {
        if (!StringUtils.hasText(pictureUrl)) {
            throw new IllegalArgumentException("EUA图片地址为空");
        }
        Path dir = Paths.get(cqDockProperties.getPictureLocalPath(), taskCode);
        Files.createDirectories(dir);
        String safeName = FileNameUtils.convertChineseToPinyinInitials(pictureName);
        Path target = dir.resolve(safeName);
//      todo 暂时按照url可直接读取进行编写，后续可能调整
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
