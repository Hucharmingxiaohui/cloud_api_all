package com.dji.sample.center.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author 姜学云
 * @Time 2022/1/18 15:13
 */
public class TaskUtils {
    /**
     * 按国标task_patrolled_id规则，生成uuid串
     * <p>
     * 格式为：任务编码_任务执行开始时间（年月日时分秒），示例1_20190101121212
     *
     * @author 姜学云
     * @date 2022/1/18 15:16
     **/
    public static synchronized String genTaskPatrolledId(String planNo) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        String timeStr = format.format(new Date());
        return String.format("%s_%s", planNo, timeStr);
    }
}
