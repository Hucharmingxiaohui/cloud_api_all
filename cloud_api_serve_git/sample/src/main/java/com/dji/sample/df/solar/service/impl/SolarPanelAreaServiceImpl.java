package com.dji.sample.df.solar.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.df.framework.vo.Result;
import com.dji.sample.df.commonDf.util.PageUtil;
import com.dji.sample.df.solar.dao.SolarPanelAreaMapper;
import com.dji.sample.df.solar.model.dto.SolarDetectRequestCutDTO;
import com.dji.sample.df.solar.model.dto.SolarDetectRequestDTO;
import com.dji.sample.df.solar.model.dto.SolarDetectResponseDTO;
import com.dji.sample.df.solar.model.entity.SolarPanelArea;
import com.dji.sample.df.solar.service.SolarPanelAreaService;
import com.dji.sample.df.wind.config.WaylineUrlConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SolarPanelAreaServiceImpl extends ServiceImpl<SolarPanelAreaMapper, SolarPanelArea> implements SolarPanelAreaService {

    @Resource
    private SolarPanelAreaMapper solarPanelAreaMapper;

    @Resource
    WaylineUrlConfig WaylineUrlConfig;

    private final RestTemplate restTemplate;

    public SolarPanelAreaServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public boolean saveSolarPanelArea(SolarPanelArea solarPanelArea) {
        // 主键自增，无需设置 ID；如有默认值字段可在此初始化
        // solarPanel.setXXX(默认值);
        String id = UUID.randomUUID().toString();
        solarPanelArea.setId(id);
        int insert = solarPanelAreaMapper.insert(solarPanelArea);
        return insert > 0;
    }

    @Override
    public boolean updateSolarPanelAreaById(SolarPanelArea solarPanelArea) {
        int i = solarPanelAreaMapper.updateById(solarPanelArea);
        return i > 0;
    }

    @Override
    public boolean removeSolarPanelAreaById(String id) {
        int i = solarPanelAreaMapper.deleteById(id);
        return i > 0;
    }

    @Override
    public SolarPanelArea getSolarPanelAreaById(Long id) {
        return solarPanelAreaMapper.selectById(id);
    }

    @Override
    public Map<String, Object> selectList(Map<String, Object> params) {
        // 使用原风格的分页工具类 PageUtil（需存在）
        PageUtil.setPageArgs(params);
        List<SolarPanelArea> list = solarPanelAreaMapper.selectListByMap(params);
        int count = solarPanelAreaMapper.selectListCount(params);

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
    public Result detectAreaGenSolar(SolarDetectRequestDTO solarDetectRequestDTO) {

        log.info("接收光伏检测请求, 区域名称: {}", solarDetectRequestDTO.getSolar_area_name());

        // 1. 将完整DTO转换为精简版DTO
        SolarDetectRequestCutDTO cutRequest = convertToCutDto(solarDetectRequestDTO);

        // 2. 验证转换结果
        if (cutRequest.getSolar_area_name() == null || cutRequest.getDetect_areas() == null) {
            return Result.error("转换后的数据无效");
        }

        // 3. 使用精简版DTO调用外部接口
        log.info("调用外部检测接口, URL: {}", WaylineUrlConfig.getDetectAreaGenUrl());
        SolarDetectResponseDTO solarDetectResponseDTO = callDetectApi(cutRequest);
        List<SolarDetectResponseDTO.DetectAreaResponseDTO> detectAreas = solarDetectResponseDTO.getData().getDetect_areas();
        boolean result = true;
        for (int i = 0; i < detectAreas.size(); i++) {
            // 1. 创建一个空的SolarPanel对象
            SolarPanelArea solarPanelArea = new SolarPanelArea();

            // 2. 从请求参数中赋值
            SolarDetectRequestDTO.DetectAreaDTO area = solarDetectRequestDTO.getDetect_areas().get(i);
            solarPanelArea.setOrthophotoId(solarDetectRequestDTO.getOrthophoto_id());
            solarPanelArea.setSolarPanelAreaName(area.getArea_name());
            solarPanelArea.setTiltAngle(area.getTilt_angle());
            solarPanelArea.setAreaHeight(area.getArea_height());
            solarPanelArea.setPanelHeight(area.getPanel_height());

            // 3. 从响应结果中获取地理坐标数据
//          SolarDetectResponseDTO.ResponseData responseData = solarDetectResponseDTO.getData();
//          List<SolarDetectResponseDTO.DetectAreaResponseDTO> detectAreas = responseData.getDetect_areas();
            if (detectAreas != null && !detectAreas.isEmpty()) {
                // 注意：这里需要处理地理坐标的转换逻辑
                // 通常检测区域会有多个，您需要决定如何将地理坐标保存到SolarPanel的四个角点字段中
                SolarDetectResponseDTO.DetectAreaResponseDTO firstArea = detectAreas.get(i);
                List<SolarDetectResponseDTO.GeoCoordinateDTO> cornersGeo = firstArea.getCorners_geo();

                if (cornersGeo != null && cornersGeo.size() >= 4) {
                    // 假设corners_geo列表中的4个点对应矩形的4个角
                    solarPanelArea.setCorner1Lng(cornersGeo.get(0).getLon());  // 第一个点的经度
                    solarPanelArea.setCorner1Lat(cornersGeo.get(0).getLat());  // 第一个点的纬度

                    solarPanelArea.setCorner2Lng(cornersGeo.get(1).getLon());  // 第二个点的经度
                    solarPanelArea.setCorner2Lat(cornersGeo.get(1).getLat());  // 第二个点的纬度

                    solarPanelArea.setCorner3Lng(cornersGeo.get(2).getLon());  // 第三个点的经度
                    solarPanelArea.setCorner3Lat(cornersGeo.get(2).getLat());  // 第三个点的纬度

                    solarPanelArea.setCorner4Lng(cornersGeo.get(3).getLon());  // 第四个点的经度
                    solarPanelArea.setCorner4Lat(cornersGeo.get(3).getLat());  // 第四个点的纬度
//                      保存像素点到数据库
                    solarPanelArea.setCorner1Col(cutRequest.getDetect_areas().get(i).getCorners_pixels().get(0).getCol());
                    solarPanelArea.setCorner1Row(cutRequest.getDetect_areas().get(i).getCorners_pixels().get(0).getRow());
                    solarPanelArea.setCorner2Col(cutRequest.getDetect_areas().get(i).getCorners_pixels().get(1).getCol());
                    solarPanelArea.setCorner2Row(cutRequest.getDetect_areas().get(i).getCorners_pixels().get(1).getRow());
                    solarPanelArea.setCorner3Col(cutRequest.getDetect_areas().get(i).getCorners_pixels().get(2).getCol());
                    solarPanelArea.setCorner3Row(cutRequest.getDetect_areas().get(i).getCorners_pixels().get(2).getRow());
                    solarPanelArea.setCorner4Col(cutRequest.getDetect_areas().get(i).getCorners_pixels().get(3).getCol());
                    solarPanelArea.setCorner4Row(cutRequest.getDetect_areas().get(i).getCorners_pixels().get(3).getRow());
                }

                log.info(solarPanelArea.toString());
                boolean success = saveSolarPanelArea(solarPanelArea);
                result = result && success;
            }
        }
        return result ? Result.success("更新成功") : Result.error("更新失败");
    }


    /**
     * 将完整的SolarDetectRequestDTO转换为精简的SolarDetectRequestCutDTO
     * 直接在Controller中实现转换逻辑
     */
    public SolarDetectRequestCutDTO convertToCutDto(SolarDetectRequestDTO fullDto) {
        SolarDetectRequestCutDTO cutDto = new SolarDetectRequestCutDTO();

        // 1. 复制基本字段
        cutDto.setSolar_area_name(fullDto.getSolar_area_name());

        // 2. 复制检测区域列表
        if (fullDto.getDetect_areas() != null) {
            List<SolarDetectRequestCutDTO.DetectAreaDTO> cutAreas =
                    fullDto.getDetect_areas().stream()
                            .map(fullArea -> {
                                SolarDetectRequestCutDTO.DetectAreaDTO cutArea =
                                        new SolarDetectRequestCutDTO.DetectAreaDTO();

                                // 复制区域名称
                                cutArea.setArea_name(fullArea.getArea_name());

                                // 复制角点像素
                                if (fullArea.getCorners_pixels() != null) {
                                    List<SolarDetectRequestCutDTO.CornerPixelDTO> cutPixels =
                                            fullArea.getCorners_pixels().stream()
                                                    .map(fullPixel -> {
                                                        SolarDetectRequestCutDTO.CornerPixelDTO cutPixel =
                                                                new SolarDetectRequestCutDTO.CornerPixelDTO();
                                                        cutPixel.setRow(fullPixel.getRow());
                                                        cutPixel.setCol(fullPixel.getCol());
                                                        return cutPixel;
                                                    })
                                                    .collect(Collectors.toList());
                                    cutArea.setCorners_pixels(cutPixels);
                                }

                                return cutArea;
                            })
                            .collect(Collectors.toList());
            cutDto.setDetect_areas(cutAreas);
        }

        return cutDto;
    }
    /**
     * 调用检测接口，直接返回DTO
     */
    private SolarDetectResponseDTO callDetectApi(SolarDetectRequestCutDTO cutRequest) {
        try {
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 创建请求实体
            HttpEntity<SolarDetectRequestCutDTO> requestEntity =
                    new HttpEntity<>(cutRequest, headers);

            // 发送POST请求，直接接收DTO
            ResponseEntity<SolarDetectResponseDTO> response = restTemplate.exchange(
                    WaylineUrlConfig.getDetectAreaGenUrl(),
                    HttpMethod.POST,
                    requestEntity,
                    SolarDetectResponseDTO.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new RuntimeException("外部接口调用失败, 状态码: " + response.getStatusCodeValue());
            }

        } catch (Exception e) {
            log.error("调用检测接口失败", e);
            throw new RuntimeException("外部接口调用失败: " + e.getMessage(), e);
        }
    }

}
