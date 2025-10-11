package com.df.server.vo.FileDownLoad;

import lombok.Data;

import java.io.Serializable;

/**
 * 文件下载
 *
 * @author Administrator
 */
@Data
public class FileDownLoadVO implements Serializable {

    private static final long serialVersionUID = 4974398557126917982L;

    /**
     * 文件存放路径
     */
    private String fileFullPath;

}
