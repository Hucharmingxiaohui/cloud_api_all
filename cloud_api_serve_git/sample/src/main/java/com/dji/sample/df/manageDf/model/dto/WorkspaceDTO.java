package com.dji.sample.df.manageDf.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author sean.zhou
 * @date 2021/11/22
 * @version 0.1
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WorkspaceDTO {

    private Integer id;

    private String workspaceId;

    private String workspaceName;

    private String workspaceDesc;

    private String platformName;

    private Integer createTime;

    private Integer updateTime;

    private String bindCode;
}