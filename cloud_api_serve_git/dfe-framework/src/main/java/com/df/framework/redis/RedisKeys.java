package com.df.framework.redis;

/**
 * @author Mark sunlightcs@gmail.com
 * @since 1.0.0
 */
public class RedisKeys {
    /**
     * Token
     */
    public static String getPointTreeKey(String subCode) {
        return "tree:" + subCode + ":";
    }

    public static String getAlarmLimitKey() {
        return "alarm:limit:";
    }

    public static String getAlarmInfoKey() {
        return "alarm:info:";
    }
}
