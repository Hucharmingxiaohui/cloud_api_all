package com.dji.sample.df.wind.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.df.framework.redis.RedisUtils;
import com.dji.sample.common.model.CustomClaim;
import com.dji.sample.control.model.param.FlyToPointParam;
import com.dji.sample.control.model.param.InFlightWaylineDeliverParam;
import com.dji.sample.control.service.IControlService;
import com.dji.sample.control.service.IControlService2;
import com.dji.sample.df.electricInspectionDf.dao.PubWaylineJobPlanDfMapper;
import com.dji.sample.df.electricInspectionDf.model.PubWaylineJobPlanDfEntity;
import com.dji.sample.df.electricInspectionDf.service.PubWaylineJobPlanDfService;
import com.dji.sample.df.importKmzNoValiDf.service.ImportKmzNoValiService;
import com.dji.sample.df.wind.config.WaylineUrlConfig;
import com.dji.sample.df.wind.dao.FanWaylinePointsMapper;
import com.dji.sample.df.wind.dao.PointOfInterestMapper;
import com.dji.sample.df.wind.dao.WindTurbineMapper;
import com.dji.sample.df.wind.model.entity.FanWaylinePoints;
import com.dji.sample.df.wind.model.entity.PointOfInterest;
import com.dji.sample.df.wind.model.entity.WindTurbine;
import com.dji.sample.df.wind.service.RoutePlanService;
import com.dji.sample.df.wind.service.WindTurbineService;
import com.dji.sample.manage.dao.IDeviceMapper;
import com.dji.sample.manage.dao.IWorkspaceMapper;
import com.dji.sample.manage.model.entity.DeviceEntity;
import com.dji.sample.manage.model.entity.WorkspaceEntity;
import com.dji.sample.wayline.model.entity.WaylineFileEntity;
import com.dji.sample.wayline.model.param.CreateJobParam;
import com.dji.sample.wayline.service.IWaylineFileService;
import com.dji.sdk.cloudapi.control.FileParam;
import com.dji.sdk.cloudapi.control.Point;
import com.dji.sdk.cloudapi.device.ExitWaylineWhenRcLostEnum;
import com.dji.sdk.cloudapi.wayline.*;
import com.dji.sdk.common.HttpResultResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.dji.sample.component.AuthInterceptor.TOKEN_CLAIM;
import static com.dji.sample.df.wind.controller.WindTurbineWaylineController.convert;

@Service
public class RoutePlanServiceImpl implements RoutePlanService {

    @Resource
    private WindTurbineService windTurbineService;

    @Autowired
    private IControlService2 controlService2;

    @Resource
    private IDeviceMapper deviceMapper;

    @Autowired
    private IWorkspaceMapper workspaceMapper;

    @Autowired
    private ImportKmzNoValiService importKmzNoValiService;

    @Resource
    private RedisUtils redisUtils;

    @Autowired
    private IWaylineFileService waylineFileService;

    @Resource
    WindTurbineMapper windTurbineMapper;

    @Resource
    PointOfInterestMapper pointOfInterestMapper;

    @Autowired
    private PubWaylineJobPlanDfService pubWaylineJobPlanDfService;

    @Autowired
    private PubWaylineJobPlanDfMapper pubWaylineJobPlanDfMapper;

    @Autowired
    private WaylineUrlConfig waylineUrlConfig;

    @Autowired
    FanWaylinePointsMapper fanWaylinePointsMapper;

    private static final Logger log = LoggerFactory.getLogger(RoutePlanServiceImpl.class);

    // 最大重试次数，防止无限循环
    private static final int MAX_RETRY_COUNT = 10;
    // 重试间隔时间（毫秒）
    private static final long RETRY_INTERVAL_MS = 2000;

