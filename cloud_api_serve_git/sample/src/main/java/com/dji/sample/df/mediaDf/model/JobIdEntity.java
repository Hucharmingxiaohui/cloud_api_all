package com.dji.sample.df.mediaDf.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobIdEntity {

    private String filePath;

    private String drone;

    private String payload;

    private Long createTime;

    private String jobId;
}
