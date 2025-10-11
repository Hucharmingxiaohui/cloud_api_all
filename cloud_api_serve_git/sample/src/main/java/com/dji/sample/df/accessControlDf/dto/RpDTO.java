package com.dji.sample.df.accessControlDf.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;
@Data
public class RpDTO implements Serializable {
    @TableField(value = "privs_id")
    private String privsId; // 权限ID

    @TableField(value = "privs_name")
    private String privsName; // 权限名称

    @TableField(value = "privs_des")
    private String privsDes; // 权限描述
    @TableField(value = "has_rp")
    private boolean hasRp;//是否具有关联性
}