    // 写发送逻辑
//    目前不需要指令飞行，如果用指令飞行需要考虑循环依赖问题
//    public void flyToFront(String name, Double yaw) {
////      风机名为获取
//        WindTurbine windTurbine = windTurbineService.selectByName(name);
//        String url = "http://172.20.63.157:5001/single";
//        JSONObject jsonObject = new JSONObject();
//        jsonObject.put("turbine_name", windTurbine.getTurbineName());
//        jsonObject.put("lon0_deg", windTurbine.getAirportLongitude());
//        jsonObject.put("lat0_deg", windTurbine.getAirportLatitude());
//        jsonObject.put("h0", windTurbine.getAirportAltitude());
//        //      转向角为获取
//        jsonObject.put("yaw_ver", yaw);
//        jsonObject.put("lon_in_deg", windTurbine.getPeakLongitude());
//        jsonObject.put("lat_in_deg", windTurbine.getPeakLatitude());
//        jsonObject.put("h_in", windTurbine.getPeakAltitude());
//        jsonObject.put("h_c", windTurbine.getBladeCenterHeight());
//        jsonObject.put("theta_deg", windTurbine.getBladeStopAngle());
//        jsonObject.put("length", windTurbine.getBladeLength());
//        jsonObject.put("dist", windTurbine.getUavBladeDistance());
//        jsonObject.put("h_b", windTurbine.getBladeBottomHeight());
//        jsonObject.put("blade_points", windTurbine.getBladePoints());
//        jsonObject.put("tower_points", windTurbine.getTowerPoints());
//        String jsonInput = jsonObject.toString();
//        try {
//            URL obj = new URL(url);
//            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
//            // 设置请求方法
//            con.setRequestMethod("POST");
//            con.setRequestProperty("Content-Type", "application/json");
//            con.setDoOutput(true);
//            // 发送请求
//            try (OutputStream os = con.getOutputStream()) {
//                byte[] input = jsonInput.getBytes("utf-8");
//                os.write(input, 0, input.length);
//            }
//            // 获取响应
//            int responseCode = con.getResponseCode();
//            System.out.println("Response Code: " + responseCode);
//            try (BufferedReader br = new BufferedReader(
//                    new InputStreamReader(con.getInputStream(), "utf-8"))) {
//                StringBuilder response1 = new StringBuilder();
//                String responseLine;
//                while ((responseLine = br.readLine()) != null) {
//                    response1.append(responseLine.trim());
//                }
//                // 使用FastJSON解析响应
//                JSONObject jsonResponse = JSONObject.parseObject(response1.toString());
//                System.out.println("JSON Response: " + jsonResponse.toJSONString());
//                // 提取point对象
//                JSONObject pointObj = jsonResponse.getJSONObject("point");
//                // 获取具体坐标值
//                float latitude = pointObj.getFloat("lat");
//                float longitude = pointObj.getFloat("lon");
//                float height = pointObj.getFloat("height");
//                FlyToPointParam flyToPointParam = new FlyToPointParam();
//                List<Point> points = new ArrayList<>();
//                Point point = new Point();
//                point.setLongitude(longitude);
//                point.setLatitude(latitude);
//                point.setHeight(height);
//                points.add(point);
//                flyToPointParam.setPoints(points);

    /// /              最大速度是固定的吗
//                flyToPointParam.setMaxSpeed(14);
//                log.info("指令飞行参数为" + flyToPointParam);
//                DeviceEntity deviceEntity = deviceMapper.selectOne(new LambdaQueryWrapper<DeviceEntity>().eq(DeviceEntity::getDomain, 3));
//                log.info("飞行设备为" + deviceEntity.getDeviceSn());
//                controlService.flyToPoint(deviceEntity.getDeviceSn(), flyToPointParam);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    // 飞向中心点空中航线
    public void flyToWayline(String name,Double value) {
//      风机名为获取
        WindTurbine windTurbine = windTurbineService.selectByName(name);
//        String url = "http://172.20.63.157:5001/single";
        String url = waylineUrlConfig.getBuildKmzUrl().getSingleWayline();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("turbine_name", windTurbine.getTurbineName());
        jsonObject.put("lon0_deg", windTurbine.getAirportLongitude());
        jsonObject.put("lat0_deg", windTurbine.getAirportLatitude());
        jsonObject.put("h0", windTurbine.getAirportAltitude());
        //      转向角为获取
        jsonObject.put("yaw_ver", value);
        jsonObject.put("lon_in_deg", windTurbine.getPeakLongitude());
        jsonObject.put("lat_in_deg", windTurbine.getPeakLatitude());
        jsonObject.put("h_in", windTurbine.getPeakAltitude());
        jsonObject.put("h_c", windTurbine.getBladeCenterHeight());
        jsonObject.put("theta_deg", windTurbine.getBladeStopAngle());
        jsonObject.put("length", windTurbine.getBladeLength());
        jsonObject.put("dist", windTurbine.getUavBladeDistance());
        jsonObject.put("h_b", windTurbine.getBladeBottomHeight());
        jsonObject.put("blade_points", windTurbine.getBladePoints());
        jsonObject.put("tower_points", windTurbine.getTowerPoints());
        String jsonInput = jsonObject.toString();
        try {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            // 设置请求方法
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoOutput(true);
            // 发送请求
            try (OutputStream os = con.getOutputStream()) {
                byte[] input = jsonInput.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            // 获取响应
            int responseCode = con.getResponseCode();
            System.out.println("Response Code: " + responseCode);
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), "utf-8"))) {
                StringBuilder response1 = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response1.append(responseLine.trim());
                }
                log.info("执行飞向中心点空中航线1");
                // 使用FastJSON解析响应
                JSONObject jsonResponse = JSONObject.parseObject(response1.toString());
                System.out.println("JSON Response: " + jsonResponse.toJSONString());
                String routeName = jsonResponse.getString("routeName");
                // 项目根目录下的文件路径（根据实际部署环境调整）
                String projectPath = System.getProperty("user.dir");
                String filePath = projectPath + File.separator + "file" + File.separator + "kmz" + File.separator + routeName + ".kmz";
                MultipartFile file = convert(filePath);
                if (Objects.isNull(file)) {
                    log.error("kmz文件未检测到");
                }
                log.info("执行飞向中心点空中航线2");
                String workspaceId = redisUtils.get("workspaceId").toString();
                String creator = redisUtils.get("creator").toString();

                importKmzNoValiService.importKmzFile(file, workspaceId, creator, null);
                String fileName = file.getOriginalFilename();
                if (fileName != null && fileName.endsWith(".kmz")) {
                    fileName = fileName.substring(0, fileName.length() - 4);
                }
                log.info("执行飞向中心点空中航线3");
                WaylineFileEntity entity = importKmzNoValiService.getWaylineByFileName(fileName);
                if (Objects.isNull(entity)) {
                    log.error("导入外部航线失败");
                }
                log.info("执行飞向中心点空中航线4");
                InFlightWaylineDeliverParam param = new InFlightWaylineDeliverParam();
                DeviceEntity deviceEntity = deviceMapper.selectOne(new LambdaQueryWrapper<DeviceEntity>().eq(DeviceEntity::getDomain, 3));

                // get wayline file
                WorkspaceEntity workspaceEntity = workspaceMapper.selectOne(new LambdaQueryWrapper<>());

                Optional<GetWaylineListResponse> waylineFile = waylineFileService.getWaylineByWaylineId(workspaceEntity.getWorkspaceId(), entity.getWaylineId());
                if (waylineFile.isEmpty()) {
                    log.error("The wayline file doesn't exist.");
                }
                // get file url
                URL url1 = waylineFileService.getObjectUrl(workspaceEntity.getWorkspaceId(), waylineFile.get().getId());
                log.info("执行飞向中心点空中航线5");
                FileParam fileParam = new FileParam();
                fileParam.setFingerprint(waylineFile.get().getSign());
                fileParam.setUrl(url1.toString());
                param.setFile(fileParam);
