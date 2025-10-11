package com.dji.sample.df.electricInspectionDf.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dji.sample.component.oss.model.OssConfiguration;
import com.dji.sample.component.oss.service.impl.OssServiceContext;
import com.dji.sample.df.electricInspectionDf.dao.PubModelDfMapper;
import com.dji.sample.df.electricInspectionDf.model.PubModelDfEntity;
import com.dji.sample.df.electricInspectionDf.service.PubModelDfService;
import com.dji.sdk.common.Pagination;
import com.dji.sdk.common.PaginationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class PubModelDfServiceImpl implements PubModelDfService{
    @Autowired
    private OssServiceContext ossService;

    @Autowired
    private PubModelDfMapper mapper;

   // 导入模型到minIO
    @Override
    public boolean importModelFile(MultipartFile file, String workspaceId,String filePath, String creator) {
//        ossService.putObject(OssConfiguration.bucket, waylineFile.getObjectKey(), file.getInputStream());
        String fileName = file.getOriginalFilename();
        if (fileName != null) {
            String objectKey = filePath;
            try {
                // 上传到 MinIO
                ossService.putObject(OssConfiguration.modelsBucket, objectKey, file.getInputStream());
                return true;
            } catch (Exception e) {
                return false;
            }
        } else {
            return false;
        }
    }

    // 插入模型信息
    public Integer saveModelFile(String workspaceId, String filePath, String subCode, String subName, String jsonName) {
        // 获取当前时间戳
        Long currentTime = System.currentTimeMillis();
        String extractedFileName = Paths.get(filePath).getName(0).toString(); // 获取路径的第一部分作为文件夹名

        // 创建 PubModelDfEntity 实体并设置属性
        PubModelDfEntity fileEntity = PubModelDfEntity.builder()
                .modelId(UUID.randomUUID().toString())         // 设置 modelId，使用 UUID 生成唯一标识符
                .modelName(extractedFileName)                           // 设置 modelName 为文件名称
                .workspaceId(workspaceId)                      // 设置 workspaceId
                .objectKey(extractedFileName)                           // 设置 objectKey 为文件路径
                .subCode(subCode)                              // 设置 subCode
                .subName(subName)                              // 设置 subName
                .jsonName(jsonName)                              // 设置 subName
                .updateTime(currentTime)                       // 设置 updateTime 为当前时间戳
                .build();

        // 调用 mapper 插入数据
        return mapper.insert(fileEntity); // 返回插入的行数
    }

    // 按名称查询数据库
    public List<PubModelDfEntity> queryModelByName(String fileName) {
        // 根据 fileName 查询数据库，查找是否存在对应的多个模型
        List<PubModelDfEntity> modelEntities = mapper.selectList(new LambdaQueryWrapper<PubModelDfEntity>()
                .eq(PubModelDfEntity::getModelName, fileName));

        // 如果查询结果为空，返回空的列表
        if (modelEntities == null || modelEntities.isEmpty()) {
            return new ArrayList<>(); // 返回空的列表而非 null
        }

        // 返回查询到的多个 modelEntity 记录
        return modelEntities;
    }

    // 查询所有的点位信息
    @Override
    public PaginationData<PubModelDfEntity> getAllPubModelDfEntities(long page, long pageSize) {
        Page<PubModelDfEntity> pageData = mapper.selectPage(
                new Page<PubModelDfEntity>(page, pageSize),
                new LambdaQueryWrapper<PubModelDfEntity>()
                        .orderByDesc(PubModelDfEntity::getId));
        List<PubModelDfEntity> records = pageData.getRecords();
        return new PaginationData<PubModelDfEntity>(records, new Pagination(pageData.getCurrent(), pageData.getSize(), pageData.getTotal()));
    }

    // 删除模型信息
    @Override
    public boolean deletePubModelDfEntityById(String workspaceId, Integer id) {
        Optional<PubModelDfEntity> ModelOpt = this.getModelByFileId(workspaceId,id);
        if ( ModelOpt.isEmpty()) {
            return true;
        }
        PubModelDfEntity model = ModelOpt.get();
        boolean isDel = mapper.delete(new LambdaUpdateWrapper<PubModelDfEntity>()
                .eq(PubModelDfEntity::getWorkspaceId, workspaceId)
                .eq(PubModelDfEntity::getId, id))
                > 0;
        if (!isDel) {
            return false;
        }
        // 删除文件夹中的所有文件
        ossService.deleteModelFolder(model.getObjectKey()+"/");

        return true;
    }

    private Optional<PubModelDfEntity> getModelByFileId(String workspaceId, Integer Id) {
        PubModelDfEntity fileEntity = mapper.selectOne(new LambdaQueryWrapper<PubModelDfEntity>()
                .eq(PubModelDfEntity::getWorkspaceId, workspaceId)
                .eq(PubModelDfEntity::getId, Id));
        return Optional.ofNullable(fileEntity);
    }
}