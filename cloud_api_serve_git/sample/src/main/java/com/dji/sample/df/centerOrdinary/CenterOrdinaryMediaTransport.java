package com.dji.sample.df.centerOrdinary;

import com.dji.sample.center.entity.UniPoint;
import com.dji.sample.center.utils.DateUtils;
import com.dji.sample.center.utils.ftp.FtpUtils;
import com.dji.sample.center.v2022.command.base.PatrolHostCommand;
import com.dji.sample.center.v2022.command.upload.PatrolResultItem;
import com.dji.sample.center.v2022.handler.PatrolHostSocketClient;
import com.dji.sample.df.mediaDf.model.MediaFileEntity;
import com.dji.sample.df.windDf.config.FjFileConfig;
import com.dji.sample.media.service.IFileService;
import com.dji.sample.wayline.model.entity.WaylineJobEntity;
import org.springframework.stereotype.Component;
import com.dji.sample.df.substationDf.dao.RecgPointsEntityMapper;
import com.dji.sample.df.substationDf.model.entity.RecgPointsEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CenterOrdinaryMediaTransport {
    private final IFileService files;
    private final FjFileConfig config;
    private final PatrolHostSocketClient socket;
    @org.springframework.beans.factory.annotation.Autowired
    private RecgPointsEntityMapper results;

    public CenterOrdinaryMediaTransport(IFileService files, FjFileConfig config, PatrolHostSocketClient socket) {
        this.files=files; this.config=config; this.socket=socket;
    }

    public String report(CenterOrdinaryTaskEntity task, WaylineJobEntity job, UniPoint point, MediaFileEntity file, String localPath) {
        String directory = "/center-ordinary/" + task.getId() + "/" + job.getJobId();
        String destination = file.getFileId()+".jpg";
        if (!Boolean.TRUE.equals(FtpUtils.getInstance().uploadToCenterNormal(localPath, directory, destination)))
            throw new IllegalStateException("上级普通任务选中照片FTP上传失败");
        String ftpPath = directory+"/"+destination;
        PatrolResultItem item = resultItem(task, job, point, file, ftpPath);
        RecgPointsEntity result = results.selectOne(new QueryWrapper<RecgPointsEntity>()
                .eq("task_patrolled_id", job.getJobId()).eq("point_code", point.getPointCode()).eq("pic_type", point.getPicType()));
        if (result != null) {
            item.setValue(result.getPointVal() == null ? "" : result.getPointVal());
            item.setValue_unit(Integer.valueOf(1).equals(point.getPicType()) ? item.getValue()
                    : result.getPointValUnit() == null ? "" : result.getPointValUnit());
        }
        log.info("上级普通任务上报已保存结果图：任务={}，点位={}，出站名称={}，类型={}",
                job.getJobId(), point.getPointCode(), item.getDevice_name(), point.getPicType());
        PatrolHostCommand command = socket.getBaseCommand("61", "", task.getSubCode());
        command.addItem(item);
        if (!socket.sendCommand(command, PatrolResultItem.class)) throw new IllegalStateException("上级普通任务点位结果发送未确认");
        return ftpPath;
    }

    static PatrolResultItem resultItem(CenterOrdinaryTaskEntity task, WaylineJobEntity job, UniPoint point, MediaFileEntity file, String ftpPath) {
        PatrolResultItem item = new PatrolResultItem();
        item.setPatroldevice_name("无人机"); item.setPatroldevice_code(file.getDrone()==null ? "" : file.getDrone());
        item.setTask_code(task.getTaskCode()); item.setTask_name(task.getTaskName()); item.setTask_patrolled_id(job.getJobId());
        item.setDevice_id(point.getPointCode()); item.setDevice_name(com.dji.sample.center.utils.StationDeviceName.format(point.getPointName(), point.getPicType()));
        item.setFile_type(Integer.valueOf(1).equals(point.getPicType()) ? "1" : "2"); item.setFile_path(ftpPath);
        item.setValue(""); item.setUnit(""); item.setValue_unit(""); item.setDefect_description("");
        item.setTime(DateUtils.getNowDateTimeStr()); item.setRecognition_type(""); item.setRectangle("");
        item.setObj_id(""); item.setValid("1");
        return item;
    }
}