//              失控返航
                param.setOutOfControlAction(OutOfControlActionEnum.RETURN_TO_HOME);
                param.setExitWaylineWhenRcLost(ExitWaylineWhenRcLostEnum.EXECUTE_RC_LOST_ACTION);
                param.setRthAltitude(30);
                param.setRthMode(RthModeEnum.PRESET_HEIGHT);
                param.setWaylinePrecisionType(WaylinePrecisionTypeEnum.RTK);
                String waylineName = entity.getName();
                CreateJobParam createJobParam = new CreateJobParam();
                createJobParam.setName(waylineName);
                createJobParam.setFileId(entity.getWaylineId());
                createJobParam.setDockSn(deviceEntity.getDeviceSn());
                createJobParam.setWaylineType(WaylineTypeEnum.WAYPOINT);
//              任务类型为立即执行，是否后续不需要额外判断了
                createJobParam.setTaskType(TaskTypeEnum.IMMEDIATE);
                createJobParam.setRthAltitude(30);
                createJobParam.setOutOfControlAction(OutOfControlActionEnum.RETURN_TO_HOME);
                createJobParam.setMinBatteryCapacity(50);
                createJobParam.setMinStorageCapacity(null);
                List<Long> task_days = new ArrayList<>();
                createJobParam.setTaskDays(task_days);
                List<List<Long>> task_periods = new ArrayList<>();
                createJobParam.setTaskPeriods(task_periods);
