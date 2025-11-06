package com.dji.sample.df.wind.timer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dji.sample.component.oss.model.OssConfiguration;
import com.dji.sample.component.oss.service.impl.OssServiceContext;
import com.dji.sample.wayline.dao.IWaylineFileMapper;
import com.dji.sample.wayline.dao.IWaylineJobMapper;
import com.dji.sample.wayline.model.entity.WaylineFileEntity;
import com.dji.sample.wayline.model.entity.WaylineJobEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


@Component
public class DeleteWaylineFileTimer {

    @Autowired
    private OssServiceContext ossService;

    @Autowired
    private IWaylineFileMapper waylineFileMapper;

    // 每天凌晨2点执行
    @Scheduled(cron = "0 0 2 * * ?")
    public void deleteKmzFiles() {
        String projectPath = System.getProperty("user.dir");
        String kmzFolderPath = projectPath + File.separator + "kmz";

        File kmzFolder = new File(kmzFolderPath);

        // 检查文件夹是否存在
        if (!kmzFolder.exists() || !kmzFolder.isDirectory()) {
            System.out.println("KMZ文件夹不存在: " + kmzFolderPath);
            return;
        }

        // 获取所有.kmz文件
        File[] kmzFiles = kmzFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".kmz"));

        if (kmzFiles == null || kmzFiles.length == 0) {
            System.out.println("未找到KMZ文件");
            return;
        }

        List<String> deletedFileNames = new ArrayList<>();

        // 删除文件并记录文件名
        for (File file : kmzFiles) {
            if (file.delete()) {
                String fileName = file.getName();
                deletedFileNames.add(fileName);
                System.out.println("成功删除文件: " + fileName);
                WaylineFileEntity waylineFileEntity = waylineFileMapper.selectOne(new LambdaQueryWrapper<WaylineFileEntity>().
                        eq(WaylineFileEntity::getName, fileName));
                ossService.deleteObject(OssConfiguration.bucket, waylineFileEntity.getObjectKey());

            } else {
                System.out.println("删除文件失败: " + file.getName());
            }
        }

        // 返回删除的文件名列表（可以根据需要记录到日志或数据库）
        System.out.println("已删除的文件列表: " + deletedFileNames);
    }

}
