package com.dji.sample.df.wind.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.df.framework.vo.Result;
import com.dji.sample.common.model.CustomClaim;
import com.dji.sample.df.importKmzNoValiDf.service.ImportKmzNoValiService;
import com.dji.sample.df.wind.model.entity.WindTurbine;
import com.dji.sample.df.wind.service.WindTurbineService;
import com.dji.sample.wayline.model.entity.WaylineFileEntity;
import com.dji.sample.wayline.model.entity.WaylineJobEntity;
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
import java.util.Objects;

import static com.dji.sample.component.AuthInterceptor.TOKEN_CLAIM;

@RestController
@RequestMapping("/api/WindTurbineWayline")
public class WindTurbineWaylineController {


    @Resource
    WindTurbineService windTurbineService;

    @Autowired
    ImportKmzNoValiService importKmzNoValiService;

    @GetMapping("/excute")
    public Result excute(HttpServletRequest request,@RequestParam String windTurbineName) {
        // 写发送逻辑
        WindTurbine windTurbine = windTurbineService.selectByName(windTurbineName);
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
                // 项目根目录下的文件路径（根据实际部署环境调整）
                String projectPath = System.getProperty("user.dir");
                String filePath = projectPath + File.separator + "file" + File.separator + "kmz" + File.separator + "风机Htop.kmz";
                MultipartFile file = convert(filePath);
                if (Objects.isNull(file)) {
                    return Result.error("kmz文件未检测到");
                }
                CustomClaim customClaim = (CustomClaim) request.getAttribute(TOKEN_CLAIM);
                String workspaceId = customClaim.getWorkspaceId();
                String creator = customClaim.getUsername();
                importKmzNoValiService.importKmzFile(file, workspaceId, creator, null);
                String fileName = file.getOriginalFilename();
                if (fileName != null && fileName.endsWith(".kmz")) {
                    fileName = fileName.substring(0, fileName.length() - 4);
                }
                WaylineFileEntity entity = importKmzNoValiService.getWaylineByFileName(fileName);
                }

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
                return Result.success();
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @PostMapping("/test")
    public Result test() {
        try {
            // 项目根目录下的文件路径（根据实际部署环境调整）
            String projectPath = System.getProperty("user.dir");
            String filePath = projectPath + File.separator + "file" + File.separator + "kmz" + File.separator + "风机Htop.kmz";

            MultipartFile multipartFile = convert(filePath);
            System.out.println("转换成功：" + multipartFile.getOriginalFilename() +
                    "，大小：" + multipartFile.getSize() + "字节");
            return  Result.success();
        } catch (IOException e) {
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
}