//              空中航线没有创建计划，计划id和job_id设为一样
                String job_id = UUID.randomUUID().toString();
                createJobParam.setPlanId(job_id);
                param.setInFlightWaylineId(job_id);
                log.info("执行飞向中心点空中航线6");
                performDeliveryWithRetry(deviceEntity.getDeviceSn(), param, createJobParam);
                log.info("执行飞向中心点空中航线7----");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void performDeliveryWithRetry(String sn, InFlightWaylineDeliverParam param, CreateJobParam createJobParam) {
        int retryCount = 0;
        int code = -1; // 初始化为-1以进入循环

        // 当返回码为-1且未超过最大重试次数时，持续重试
        while (code == -1 && retryCount < MAX_RETRY_COUNT) {
            retryCount++;
            System.out.println("第 " + retryCount + " 次尝试发送...");

            // 调用您的业务方法
            // 此处替换为您实际的 service 调用
            HttpResultResponse httpResultResponse = controlService2.inFlightWaylineDeliver(sn, param, createJobParam);

            code = httpResultResponse.getCode();

            // 如果本次返回码仍为-1，且未达到最大重试次数，则等待后继续
            if (code == -1 && retryCount < MAX_RETRY_COUNT) {
                System.out.println("发送失败(code=-1)，" + RETRY_INTERVAL_MS + "毫秒后重试...");
                try {
                    // 线程休眠，间隔一段时间再重试
                    TimeUnit.MILLISECONDS.sleep(RETRY_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.out.println("重试过程被中断");
                    break;
                }
            }
        }

        // 循环结束后，根据最终状态输出结果
        if (code != -1) {
            System.out.println("发送成功！最终返回码: " + code);
        } else {
            System.out.println("已达到最大重试次数 (" + MAX_RETRY_COUNT + ")，发送最终失败。");
        }
    }

    // 不停机巡检
    public void workingWayline(String name) {
//      风机名为获取
        WindTurbine windTurbine = windTurbineService.selectByName(name);
//        String url = "http://172.20.63.157:5001/working";
        String url = waylineUrlConfig.getBuildKmzUrl().getWorkingWayline();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("turbine_name", windTurbine.getTurbineName());
        jsonObject.put("lon0_deg", windTurbine.getAirportLongitude());
        jsonObject.put("lat0_deg", windTurbine.getAirportLatitude());
        jsonObject.put("h0", windTurbine.getAirportAltitude());
        //      转向角为获取
        jsonObject.put("yaw_ver", windTurbine.getApproachYaw());
        jsonObject.put("lon_in_deg", windTurbine.getPeakLongitude());
        jsonObject.put("lat_in_deg", windTurbine.getPeakLatitude());
        jsonObject.put("h_in", windTurbine.getPeakAltitude());
        jsonObject.put("h_c", windTurbine.getBladeCenterHeight());
        jsonObject.put("theta_deg", windTurbine.getBladeStopAngle());
        jsonObject.put("length", windTurbine.getBladeLength());
        jsonObject.put("dist", windTurbine.getUavBladeDistance());
        jsonObject.put("h_b", windTurbine.getBladeBottomHeight());
        jsonObject.put("blade_points", windTurbine.getBladePoints());
        jsonObject.put("tower_points", windTurbine.getTowerPoints());
        String jsonInput = jsonObject.toString();
        try {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            // 设置请求方法
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoOutput(true);
            // 发送请求
            try (OutputStream os = con.getOutputStream()) {
                byte[] input = jsonInput.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            // 获取响应
            int responseCode = con.getResponseCode();
            System.out.println("Response Code: " + responseCode);
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), "utf-8"))) {
                StringBuilder response1 = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response1.append(responseLine.trim());
                }
                // 使用FastJSON解析响应
                JSONObject jsonResponse = JSONObject.parseObject(response1.toString());
                System.out.println("JSON Response: " + jsonResponse.toJSONString());
                String routeName = jsonResponse.getString("routeName");
                JSONArray points = jsonResponse.getJSONArray("points");
//                redisUtils.set("fanPoints",points.toJSONString());
                FanWaylinePoints fanWaylinePoints = new FanWaylinePoints();
                fanWaylinePoints.setJobId(redisUtils.get("jobId").toString());
                fanWaylinePoints.setDjiFanPoints(points.toJSONString());
                fanWaylinePoints.setJobType(1);
                fanWaylinePointsMapper.insert(fanWaylinePoints);
                // 项目根目录下的文件路径（根据实际部署环境调整）
                String projectPath = System.getProperty("user.dir");
                String filePath = projectPath + File.separator + "file" + File.separator + "kmz" + File.separator + routeName + ".kmz";
                MultipartFile file = convert(filePath);
                if (Objects.isNull(file)) {
                    log.error("kmz文件未检测到");
                }
                String workspaceId = redisUtils.get("workspaceId").toString();
                String creator = redisUtils.get("creator").toString();

                importKmzNoValiService.importKmzFile(file, workspaceId, creator, null);
                String fileName = file.getOriginalFilename();
                if (fileName != null && fileName.endsWith(".kmz")) {
                    fileName = fileName.substring(0, fileName.length() - 4);
                }
                WaylineFileEntity entity = importKmzNoValiService.getWaylineByFileName(fileName);
                if (Objects.isNull(entity)) {
                    log.error("导入外部航线失败");
                }
                InFlightWaylineDeliverParam param = new InFlightWaylineDeliverParam();
                DeviceEntity deviceEntity = deviceMapper.selectOne(new LambdaQueryWrapper<DeviceEntity>().eq(DeviceEntity::getDomain, 3));

                // get wayline file
                WorkspaceEntity workspaceEntity = workspaceMapper.selectOne(new LambdaQueryWrapper<>());

                Optional<GetWaylineListResponse> waylineFile = waylineFileService.getWaylineByWaylineId(workspaceEntity.getWorkspaceId(), entity.getWaylineId());
                if (waylineFile.isEmpty()) {
                    log.error("The wayline file doesn't exist.");
                }
                // get file url
                URL url1 = waylineFileService.getObjectUrl(workspaceEntity.getWorkspaceId(), waylineFile.get().getId());

                FileParam fileParam = new FileParam();
                fileParam.setFingerprint(waylineFile.get().getSign());
                fileParam.setUrl(url1.toString());
                param.setFile(fileParam);
//              失控返航
                param.setOutOfControlAction(OutOfControlActionEnum.RETURN_TO_HOME);
                param.setExitWaylineWhenRcLost(ExitWaylineWhenRcLostEnum.EXECUTE_RC_LOST_ACTION);
                param.setRthAltitude(30);
                param.setRthMode(RthModeEnum.PRESET_HEIGHT);
                param.setWaylinePrecisionType(WaylinePrecisionTypeEnum.RTK);
                String waylineName = entity.getName();
                CreateJobParam createJobParam = new CreateJobParam();
                createJobParam.setName(waylineName);
                createJobParam.setFileId(entity.getWaylineId());
                createJobParam.setDockSn(deviceEntity.getDeviceSn());
                createJobParam.setWaylineType(WaylineTypeEnum.WAYPOINT);
