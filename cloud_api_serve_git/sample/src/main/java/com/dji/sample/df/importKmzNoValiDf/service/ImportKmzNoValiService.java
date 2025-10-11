package com.dji.sample.df.importKmzNoValiDf.service;

import com.dji.sample.wayline.model.dto.WaylineFileDTO;
import com.dji.sample.wayline.model.entity.WaylineFileEntity;
import org.springframework.web.multipart.MultipartFile;

public interface ImportKmzNoValiService {
    //存储数据库
    Integer saveWaylineFile(String workspaceId, WaylineFileDTO metadata,Integer waylinePos);
    //导入
    void importKmzFile(MultipartFile file, String workspaceId, String creator,Integer waylinePos);
    //按名称查询
    WaylineFileEntity getWaylineByFileName(String fileName);
}
