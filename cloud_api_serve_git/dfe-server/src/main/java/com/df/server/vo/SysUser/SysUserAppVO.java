package com.df.server.vo.SysUser;

import lombok.Data;

import java.io.Serializable;

/**
 * 返回值
 */
@Data
public class SysUserAppVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Integer id;
    /**
     * 用户名
     */
    private String userName;
    /**
     * 姓名
     */
    private String realName;
    /**
     * 所属角色ID
     */
    private Integer roleId;
    /**
     * SM2公钥
     */
    private String publicKey;
    /**
     * SM2私钥
     */
    private String privateKey;
    /**
     * 是否第一次登录：0否，1是
     */
    private Integer isFirstLogin;
    /**
     * 最后登陆IP
     */
    private String loginIp;
    /**
     * 优先级
     */
    private Integer priority;
    /**
     * uk公钥
     */
    private String ukPublicKey;
    /**
     * uk秘钥
     */
    private String ukPwd;
    /**
     * 部门ID
     */
    private Integer deptId;
}
