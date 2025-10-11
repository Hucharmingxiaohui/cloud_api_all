package com.dji.sample.df.mediaDf.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author sean
 * @version 0.2
 * @date 2021/12/9
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaFileDTO {

    private String fileId;

    private String fileName;

    private String filePath;

    private String objectKey;

    private String subFileType;

    private Boolean isOriginal;

    private String drone;

    private String payload;

    private String tinnyFingerprint;

    private String fingerprint;

    private Long createTime;

    private Long updateTime;

    private String jobId;
}
