package com.dji.sample.df.centerOrdinary;

import com.alibaba.fastjson.JSON;
import com.dji.sample.df.centerOrdinary.dao.CenterOrdinaryTaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.dji.sample.center.entity.UniPoint;
import com.dji.sample.df.mediaDf.OrdinaryPhotoMatcher;
import com.dji.sample.df.mediaDf.dao.IFileMapperDf;
import com.dji.sample.df.mediaDf.model.MediaFileEntity;
import com.dji.sample.wayline.model.entity.WaylineJobEntity;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class CenterOrdinaryMediaService {
    private final CenterOrdinaryTaskMapper tasks;
    private final IFileMapperDf files;
    private final CenterOrdinaryMediaTransport transport;

    public CenterOrdinaryMediaService(CenterOrdinaryTaskMapper tasks, IFileMapperDf files, CenterOrdinaryMediaTransport transport) {
        this.tasks=tasks; this.files=files; this.transport=transport;
    }

    public void complete(CenterOrdinaryTaskEntity task, WaylineJobEntity job) throws Exception {
        if (!Objects.equals(task.getJobId(), job.getJobId()) || !Objects.equals(task.getRouteId(), job.getFileId()))
            throw new IllegalStateException("上级普通任务jobId或航线不一致，拒绝处理媒体");
        if (tasks.finish(task.getId(), "DISPATCHED", "REPORTING", "媒体已稳定，开始上报选中点位")!=1) return;
        try {
            List<Map<String,Object>> results = new ArrayList<>();
            List<MediaFileEntity> selected = new ArrayList<>();
            List<UniPoint> points = new ArrayList<>();
            List<UniPoint> routePoints=CenterOrdinarySelection.routePoints(task.getPointSnapshot(),task.getSubCode(),task.getRouteId());
            List<String> requested = JSON.parseArray(task.getPointCodes(), String.class);
            List<String> frozenCodes=JSON.parseObject(task.getPointSnapshot()).getJSONArray("selectedCodes").toJavaList(String.class);
            if (requested==null || !new HashSet<>(requested).equals(new HashSet<>(frozenCodes)))
                throw new IllegalStateException("选中编码与冻结快照不一致");
            List<MediaFileEntity> routePhotos=files.selectList(new QueryWrapper<MediaFileEntity>().eq("job_id", job.getJobId())
                    .eq("workspace_id",job.getWorkspaceId())).stream()
                    .filter(f -> Objects.equals(job.getJobId(),f.getJobId()) && Objects.equals(job.getWorkspaceId(),f.getWorkspaceId()))
                    .collect(java.util.stream.Collectors.toList());
            Map<MediaFileEntity,UniPoint> mapping=OrdinaryPhotoMatcher.match(task.getRouteId(),routePoints,routePhotos);
            for (Map.Entry<MediaFileEntity,UniPoint> entry : mapping.entrySet()) {
                MediaFileEntity file=entry.getKey(); UniPoint point=entry.getValue();
                if (requested==null || !requested.contains(point.getPointCode())) continue;
                // 先过滤再生成业务结果；未选照片不落入结果快照，也不会下载、分析或上传。
                Map<String,Object> result = new LinkedHashMap<>();
                result.put("point", point); result.put("media", file); result.put("analysis", "NOT_REQUESTED");
                results.add(result); selected.add(file); points.add(point);
            }
            if (tasks.saveResults(task.getId(), JSON.toJSONString(results))!=1) throw new IllegalStateException("选中点位结果快照保存失败");
            Set<String> reportedCodes = new HashSet<>();
            for (int i=0; i<selected.size(); i++) {
                MediaFileEntity file = selected.get(i); UniPoint point = points.get(i);
                String localPath = transport.store(task, job, point, file);
                results.get(i).put("localPath", localPath);
                if (tasks.saveResults(task.getId(), JSON.toJSONString(results))!=1) throw new IllegalStateException("图片结果保存失败");
                String ftpPath = transport.report(task, job, point, file, localPath);
                results.get(i).put("ftpPath", ftpPath); results.get(i).put("reported", true);
                if (tasks.saveResults(task.getId(), JSON.toJSONString(results))!=1) throw new IllegalStateException("上报结果保存未确认");
                reportedCodes.add(point.getPointCode());
            }
            if (requested != null && !reportedCodes.containsAll(requested)) {
                tasks.finish(task.getId(), "REPORTING", "INCOMPLETE", "部分选中点位没有匹配媒体，未生成虚假结果");
            } else {
                tasks.finish(task.getId(), "REPORTING", "COMPLETED", "普通采集完成，未执行AI分析");
            }
        } catch (Exception e) {
            // FTP/TCP发送可能已经成功，不能自动重复上报或重新飞行。
            tasks.finish(task.getId(), "REPORTING", "REPORT_UNKNOWN", "媒体处理或上报未确认，需人工核验，禁止自动重发");
            throw e;
        }
    }
}
