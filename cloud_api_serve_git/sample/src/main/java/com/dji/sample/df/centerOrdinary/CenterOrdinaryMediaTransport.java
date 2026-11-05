package com.dji.sample.df.centerOrdinary;

import com.dji.sample.center.entity.UniPoint;
import com.dji.sample.center.utils.DateUtils;
import com.dji.sample.center.utils.ftp.FtpUtils;
import com.dji.sample.center.v2022.command.base.PatrolHostCommand;
import com.dji.sample.center.v2022.command.upload.PatrolResultItem;
import com.dji.sample.center.v2022.handler.PatrolHostSocketClient;
import com.dji.sample.df.mediaDf.model.MediaFileEntity;
import com.dji.sample.df.uavCommonHandleDf.handler.PictureSaveHandler;
import com.dji.sample.df.windDf.config.FjFileConfig;
import com.dji.sample.media.service.IFileService;
import com.dji.sample.wayline.model.entity.WaylineJobEntity;
import org.springframework.stereotype.Component;
import java.io.File;

@Component
public class CenterOrdinaryMediaTransport {
    private final IFileService files;
    private final FjFileConfig config;
    private final PatrolHostSocketClient socket;

    public CenterOrdinaryMediaTransport(IFileService files, FjFileConfig config, PatrolHostSocketClient socket) {
        this.files=files; this.config=config; this.socket=socket;
    }

    public String store(CenterOrdinaryTaskEntity task, WaylineJobEntity job, UniPoint point, MediaFileEntity file) throws Exception {
        String type = Integer.valueOf(1).equals(point.getPicType()) ? "T" : "V";
        String regId = "point_" + point.getId() + type;
        String relative = PictureSaveHandler.downloadAndConvertToJpeg2(regId,
                files.getObjectUrl(job.getWorkspaceId(), file.getFileId()).toString(), file.getFileName(),
                config.getRecfilePath()+config.getRecfileNativePath(), job.getJobId(), config.getRecfileNativePath());
        return config.getRecfilePath()+relative;
    }

    public String report(CenterOrdinaryTaskEntity task, WaylineJobEntity job, UniPoint point, MediaFileEntity file, String localPath) {
        String directory = "/center-ordinary/" + task.getId() + "/" + job.getJobId();
        String destination = file.getFileId()+".jpg";
        if (!Boolean.TRUE.equals(FtpUtils.getInstance().uploadToCenterNormal(localPath, directory, destination)))
            throw new IllegalStateException("上级普通任务选中照片FTP上传失败");
        String ftpPath = directory+"/"+destination;
        PatrolResultItem item = resultItem(task, job, point, file, ftpPath);
        PatrolHostCommand command = socket.getBaseCommand("61", "", task.getSubCode());
        command.addItem(item);
        if (!socket.sendCommand(command, PatrolResultItem.class)) throw new IllegalStateException("上级普通任务点位结果发送未确认");
        return ftpPath;
    }

    static PatrolResultItem resultItem(CenterOrdinaryTaskEntity task, WaylineJobEntity job, UniPoint point, MediaFileEntity file, String ftpPath) {
        PatrolResultItem item = new PatrolResultItem();
        item.setPatroldevice_name("无人机"); item.setPatroldevice_code(file.getDrone()==null ? "" : file.getDrone());
        item.setTask_code(task.getTaskCode()); item.setTask_name(task.getTaskName()); item.setTask_patrolled_id(job.getJobId());
        item.setDevice_id(point.getPointCode()); item.setDevice_name(point.getPointName());
        item.setFile_type(Integer.valueOf(1).equals(point.getPicType()) ? "1" : "2"); item.setFile_path(ftpPath);
        item.setValue(""); item.setUnit(""); item.setValue_unit(""); item.setDefect_description("");
        item.setTime(DateUtils.getNowDateTimeStr()); item.setRecognition_type(""); item.setRectangle("");
        item.setObj_id(""); item.setValid("1");
        return item;
    }
}
