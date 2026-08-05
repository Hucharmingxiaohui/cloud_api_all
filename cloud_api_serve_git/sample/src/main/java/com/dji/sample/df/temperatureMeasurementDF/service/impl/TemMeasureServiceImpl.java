package com.dji.sample.df.temperatureMeasurementDF.service.impl;

import com.dji.sample.df.substationDf.dao.UniPointMapper2;
import com.dji.sample.center.entity.UniPoint;
import com.dji.sample.df.temperatureMeasurementDF.modol.TemParamEntity;
import com.dji.sample.df.temperatureMeasurementDF.modol.TemResultEntity;
import com.dji.sample.df.temperatureMeasurementDF.service.IFileServiceDF;
import com.dji.sample.df.temperatureMeasurementDF.service.TemMeasureService;
import com.dji.sample.df.commonDf.util.DeleteFile;
import com.dji.sample.media.model.MediaFileEntity;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

//红外测温实现
@Service
@Transactional
@Slf4j
public class TemMeasureServiceImpl implements TemMeasureService {
    @Autowired
    private IFileServiceDF fileService;
    //1.根据workspace_id file_id下载图片并返回温度，点测温返回温度（坐标），区域测温返回最大、最小温度坐标
    @Value("${singlePointUrl}")
    private String singlePointUrl;
//
    // 新增的远程配置
    @Value("${remote.ssh.host}")
    private String remoteHost;
    @Value("${remote.ssh.port}")
    private int remotePort;
    @Value("${remote.ssh.username}")
    private String remoteUser;
    @Value("${remote.ssh.password}")
    private String remotePassword;
    @Value("${remote.dji_irp.path}")
    private String remoteDjiIrpPath;
    @Value("${remote.dji_irp.libraryPath}")
    private String remotelibraryPath;
    @Value("${remote.temp.dir}")
    private String remoteTempDir;
    @Autowired
    UniPointMapper2 uniPointMapper;
//    1方法：适用于测温sdk部署在本地服务器     2方法：适用于无人机系统部署在arm架构服务器，需另加一台x86服务器用来部署测温sdk用来系统远程调用

