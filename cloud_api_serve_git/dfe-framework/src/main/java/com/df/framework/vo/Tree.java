package com.df.framework.vo;


import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public abstract class Tree implements Serializable {
    private static final long serialVersionUID = -3210884885630038713L;
    private String name;
    private String nodeType;
    private String code;
    private List<Tree> children;

    public Tree() {
        this.children = new ArrayList<>();
    }
}
