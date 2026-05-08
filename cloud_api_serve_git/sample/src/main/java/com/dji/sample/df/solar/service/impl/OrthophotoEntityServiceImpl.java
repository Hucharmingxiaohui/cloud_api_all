package com.dji.sample.df.solar.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dji.sample.df.commonDf.util.PageUtil;
import com.dji.sample.df.solar.dao.OrthophotoEntityMapper;
import com.dji.sample.df.solar.dao.SolarPanelAreaMapper;
import com.dji.sample.df.solar.dao.SolarPanelComponentMapper;
import com.dji.sample.df.solar.dao.SolarPanelMapper;
import com.dji.sample.df.solar.model.entity.OrthophotoEntity;
import com.dji.sample.df.solar.model.entity.SolarPanel;
import com.dji.sample.df.solar.model.entity.SolarPanelArea;
import com.dji.sample.df.solar.model.entity.SolarPanelComponent;
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
import java.util.UUID;

@Service
@Slf4j
public class OrthophotoEntityServiceImpl extends ServiceImpl<OrthophotoEntityMapper, OrthophotoEntity> implements OrthophotoEntityService {
    @Value("${orthophoto.upload-dir}")
    private String uploadDir;

    @Resource
    OrthophotoEntityMapper orthophotoMapper;

    @Resource
    private SolarPanelMapper solarPanelMapper;
    @Resource
    private SolarPanelComponentMapper solarPanelComponentMapper;
    @Resource
    private SolarPanelAreaMapper solarPanelAreaMapper;

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
        String id = UUID.randomUUID().toString();
        String accessPath = generateAccessPath(newFileName);
        OrthophotoEntity entity = OrthophotoEntity.builder()
                .id(id)
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
     * 数据库记录级联删除，包括正射图下的巡视区域、光伏板、光伏组件
     * @param id 正射图ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrthophoto(String id) {
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
        solarPanelComponentMapper.delete(new LambdaQueryWrapper<SolarPanelComponent>().eq(SolarPanelComponent::getOrthophotoId,id));
        solarPanelMapper.delete(new LambdaQueryWrapper<SolarPanel>().eq(SolarPanel::getOrthophotoId,id));
        solarPanelAreaMapper.delete(new LambdaQueryWrapper<SolarPanelArea>().eq(SolarPanelArea::getOrthophotoId,id));
        int rows = orthophotoMapper.deleteById(id);
        if (rows == 0) {
            throw new RuntimeException("删除数据库记录失败，ID: " + id);
        }
        log.info("正射图记录删除成功，ID: {}", id);
    }

    @Override
    public OrthophotoEntity selectById(String id) {
        OrthophotoEntity orthophotoEntity = orthophotoMapper.selectById(id);
        return orthophotoEntity;
    }

    @Override
    public Map<String, Object> selectList(Map<String, Object> params) {
        // 使用原风格的分页工具类 PageUtil（需存在）
        PageUtil.setPageArgs(params);
        List<OrthophotoEntity> list = orthophotoMapper.selectListByMap(params);
        int count = orthophotoMapper.selectListCount(params);
        for(OrthophotoEntity orthophotoEntity : list) {
            JSONArray objects = selectComponentsById(orthophotoEntity.getId());
            Map<String, Object> map = new HashMap<>();
            map.put("orthophotoId", orthophotoEntity.getId());
            int solarPanelTotal = solarPanelMapper.selectListCount(map);
            orthophotoEntity.setSolarPanelTotal(solarPanelTotal);
            int componentTotal = solarPanelComponentMapper.selectCountByOrthophotoId(orthophotoEntity.getId());
            orthophotoEntity.setComponentTotal(componentTotal);
            orthophotoEntity.setComponentList(objects);
        }
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("page", Integer.parseInt(params.get("page").toString()));
        pagination.put("pageSize", Integer.parseInt(params.get("pageSize").toString()));
        pagination.put("total", count);
        result.put("list", list);
        result.put("pagination", pagination);
        return result;
    }

    @Override
    public JSONArray selectComponentsById(String id) {
        JSONArray jsonArray = new JSONArray();
        List<SolarPanel> solarPanels = solarPanelMapper.selectList(
                new LambdaQueryWrapper<SolarPanel>().eq(SolarPanel::getOrthophotoId, id)
        );

        for (SolarPanel solarPanel : solarPanels) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("solarPanelId", solarPanel.getId());
            jsonObject.put("solarPanelName", solarPanel.getSolarPanelName());

            List<SolarPanelComponent> solarPanelComponents = solarPanelComponentMapper.selectList(
                    new LambdaQueryWrapper<SolarPanelComponent>()
                            .eq(SolarPanelComponent::getSolarPanelId, solarPanel.getId())
            );
            JSONArray childrenArray = new JSONArray();
            for (SolarPanelComponent component : solarPanelComponents) {
                // 关键：每次循环创建一个新的 JSONObject
                JSONObject child = new JSONObject();
                child.put("componentId", component.getId());
                child.put("componentName", component.getSolarPanelComponentName());
                childrenArray.add(child);
            }
            jsonObject.put("children", childrenArray);
            jsonArray.add(jsonObject);
        }
        return  jsonArray;
    }

}