    @Override
    public TemResultEntity getTemByWorkSpaceIdAndFileId(String workspace_id, String file_id, TemParamEntity temParamEntity) {
        DeleteFile deleteFile = new DeleteFile();
        //0存储温度结构
        TemResultEntity temResultEntity=new TemResultEntity();
        //1.下载图片
        //1.1下载路径
        String targetFolder =singlePointUrl + "dji_thermal_sdk_v1.6_20240927/temTest";
        //1.2获取文件信息
        Optional<MediaFileEntity> fileEntity = fileService.getMediaByFileId(workspace_id, file_id);
        //1.4检查数据库图像信息
        String targetFileName = "";
        Path outputPath = null;
        //        fileEntity.isPresent()
        //1.5检查图片是否存入数据库
        if (fileEntity!=null) {
            targetFileName = fileEntity.get().getFileName();
            outputPath = Path.of(targetFolder, targetFileName);
        } else{
            temResultEntity.setError("图像信息没有存入数据库");
            return temResultEntity;
        }
        //1.6 获取下载路径url
        URL imageRemoteAddr = fileService.getObjectUrl(workspace_id, file_id);
        //1.7下载
        try {
            InputStream in = imageRemoteAddr.openStream();
            Files.copy(in, outputPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            temResultEntity.setError("图像信息没有存入minio");
            return temResultEntity;
        }
        //2根据图像生成raw图像
        String command = singlePointUrl + "dji_thermal_sdk_v1.6_20240927/utility/bin/linux/release_x64/dji_irp";
        String osName = System.getProperty("os.name").toLowerCase();
        if(osName.contains("windows")){
            command = singlePointUrl + "dji_thermal_sdk_v1.6_20240927/utility/bin/windows/release_x64/dji_irp.exe";
        }

        String inputFilePath =  targetFolder+"/"+ fileEntity.get().getFileName();
        String outputFilePath = targetFolder+"/" + "measure.raw";
        // 2.2构建命令数组
        String[] cmd = {
                command,
                "-s", inputFilePath,
                "-a", "measure",
                "-o", outputFilePath
        };

        try {
            // 2.3创建 ProcessBuilder
            ProcessBuilder processBuilder = new ProcessBuilder(cmd);
            //linux 需要
//            Map<String, String> env = processBuilder.environment();
 //           env.put("LD_LIBRARY_PATH", "/usr/local/lib:"+ singlePointUrl+ "dji_thermal_sdk_v1.6_20240927/utility/bin/linux/release_x64");
            processBuilder.redirectErrorStream(true); // 合并标准错误流和标准输出流
            Process process = processBuilder.start();

            // 2.4读取命令执行的输出
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }

            // 2.5等待进程完成并获取退出码
            int exitCode = process.waitFor();
            System.out.println("Command executed with exit code: " + exitCode);

        } catch (Exception e) {
            deleteFile.deleteFileInFolder(targetFolder, fileEntity.get().getFileName());
            deleteFile.deleteFileInFolder(targetFolder, "measure.raw");
            temResultEntity.setError("生成raw格式温度图出错");
            e.printStackTrace();
            deleteFile.deleteFileInFolder(targetFolder, fileEntity.get().getFileName());
            deleteFile.deleteFileInFolder(targetFolder, "measure.raw");
            return temResultEntity;
        }
        //3根据raw文件生成温度矩阵
        //定义温度矩阵宽和高
        Integer width=null;
        Integer height=null;
        //3.1读像素定义宽高
        String imagePath = targetFolder+"/"+ fileEntity.get().getFileName();

        try {
            // 读取图像文件
            File imageFile = new File(imagePath);
            BufferedImage image = ImageIO.read(imageFile);

            if (image != null) {
                // 获取图像宽度和高度
                width = image.getWidth();
                height = image.getHeight();
                System.out.println("Image Width: " + width);
                System.out.println("Image Height: " + height);
                width=640;
                height=512;

            } else {
                temResultEntity.setError("未找到图像获取像素值");
                deleteFile.deleteFileInFolder(targetFolder, fileEntity.get().getFileName());
                deleteFile.deleteFileInFolder(targetFolder, "measure.raw");
               return temResultEntity;
            }
        } catch (IOException e) {
            deleteFile.deleteFileInFolder(targetFolder, fileEntity.get().getFileName());
            deleteFile.deleteFileInFolder(targetFolder, "measure.raw");
            e.printStackTrace();
            temResultEntity.setError("获取图像像素值失败");
        }
        //3.2定义温度矩阵
        double[][] temperatureMatrix = new double[height][width];
        try {
            String filePath = targetFolder + "/" + "measure.raw";
            // 1. 读取 .raw 文件
            File file = new File(filePath);

            // 校验文件大小是否符合预期
            long expectedSize = width * height * 2; // 每个 short 数据占 2 字节
            //校验长度就行
//            if (file.length() != expectedSize) {
//                throw new IOException("File size mismatch. Expected: " + expectedSize + " bytes, but found: " + file.length() + " bytes.");
//            }
            if (file.length() != expectedSize) {
                throw new IOException("File size mismatch. Expected: " + expectedSize + " bytes, but found: " + file.length() + " bytes.");
            }

            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[(int) file.length()];
            fis.read(buffer);
            fis.close();

            // 2. 将字节数组转换为 short 数组
            ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN); // 根据文件的字节序调整（常见为小端）

            double minTemperature = Double.MAX_VALUE;
            double maxTemperature = Double.MIN_VALUE;
            boolean hasInvalidData = false;

            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    if (!byteBuffer.hasRemaining()) {
                        throw new IOException("Unexpected end of file. Data might be corrupted.");
                    }
                    // 每两个字节转换为一个 short
                    short rawValue = byteBuffer.getShort();
                    // 转换为温度值
                    double temperature = rawValue / 10.0;

                    // 校验温度范围是否合理
                    if (temperature < -50 || temperature > 500) {
                        hasInvalidData = true;
                        System.err.printf("Invalid temperature value at [%d][%d]: %.1f°C%n", i, j, temperature);
                    }

                    // 更新温度范围
                    minTemperature = Math.min(minTemperature, temperature);
                    maxTemperature = Math.max(maxTemperature, temperature);

                    // 填充温度矩阵
                    temperatureMatrix[i][j] = temperature;
                }
            }

            // 3. 打印校验结果
            //System.out.println("Temperature matrix loaded successfully.");
            System.out.printf("Temperature range: [%.1f°C, %.1f°C]%n", minTemperature, maxTemperature);
            //设置温度范围
            String range ="(" + minTemperature + "°C"+","+maxTemperature+"°C"+")";
            temResultEntity.setRange(range);
            if (hasInvalidData) {
                temResultEntity.setError("温度值异常");
                deleteFile.deleteFileInFolder(targetFolder, fileEntity.get().getFileName());
                deleteFile.deleteFileInFolder(targetFolder, "measure.raw");
                return temResultEntity;
                //System.err.println("Warning: Detected invalid temperature values in the file.");
            }

            // 4. 输出部分温度值验证
            //System.out.println("Sample temperatures:");
            //System.out.println(temperatureMatrix[511][0]);

        } catch (IOException e) {
            deleteFile.deleteFileInFolder(targetFolder, fileEntity.get().getFileName());
            deleteFile.deleteFileInFolder(targetFolder, "measure.raw");
            temResultEntity.setError("温度处理异常");
            System.err.println("Error processing the file: " + e.getMessage());
            deleteFile.deleteFileInFolder(targetFolder, fileEntity.get().getFileName());
            deleteFile.deleteFileInFolder(targetFolder, "measure.raw");
            return temResultEntity;
        }
        //点测温
        if(temParamEntity.getPoint_x()!=null&&temParamEntity.getPoint_y()!=null){
            double roundedNum = Math.round(temperatureMatrix[temParamEntity.getPoint_y()][temParamEntity.getPoint_x()]* 10) / 10.0;
            temResultEntity.setPoint_tem(roundedNum);
            //设置测温点
            String pointPisition = "("+temParamEntity.getPoint_x()+","+temParamEntity.getPoint_y()+")";
            temResultEntity.setPoint_position(pointPisition);
            deleteFile.deleteFileInFolder(targetFolder, fileEntity.get().getFileName());
            deleteFile.deleteFileInFolder(targetFolder, "measure.raw");
            return  temResultEntity;
        }else{
            //宽度判断 高度判断
            if(temParamEntity.getLeft_top_x()> temParamEntity.getRight_bottom_x()||
                    temParamEntity.getLeft_top_y()> temParamEntity.getRight_bottom_y()){
                temResultEntity.setError("区域测温参数设置错误");
                deleteFile.deleteFileInFolder(targetFolder, fileEntity.get().getFileName());
                deleteFile.deleteFileInFolder(targetFolder, "measure.raw");
                return temResultEntity;
            }
            //设置测温点
            temResultEntity.setLeft_top_position("("+temParamEntity.getLeft_top_x()+","+temParamEntity.getLeft_top_y()+")");
            temResultEntity.setRight_bottum_position("("+temParamEntity.getRight_bottom_x()+","+temParamEntity.getRight_bottom_y()+")");
            //计算最大值，最小值，平均值
            //宽度温度个数
            int width1 = temParamEntity.getRight_bottom_x() - temParamEntity.getLeft_top_x()+1;
            //高
            int height1 = temParamEntity.getRight_bottom_y() -temParamEntity.getLeft_top_y()+1;
            //总温度
            Double sumTem=0.0;
            //设置最大
            Double temMax = temperatureMatrix[temParamEntity.getLeft_top_y()][temParamEntity.getLeft_top_x()];
            //设置最小温度
            Double temMin = temperatureMatrix[temParamEntity.getLeft_top_y()][temParamEntity.getLeft_top_x()];

            for(int i1=0;i1<height1;i1++){//高
                for(int j1=0;j1<width1;j1++){//宽
                    sumTem=sumTem+temperatureMatrix[i1+temParamEntity.getLeft_top_y()][j1+temParamEntity.getLeft_top_x()];
                    if(temperatureMatrix[i1+temParamEntity.getLeft_top_y()][j1+temParamEntity.getLeft_top_x()]>temMax){
                        temMax=temperatureMatrix[i1+temParamEntity.getLeft_top_y()][j1+temParamEntity.getLeft_top_x()];
                    }
                    if(temperatureMatrix[i1+temParamEntity.getLeft_top_y()][j1+temParamEntity.getLeft_top_x()]<temMin){
                        temMin=temperatureMatrix[i1+temParamEntity.getLeft_top_y()][j1+temParamEntity.getLeft_top_x()];
                    }

                }
            }
            Double avarengeTem = sumTem/(width1*height1);
            //设置区域测温温度信息
            //平均
            temResultEntity.setAverage_tem(Math.round(avarengeTem*10)/10.0);
            //最大
            temResultEntity.setMax_tem(Math.round(temMax*10)/10.0);
            //最小
            temResultEntity.setMin_tem(Math.round(temMin*10)/10.0);
            deleteFile.deleteFileInFolder(targetFolder, fileEntity.get().getFileName());
            deleteFile.deleteFileInFolder(targetFolder, "measure.raw");

            //返回测温结果
            return temResultEntity;
        }
    }


