package com.dji.sample.center.utils.ftp;

import com.dji.sample.center.app.AppContext;

import java.util.Hashtable;


public class FtpsPool extends ObjectPool<FtpsHelper> {

    private FtpsCommonConfig ftpsCommonConfig = AppContext.getBean(FtpsCommonConfig.class);

    public static Hashtable<FtpsCommonConfig, FtpsPool> instance = new Hashtable<FtpsCommonConfig, FtpsPool>();

    public static FtpsPool getInstance(FtpsCommonConfig cfg) {
        if (instance.get(cfg) == null) {
            synchronized (FtpsPool.class) {
                if (instance.get(cfg) == null) {
                    instance.put(cfg, new FtpsPool(cfg));
                }
            }
        }
        return instance.get(cfg);
    }

    public FtpsPool(FtpsCommonConfig ftpCfg) {
        ftpsCommonConfig = ftpCfg;
    }

    @Override
    FtpsHelper create() {
        FtpsHelper ftpClient = new FtpsHelper();

        boolean login = ftpClient.login(
                ftpsCommonConfig.getFtpIp(),
                ftpsCommonConfig.getFtpPort(),
                ftpsCommonConfig.getUserName(),
                ftpsCommonConfig.getPassword(),
                ftpsCommonConfig.getImplicit());
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
