package com.dji.sample.df.patrolDf.config.interceptor;


import com.df.framework.annotation.NoToken;
import com.df.framework.constant.Constants;
import com.df.framework.exception.FastException;
import com.df.framework.utils.HttpUtils;
import com.df.server.utils.token.TokenService;
import com.df.server.vo.SysUser.SysUserAppVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 权限(Token)验证
 *
 * @author lyc
 **/
@Slf4j
@Component
public class AuthorizationInterceptor extends HandlerInterceptorAdapter {

    @Autowired
    private TokenService tokenService;

    /**
     * 拦截每个带AuthToken的方法，验证Token
     *
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException, ServletException {
        NoToken noToken = null;
        if (handler instanceof HandlerMethod) {
            //优先方法注解判断
            noToken = ((HandlerMethod) handler).getMethodAnnotation(NoToken.class);
            if (noToken == null) {
                //方法的类注解判断
                noToken = ((HandlerMethod) handler).getMethod().getDeclaringClass().getAnnotation(NoToken.class);
            }
        }
        if (noToken != null) {
            //如果有@NoToken 不验证Token，前提保证业务不涉及获取用户信息，但禁止加在涉及与用户交互的业务上！
            return true;
        }
        String token = request.getHeader(HttpUtils.AUTHORIZATION_KEY);
        if (token == null) {
            token = request.getParameter(HttpUtils.TOKEN_KEY);
        }
        if (StringUtils.isBlank(token)) {
            log.error("【！未携带验证Token！】：{} 【token】：{} ",
                    request.getRequestURL(), token);
        }
        if (StringUtils.isBlank(token)) {
            throw new FastException("【token验证失败】请求没有token信息，跳转至门户");
        }
        SysUserAppVO userEntity = tokenService.checkToken(token);
        if (userEntity == null || StringUtils.isBlank(userEntity.getUserName())) {
            throw new FastException(501, "【token验证失败】请重新登录");
        }
        request.setAttribute(Constants.REQUEST_USER_KEY, userEntity);
        return true;
    }
}
