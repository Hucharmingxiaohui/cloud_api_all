package com.df.framework.constant;

import java.util.Arrays;
import java.util.List;

/**
 * 通用常量信息
 *
 * @author ruoyi
 */
public class Constants {
    /**
     * UTF-8 字符集
     */
    public static final String UTF8 = "UTF-8";

    /**
     * GBK 字符集
     */
    public static final String GBK = "GBK";

    /**
     * 系统用户授权缓存
     */
    public static final String SYS_AUTH_CACHE = "sys-authCache";

    /**
     * 参数管理 cache name
     */
    public static final String SYS_CONFIG_CACHE = "sys-config";

    /**
     * 资源映射路径 前缀
     */
    public static final String RESOURCE_PREFIX = "/profile";
    /**
     * 字典管理 cache name
     */
    public static final String SYS_DICT_CACHE = "sys-dict";
    /**
     * 字典管理 cache key
     */
    public static final String SYS_DICT_KEY = "sys_dict:";

    public static final String REQUEST_USER_KEY = "userInfo";

    /**
     * 不需要拦截的访问 登录前的请求 不拦截 ：
     * 登录、getsm4、验证码
     */
    public static final List<String> EXCLUDE_PATH = Arrays.asList(
            "/tologin",//权限管理平台登录
            "/getsm2public",
            "/getsm4", //sm4加密套件
            "/sysConfig/getConfigParams",
            "/captcha/captchaImage",//验证码
            "/app/**"//三方平台权限认证接口
    );
}
