package com.dji.sample.df.uavCommonHandleDf.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

// 加上 @JsonIgnoreProperties(ignoreUnknown = true) 可以避免如果 JSON 包含未定义字段时抛出异常，增加代码健壮性
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnalysisResponse {
    private List<ResultItem> resultsList;

    public AnalysisResponse() {}

    public AnalysisResponse(List<ResultItem> resultsList) {
        this.resultsList = resultsList;
    }

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
        private List<List<Integer>> center_points;
        private String code;
        private Double conf;
        private Object desc;
        private String function;
        private String resImagePath;
        private String value;

        // 【新增】对应 JSON 中的 panel_boxes 字段
        private List<PanelBox> panel_boxes;

        public ResultItem() {}

        public ResultItem(List<List<Integer>> center_points, String code, Double conf,
                          Object desc, String function, String resImagePath, String value,
                          List<PanelBox> panel_boxes) {
            this.center_points = center_points;
            this.code = code;
            this.conf = conf;
            this.desc = desc;
            this.function = function;
            this.resImagePath = resImagePath;
            this.value = value;
            this.panel_boxes = panel_boxes;
        }

        // --- Getter 和 Setter 方法 ---

        public List<List<Integer>> getCenter_points() { return center_points; }

        @JsonProperty("center_points")
        public void setCenter_points(Object centerPoints) {
            if (centerPoints instanceof List) {
                this.center_points = (List<List<Integer>>) centerPoints;
            } else {
                this.center_points = new ArrayList<>();
            }
        }

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

        // 【新增】panel_boxes 的 Getter 和 Setter
        public List<PanelBox> getPanel_boxes() { return panel_boxes; }

        @JsonProperty("panel_boxes")
        public void setPanel_boxes(List<PanelBox> panel_boxes) {
            this.panel_boxes = panel_boxes;
        }

        // 便捷方法：检查desc类型
        public boolean isDescString() {
            return desc instanceof String;
        }
        public boolean isDescList() {
            return desc instanceof List;
        }
        public String getDescAsString() {
            return isDescString() ? (String) desc : null;
        }
        @SuppressWarnings("unchecked")
        public List<String> getDescAsList() {
            return isDescList() ? (List<String>) desc : null;
        }

        @Override
        public String toString() {
            return "ResultItem{" +
                    "center_points=" + center_points +
                    ", code='" + code + '\'' +
                    ", conf=" + conf +
                    ", desc=" + desc +
                    ", function='" + function + '\'' +
                    ", resImagePath='" + resImagePath + '\'' +
                    ", value='" + value + '\'' +
                    ", panel_boxes=" + panel_boxes +  // 【修改】toString 中增加 panel_boxes
                    '}';
        }

        // 【新增】对应 JSON 中 panel_boxes 数组里的具体 Box 结构
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class PanelBox {
            private Integer center_col;
            private Integer center_row;
            private Integer height;
            private Integer width;

            public PanelBox() {}

            public PanelBox(Integer center_col, Integer center_row, Integer height, Integer width) {
                this.center_col = center_col;
                this.center_row = center_row;
                this.height = height;
                this.width = width;
            }

            // Getters and Setters
            public Integer getCenter_col() { return center_col; }
            public void setCenter_col(Integer center_col) { this.center_col = center_col; }

            public Integer getCenter_row() { return center_row; }
            public void setCenter_row(Integer center_row) { this.center_row = center_row; }

            public Integer getHeight() { return height; }
            public void setHeight(Integer height) { this.height = height; }

            public Integer getWidth() { return width; }
            public void setWidth(Integer width) { this.width = width; }

            @Override
            public String toString() {
                return "PanelBox{" +
                        "center_col=" + center_col +
                        ", center_row=" + center_row +
                        ", height=" + height +
                        ", width=" + width +
                        '}';
            }
        }
    }
}
