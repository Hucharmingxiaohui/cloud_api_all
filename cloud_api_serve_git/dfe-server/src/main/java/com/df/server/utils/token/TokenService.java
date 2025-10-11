package com.df.server.utils.token;


import com.df.server.vo.SysUser.SysUserAppVO;

/**
 * 用户Token
 * <p>
 * Created by lyc on 2020-02-21
 */
public interface TokenService {


    SysUserAppVO checkToken(String token);
}
