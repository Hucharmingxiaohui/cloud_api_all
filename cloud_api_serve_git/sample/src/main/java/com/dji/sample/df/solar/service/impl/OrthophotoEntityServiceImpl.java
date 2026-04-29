package com.dji.sample.df.solar.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dji.sample.df.commonDf.util.PageUtil;
import com.dji.sample.df.solar.dao.OrthophotoEntityMapper;
import com.dji.sample.df.solar.model.entity.OrthophotoEntity;
import com.dji.sample.df.solar.service.OrthophotoEntityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class OrthophotoEntityServiceImpl extends ServiceImpl<OrthophotoEntityMapper, OrthophotoEntity> implements OrthophotoEntityService {
    @Value("${orthophoto.upload-dir}")
    private String uploadDir;

    @Resource
    OrthophotoEntityMapper orthophotoMapper;

    /**
     * 导入正射图
     * @param file 上传的图片文件
     * @param name 正射图名称
     * @return 保存后的实体
     */
    @Transactional(rollbackFor = Exception.class)
    public OrthophotoEntity importOrthophoto(MultipartFile file, String name) {
        // 1. 文件非空校验
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }
        // 2. 文件格式校验
        String originalFilename = file.getOriginalFilename();
        if (!isImageFile(originalFilename)) {
            throw new RuntimeException("仅支持 jpg、png、jpeg 格式的图片");
        }
        // 3. 提取扩展名，拼接最终文件名
        String extension = "";
        if (originalFilename != null && originalFilename.lastIndexOf('.') > 0) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        String newFileName = name + extension;
        // 4. 文件名并保存到服务器
        Path targetPath = Paths.get(uploadDir, newFileName);

        try {
            // 创建目录（如果不存在）
            Files.createDirectories(targetPath.getParent());
            // 保存文件
            file.transferTo(targetPath.toFile());
            log.info("文件保存成功：{}", targetPath.toString());
        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw new RuntimeException("文件保存失败：" + e.getMessage());
        }
        // 5. 保存记录到数据库
        String accessPath = generateAccessPath(newFileName);
        OrthophotoEntity entity = OrthophotoEntity.builder()
                .name(name)                 // 注意实体类属性名首字母大写
                .path(accessPath)
                .build();
        orthophotoMapper.insert(entity);
        log.info("正射图记录保存成功，ID：{}", entity.getId());
        return entity;

    }

    private String generateAccessPath(String fileName) {
        return Paths.get(uploadDir, fileName).toString();
    }

    private boolean isImageFile(String filename) {
        if (filename == null || filename.isEmpty()) {
            return false;
        }
        String ext = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        return ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png");
    }

    /**
     * 删除正射图（同时删除文件和数据库记录）
     * @param id 正射图ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrthophoto(Long id) {
        // 1. 查询数据库记录
        OrthophotoEntity entity = orthophotoMapper.selectById(id);
        if (entity == null) {
            throw new RuntimeException("正射图记录不存在，ID: " + id);
        }

        // 2. 获取文件路径
        String filePath = entity.getPath();
        if (filePath != null && !filePath.isEmpty()) {
            // 将存储的路径转换为实际文件系统路径
            Path physicalPath = Paths.get(filePath);
            try {
                Files.deleteIfExists(physicalPath);
                log.info("文件删除成功：{}", physicalPath.toString());
            } catch (IOException e) {
                log.error("文件删除失败", e);
            }
        }
        // 3. 删除数据库记录
        int rows = orthophotoMapper.deleteById(id);
        if (rows == 0) {
            throw new RuntimeException("删除数据库记录失败，ID: " + id);
        }
        log.info("正射图记录删除成功，ID: {}", id);
    }

    @Override
    public OrthophotoEntity selectById(Long id) {
        OrthophotoEntity orthophotoEntity = orthophotoMapper.selectById(id);
        return orthophotoEntity;
    }

    @Override
    public Map<String, Object> selectList(Map<String, Object> params) {
        // 使用原风格的分页工具类 PageUtil（需存在）
        PageUtil.setPageArgs(params);
        List<OrthophotoEntity> list = orthophotoMapper.selectList(params);
        int count = orthophotoMapper.selectListCount(params);

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", Integer.parseInt(params.get("page").toString()));
        pagination.put("pageSize", Integer.parseInt(params.get("pageSize").toString()));
        pagination.put("total", count);
        result.put("list", list);
        result.put("pagination", pagination);
        return result;
    }


}
