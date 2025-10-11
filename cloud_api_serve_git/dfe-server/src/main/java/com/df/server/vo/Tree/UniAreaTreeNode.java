package com.df.server.vo.Tree;

import com.df.framework.vo.Tree;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class UniAreaTreeNode extends Tree implements Serializable {
    private static final long serialVersionUID = -3210884885630038713L;
    private String areaId;

    public UniAreaTreeNode() {
        super();
        this.setNodeType("area");
    }
}
