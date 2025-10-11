package com.dji.sample.df.electricInspectionDf.service;

import com.dji.sample.df.electricInspectionDf.model.PubModelDfEntity;
import com.dji.sdk.common.PaginationData;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PubModelDfService {

    // 导入模型
    boolean importModelFile(MultipartFile file, String workspaceId, String filePath, String creator);
    // 模型信息保存数据库
    Integer saveModelFile(String workspaceId, String filePath, String sub_code, String subName, String jsonName);

    //按名称查询数据
    List<PubModelDfEntity> queryModelByName(String fileName);

    // 查询所有的模型信息
    PaginationData<PubModelDfEntity> getAllPubModelDfEntities(long page, long pageSize);

    // 删除模型信息
    boolean deletePubModelDfEntityById(String workspaceId , Integer id);

}
