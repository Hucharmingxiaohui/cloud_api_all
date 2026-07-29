package com.dji.sample.df.solar.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dji.sample.center.utils.StringUtils;
import com.dji.sample.df.solar.model.entity.GfPositionRequest;
import com.dji.sample.df.solar.model.entity.GfPositionResponse;
import com.dji.sample.df.solar.service.GfReportService;
import com.dji.sample.df.wind.config.WaylineUrlConfig;
import com.dji.sample.df.wind.dao.*;
import com.dji.sample.df.wind.model.entity.AnalysisRequest;
import com.dji.sample.df.wind.model.entity.AnalysisResponse;
import com.dji.sample.df.wind.model.entity.DefectEntity;
import com.dji.sample.wayline.dao.IWaylineJobMapper;
import com.dji.sample.wayline.model.entity.WaylineJobEntity;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


@Service
@Slf4j
public class GfReportServiceImpl implements GfReportService {

    @Autowired
    private WaylineUrlConfig waylineUrlConfig;
    @Autowired
    DefectEntityMapper defectEntityMapper;
    @Autowired
    IWaylineJobMapper waylineJobMapper;

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public AnalysisResponse sendGfAnalysisRequest(AnalysisRequest request) {
        try {
            String requestBody = objectMapper.writeValueAsString(request);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(waylineUrlConfig.getGfAnalysisUrl()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> httpResponse = httpClient.send(httpRequest,
                    HttpResponse.BodyHandlers.ofString());

            if (httpResponse.statusCode() == 200) {
                return objectMapper.readValue(httpResponse.body(), AnalysisResponse.class);
            } else {
                System.err.println("请求失败，状态码: " + httpResponse.statusCode());
                return null;
            }
        } catch (Exception e) {
            System.err.println("发送分析请求时发生错误: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public GfPositionResponse sendGfPositionRequest(GfPositionRequest request) {
        try {
            String requestBody = objectMapper.writeValueAsString(request);
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            HttpRequest httpRequest = HttpRequest.newBuilder()
//                    需改
                    .uri(URI.create(waylineUrlConfig.getGfDefectLocalizationUrl()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> httpResponse = httpClient.send(httpRequest,
                    HttpResponse.BodyHandlers.ofString());

            if (httpResponse.statusCode() == 200) {
                return objectMapper.readValue(httpResponse.body(), GfPositionResponse.class);
            } else {
                System.err.println("请求失败，状态码: " + httpResponse.statusCode());
                return null;
            }
        } catch (Exception e) {
            System.err.println("发送分析请求时发生错误: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取当前时间
     */
    private String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 将API响应数据转换为缺陷对象列表
     * @param response API响应数据
     * @return 缺陷对象列表
     */
    public List<DefectEntity> convertToDefects(AnalysisResponse response) throws JsonProcessingException {
        List<DefectEntity> defects = new ArrayList<>();
        String currentTime = getCurrentTime();

        for (AnalysisResponse.ResultItem item : response.getResultsList()) {
            // 处理所有类型的desc数据
            if (item.isDescList()) {
                // desc是列表类型,代表有缺陷
                List<String> defectTypes = item.getDescAsList();
                if (defectTypes != null) {
                    DefectEntity defect = createDefectFromResult(item, defectTypes, currentTime);
                    defects.add(defect);
                }
            } else if (item.isDescString()) {
                // desc是字符串类型，包括"无缺陷/无结果"
                String descString = item.getDescAsString();
                if (descString != null) {
                    // 将字符串转换为单元素列表
                    List<String> defectTypes = Collections.singletonList(descString);
                    DefectEntity defect = createDefectFromResult(item, defectTypes, currentTime);
                    defects.add(defect);
                }
            }else {
//              适配红外情况
                List<String> defectTypes = Collections.singletonList("未见异常");
                DefectEntity defect = createDefectFromResult(item, defectTypes, currentTime);
                defects.add(defect);
            }
        }

        return defects;
    }

    /**
     * 从结果项创建缺陷对象
     */
    private DefectEntity createDefectFromResult(AnalysisResponse.ResultItem item,
                                                List<String> defectTypes, String currentTime) throws JsonProcessingException {
        String imagePath = item.getResImagePath();
        DefectEntity defect = new DefectEntity();
        defect.setAcquisitionTime(currentTime);
        defect.setImagePath(imagePath);

        // 设置缺陷类型和描述
        Map<String, Integer> defectCount = countDefectTypes(defectTypes);
        String mainDefectType = getMainDefectType(defectCount);
        defect.setOriginalDefectType(defectTypes.toString());
        List<List<Integer>> centerPoints = item.getCenter_points();
        if (centerPoints != null && !centerPoints.isEmpty()) {
            defect.setDefectPosition(centerPoints.toString());
        }
        List<AnalysisResponse.ResultItem.PanelBox> panelBoxes = item.getPanel_boxes();
        if (panelBoxes != null && !panelBoxes.isEmpty()) {
            ObjectMapper objectMapper = new ObjectMapper();
            // 将 list 直接转为 JSON 字符串存入数据库
            String defectPositionJson = objectMapper.writeValueAsString(panelBoxes);
            defect.setDefectPosition(defectPositionJson);
        }
        defect.setDefectType(mainDefectType);
        defect.setDefectDescription(generateDefectDescription(defectCount));

        return defect;
    }

    /**
     * 统计缺陷类型数量
     */
    public Map<String, Integer> countDefectTypes(List<String> defectTypes) {
        Map<String, Integer> countMap = new HashMap<>();
        for (String type : defectTypes) {
            countMap.put(type, countMap.getOrDefault(type, 0) + 1);
        }
        return countMap;
    }

    /**
     * 获取主要缺陷类型
     */
    public String getMainDefectType(Map<String, Integer> defectCount) {
        return defectCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("未知缺陷");
    }

    /**
     * 生成缺陷描述
     */
    public String generateDefectDescription(Map<String, Integer> defectCount) {
        StringBuilder description = new StringBuilder();
        for (Map.Entry<String, Integer> entry : defectCount.entrySet()) {
            if (description.length() > 0) {
                description.append("; ");
            }
            description.append(entry.getKey()).append("(").append(entry.getValue()).append("处)");
        }
        return description.toString();
    }
    /**
     * 处理API响应并新增缺陷数据
     * @param response API响应数据
     */
    public void processAndAddDefects(AnalysisResponse response,String jobId) throws JsonProcessingException {
        if (response == null || response.getResultsList() == null) {
            System.out.println("响应数据为空");
            return;
        }

        List<DefectEntity> defects = convertToDefects(response);
        addDefects(defects,jobId);
    }

    @Override
    public List<GfPositionRequest.Image> producePositionParam(List<DefectEntity> defectEntities){
        List<GfPositionRequest.Image> images = new ArrayList<>();
        for (DefectEntity defectEntity : defectEntities) {
            GfPositionRequest.Image image = new GfPositionRequest.Image();
            image.setImageName(extractOriginalFileName(defectEntity.getImagePath()));
            Integer imageType = defectEntity.getImageType();
            image.setDefectId(defectEntity.getId());
            if (imageType == 0) {
                image.setImageType("visable");
            }else if(imageType == 1){
                image.setImageType("ir");
            }
            Integer isDefect = defectEntity.getIsDefect();
            if(imageType == 1){
//              红外直接传defects参数
                // === 红外缺陷处理 ===
                image.setHasDefect(true);
                String defectPositionJson = defectEntity.getDefectPosition();
                List<GfPositionRequest.Defect> defects = new ArrayList<>();
                if (defectPositionJson != null && !defectPositionJson.isEmpty()) {
                    try {
                        // 1. 尝试直接解析为 JSON 列表（新数据库格式）
                        defects = objectMapper.readValue(
                                defectPositionJson,
                                new TypeReference<List<GfPositionRequest.Defect>>(){}
                        );
                    } catch (Exception e) {
                      e.printStackTrace();
                    }
                }
                image.setDefects(defects);

            }else if(imageType == 0 && isDefect == 1){
//              有缺陷才传defects参数
                image.setHasDefect(true);
                String originalDefectType = defectEntity.getOriginalDefectType();
                String defectPosition = defectEntity.getDefectPosition();
                List<GfPositionRequest.Defect> defects = new ArrayList<>();

                // 1. 解析缺陷类型列表
                String typeContent = originalDefectType.substring(1, originalDefectType.length() - 1); // 去掉首尾方括号
                String[] types = typeContent.split(","); // 按逗号分割
                List<String> typeList = new ArrayList<>();
                for (String t : types) {
                    typeList.add(t.trim()); // 去除首尾空格
                }

                // 2. 解析坐标列表
                String posContent = defectPosition.substring(1, defectPosition.length() - 1); // 去掉首尾方括号
                // 按 "], [" 分割，注意正则表达式需要转义
                String[] coordPairs = posContent.split("\\], \\[");
                List<int[]> coordList = new ArrayList<>();
                for (String pair : coordPairs) {
                    // 去除可能残留的方括号
                    String clean = pair.replace("[", "").replace("]", "");
                    String[] xy = clean.split(",");
                    int col = Integer.parseInt(xy[0].trim()); // 第一个数字作为 col（x坐标）
                    int row = Integer.parseInt(xy[1].trim()); // 第二个数字作为 row（y坐标）
                    coordList.add(new int[]{col, row});
                }

                // 3. 确保两个列表长度一致
                if (typeList.size() != coordList.size()) {
                    // 可根据实际业务处理异常，例如抛异常或只处理最小长度
                    throw new IllegalStateException("缺陷类型数量与坐标数量不匹配");
                }
                // 4. 组装 defect 对象
                for (int i = 0; i < typeList.size(); i++) {
                    GfPositionRequest.Defect defect = new GfPositionRequest.Defect();
                    defect.setDefectType(typeList.get(i));
                    defect.setCol(coordList.get(i)[0]);
                    defect.setRow(coordList.get(i)[1]);
                    defects.add(defect);
                }
                image.setDefects(defects);
            }else {
                image.setHasDefect(false);
            }
            images.add(image);
        }
        return images;
    }


    public static String extractOriginalFileName(String imagePath) {
        // 获取最后一个斜杠后的完整文件名
        String fileName = imagePath.substring(imagePath.lastIndexOf('/') + 1);
        // 去掉 "_result数字" 部分（例如 _result0）
        return fileName.replaceAll("_result\\d+", "");
    }


    @Override
    public void processAndUptDefects(GfPositionResponse response, String jobId) {
        if (response == null || response.getData() == null) {
            System.out.println("响应数据为空");
            return;
        }
        List<GfPositionResponse.ResultItem> results = response.getData().getResults();
        for (GfPositionResponse.ResultItem item : results) {
            DefectEntity defectEntity = defectEntityMapper.selectById(item.getDefectId());
            if (defectEntity != null) {
                defectEntity.setSolarPanelName(item.getSolarPanelName());
                if(item.getHasDefect()){
                    List<GfPositionResponse.Defect> defects = item.getDefects();
                    List<String> defectComponentNames =new ArrayList<>();
                    List<String> defectTypes =new ArrayList<>();
                    for (GfPositionResponse.Defect defect : defects) {
                        defectComponentNames.add(defect.getSolarPanelComponentName());
                        defectTypes.add(defect.getDefectType());
                    }
                    if(defectEntity.getImageType()==1){
                        defectEntity.setDefectType(defectTypes.toString());
                        defectEntity.setDefectDescription(defectTypes.toString());
                    }
                    defectEntity.setDefectComponentName(defectComponentNames.toString());
                    defectEntityMapper.updateById(defectEntity);
                }
                defectEntityMapper.updateById(defectEntity);
            }
        }
        log.info("保存缺陷分布图--------");
        WaylineJobEntity waylineJobEntity = waylineJobMapper.selectOne(new LambdaQueryWrapper<WaylineJobEntity>().eq(WaylineJobEntity::getJobId, jobId));
        String annotatedImage = response.getData().getAnnotatedImage();
        String annotatedImageIr = response.getData().getAnnotatedImageIr();
        waylineJobEntity.setAnnotatedImage(annotatedImage);
        waylineJobEntity.setAnnotatedImageIr(annotatedImageIr);
        waylineJobMapper.updateById(waylineJobEntity);
    }

    /**
     * 新增缺陷数据
     */
    public void addDefects(List<DefectEntity> defects,String jobId) {
        if (defects == null || defects.isEmpty()) {
            System.out.println("没有需要新增的缺陷数据");
            return;
        }
        System.out.println("开始新增 " + defects.size() + " 条缺陷数据:");

        for (int i = 0; i < defects.size(); i++) {
            DefectEntity defect = defects.get(i);
            defect.setJobId(jobId);
            String defectType = defect.getDefectType();
            if(StringUtils.isNotEmpty(defectType)){
                if(defectType.equals("未见异常") || defectType.equals("无缺陷/无结果")){
                    defect.setIsDefect(0);
                }else {
                    defect.setIsDefect(1);
                }
            }
            defect.setImageType(extractDefectType(defect.getImagePath()));
//          插入缺陷数据
            defectEntityMapper.insert(defect);
            System.out.println((i + 1) + ". " + defect);
        }
        System.out.println("缺陷数据新增完成");
    }

    public static Integer extractDefectType(String imagePath) {
        if (imagePath == null) return null;
        if (imagePath.contains("_V_") || imagePath.contains("_V")) {
            return 0;
        } else if (imagePath.contains("_T_") || imagePath.contains("_T")) {
            return 1;
        }
        return null; // 或其他默认值
    }

}
