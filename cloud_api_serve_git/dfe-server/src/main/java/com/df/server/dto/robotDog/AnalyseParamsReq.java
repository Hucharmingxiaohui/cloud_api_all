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
    //  57号文标准：imageNormalPath   一般: imageNormalUrlPath
    /**
     * 判别基准图
     */
    private String imageNormalUrlPath;
    private String imageNormalPath;
    /**
     * 类型列表
     */
    private List<String> typeList;
    //  57号文标准：imagePathList   一般: imageUrlList
    /**
     * 图片URL列表
     */
    private List<String> imageUrlList;
    private List<String> imagePathList;

    public void setImageNormalPathByStandard(String imageNormalPath, boolean document57) {
        if (document57) {
            this.imageNormalPath = imageNormalPath;
            this.imageNormalUrlPath = null;
        } else {
            this.imageNormalUrlPath = imageNormalPath;
            this.imageNormalPath = null;
        }
    }

    public void setImagePathListByStandard(List<String> imagePathList, boolean document57) {
        if (document57) {
            this.imagePathList = imagePathList;
            this.imageUrlList = null;
        } else {
            this.imageUrlList = imagePathList;
            this.imagePathList = null;
        }
    }
}