//    @Override
//    public TemResultEntity getTemByWorkSpaceIdAndFileId(String workspace_id, String file_id, TemParamEntity temParamEntity) {
//        DeleteFile deleteFile = new DeleteFile();
//        TemResultEntity temResultEntity = new TemResultEntity();
//
//        // ---------- 1. 下载图片（保持不变） ----------
//        String targetFolder = singlePointUrl + "dji_thermal_sdk_v1.6_20240927/temTest";
//        Optional<MediaFileEntity> fileEntity = fileService.getMediaByFileId(workspace_id, file_id);
//        if (fileEntity.isEmpty()) {
//            temResultEntity.setError("图像信息没有存入数据库");
//            return temResultEntity;
//        }
//        String targetFileName = fileEntity.get().getFileName();
//        Path outputPath = Path.of(targetFolder, targetFileName);
//        URL imageRemoteAddr = fileService.getObjectUrl(workspace_id, file_id);
//        try (InputStream in = imageRemoteAddr.openStream()) {
//            Files.copy(in, outputPath, StandardCopyOption.REPLACE_EXISTING);
//        } catch (IOException e) {
//            temResultEntity.setError("图像信息没有存入minio");
//            return temResultEntity;
//        }
//
//        // ---------- 2. 远程执行 dji_irp 生成 raw 文件 ----------
//        String localImagePath = outputPath.toString();
//        String remoteImagePath = remoteTempDir + "/" + targetFileName;
//        String remoteRawPath = remoteTempDir + "/measure.raw";
//        String localRawPath = targetFolder + "/measure.raw";
//
//        try {
//            log.info("===== 开始远程执行 dji_irp =====");
//            log.info("目标服务器: {}:{}", remoteHost, remotePort);
//
//            // 2.1 建立 SSH 会话
//            log.info("正在建立 SSH 连接...");
//            JSch jsch = new JSch();
//            Session session = jsch.getSession(remoteUser, remoteHost, remotePort);
//            session.setPassword(remotePassword);
//            session.setConfig("StrictHostKeyChecking", "no");
//            session.connect();
//            log.info("SSH 连接成功，会话 ID: {}", session);
//
//            // 2.2 创建远程临时目录
//            log.info("确保远程临时目录存在: {}", remoteTempDir);
//            executeCommand(session, "mkdir -p " + remoteTempDir);
//            log.info("远程临时目录准备就绪");
//
//            // 2.3 上传图片到远程服务器
//            log.info("开始上传图片: 本地路径 {} -> 远程路径 {}", localImagePath, remoteImagePath);
//            scpToRemote(session, localImagePath, remoteImagePath);
//            log.info("图片上传完成");
//
//            // 2.4 构造并执行远程命令
////          这一步要根据不同服务器sdk位置进行路径调整
//            String libraryPath = remotelibraryPath;
//            String command = String.format("export LD_LIBRARY_PATH=%s; %s -s '%s' -a measure -o '%s'",
//                    libraryPath, remoteDjiIrpPath, remoteImagePath, remoteRawPath);
//            log.info("执行远程命令: {}", command);
//            String execResult = executeCommand(session, command);
//            log.info("远程命令执行完成，输出结果: {}", execResult);
//
//            // 2.5 下载 raw 文件到本地
//            log.info("开始下载 raw 文件: 远程路径 {} -> 本地路径 {}", remoteRawPath, localRawPath);
//            scpFromRemote(session, remoteRawPath, localRawPath);
//            log.info("raw 文件下载完成");
//
//            // 2.6 清理远程临时文件
//            log.info("清理远程临时文件: {} 和 {}", remoteImagePath, remoteRawPath);
//            executeCommand(session, "rm -f " + remoteImagePath + " " + remoteRawPath);
//            log.info("远程临时文件清理完成");
//
//            session.disconnect();
//            log.info("SSH 会话已关闭，远程操作全部成功");
//
//        } catch (Exception e) {
//            log.error("远程执行 dji_irp 失败", e);
//            deleteFile.deleteFileInFolder(targetFolder, targetFileName);
//            deleteFile.deleteFileInFolder(targetFolder, "measure.raw");
//            temResultEntity.setError("远程执行 dji_irp 失败");
//            return temResultEntity;
//        }
//
//        //3根据raw文件生成温度矩阵
//        //定义温度矩阵宽和高
//        Integer width = null;
//        Integer height = null;
//        //3.1读像素定义宽高
//        String imagePath = targetFolder + "/" + fileEntity.get().getFileName();
//
//        try {
//            // 读取图像文件
//            File imageFile = new File(imagePath);
//            BufferedImage image = ImageIO.read(imageFile);
//
//            if (image != null) {
//                // 获取图像宽度和高度
//                width = image.getWidth();
//                height = image.getHeight();
//                System.out.println("Image Width: " + width);
//                System.out.println("Image Height: " + height);
//                width = 640;
//                height = 512;
//
//            } else {
//                temResultEntity.setError("未找到图像获取像素值");
//                deleteFile.deleteFileInFolder(targetFolder, fileEntity.get().getFileName());
//                deleteFile.deleteFileInFolder(targetFolder, "measure.raw");
//                return temResultEntity;
//            }
//        } catch (IOException e) {
//            deleteFile.deleteFileInFolder(targetFolder, fileEntity.get().getFileName());
//            deleteFile.deleteFileInFolder(targetFolder, "measure.raw");
//            e.printStackTrace();
//            temResultEntity.setError("获取图像像素值失败");
//        }
//        //3.2定义温度矩阵
//        double[][] temperatureMatrix = new double[height][width];
//        try {
//            String filePath = targetFolder + "/" + "measure.raw";
//            // 1. 读取 .raw 文件
//            File file = new File(filePath);
//
//            // 校验文件大小是否符合预期
//            long expectedSize = width * height * 2; // 每个 short 数据占 2 字节
//            //校验长度就行
////            if (file.length() != expectedSize) {
////                throw new IOException("File size mismatch. Expected: " + expectedSize + " bytes, but found: " + file.length() + " bytes.");
////            }
//            if (file.length() != expectedSize) {
//                throw new IOException("File size mismatch. Expected: " + expectedSize + " bytes, but found: " + file.length() + " bytes.");
//            }
//
//            FileInputStream fis = new FileInputStream(file);
//            byte[] buffer = new byte[(int) file.length()];
//            fis.read(buffer);
//            fis.close();
//
//            // 2. 将字节数组转换为 short 数组
//            ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
//            byteBuffer.order(ByteOrder.LITTLE_ENDIAN); // 根据文件的字节序调整（常见为小端）
//
//            double minTemperature = Double.MAX_VALUE;
//            double maxTemperature = Double.MIN_VALUE;
//            boolean hasInvalidData = false;
//
//            for (int i = 0; i < height; i++) {
//                for (int j = 0; j < width; j++) {
//                    if (!byteBuffer.hasRemaining()) {
//                        throw new IOException("Unexpected end of file. Data might be corrupted.");
//                    }
//                    // 每两个字节转换为一个 short
//                    short rawValue = byteBuffer.getShort();
//                    // 转换为温度值
//                    double temperature = rawValue / 10.0;
//
//                    // 校验温度范围是否合理
//                    if (temperature < -50 || temperature > 500) {
//                        hasInvalidData = true;
//                        System.err.printf("Invalid temperature value at [%d][%d]: %.1f°C%n", i, j, temperature);
//                    }
//
//                    // 更新温度范围
//                    minTemperature = Math.min(minTemperature, temperature);
//                    maxTemperature = Math.max(maxTemperature, temperature);
//
//                    // 填充温度矩阵
//                    temperatureMatrix[i][j] = temperature;
//                }
//            }
//
//            // 3. 打印校验结果
//            //System.out.println("Temperature matrix loaded successfully.");
//            System.out.printf("Temperature range: [%.1f°C, %.1f°C]%n", minTemperature, maxTemperature);
//            //设置温度范围
//            String range = "(" + minTemperature + "°C" + "," + maxTemperature + "°C" + ")";
//            temResultEntity.setRange(range);
//            if (hasInvalidData) {
//                temResultEntity.setError("温度值异常");
//                deleteFile.deleteFileInFolder(targetFolder, fileEntity.get().getFileName());
//                deleteFile.deleteFileInFolder(targetFolder, "measure.raw");
//                return temResultEntity;
//                //System.err.println("Warning: Detected invalid temperature values in the file.");
//            }
//
//            // 4. 输出部分温度值验证
//            //System.out.println("Sample temperatures:");
//            //System.out.println(temperatureMatrix[511][0]);
//
//        } catch (IOException e) {
//            deleteFile.deleteFileInFolder(targetFolder, fileEntity.get().getFileName());
//            deleteFile.deleteFileInFolder(targetFolder, "measure.raw");
//            temResultEntity.setError("温度处理异常");
//            System.err.println("Error processing the file: " + e.getMessage());
//            deleteFile.deleteFileInFolder(targetFolder, fileEntity.get().getFileName());
//            deleteFile.deleteFileInFolder(targetFolder, "measure.raw");
//            return temResultEntity;
//        }
//        //点测温
//        if (temParamEntity.getPoint_x() != null && temParamEntity.getPoint_y() != null) {
//            double roundedNum = Math.round(temperatureMatrix[temParamEntity.getPoint_y()][temParamEntity.getPoint_x()] * 10) / 10.0;
//            temResultEntity.setPoint_tem(roundedNum);
//            //设置测温点
//            String pointPisition = "(" + temParamEntity.getPoint_x() + "," + temParamEntity.getPoint_y() + ")";
//            temResultEntity.setPoint_position(pointPisition);
//            deleteFile.deleteFileInFolder(targetFolder, fileEntity.get().getFileName());
//            deleteFile.deleteFileInFolder(targetFolder, "measure.raw");
//            return temResultEntity;
//        } else {
//            //宽度判断 高度判断
//            if (temParamEntity.getLeft_top_x() > temParamEntity.getRight_bottom_x() ||
//                    temParamEntity.getLeft_top_y() > temParamEntity.getRight_bottom_y()) {
//                temResultEntity.setError("区域测温参数设置错误");
//                deleteFile.deleteFileInFolder(targetFolder, fileEntity.get().getFileName());
//                deleteFile.deleteFileInFolder(targetFolder, "measure.raw");
//                return temResultEntity;
//            }
//            //设置测温点
//            temResultEntity.setLeft_top_position("(" + temParamEntity.getLeft_top_x() + "," + temParamEntity.getLeft_top_y() + ")");
//            temResultEntity.setRight_bottum_position("(" + temParamEntity.getRight_bottom_x() + "," + temParamEntity.getRight_bottom_y() + ")");
//            //计算最大值，最小值，平均值
//            //宽度温度个数
//            int width1 = temParamEntity.getRight_bottom_x() - temParamEntity.getLeft_top_x() + 1;
//            //高
//            int height1 = temParamEntity.getRight_bottom_y() - temParamEntity.getLeft_top_y() + 1;
//            //总温度
//            Double sumTem = 0.0;
//            //设置最大
//            Double temMax = temperatureMatrix[temParamEntity.getLeft_top_y()][temParamEntity.getLeft_top_x()];
//            //设置最小温度
//            Double temMin = temperatureMatrix[temParamEntity.getLeft_top_y()][temParamEntity.getLeft_top_x()];
//
//            for (int i1 = 0; i1 < height1; i1++) {//高
//                for (int j1 = 0; j1 < width1; j1++) {//宽
//                    sumTem = sumTem + temperatureMatrix[i1 + temParamEntity.getLeft_top_y()][j1 + temParamEntity.getLeft_top_x()];
//                    if (temperatureMatrix[i1 + temParamEntity.getLeft_top_y()][j1 + temParamEntity.getLeft_top_x()] > temMax) {
//                        temMax = temperatureMatrix[i1 + temParamEntity.getLeft_top_y()][j1 + temParamEntity.getLeft_top_x()];
//                    }
//                    if (temperatureMatrix[i1 + temParamEntity.getLeft_top_y()][j1 + temParamEntity.getLeft_top_x()] < temMin) {
//                        temMin = temperatureMatrix[i1 + temParamEntity.getLeft_top_y()][j1 + temParamEntity.getLeft_top_x()];
//                    }
//
//                }
//            }
//            Double avarengeTem = sumTem / (width1 * height1);
//            //设置区域测温温度信息
//            //平均
//            temResultEntity.setAverage_tem(Math.round(avarengeTem * 10) / 10.0);
//            //最大
//            temResultEntity.setMax_tem(Math.round(temMax * 10) / 10.0);
//            //最小
//            temResultEntity.setMin_tem(Math.round(temMin * 10) / 10.0);
//            deleteFile.deleteFileInFolder(targetFolder, fileEntity.get().getFileName());
//            deleteFile.deleteFileInFolder(targetFolder, "measure.raw");
//
//            return temResultEntity;
//        }
//    }

    @Override
    public boolean bindPoint(TemParamEntity temParamEntity) {
    try {
        UniPoint uniPoint = uniPointMapper.selectById(temParamEntity.getPoint_id());
        String coordinates = String.format("[%d,%d,%d,%d]",
                temParamEntity.getLeft_top_x(),
                temParamEntity.getLeft_top_y(),
                temParamEntity.getRight_bottom_x(),
                temParamEntity.getRight_bottom_y());
        uniPoint.setInfraredImageCoordinate(coordinates);
        uniPointMapper.updateById(uniPoint);
        return true;
        }catch (Exception e){
        e.printStackTrace();
        return false;
       }

    }

    /**
     * 执行远程命令并返回输出
     */
    private String executeCommand(Session session, String command) throws Exception {
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
        channel.setOutputStream(outputStream);
        channel.setErrStream(errorStream); // 关键：捕获错误流

        channel.connect();

        while (!channel.isClosed()) {
            Thread.sleep(100);
        }
        int exitCode = channel.getExitStatus();
        String stdout = outputStream.toString();
        String stderr = errorStream.toString();
        channel.disconnect();

        if (exitCode != 0) {
            throw new RuntimeException(String.format(
                    "Remote command failed with exit code %d\nSTDOUT: %s\nSTDERR: %s",
                    exitCode, stdout, stderr));
        }
        return stdout;
    }

    /**
     * 将本地文件上传到远程服务器
     */
    private void scpToRemote(Session session, String localFile, String remoteFile) throws Exception {
        boolean ptimestamp = false;
        String command = "scp " + (ptimestamp ? "-p" : "") + " -t " + remoteFile;
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);

        OutputStream out = channel.getOutputStream();
        InputStream in = channel.getInputStream();

        channel.connect();

        if (checkAck(in) != 0) {
            throw new RuntimeException("SCP failed");
        }

        File _lfile = new File(localFile);
        long filesize = _lfile.length();

        // 发送 "C0644 filesize filename"
        command = "C0644 " + filesize + " ";
        command += _lfile.getName();
        command += "\n";
        out.write(command.getBytes());
        out.flush();

        if (checkAck(in) != 0) {
            throw new RuntimeException("SCP failed");
        }

        // 发送文件内容
        try (FileInputStream fis = new FileInputStream(localFile)) {
            byte[] buf = new byte[1024];
            int len;
            while ((len = fis.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            out.flush();
        }

        // 发送结束标志
        out.write(0);
        out.flush();

        if (checkAck(in) != 0) {
            throw new RuntimeException("SCP failed");
        }

        channel.disconnect();
    }

    /**
     * 从远程服务器下载文件到本地
     */
    private void scpFromRemote(Session session, String remoteFile, String localFile) throws Exception {
        String command = "scp -f " + remoteFile;
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        channel.setCommand(command);

        OutputStream out = channel.getOutputStream();
        InputStream in = channel.getInputStream();

        channel.connect();

        byte[] buf = new byte[1024];

        // 发送初始确认
        buf[0] = 0;
        out.write(buf, 0, 1);
        out.flush();

        while (true) {
            int c = checkAck(in);
            if (c == 'C') {
                // 读取文件信息
                in.read(buf, 0, 5);
                long filesize = 0L;
                while (true) {
                    if (in.read(buf, 0, 1) < 0) break;
                    if (buf[0] == ' ') break;
                    filesize = filesize * 10L + (buf[0] - '0');
                }
                String fileName = null;
                for (int i = 0;; i++) {
                    in.read(buf, i, 1);
                    if (buf[i] == (byte) 0x0a) {
                        fileName = new String(buf, 0, i);
                        break;
                    }
                }

                // 发送确认
                buf[0] = 0;
                out.write(buf, 0, 1);
                out.flush();

                // 读取文件内容
                try (FileOutputStream fos = new FileOutputStream(localFile)) {
                    int len;
                    while (filesize > 0) {
                        len = (int) Math.min(buf.length, filesize);
                        len = in.read(buf, 0, len);
                        if (len < 0) break;
                        fos.write(buf, 0, len);
                        filesize -= len;
                    }
                }

                // 检查结束确认
                if (checkAck(in) != 0) {
                    throw new RuntimeException("SCP failed");
                }

                // 发送最终确认
                buf[0] = 0;
                out.write(buf, 0, 1);
                out.flush();
                break;
            } else if (c == 'E') {
                break;
            }
        }
        channel.disconnect();
    }

    /**
     * SCP 协议中的 ACK 检查
     */
    private int checkAck(InputStream in) throws IOException {
        int b = in.read();
        if (b == 0) return 0;
        if (b == -1) return -1;
        if (b == 1 || b == 2) {
            StringBuilder sb = new StringBuilder();
            int c;
            do {
                c = in.read();
                sb.append((char) c);
            } while (c != '\n');
            if (b == 1) {
                System.err.print("SCP warning: " + sb.toString());
            } else {
                throw new IOException("SCP error: " + sb.toString());
            }
        }
        return b;
    }

}
