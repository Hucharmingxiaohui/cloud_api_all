package com.dji.sample.df.TemperatureMeasurementDF.service.impl;

import com.dji.sample.df.TemperatureMeasurementDF.modol.TemParamEntity;
import com.dji.sample.df.TemperatureMeasurementDF.modol.TemResultEntity;
import com.dji.sample.df.TemperatureMeasurementDF.service.IFileServiceDF;
import com.dji.sample.df.TemperatureMeasurementDF.service.TemMeasureService;
import com.dji.sample.df.commonDf.util.DeleteFile;
import com.dji.sample.media.model.MediaFileEntity;
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
public class TemMeasureServiceImpl implements TemMeasureService {
    @Autowired
    private IFileServiceDF fileService;
    //1.根据workspace_id file_id下载图片并返回温度，点测温返回温度（坐标），区域测温返回最大、最小温度坐标
    @Value("${singlePointUrl}")
    private String singlePointUrl;
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


}
