package com.dji.sample.df.accessControlDf.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class RpsDTO implements Serializable {
    @TableField(value = "role_id")
    private String roleId; // 角色ID

    @TableField(value = "role_name")
    private String roleName; // 角色名称

    @TableField(value = "role_des")
    private String roleDes; // 角色描述
    @TableField(value = "rpDTO_list")
    private List<RpDTO> rpDTOList;//每个角色具有的权限列表
}
