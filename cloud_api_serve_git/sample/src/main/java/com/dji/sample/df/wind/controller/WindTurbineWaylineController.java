package com.dji.sample.df.wind.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.df.framework.redis.RedisUtils;
import com.df.framework.vo.Result;
import com.dji.sample.common.model.CustomClaim;
import com.dji.sample.component.oss.service.impl.OssServiceContext;
import com.dji.sample.df.electricInspectionDf.model.PubWaylineJobPlanDfEntity;
import com.dji.sample.df.electricInspectionDf.service.PubWaylineJobPlanDfService;
import com.dji.sample.df.importKmzNoValiDf.service.ImportKmzNoValiService;
import com.dji.sample.df.wind.dao.WindTurbineMapper;
import com.dji.sample.df.wind.model.entity.WindTurbine;
import com.dji.sample.df.wind.service.WindTurbineService;
import com.dji.sample.manage.dao.IDeviceMapper;
import com.dji.sample.manage.dao.IWorkspaceMapper;
import com.dji.sample.manage.model.entity.DeviceEntity;
import com.dji.sample.manage.model.entity.WorkspaceEntity;
import com.dji.sample.wayline.model.entity.WaylineFileEntity;
import com.dji.sdk.common.HttpResultResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import static com.dji.sample.component.AuthInterceptor.TOKEN_CLAIM;

@RestController
@RequestMapping("/api/WindTurbineWayline")
public class WindTurbineWaylineController {


    @Resource
    WindTurbineService windTurbineService;

    @Resource
    WindTurbineMapper windTurbineMapper;

    @Autowired
    ImportKmzNoValiService importKmzNoValiService;

    @Autowired
    private PubWaylineJobPlanDfService pubWaylineJobPlanDfService;

    @Autowired
    IWorkspaceMapper workspaceMapper;

    @Resource
    IDeviceMapper deviceMapper;

    @Resource
    RedisUtils redisUtils;

//  页面在风机列表，在每个风机右侧点击执行任务，获取风机名称，
//  风机名称全局统一唯一，包括航线
    @PostMapping("/excute")
    public HttpResultResponse excute(HttpServletRequest request, @RequestBody JSONObject jsonObject1) {
        // 写发送逻辑
        String id = jsonObject1.getString("id");
        WindTurbine windTurbine = windTurbineMapper.selectById(id);
//        WindTurbine windTurbine = windTurbineService.selectByName(windTurbineName);
        String url = "http://172.20.63.157:5001/top";
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
                try(BufferedReader br = new BufferedReader(
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
//                    MultipartFile file1 = addTimestampToFilename(file);
                    if (Objects.isNull(file)) {
//                        return log.error("kmz文件未检测到");
                    }

                    CustomClaim customClaim = (CustomClaim) request.getAttribute(TOKEN_CLAIM);
                    String workspaceId = customClaim.getWorkspaceId();
                    String creator = customClaim.getUsername();
                    redisUtils.set("workspaceId", workspaceId);
                    redisUtils.set("creator", creator);

                    importKmzNoValiService.importKmzFile(file, workspaceId, creator, null);
                    String fileName = file.getOriginalFilename();
                    if (fileName != null && fileName.endsWith(".kmz")) {
                        fileName = fileName.substring(0, fileName.length() - 4);
                    }
                    WaylineFileEntity entity = importKmzNoValiService.getWaylineByFileName(fileName);
                    if (Objects.isNull(entity)) {
//                        return  HttpResultResponse.error("导入外部航线失败");
                    }

                    PubWaylineJobPlanDfEntity pubWaylineJobPlanDfEntity=new PubWaylineJobPlanDfEntity();
                    pubWaylineJobPlanDfEntity.setName(routeName);
                    WorkspaceEntity workspaceEntity = workspaceMapper.selectOne(new LambdaQueryWrapper<>());
                    pubWaylineJobPlanDfEntity.setWorkspaceId(workspaceEntity.getWorkspaceId());
                    pubWaylineJobPlanDfEntity.setFileId(entity.getWaylineId());
                    DeviceEntity deviceEntity = deviceMapper.selectOne(new LambdaQueryWrapper<DeviceEntity>().eq(DeviceEntity::getDomain, 3));
                    pubWaylineJobPlanDfEntity.setDockSn(deviceEntity.getDeviceSn());
//                  航线类型：航点
                    pubWaylineJobPlanDfEntity.setWaylineType(0);
//                  任务类型：立即任务
                    pubWaylineJobPlanDfEntity.setTaskType(0);
//                  返航高度30
                    pubWaylineJobPlanDfEntity.setRthAltitude(30);
//                  失控行为返航
                    pubWaylineJobPlanDfEntity.setOutOfControl(1);
//                  状态启用
                    pubWaylineJobPlanDfEntity.setEnableStatus(0);
//                  优先级为1
                    pubWaylineJobPlanDfEntity.setPlanPriority(1);
//                  变电站写死(380v变电站)
                    pubWaylineJobPlanDfEntity.setSubCode("faf3362c-3c90-2fce-0f88-b059716cb160");
                    pubWaylineJobPlanDfEntity.setPlanSource("系统创建");
                    pubWaylineJobPlanDfEntity.setMajor("变电");
//                  风机名称
                    pubWaylineJobPlanDfEntity.setFanName(windTurbine.getTurbineName());

//                  创建计划存数据库
                    pubWaylineJobPlanDfService.createWaylineJObPlan(pubWaylineJobPlanDfEntity);
//                  直接执行任务（可以不用查计划？）
                    return pubWaylineJobPlanDfService.expressPlan(customClaim,pubWaylineJobPlanDfEntity);

                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @PostMapping("/test")
    public Result test() {
        try {

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static MultipartFile convert(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("文件不存在：" + filePath);
        }
        // 创建DiskFileItem（commons-fileupload核心类）
        FileItem fileItem = new DiskFileItem(
                "file", // form表单字段名
                "application/vnd.google-earth.kmz", // KMZ文件MIME类型
                false, // 是否为表单字段（false表示文件）
                file.getName(), // 文件名
                (int) file.length(), // 文件大小
                file.getParentFile() // 临时文件存储目录
        );

        // 将文件内容写入FileItem
        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = fileItem.getOutputStream()) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
        }

        // 包装为CommonsMultipartFile（Spring兼容的MultipartFile实现）
        return new CommonsMultipartFile(fileItem);
    }


    public static MultipartFile addTimestampToFilename(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return file;
        }

        String originalName = file.getOriginalFilename();
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        // 处理文件名和扩展名
        int dotIndex = originalName.lastIndexOf(".");
        String newName = dotIndex > 0
                ? originalName.substring(0, dotIndex) + "-" + timestamp + originalName.substring(dotIndex)
                : originalName + "-" + timestamp;

        // 返回新的MultipartFile实现
        return new MultipartFile() {
            @Override public String getName() { return file.getName(); }
            @Override public String getOriginalFilename() { return newName; }
            @Override public String getContentType() { return file.getContentType(); }
            @Override public boolean isEmpty() { return file.isEmpty(); }
            @Override public long getSize() { return file.getSize(); }
            @Override public byte[] getBytes() throws IOException { return file.getBytes(); }
            @Override public InputStream getInputStream() throws IOException { return file.getInputStream(); }
            @Override public void transferTo(File dest) throws IOException, IllegalStateException { file.transferTo(dest); }
        };
    }
}