//              任务类型为立即执行，是否后续不需要额外判断了
                createJobParam.setTaskType(TaskTypeEnum.IMMEDIATE);
                createJobParam.setRthAltitude(30);
                createJobParam.setOutOfControlAction(OutOfControlActionEnum.RETURN_TO_HOME);
                createJobParam.setMinBatteryCapacity(50);
                createJobParam.setMinStorageCapacity(null);
                List<Long> task_days = new ArrayList<>();
                createJobParam.setTaskDays(task_days);
                List<List<Long>> task_periods = new ArrayList<>();
                createJobParam.setTaskPeriods(task_periods);
//              空中航线没有创建计划，计划id和job_id设为一样
                String job_id = UUID.randomUUID().toString();
                createJobParam.setPlanId(job_id);
                param.setInFlightWaylineId(job_id);
                performDeliveryWithRetry(deviceEntity.getDeviceSn(), param, createJobParam);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 停机巡检
//  todo 停机巡检也得空中下发航线，需验证是否照片存入minio
    public void stopWayline(String name, Double yaw) {
//      风机名为获取
        WindTurbine windTurbine = windTurbineService.selectByName(name);
//        String url = "http://172.20.63.157:5001/stop";
        String url = waylineUrlConfig.getBuildKmzUrl().getStopWayline();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("turbine_name", windTurbine.getTurbineName());
        jsonObject.put("lon0_deg", windTurbine.getAirportLongitude());
        jsonObject.put("lat0_deg", windTurbine.getAirportLatitude());
        jsonObject.put("h0", windTurbine.getAirportAltitude());
        //      转向角为获取
        jsonObject.put("yaw_ver", windTurbine.getApproachYaw());
        jsonObject.put("lon_in_deg", windTurbine.getPeakLongitude());
        jsonObject.put("lat_in_deg", windTurbine.getPeakLatitude());
        jsonObject.put("h_in", windTurbine.getPeakAltitude());
        jsonObject.put("h_c", windTurbine.getBladeCenterHeight());
        jsonObject.put("theta_deg", yaw);
        jsonObject.put("length", windTurbine.getBladeLength());
        jsonObject.put("dist", windTurbine.getUavBladeDistance());
        jsonObject.put("h_b", windTurbine.getBladeBottomHeight());
        jsonObject.put("blade_points", windTurbine.getBladePoints());
        jsonObject.put("tower_points", windTurbine.getTowerPoints());
        String jsonInput = jsonObject.toString();
        try {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            // 设置请求方法
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoOutput(true);
            // 发送请求
            try (OutputStream os = con.getOutputStream()) {
                byte[] input = jsonInput.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            // 获取响应
            int responseCode = con.getResponseCode();
            System.out.println("Response Code: " + responseCode);
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), "utf-8"))) {
                StringBuilder response1 = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response1.append(responseLine.trim());
                }
                // 使用FastJSON解析响应
                JSONObject jsonResponse = JSONObject.parseObject(response1.toString());
                System.out.println("JSON Response: " + jsonResponse.toJSONString());
                String routeName = jsonResponse.getString("routeName");
//              把扇叶位置存到redis
                JSONArray points = jsonResponse.getJSONArray("points");
