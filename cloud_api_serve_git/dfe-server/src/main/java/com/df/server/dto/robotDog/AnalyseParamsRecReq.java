package com.df.server.dto.robotDog;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
 * 接收智能分析结果的请求参数
 */
@Data
@ToString
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class AnalyseParamsRecReq {
    /**
     * 请求唯一标识
     */
    private String requestId;
    /**
     * 结果集
     */
    private List<ResultList> resultList;

    @Data
    @NoArgsConstructor
    public static class ResultList {
        /**
         * 分析点位标识
         */
        private String objectId;
        /**
         * 分析结果
         */
        private List<Result> results;

        @Data
        @NoArgsConstructor
        @JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
        public static class Result {
            private String code;
            private String conf;
            private String desc;
            private String resImagePath;
            private String type;
            private String value;
            private List<Pos> pos;

            @Data
            @NoArgsConstructor
            public static class Pos {
                private List<PosXY> areas;

                @Data
                @NoArgsConstructor
                public static class PosXY {
                    private String x;
                    private String y;
                }
            }
        }
    }
}
