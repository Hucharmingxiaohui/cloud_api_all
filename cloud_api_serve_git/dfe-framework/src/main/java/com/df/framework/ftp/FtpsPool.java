package com.df.framework.ftp;


import com.df.framework.config.FtpsConfig;
import com.df.framework.utils.SpringUtils;

import java.util.Hashtable;

public class FtpsPool extends ObjectPool<FtpsHelper> {

    private FtpsConfig ftpConfig = SpringUtils.getBean(FtpsConfig.class);

    public static Hashtable<FtpsConfig, FtpsPool> instance = new Hashtable<FtpsConfig, FtpsPool>();

    public static FtpsPool getInstance(FtpsConfig cfg) {
        if (instance.get(cfg) == null) {
            synchronized (FtpsPool.class) {
                if (instance.get(cfg) == null) {
                    instance.put(cfg, new FtpsPool(cfg));
                }
            }
        }
        return instance.get(cfg);
    }

    public FtpsPool(FtpsConfig ftpCfg) {
        ftpConfig = ftpCfg;
    }

    @Override
    FtpsHelper create() {
        FtpsHelper ftpClient = new FtpsHelper();

        boolean login = ftpClient.login(
                ftpConfig.getFtpIp(),
                ftpConfig.getFtpPort(),
                ftpConfig.getUserName(),
                ftpConfig.getPassword(),
                ftpConfig.getImplicit());
        if (!login) {
            return null;
        }
        return ftpClient;
    }

    @Override
    boolean valide(FtpsHelper ftpClient) {
        return ftpClient.isUsing();
    }

    @Override
    public void destory(FtpsHelper ftpClient) {
        ftpClient.close();
    }
}
