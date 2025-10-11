package com.dji.sample.center.v2022.tool;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author 姜学云
 * @Time 2022/4/23 9:23
 */
public class CenterSNTool {

    private static volatile CenterSNTool centerSNTool;

    //成员变量
    private AtomicLong sendSN;

    public CenterSNTool() {
        this.sendSN = new AtomicLong(0);
    }

    public static CenterSNTool getInstance() {
        if (centerSNTool == null) {
            synchronized (CenterSNTool.class) {
                if (centerSNTool == null) {
                    centerSNTool = new CenterSNTool();
                }
            }
        }
        return centerSNTool;
    }

    /**
     * 自增序号，并返回自增结果值
     * @return
     */
    public long increaseSN() {
        return this.sendSN.incrementAndGet();
    }
}
