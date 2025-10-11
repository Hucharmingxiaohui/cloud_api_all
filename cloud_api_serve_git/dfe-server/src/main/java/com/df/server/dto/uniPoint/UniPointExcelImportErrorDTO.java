package com.df.server.dto.uniPoint;


import com.df.framework.dto.PageDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class UniPointExcelImportErrorDTO extends PageDTO implements Serializable {

    private static final long serialVersionUID = -6390964891599302506L;

    private Integer excelId;

    private String errorReason;
}
