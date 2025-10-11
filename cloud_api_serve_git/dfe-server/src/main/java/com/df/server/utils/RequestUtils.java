package com.df.server.utils;


import com.df.framework.exception.FastException;
import com.df.server.vo.SysUser.SysUserAppVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

/**
 * shiro 工具类
 */
@Slf4j
@Component
public class RequestUtils {

    public static final String USER_INFO = "userInfo";


    public static SysUserAppVO getSysUser(HttpServletRequest request) {
        Object userInfo = request.getAttribute(USER_INFO);
        if (!(userInfo instanceof SysUserAppVO)) {
            throw new FastException("请求没有用户信息，排查【" + request.getRequestURL() + "】是否被验证Token");
        }
        return (SysUserAppVO) userInfo;
    }

    public static Integer getUserId(HttpServletRequest request) {
        try {
            return getSysUser(request).getId();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static Integer getPriority(HttpServletRequest request) {
        try {
            return getSysUser(request).getPriority();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static Integer getDeptId(HttpServletRequest request) {
        try {
            Integer deptId = getSysUser(request).getDeptId();
            if (deptId == null) {
                throw new FastException("当前用户没有归属运维班组！");
            }
            return deptId;
        } catch (Exception e) {
            throw new FastException("获取用户运维班组失败！");
        }
    }

    public static String getUserName(HttpServletRequest request) {
        try {
            return getSysUser(request).getUserName();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String getRealName(HttpServletRequest request) {
        try {
            return getSysUser(request).getRealName();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String getIp(HttpServletRequest request) {
        try {
            String ipAddress = request.getHeader("x-forwarded-for");
            if (ipAddress == null) {
                ipAddress = request.getHeader("X-Real-IP");
            }
            if (StringUtils.isNotBlank(ipAddress)) {
                for (String s : ipAddress.split(",")) {
                    if (!s.equals("127.0.0.1")) {
                        return s.trim();
                    }
                }
            }
            return ipAddress;
        } catch (Exception e) {
            return null;
        }
    }
}
