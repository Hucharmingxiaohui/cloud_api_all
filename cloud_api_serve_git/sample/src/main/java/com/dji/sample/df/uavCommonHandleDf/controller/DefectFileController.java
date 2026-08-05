package com.dji.sample.df.uavCommonHandleDf.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.df.framework.vo.Result;
import com.dji.sample.df.uavCommonHandleDf.dao.DefectEntityMapper;
import com.dji.sample.df.uavCommonHandleDf.model.entity.DefectEntity;
import com.dji.sample.df.uavCommonHandleDf.model.entity.DefectType;
import com.dji.sample.df.uavCommonHandleDf.service.impl.uavReportServiceImpl;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/file")
public class DefectFileController {

    @Autowired
    uavReportServiceImpl uavReportServiceImpl;

    @Autowired
    DefectEntityMapper defectEntityMapper;
//  获取缺陷图片http路径
    @GetMapping("/defect")
    public void getDefectImage(@RequestParam String path,
                               HttpServletResponse response) throws IOException {
        try {
            File file = new File(path);
            if (!file.exists()) {
                response.setStatus(404);
                return;
            }

            String contentType = Files.probeContentType(file.toPath());
            if (contentType == null) {
                contentType = "image/jpeg"; // 默认类型
            }

            response.setContentType(contentType);
            Files.copy(file.toPath(), response.getOutputStream());

        } catch (Exception e) {
            response.setStatus(500);
        }
    }
    //  人工修正缺陷
    @PostMapping("/updateDefect")
    public Result updateDefect(@RequestBody JSONObject jsonObject) {
        // 设置缺陷类型和描述
        Integer id = jsonObject.getInteger("id");
        JSONArray defects = jsonObject.getJSONArray("defects");
        List<String> defectList = defects.toJavaList(String.class);
        DefectEntity defectEntity = defectEntityMapper.selectById(id);
        Map<String, Integer> defectCount = uavReportServiceImpl.countDefectTypes(defectList);
        String mainDefectType = uavReportServiceImpl.getMainDefectType(defectCount);
        defectEntity.setDefectType(mainDefectType);
        defectEntity.setDefectDescription(uavReportServiceImpl.generateDefectDescription(defectCount));
        defectEntityMapper.updateById(defectEntity);
        return Result.success("人工修正缺陷成功");
    }

    //  查询缺陷枚举类
    @GetMapping("/queryDefectType")
    public List<DefectTypeDTO> queryDefectType(){
        return Arrays.stream(DefectType.values())
                .map(DefectTypeDTO::fromDefectType)
                .collect(Collectors.toList());

    }

    /**
     * 缺陷类型DTO
     */
    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DefectTypeDTO {
        private String code;
        private String name;
        private String description;

        public static DefectTypeDTO fromDefectType(DefectType defectType) {
            DefectTypeDTO dto = new DefectTypeDTO();
            dto.setCode(defectType.name());
            dto.setName(defectType.getDescription());
            dto.setDescription(defectType.getDescription());
            return dto;
        }
    }
}
