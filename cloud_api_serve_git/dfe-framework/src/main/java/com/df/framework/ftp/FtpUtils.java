package com.df.framework.ftp;

import com.df.framework.config.FtpsConfig;
import com.df.framework.utils.SpringUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * FTP工具类 todo 2022.8.24 ftp 修改为资源池方式
 */
@Slf4j
public class FtpUtils {
    private FtpsConfig ftpsConfig = SpringUtils.getBean(FtpsConfig.class);

    //public static final ConcurrentHashMap<String, Thread> UploadThreadMap = new ConcurrentHashMap<>();

    private FtpUtils() {
    }

    private static class FtpUtilsHolder {
        public static final FtpUtils HOLDER = new FtpUtils();
    }

    public static FtpUtils getInstance() {
        return FtpUtilsHolder.HOLDER;
    }


    /**
     * 文件上传文件到本地巡视主机
     *
     * @param localPath  本地文件路径，如："D:\\Temp\\1\\Model\\device_model_33.xml"
     * @param remotePath ftp服务器上存放地址，如："/1/Model/device_model_33.xml"
     * @return
     */
    public boolean downloadFromFtps(String remotePath, String localPath) {
        boolean result = false;
        FtpsPool ftpsPool = FtpsPool.getInstance(ftpsConfig);
        FtpsHelper ftpsHelper = ftpsPool.takeOut();
        if (ftpsHelper == null) {
            log.error("【FTP上传失败】 没有有效的FTP连接");
            return false;
        }
        if (ftpsHelper.isLogin()) {
            try {
                result = ftpsHelper.download(remotePath, localPath);
                if (!result) {
                    ftpsPool.destory(ftpsHelper);
                    ftpsPool.remove(ftpsHelper);
                    return result;
                }
            } catch (Exception e) {
                e.printStackTrace();
                ftpsPool.destory(ftpsHelper);
                ftpsPool.remove(ftpsHelper);
                return result;
            }
            ftpsPool.takeIn(ftpsHelper);
        } else {
            ftpsPool.destory(ftpsHelper);
            ftpsPool.remove(ftpsHelper);
        }
        return result;
    }


    /**
     * 上传文件到巡视主机FTP
     *
     * @return
     */
    public Boolean uploadToFtps(String localFile, String destDir, String destFileName) {
        FtpsPool ftpsPool = FtpsPool.getInstance(ftpsConfig);
        boolean result = false;
        //Thread thread = Thread.currentThread();
        //String uuid = UUID.randomUUID().toString().replaceAll("-", "") + "_" + SystemClock.now();
        //UploadThreadMap.put(uuid, thread);
        for (int i = 0; i < 3; i++) {
            FtpsHelper ftpsHelper = ftpsPool.takeOut();
            if (ftpsHelper == null) {
                log.error("【FTPS上传失败】 没有有效的FTP连接");
                continue;
            }
            if (ftpsHelper.isLogin()) {
                try {
                    result = ftpsHelper.uploadFile(localFile, destDir, destFileName);
                    if (!result) {
                        ftpsPool.destory(ftpsHelper);
                        ftpsPool.remove(ftpsHelper);
                    } else {
                        ftpsPool.takeIn(ftpsHelper);
                        break;
                    }
                } catch (Exception e) {
                    log.error("【FTPS上传失败】 {} {} ", e.getMessage(), e);
                    ftpsPool.destory(ftpsHelper);
                    ftpsPool.remove(ftpsHelper);
                    return result;
                }
            } else {
                ftpsPool.destory(ftpsHelper);
                ftpsPool.remove(ftpsHelper);
            }
        }
        //UploadThreadMap.remove(uuid);
        return result;
    }

    public static void main(String[] args) {
        String localFile = "D:\\001.png";
        String destDir = "烟台/XX区域/500kVXX桥变电站/缺陷/202204/";
        String destFileName = "demo.jpg";
        String passiveMode = "true";
        FtpsHelper ftpClient = new FtpsHelper();
        try {
            ftpClient.login("172.20.63.150", 10012, "dfftp", "dfe2k_1500", true);
            ftpClient.uploadFile(localFile, "", "demo.jpg","true");
            ftpClient.close();
        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            ftpClient.close();
        }
    }

}