//                redisUtils.set("fanPoints",points.toJSONString());
                FanWaylinePoints fanWaylinePoints = new FanWaylinePoints();
                fanWaylinePoints.setJobId(redisUtils.get("jobId").toString());
                fanWaylinePoints.setDjiFanPoints(points.toJSONString());
                fanWaylinePoints.setJobType(0);
                fanWaylinePointsMapper.insert(fanWaylinePoints);
                // 项目根目录下的文件路径（根据实际部署环境调整）
                String projectPath = System.getProperty("user.dir");
                String filePath = projectPath + File.separator + "file" + File.separator + "kmz" + File.separator + routeName + ".kmz";
                MultipartFile file = convert(filePath);
                if (Objects.isNull(file)) {
                    log.error("kmz文件未检测到");
                }

                String workspaceId = redisUtils.get("workspaceId").toString();
                String creator = redisUtils.get("creator").toString();

                importKmzNoValiService.importKmzFile(file, workspaceId, creator, null);
                String fileName = file.getOriginalFilename();
                if (fileName != null && fileName.endsWith(".kmz")) {
                    fileName = fileName.substring(0, fileName.length() - 4);
                }

                WaylineFileEntity entity = importKmzNoValiService.getWaylineByFileName(fileName);
                if (Objects.isNull(entity)) {
                    log.error("导入外部航线失败");
                }
                InFlightWaylineDeliverParam param = new InFlightWaylineDeliverParam();
                DeviceEntity deviceEntity = deviceMapper.selectOne(new LambdaQueryWrapper<DeviceEntity>().eq(DeviceEntity::getDomain, 3));

                // get wayline file
                WorkspaceEntity workspaceEntity = workspaceMapper.selectOne(new LambdaQueryWrapper<>());

                Optional<GetWaylineListResponse> waylineFile = waylineFileService.getWaylineByWaylineId(workspaceEntity.getWorkspaceId(), entity.getWaylineId());
                if (waylineFile.isEmpty()) {
                    log.error("The wayline file doesn't exist.");
                }
                // get file url
                URL url1 = null;
                try {
                    url1 = waylineFileService.getObjectUrl(workspaceEntity.getWorkspaceId(), waylineFile.get().getId());
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
                log.info("空中航线前1----------");
                FileParam fileParam = new FileParam();
                fileParam.setFingerprint(waylineFile.get().getSign());
                fileParam.setUrl(url1.toString());
                param.setFile(fileParam);
//              失控返航
                param.setOutOfControlAction(OutOfControlActionEnum.RETURN_TO_HOME);
                param.setExitWaylineWhenRcLost(ExitWaylineWhenRcLostEnum.EXECUTE_RC_LOST_ACTION);
                param.setRthAltitude(30);
                param.setRthMode(RthModeEnum.PRESET_HEIGHT);
                param.setWaylinePrecisionType(WaylinePrecisionTypeEnum.RTK);
                String waylineName = entity.getName();
                CreateJobParam createJobParam = new CreateJobParam();
                createJobParam.setName(waylineName);
                createJobParam.setFileId(entity.getWaylineId());
                createJobParam.setDockSn(deviceEntity.getDeviceSn());
                createJobParam.setWaylineType(WaylineTypeEnum.WAYPOINT);
//              任务类型为立即执行，是否后续不需要额外判断了
                createJobParam.setTaskType(TaskTypeEnum.IMMEDIATE);
                createJobParam.setRthAltitude(30);
                createJobParam.setOutOfControlAction(OutOfControlActionEnum.RETURN_TO_HOME);
                createJobParam.setMinBatteryCapacity(50);
                createJobParam.setMinStorageCapacity(null);
                List<Long> task_days = new ArrayList<>();
                createJobParam.setTaskDays(task_days);
                List<List<Long>> task_periods = new ArrayList<>();
                createJobParam.setTaskPeriods(task_periods);
//              空中航线没有创建计划，计划id和job_id设为一样
                String job_id = UUID.randomUUID().toString();
                createJobParam.setPlanId(job_id);
                param.setInFlightWaylineId(job_id);
                log.info("空中航线前2----------");
                performDeliveryWithRetry(deviceEntity.getDeviceSn(), param, createJobParam);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public Map<String,Object> buildFanWayline(PubWaylineJobPlanDfEntity pubWaylineJobPlanDfEntity) {
        // 写发送逻辑
        Map map = new HashMap();
        String fanId = pubWaylineJobPlanDfEntity.getFanId();
        redisUtils.set("windTurbineId", fanId);
        WindTurbine windTurbine = windTurbineMapper.selectById(fanId);
//        String url = "http://172.20.63.157:5001/top";
        String url = waylineUrlConfig.getBuildKmzUrl().getTopWayline();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("turbine_name", windTurbine.getTurbineName());
        jsonObject.put("lon0_deg", windTurbine.getAirportLongitude());
        jsonObject.put("lat0_deg", windTurbine.getAirportLatitude());
        jsonObject.put("h0", windTurbine.getAirportAltitude());
        //      转向角为获取
        jsonObject.put("yaw_ver", windTurbine.getApproachYaw());
        jsonObject.put("lon_in_deg", windTurbine.getPeakLongitude());
        jsonObject.put("lat_in_deg", windTurbine.getPeakLatitude());
        jsonObject.put("h_in", windTurbine.getPeakAltitude());
        jsonObject.put("h_c", windTurbine.getBladeCenterHeight());
        jsonObject.put("theta_deg", windTurbine.getBladeStopAngle());
        jsonObject.put("length", windTurbine.getBladeLength());
        jsonObject.put("dist", windTurbine.getUavBladeDistance());
        jsonObject.put("h_b", windTurbine.getBladeBottomHeight());
        jsonObject.put("blade_points", windTurbine.getBladePoints());
        jsonObject.put("tower_points", windTurbine.getTowerPoints());
        String jsonInput = jsonObject.toString();
        try {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            // 设置请求方法
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoOutput(true);
            // 发送请求
            try (OutputStream os = con.getOutputStream()) {
                byte[] input = jsonInput.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            // 获取响应
            int responseCode = con.getResponseCode();
            System.out.println("Response Code: " + responseCode);
//          如果响应成功，则成功生成kmz文件转为multipartFile，导入minio
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(con.getInputStream(), "utf-8"))) {
                    StringBuilder response1 = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response1.append(responseLine.trim());
                    }
                    JSONObject jsonResponse = JSONObject.parseObject(response1.toString());
                    String routeName = jsonResponse.getString("routeName");
                    // 项目根目录下的文件路径（根据实际部署环境调整），注意linux是否可行
                    String projectPath = System.getProperty("user.dir");
                    String filePath = projectPath + File.separator + "file" + File.separator + "kmz" + File.separator + routeName + ".kmz";
                    MultipartFile file = convert(filePath);
                    if (Objects.isNull(file)) {
                         log.error("kmz文件未检测到");
                    }

                    String workspaceId = redisUtils.get("workspaceId").toString();
                    String creator = redisUtils.get("creator").toString();
                    importKmzNoValiService.importKmzFile(file, workspaceId, creator, null);
                    String fileName = file.getOriginalFilename();
                    if (fileName != null && fileName.endsWith(".kmz")) {
                        fileName = fileName.substring(0, fileName.length() - 4);
                    }
                    WaylineFileEntity entity = importKmzNoValiService.getWaylineByFileName(fileName);
                    if (Objects.isNull(entity)) {
                        log.error("导入外部航线失败");
                    }
                    pubWaylineJobPlanDfEntity.setName(routeName);
                    pubWaylineJobPlanDfEntity.setFileId(entity.getWaylineId());
//                  航线类型：航点
                    pubWaylineJobPlanDfEntity.setWaylineType(0);
//                  风机名称
                    pubWaylineJobPlanDfEntity.setFanName(windTurbine.getTurbineName());
//                  创建计划存数据库
                    pubWaylineJobPlanDfEntity.setPlanId(UUID.randomUUID().toString());
                    // 获取当前系统时间戳（以毫秒为单位）
                    long currentTimeMillis = System.currentTimeMillis();
                    //如果是立即执行任务，添加begin_time
                    if(pubWaylineJobPlanDfEntity.getTaskType()==0){
                        pubWaylineJobPlanDfEntity.setBeginTime(currentTimeMillis);
                    }
                    pubWaylineJobPlanDfEntity.setCreateTime(currentTimeMillis);
                    pubWaylineJobPlanDfEntity.setUpdateTime(currentTimeMillis);
                    pubWaylineJobPlanDfEntity.setWaylineType(0);
                    //校验paln_id是否重复
                    PubWaylineJobPlanDfEntity entity1 = pubWaylineJobPlanDfMapper.selectOne(new LambdaQueryWrapper<PubWaylineJobPlanDfEntity>().
                            eq(PubWaylineJobPlanDfEntity::getPlanId,pubWaylineJobPlanDfEntity.getPlanId()));
                    if(entity1!=null){//plan_id重复
                        map.put("result",false);
                        return map;
                    }else{//plan_id不重复
                        pubWaylineJobPlanDfMapper.insert(pubWaylineJobPlanDfEntity);
                        map.put("result",true);
                        map.put("plan",pubWaylineJobPlanDfEntity);
                        return map;
                    }
                }
            }
        } catch (ProtocolException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return map;
    }


    @Override
    public Map<String,Object> buildInterestPointWayline(PubWaylineJobPlanDfEntity pubWaylineJobPlanDfEntity) {
        // 写发送逻辑
        Map map = new HashMap();
        String poiId = pubWaylineJobPlanDfEntity.getPoiId();
        PointOfInterest pointOfInterest = pointOfInterestMapper.selectById(poiId);
        String url = waylineUrlConfig.getBuildKmzUrl().getInterestPointWayline();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("point_name", pointOfInterest.getPointName());
        jsonObject.put("lon0", pointOfInterest.getPointLongitude());     // 经度
        jsonObject.put("lat0", pointOfInterest.getPointLatitude());      // 纬度
        jsonObject.put("h0", pointOfInterest.getPointAltitude());        // 高度
        // 环绕参数
        jsonObject.put("orbit_height", pointOfInterest.getOrbitHeight());    // 环绕高度
        jsonObject.put("orbit_radius", pointOfInterest.getOrbitRadius());    // 环绕半径
        jsonObject.put("init_direction", pointOfInterest.getInitDirection()); // 初始方向
        jsonObject.put("orbit_points_num", pubWaylineJobPlanDfEntity.getPoiOrbitNum()); // 环绕点数
        jsonObject.put("focalLength", pointOfInterest.getFocalLength());    // 焦距
        String jsonInput = jsonObject.toString();
        try {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            // 设置请求方法
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoOutput(true);
            // 发送请求
            try (OutputStream os = con.getOutputStream()) {
                byte[] input = jsonInput.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            // 获取响应
            int responseCode = con.getResponseCode();
            System.out.println("Response Code: " + responseCode);
//          如果响应成功，则成功生成kmz文件转为multipartFile，导入minio
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(con.getInputStream(), "utf-8"))) {
                    StringBuilder response1 = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response1.append(responseLine.trim());
                    }
                    JSONObject jsonResponse = JSONObject.parseObject(response1.toString());
                    String routeName = jsonResponse.getString("pointName");
                    // 项目根目录下的文件路径（根据实际部署环境调整），注意linux是否可行
                    String projectPath = System.getProperty("user.dir");
                    String filePath = projectPath + File.separator + "file" + File.separator + "kmz" + File.separator + routeName + ".kmz";
                    MultipartFile file = convert(filePath);
                    if (Objects.isNull(file)) {
                        log.error("kmz文件未检测到");
                    }

                    String workspaceId = redisUtils.get("workspaceId").toString();
                    String creator = redisUtils.get("creator").toString();
                    importKmzNoValiService.importKmzFile(file, workspaceId, creator, null);
                    String fileName = file.getOriginalFilename();
                    if (fileName != null && fileName.endsWith(".kmz")) {
                        fileName = fileName.substring(0, fileName.length() - 4);
                    }
                    WaylineFileEntity entity = importKmzNoValiService.getWaylineByFileName(fileName);
                    if (Objects.isNull(entity)) {
                        log.error("导入外部航线失败");
                    }
                    pubWaylineJobPlanDfEntity.setName(routeName);
                    pubWaylineJobPlanDfEntity.setFileId(entity.getWaylineId());
//                  航线类型：航点
                    pubWaylineJobPlanDfEntity.setWaylineType(0);
//                  创建计划存数据库
                    pubWaylineJobPlanDfEntity.setPlanId(UUID.randomUUID().toString());
                    // 获取当前系统时间戳（以毫秒为单位）
                    long currentTimeMillis = System.currentTimeMillis();
                    //如果是立即执行任务，添加begin_time
                    if(pubWaylineJobPlanDfEntity.getTaskType()==0){
                        pubWaylineJobPlanDfEntity.setBeginTime(currentTimeMillis);
                    }
                    pubWaylineJobPlanDfEntity.setCreateTime(currentTimeMillis);
                    pubWaylineJobPlanDfEntity.setUpdateTime(currentTimeMillis);
                    pubWaylineJobPlanDfEntity.setWaylineType(0);
                    //校验paln_id是否重复
                    PubWaylineJobPlanDfEntity entity1 = pubWaylineJobPlanDfMapper.selectOne(new LambdaQueryWrapper<PubWaylineJobPlanDfEntity>().
                            eq(PubWaylineJobPlanDfEntity::getPlanId,pubWaylineJobPlanDfEntity.getPlanId()));
                    if(entity1!=null){//plan_id重复
                        map.put("result",false);
                        return map;
                    }else{//plan_id不重复
                        pubWaylineJobPlanDfMapper.insert(pubWaylineJobPlanDfEntity);
                        map.put("result",true);
                        map.put("plan",pubWaylineJobPlanDfEntity);
                        return map;
                    }
                }
            }
        } catch (ProtocolException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return map;
    }


    public static void main(String[] args) {

    }

    //                PubWaylineJobPlanDfEntity pubWaylineJobPlanDfEntity=new PubWaylineJobPlanDfEntity();
