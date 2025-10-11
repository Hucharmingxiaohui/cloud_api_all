package com.dji.sample.center.utils.ftp;

import com.dji.sample.center.app.AppContext;
import com.dji.sample.center.config.CenterFtpsNormalConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * FTP工具类 todo 2022.8.24 ftp 修改为资源池方式
 */
@Slf4j
public class FtpUtils {
    private CenterFtpsNormalConfig centerFtpsNormalConfig = AppContext.getBean(CenterFtpsNormalConfig.class);
    private FtpsCommonConfig ftpsCommonConfig = AppContext.getBean(FtpsCommonConfig.class);

    public static final ConcurrentHashMap<String, Thread> UploadThreadMap = new ConcurrentHashMap<>();

    private FtpUtils() {
    }

    private static class FtpUtilsHolder {
        public static final FtpUtils HOLDER = new FtpUtils();
    }

    public static FtpUtils getInstance() {
        return FtpUtilsHolder.HOLDER;
    }

    /**
     * 上传文件到巡视上级FTP - 国网规范
     *
     * @return
     */
    public Boolean uploadToCenterNormal(String localFile, String destDir, String destFileName) {
        ftpsCommonConfig.setFtpIp(centerFtpsNormalConfig.getFtpIp());
        ftpsCommonConfig.setFtpPort(centerFtpsNormalConfig.getFtpPort());
        ftpsCommonConfig.setImplicit(centerFtpsNormalConfig.getImplicit());
        ftpsCommonConfig.setUserName(centerFtpsNormalConfig.getUserName());
        ftpsCommonConfig.setPassword(centerFtpsNormalConfig.getPassword());

        FtpsPool ftpsPool = FtpsPool.getInstance(ftpsCommonConfig);
        boolean result = false;
        Thread thread = Thread.currentThread();
        String uuid = UUID.randomUUID().toString().replaceAll("-", "") + "_" + SystemClock.now();
        UploadThreadMap.put(uuid, thread);
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
        UploadThreadMap.remove(uuid);
        return result;
    }
}
