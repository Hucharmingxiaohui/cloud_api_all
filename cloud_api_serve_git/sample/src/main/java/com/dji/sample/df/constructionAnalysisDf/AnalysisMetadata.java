package com.dji.sample.df.constructionAnalysisDf;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dji.sample.df.constructionDf.ConstructionOverallService;
import com.dji.sample.df.mediaDf.dao.IFileMapperDf;
import com.dji.sample.df.mediaDf.model.MediaFileEntity;
import com.dji.sample.wayline.dao.IWaylineJobMapper;
import com.dji.sample.wayline.model.entity.WaylineJobEntity;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import java.util.*;

@FunctionalInterface
public interface AnalysisMetadata {
    Map<String,Map<String,Object>> resolve(String workspace,String task);
}

@Component
class NativeAnalysisMetadata implements AnalysisMetadata {
    private final IWaylineJobMapper jobs;
    private final IFileMapperDf media;
    private final ConstructionOverallService snapshots;
    NativeAnalysisMetadata(IWaylineJobMapper jobs,IFileMapperDf media,ConstructionOverallService snapshots) {
        this.jobs=jobs;this.media=media;this.snapshots=snapshots;
    }
    @Override public Map<String,Map<String,Object>> resolve(String workspace,String task) {
        // Query the native namespace before allowing the external folder fallback.
        List<WaylineJobEntity> found=jobs.selectList(new LambdaQueryWrapper<WaylineJobEntity>().eq(WaylineJobEntity::getJobId,task));
        if(found.isEmpty())return Map.of();
        if(found.stream().noneMatch(j->workspace.equals(j.getWorkspaceId())))
            throw AnalysisStore.error(HttpStatus.NOT_FOUND,"Native task does not belong to current workspace");
        Map<String,Object> snapshot=snapshots.jobSnapshot(workspace,task);
        Map<String,Map<String,Object>> out=new HashMap<>();
        for(MediaFileEntity file:media.selectList(new LambdaQueryWrapper<MediaFileEntity>()
                .eq(MediaFileEntity::getWorkspaceId,workspace).eq(MediaFileEntity::getJobId,task))) {
            Map<String,Object> match=ConstructionOverallService.matchPhoto(snapshot,file.getFileName());
            Map<String,Object> value=new LinkedHashMap<>();
            value.put("areaId",match.get("area_id"));
            // sub_file_type is not a reliable lens enum across drone families.
            value.put("type","unknown");value.put("checks",AnalysisService.CHECKS);
            value.put("metadataSource","snapshot-incomplete");value.put("ready",false);
            value.put("message","Snapshot does not confirm lens/check inheritance; confirm metadata manually");
            out.put(file.getFileId(),value);
        }
        return out;
    }
}