//                pubWaylineJobPlanDfEntity.setName(routeName);
//                WorkspaceEntity workspaceEntity = workspaceMapper.selectOne(new LambdaQueryWrapper<>());
//                pubWaylineJobPlanDfEntity.setWorkspaceId(workspaceEntity.getWorkspaceId());
//                pubWaylineJobPlanDfEntity.setFileId(entity.getWaylineId());
//                DeviceEntity deviceEntity = deviceMapper.selectOne(new LambdaQueryWrapper<DeviceEntity>().eq(DeviceEntity::getDomain, 3));
//                pubWaylineJobPlanDfEntity.setDockSn(deviceEntity.getDeviceSn());
////                  航线类型：航点
//                pubWaylineJobPlanDfEntity.setWaylineType(0);
////                  任务类型：立即任务
//                pubWaylineJobPlanDfEntity.setTaskType(0);
////                  返航高度30
//                pubWaylineJobPlanDfEntity.setRthAltitude(30);
////                  失控行为返航
//                pubWaylineJobPlanDfEntity.setOutOfControl(1);
////                  状态启用
//                pubWaylineJobPlanDfEntity.setEnableStatus(0);
////                  优先级为1
//                pubWaylineJobPlanDfEntity.setPlanPriority(1);
////                  变电站写死(380v变电站)
//                pubWaylineJobPlanDfEntity.setSubCode("841419cf-4ee6-49e1-986e-60eefad401c7");
//                pubWaylineJobPlanDfEntity.setPlanSource("系统创建");
//                pubWaylineJobPlanDfEntity.setMajor("变电");
//
////                  创建计划存数据库
//                pubWaylineJobPlanDfService.createWaylineJObPlan(pubWaylineJobPlanDfEntity);
////                  直接执行任务（可以不用查计划？）
//                CustomClaim customClaim = new CustomClaim();
//                customClaim.setWorkspaceId(workspaceId);
//                customClaim.setUsername(creator);
//                pubWaylineJobPlanDfService.expressPlan(customClaim,pubWaylineJobPlanDfEntity);
}
