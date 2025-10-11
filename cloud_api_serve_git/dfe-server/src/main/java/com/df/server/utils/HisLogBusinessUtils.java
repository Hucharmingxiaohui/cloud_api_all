package com.df.server.utils;


import com.df.framework.config.AppContext;
import com.df.framework.constant.Constants;
import com.df.framework.constant.ParamConstants;
import com.df.framework.enums.OperLogTypeEnum;
import com.df.framework.exception.FastException;
import com.df.framework.thread.ThreadPoolService;
import com.df.framework.vo.Result;
import com.df.server.entity.his.HisLogBusinessEntity;
import com.df.server.service.his.hdb.HisLogBusinessService;
import com.df.server.service.sys.SysConfigService;
import com.df.server.vo.SysUser.SysUserAppVO;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

/**
 * 操作日志工具类
 * lyc
 */
@Slf4j
public class HisLogBusinessUtils {

    private static HisLogBusinessService hisLogBusinessService = AppContext.getBean(HisLogBusinessService.class);
    private static SysConfigService sysConfigService = AppContext.getBean(SysConfigService.class);

    private static ThreadPoolService threadPoolService = AppContext.getBean(ThreadPoolService.class);


    /**
     * 记录操作日志
     *
     * @param status
     * @param subCode
     * @param logTitle
     * @param logInfo
     * @param logType
     */
    private static void record(int status, String subCode, String logTitle, String logInfo, Integer logType, HttpServletRequest request) {
        HisLogBusinessEntity entity = new HisLogBusinessEntity();
        entity.setSubCode(subCode);
        entity.setOperStatus(status);
        entity.setLogInfo(logInfo);
        entity.setLogTitle(logTitle);
        entity.setLogType(logType);
        entity.setOperTime(new Date());
        try {
            SysUserAppVO sysUserEntity = (SysUserAppVO) request.getAttribute(Constants.REQUEST_USER_KEY);
            entity.setOperUserName(sysUserEntity.getUserName());
            entity.setOperRealName(sysUserEntity.getRealName());
            entity.setOperIp(sysUserEntity.getLoginIp());
        } catch (Exception e) {
            log.error("【记录操作日志】HttpServletRequest取不到登录IP和用户名");
        }

        String busiLogRecordType = sysConfigService.getValueByKey(ParamConstants.BUSI_LOG_IS_OPEN_SELECT);
        //如果是记录查询，且配置没打钩就不记录
        if (logType.equals(OperLogTypeEnum.SELECT.getType()) && !"1".equals(busiLogRecordType)) {
            return;
        }
        threadPoolService.submit(() -> {
            hisLogBusinessService.insertHisLogBusiness(entity);
        });
    }

    /**
     * 统一记录成功
     *
     * @param subCode
     * @param logTitle
     * @param logInfo
     * @param logType
     */
    public static void recordSuccess(String subCode,
                                     String logTitle,
                                     String logInfo,
                                     Integer logType,
                                     HttpServletRequest request) {

        record(1, subCode, logTitle, logInfo + " 成功", logType, request);
    }

    /**
     * 统一异常记录并返回Result
     *
     * @param exception
     * @param subCode
     * @param logTitle
     * @param logInfo
     * @param logType
     * @return
     */
    public static Result recordAndRtnErr(Exception exception,
                                         String subCode,
                                         String logTitle,
                                         String logInfo,
                                         Integer logType,
                                         HttpServletRequest request) {
        String error = "请求不合法";
        if (exception instanceof FastException) {
            error = exception.getMessage();
        } else {
            exception.printStackTrace();
        }
        logInfo = logInfo + " 失败 " + error;
        record(0, subCode, logTitle, logInfo, logType, request);
        return Result.error(error);
    }
}
