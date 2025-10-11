package com.df.server.dto.sysDictData;

import lombok.Data;

import java.io.Serializable;

/**
 * 字典数据接口
 * <p>
 * Created by lianyc on 2022-10-13
 */
@Data
public class SysDictDataParamsDTO implements Serializable {
    private static final long serialVersionUID = 2758370420008339865L;
    /**
     * 字典标签
     */
    private String dictLabel;
    /**
     * 字典类型
     */
    private String dictType;

}
