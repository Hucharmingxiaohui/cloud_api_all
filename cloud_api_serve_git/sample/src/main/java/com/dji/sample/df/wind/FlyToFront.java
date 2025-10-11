package com.dji.sample.df.wind;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dji.sample.control.model.param.FlyToPointParam;
import com.dji.sample.control.service.IControlService;
import com.dji.sample.df.wind.model.entity.WindTurbine;
import com.dji.sample.df.wind.service.WindTurbineService;
import com.dji.sample.manage.dao.IDeviceMapper;
import com.dji.sample.manage.model.entity.DeviceEntity;
import com.dji.sdk.cloudapi.control.Point;
import com.sun.xml.bind.v2.TODO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


@Component
public class FlyToFront {

    @Resource
    WindTurbineService windTurbineService;

    @Autowired
    private IControlService controlService;

    @Resource
    IDeviceMapper deviceMapper;

    // 写发送逻辑
    public void flyToFront(String name,Double yaw){
//      风机名为获取
        WindTurbine windTurbine = windTurbineService.selectByName(name);
        String url = "http://172.20.63.157:5001/single";
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("turbine_name", windTurbine.getTurbineName());
        jsonObject.put("lon0_deg",windTurbine.getAirportLongitude());
        jsonObject.put("lat0_deg", windTurbine.getAirportLatitude());
        jsonObject.put("h0", windTurbine.getAirportAltitude());
        //      转向角为获取
        jsonObject.put("yaw_ver",yaw);
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
            try(OutputStream os = con.getOutputStream()) {
                byte[] input = jsonInput.getBytes("utf-8");
                os.write(input, 0, input.length);
            }
            // 获取响应
            int responseCode = con.getResponseCode();
            System.out.println("Response Code: " + responseCode);
            try(BufferedReader br = new BufferedReader(
                    new InputStreamReader(con.getInputStream(), "utf-8"))) {
                StringBuilder response1 = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response1.append(responseLine.trim());
                }
                // 使用FastJSON解析响应
                JSONObject jsonResponse = JSONObject.parseObject(response1.toString());
                System.out.println("JSON Response: " + jsonResponse.toJSONString());
                // 提取point对象
                JSONObject pointObj = jsonResponse.getJSONObject("point");
                // 获取具体坐标值
                float latitude = pointObj.getFloat("lat");
                float longitude = pointObj.getFloat("lon");
                float height = pointObj.getFloat("height");
                FlyToPointParam  flyToPointParam = new FlyToPointParam();
                List<Point> points =new ArrayList<>();
                Point point = new Point();
                point.setLongitude(longitude);
                point.setLatitude(latitude);
                point.setHeight(height);
                points.add(point);
                flyToPointParam.setPoints(points);
//              最大速度是固定的吗
                flyToPointParam.setMaxSpeed(14);
                DeviceEntity deviceEntity = deviceMapper.selectOne(new LambdaQueryWrapper<DeviceEntity>().eq(DeviceEntity::getDomain, 3));
                controlService.flyToPoint(deviceEntity.getDeviceSn(), flyToPointParam);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        FlyToFront  flyToFront = new FlyToFront();
        flyToFront.flyToFront("风机H",0.0);
    }

}
