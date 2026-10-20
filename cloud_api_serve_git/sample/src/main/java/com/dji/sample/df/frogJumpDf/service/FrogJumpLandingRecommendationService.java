package com.dji.sample.df.frogJumpDf.service;

import com.dji.sample.df.frogJumpDf.model.dto.FrogJumpLandingRecommendationDTO;
import com.dji.sample.df.frogJumpDf.model.dto.FrogJumpLandingRecommendationDTO.Candidate;
import com.dji.sample.manage.model.dto.DeviceDTO;
import com.dji.sample.manage.model.param.DeviceQueryParam;
import com.dji.sample.manage.service.*;
import com.dji.sample.wayline.service.IWaylineFileService;
import com.dji.sdk.cloudapi.device.*;
import com.dji.sdk.common.HttpResultResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.*;

@Service
public class FrogJumpLandingRecommendationService {
    private static final String DISCLAIMER = "仅按航线终点至机场的水平大地测量距离提供建议，不代表设备兼容或航程可达；实际执行仍须通过任务准备检查。机场坐标来自最近缓存的 OSD，观测时间未知，无法确认坐标时效。起点仅为计划指定的起飞机场，未实时核验无人机当前位置。";
    private final IDeviceService devices;
    private final IDeviceRedisService redis;
    private final IWaylineFileService files;

    public FrogJumpLandingRecommendationService(IDeviceService devices, IDeviceRedisService redis, IWaylineFileService files) {
        this.devices = devices; this.redis = redis; this.files = files;
    }

    public boolean isWorkspaceDock(String workspace, String sn) {
        return StringUtils.hasText(workspace) && StringUtils.hasText(sn)
                && devices.getDeviceBySn(sn).filter(d -> permitted(d, workspace)).isPresent();
    }

    private boolean permitted(DeviceDTO dock, String workspace) {
        return workspace.equals(dock.getWorkspaceId()) && Boolean.TRUE.equals(dock.getBoundStatus())
                && dock.getDomain() == DeviceDomainEnum.DOCK && StringUtils.hasText(dock.getDeviceSn());
    }

    public HttpResultResponse<FrogJumpLandingRecommendationDTO> recommend(String workspace, String fileId, String origin) {
        FrogJumpLandingRecommendationDTO result = new FrogJumpLandingRecommendationDTO();
        if (!isWorkspaceDock(workspace, origin)) return failure(result, "起飞机场须绑定到当前登录工作空间。");
        if (!StringUtils.hasText(fileId) || files.getWaylineByWaylineId(workspace, fileId).isEmpty()) {
            return failure(result, "当前登录工作空间中未找到该航线文件。");
        }
        try {
            URLConnection connection = files.getObjectUrl(workspace, fileId).openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            try (InputStream input = connection.getInputStream()) { result.setEndpoint(WpmlLandingEndpointParser.parse(input)); }
        } catch (IllegalArgumentException e) {
            return failure(result, "无法确定执行航线终点：" + e.getMessage() + "。");
        } catch (Exception e) {
            return failure(result, "航线文件存储暂不可用，请重试或修复当前工作空间的航线文件。");
        }
        List<DeviceDTO> docks = devices.getDevicesByParams(DeviceQueryParam.builder().workspaceId(workspace)
                .boundStatus(true).domains(List.of(DeviceDomainEnum.DOCK.getDomain())).build());
        for (DeviceDTO dock : docks) {
            if (!permitted(dock, workspace) || origin.equals(dock.getDeviceSn())) continue;
            Candidate candidate = new Candidate();
            candidate.setDockSn(dock.getDeviceSn());
            candidate.setName(StringUtils.hasText(dock.getNickname()) ? dock.getNickname() : dock.getDeviceName());
            candidate.setOnline(Boolean.TRUE.equals(redis.checkDeviceOnline(dock.getDeviceSn())));
            Optional<OsdDock> osd = redis.getDeviceOsd(dock.getDeviceSn(), OsdDock.class);
            Double lon = osd.map(OsdDock::getLongitude).map(Number::doubleValue).orElse(null);
            Double lat = osd.map(OsdDock::getLatitude).map(Number::doubleValue).orElse(null);
            if (WpmlLandingEndpointParser.validCoordinates(lon, lat)) {
                candidate.setDistanceMeters(distanceMeters(result.getEndpoint().getLongitude(), result.getEndpoint().getLatitude(), lon, lat));
            }
            candidate.setSelectable(candidate.isOnline() && candidate.getDistanceMeters() != null);
            candidate.setReason(!candidate.isOnline() ? "机场离线" : "");
            if (candidate.getDistanceMeters() == null) candidate.setReason(candidate.getReason()
                    + (candidate.getReason().isEmpty() ? "" : "；") + "机场 OSD 坐标缺失或无效");
            result.getCandidates().add(candidate);
        }
        result.getCandidates().sort(Comparator.comparing(Candidate::getDistanceMeters, Comparator.nullsLast(Double::compareTo))
                .thenComparing(Candidate::getDockSn));
        result.getCandidates().stream().filter(Candidate::isSelectable).findFirst().ifPresent(candidate -> {
            candidate.setRecommended(true); result.setRecommendedDockSn(candidate.getDockSn());
        });
        result.setMessage((result.getRecommendedDockSn() == null ? "暂无在线且坐标有效的降落机场可供推荐，请手动选择。" : "已按距离推荐最近的在线且坐标有效的机场。") + DISCLAIMER);
        return HttpResultResponse.success(result).setMessage("查询成功");
    }

    private HttpResultResponse<FrogJumpLandingRecommendationDTO> failure(FrogJumpLandingRecommendationDTO result, String reason) {
        result.setMessage(reason + "请保留手动选择降落机场的方式，并提示此警告。" + DISCLAIMER);
        return new HttpResultResponse<FrogJumpLandingRecommendationDTO>().setCode(HttpResultResponse.CODE_FAILED)
                .setMessage(result.getMessage()).setData(result);
    }

    static double distanceMeters(double lon1, double lat1, double lon2, double lat2) {
        double dLat = Math.toRadians(lat2 - lat1), dLon = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(dLat / 2), 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.pow(Math.sin(dLon / 2), 2);
        return 6371008.8 * 2 * Math.asin(Math.sqrt(Math.max(0, Math.min(1, a))));
    }
}
