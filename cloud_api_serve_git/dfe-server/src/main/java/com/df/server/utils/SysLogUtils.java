package com.df.server.utils;


import com.df.framework.config.AppContext;
import com.df.framework.constant.Constants;
import com.df.framework.exception.FastException;
import com.df.framework.thread.ThreadPoolService;
import com.df.framework.vo.Result;
import com.df.server.entity.sys.SysLogSystemEntity;
import com.df.server.service.sys.SysLogSystemService;
import com.df.server.vo.SysUser.SysUserAppVO;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

/**
 * 系统日志工具类
 *
 * @Author lyc
 */
@Slf4j
public class SysLogUtils {

    private static SysLogSystemService sysManageLogService = AppContext.getBean(SysLogSystemService.class);
    private static ThreadPoolService threadPoolService = AppContext.getBean(ThreadPoolService.class);


    /**
     * 统一记录成功  登录状态
     *
     * @param logTitle
     * @param logInfo
     * @param logType
     * @param request
     */
    public static void recordSuccessByRequest(String logTitle,
                                              String logInfo,
                                              Integer logType,
                                              HttpServletRequest request) {
        SysUserAppVO userEntity = (SysUserAppVO) request.getAttribute(Constants.REQUEST_USER_KEY);
        SysLogUtils.record(logTitle,
                logType,
                logInfo,
                userEntity.getUserName(),
                userEntity.getRealName(),
                userEntity.getLoginIp(),
                1);
    }


    /**
     * 统一记录成功 没有request 但能确定用户信息
     *
     * @param logTitle
     * @param logInfo
     * @param logType
     * @param userEntity
     */
    public static void recordByUser(String logTitle,
                                    String logInfo,
                                    Integer logType,
                                    Integer status,
                                    SysUserAppVO userEntity) {
        SysLogUtils.record(logTitle,
                logType,
                logInfo,
                userEntity.getUserName(),
                userEntity.getRealName(),
                userEntity.getLoginIp(),
                status);
    }


    /**
     * 统一记录  没有request  记录成功 异常等
     *
     * @param logTitle
     * @param logInfo
     * @param logType
     * @param userName
     * @param IP
     */
    public static void recordNoUser(String logTitle,
                                    String logInfo,
                                    Integer logType,
                                    int status,
                                    String userName,
                                    String IP) {
        SysLogUtils.record(logTitle,
                logType,
                logInfo,
                userName,
                "",
                IP,
                status);
    }


    /**
     * 统一异常记录并返回Result
     *
     * @param e
     * @param logTitle
     * @param logInfo
     * @param logType  系统日志类型(字典sys_log_type)：1用户管理，2权限管理，3系统配置，4日志审计
     * @param request
     * @return
     */
    public static Result recordAndRtnErr(Exception e,
                                         String logTitle,
                                         String logInfo,
                                         Integer logType,
                                         HttpServletRequest request) {
        String error = "请求不合法";
        if (e instanceof FastException) {
            error = e.getMessage();
        } else {
            e.printStackTrace();
        }
        logInfo = logInfo + " 失败 " + error;
        SysUserAppVO userEntity = (SysUserAppVO) request.getAttribute(Constants.REQUEST_USER_KEY);
        SysLogUtils.record(logTitle,
                logType,
                logInfo,
                userEntity.getUserName(),
                userEntity.getRealName(),
                userEntity.getLoginIp(),
                0);
        return Result.error(error);
    }


    /**
     * 记录系统日志信息
     *
     * @param logTitle
     * @param logType      系统日志类型(字典sys_log_type)：1用户管理，2权限管理，3系统配置，4日志审计
     * @param logInfo
     * @param operUsername
     * @param operRealname
     * @param ip
     * @param status       操作状态：0失败，1成功
     */
    private static void record(String logTitle,
                               int logType,
                               String logInfo,
                               String operUsername,
                               String operRealname,
                               String ip,
                               int status) {
        SysLogSystemEntity sysInfo = new SysLogSystemEntity();
        sysInfo.setLogTitle(logTitle);
        sysInfo.setLogType(logType);
        sysInfo.setLogInfo(logInfo);
        sysInfo.setOperUserName(operUsername);
        sysInfo.setOperRealName(operRealname);
        sysInfo.setOperIp(ip);
        sysInfo.setOperStatus(status);
        sysInfo.setOperTime(new Date());
        //异步记录
        threadPoolService.submit(() -> {
            sysManageLogService.save(sysInfo);
        });
    }

    public static void recordErrByRequest(String logTitle, String logInfo, int logType, HttpServletRequest request) {
        SysUserAppVO userInfoByToken = RequestUtils.getSysUser(request);
        recordByUser(logTitle, logInfo, logType, 0, userInfoByToken);
    }
}
