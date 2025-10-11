package com.df.server.vo.Tree;

import com.df.framework.vo.Tree;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class UniBayTreeNode extends Tree implements Serializable {
    private static final long serialVersionUID = -3210884885630038713L;
    /**
     * 间隔编码
     */
    private String bayId;

    public UniBayTreeNode() {
        super();
        this.setNodeType("bay");
    }

}
