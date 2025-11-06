package com.dji.sample.df.wind.model.entity;

import java.util.List;

public class AnalysisResponse {
    private List<ResultItem> resultsList;

    // 默认构造函数
    public AnalysisResponse() {}

    // 全参构造函数
    public AnalysisResponse(List<ResultItem> resultsList) {
        this.resultsList = resultsList;
    }

    // Getter和Setter方法
    public List<ResultItem> getResultsList() {
        return resultsList;
    }

    public void setResultsList(List<ResultItem> resultsList) {
        this.resultsList = resultsList;
    }

    @Override
    public String toString() {
        return "AnalysisResponse{" +
                "resultsList=" + resultsList +
                '}';
    }

    // 内部结果项类
    public static class ResultItem {
        private String code;
        private Double conf;
        private Object desc; // 可能是String或List<String>
        private String function;
        private String resImagePath;
        private String value;

        // 默认构造函数
        public ResultItem() {}

        // 全参构造函数
        public ResultItem(String code, Double conf, Object desc,
                          String function, String resImagePath, String value) {
            this.code = code;
            this.conf = conf;
            this.desc = desc;
            this.function = function;
            this.resImagePath = resImagePath;
            this.value = value;
        }

        // Getter和Setter方法
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }

        public Double getConf() { return conf; }
        public void setConf(Double conf) { this.conf = conf; }

        public Object getDesc() { return desc; }
        public void setDesc(Object desc) { this.desc = desc; }

        public String getFunction() { return function; }
        public void setFunction(String function) { this.function = function; }

        public String getResImagePath() { return resImagePath; }
        public void setResImagePath(String resImagePath) { this.resImagePath = resImagePath; }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }

        // 便捷方法：检查desc是否为字符串
        public boolean isDescString() {
            return desc instanceof String;
        }

        // 便捷方法：检查desc是否为列表
        public boolean isDescList() {
            return desc instanceof List;
        }

        // 便捷方法：获取字符串类型的desc
        public String getDescAsString() {
            return isDescString() ? (String) desc : null;
        }

        // 便捷方法：获取列表类型的desc
        @SuppressWarnings("unchecked")
        public List<String> getDescAsList() {
            return isDescList() ? (List<String>) desc : null;
        }

        @Override
        public String toString() {
            return "ResultItem{" +
                    "code='" + code + '\'' +
                    ", conf=" + conf +
                    ", desc=" + desc +
                    ", function='" + function + '\'' +
                    ", resImagePath='" + resImagePath + '\'' +
                    ", value='" + value + '\'' +
                    '}';
        }
    }
}
