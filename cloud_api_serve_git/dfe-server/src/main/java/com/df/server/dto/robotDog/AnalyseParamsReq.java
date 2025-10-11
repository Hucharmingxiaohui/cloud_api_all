package com.df.server.dto.robotDog;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 智能分析请求参数
 *
 * @author lyc
 * @date 2022/3/29 8:57
 */
@Data
public class AnalyseParamsReq implements Serializable {
    private static final long serialVersionUID = -1054618684495314629L;

    /**
     * 实物ID
     */
    private String objectId;
    /**
     * 判别基准图
     */
    private String imageNormalUrlPath;
    /**
     * 类型列表
     */
    private List<String> typeList;
    /**
     * 图片URL列表
     */
    private List<String> imageUrlList;

}
