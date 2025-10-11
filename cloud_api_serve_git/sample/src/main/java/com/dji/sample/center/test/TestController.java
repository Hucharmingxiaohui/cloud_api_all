package com.dji.sample.center.test;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dji.sample.df.electricInspectionDf.dao.PubWaylinePointDfMapper;
import com.dji.sample.df.electricInspectionDf.model.PubWaylinePointDfEntity;
import com.dji.sample.df.mediaDf.dao.IFileMapperDf;
import com.dji.sample.df.mediaDf.model.MediaFileDTO;
import com.dji.sample.df.mediaDf.model.MediaFileEntity;
import com.dji.sample.df.mediaDf.service.IFileServiceDf;
import com.dji.sample.df.thirdKmzDf.entity.pointResult.PointCount.PointCount;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author sean.zhou
 * @version 0.1
 * @date 2021/11/15
 */
@RestController
@Slf4j
@RequestMapping("test")
public class TestController {

//    @Autowired
//    private CenterMsgPushHandler centerMsgPushHandler;
    @Autowired
    private PubWaylinePointDfMapper pubWaylinePointDfMapper;
    @Autowired
    private IFileMapperDf iFileMapperDf;
    @Autowired
    private IFileServiceDf iFileServiceDf;

    @PostMapping("/{workspace_id}/devices/ota")
    public String createOtaJob() {
        try {
            File fileInputStream = new File("http://172.20.63.157:9000/djicloud/wayline/org_67c214505ee8b4ea_1732757894000.jpg?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=minioadmin%2F20241129%2Fus-east-1%2Fs3%2Faws4_request&X-Amz-Date=20241129T011301Z&X-Amz-Expires=3600&X-Amz-SignedHeaders=host&X-Amz-Signature=579a870cdf98de344818f35db96867c65284297d24db3989a785498905021198");
            return "读取成功";
        } catch (Exception e) {
            e.printStackTrace();
            return "读取失败";
        }
    }

    public void pushPoint() {
        PubWaylinePointDfEntity pubWaylinePointDfEntity = pubWaylinePointDfMapper.selectOne(
                new LambdaQueryWrapper<PubWaylinePointDfEntity>()
                        .eq(PubWaylinePointDfEntity::getPointCode, "5bbfb273-b33a-4ce7-b3fa-54a51f82c0b8")
        );
        MediaFileEntity mediaFileEntity = iFileMapperDf.selectOne(
                new LambdaQueryWrapper<MediaFileEntity>()
                        .eq(MediaFileEntity::getFileId, "48b5910a-80fe-4dab-a8aa-52c6bbd8758e")
        );
        List<PointCount> pointCounts = new ArrayList<>();
        List<MediaFileDTO> mediaFileDTOS = new ArrayList<>();

        MediaFileDTO dto = new MediaFileDTO();
        dto.setFilePath(iFileServiceDf.getObjectUrl(mediaFileEntity.getWorkspaceId(), mediaFileEntity.getFileId()).toString());
        mediaFileDTOS.add(dto);


        PointCount pointCount = new PointCount();
        pointCount.setCount(1);
        pointCount.setPubWaylinePointDfEntity(pubWaylinePointDfEntity);
        pointCount.setMediaFileDTOS(mediaFileDTOS);

        pointCounts.add(pointCount);

//        centerMsgPushHandler.pushPatrolResult(pointCounts, "3f68ca88-4dbc-4f31-bc8a-cf65c6445efd", "faf3362c-3c90-2fce-0f88-b059716cb160",
//                );
    }

}
