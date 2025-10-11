package com.df.server.vo.Tree;

import com.df.framework.vo.Tree;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * @author Administrator
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class UniPointTreeNode extends Tree implements Serializable {
    private static final long serialVersionUID = -3210884885630038713L;
    private String pointCode;
    private Integer baseImageStatus;
    private Integer recognitionType;
    private Integer pointAnalyseCategory;

    public UniPointTreeNode() {
        super();
        this.setNodeType("point");
    }
}
