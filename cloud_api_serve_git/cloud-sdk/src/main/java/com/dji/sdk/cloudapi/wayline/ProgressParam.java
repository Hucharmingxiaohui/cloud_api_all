package com.dji.sdk.cloudapi.wayline;

public class ProgressParam {

    private Integer persent;

    // 无参构造
    public ProgressParam() {
    }

    // 全参构造
    public ProgressParam(Integer persent) {
        this.persent = persent;
    }

    public Integer getPersent() {
        return persent;
    }

    public void setPersent(Integer persent) {
        this.persent = persent;
    }

    @Override
    public String toString() {
        return "ProgressParam{" +
                "persent=" + persent +
                '}';
    }

}
