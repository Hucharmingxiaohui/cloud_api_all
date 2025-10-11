package com.dji.sample.df.patrolDf.config.exception;


import com.df.framework.exception.FastException;
import com.df.framework.exception.MyAuthenticationException;
import com.df.framework.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 全局异常处理器
 *
 * @author ruoyi
 */
@Slf4j
@RestControllerAdvice
@Component("patrolGlobalExceptionHandler")
public class GlobalExceptionHandler {

    /**
     * 权限校验失败 如果请求为ajax返回json，普通请求跳转页面
     */
    @ExceptionHandler(MyAuthenticationException.class)
    public Object handleAuthorizationException(HttpServletRequest request, MyAuthenticationException e) {
        log.error(e.getMessage(), e);
        return Result.error("MyAuthenticationException");
    }

    /**
     * 请求方式不支持
     */
    @ExceptionHandler({HttpRequestMethodNotSupportedException.class})
    public Result handleException(HttpServletRequest request, HttpServletResponse response, HttpRequestMethodNotSupportedException e) {
        log.error(e.getMessage(), e);
        response.setStatus(404);
        return Result.error("请求方式出错");
    }

    /**
     * 处理自定义异常
     */
    @ExceptionHandler(FastException.class)
    public Result handleFastException(FastException ex) {
        log.error("", ex);
        return Result.error(ex.getMsg(), ex.getCode());
    }

    /**
     * 重复主键异常
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public Result handleDuplicateKeyException(DuplicateKeyException ex) {
        log.error(ex.getMessage(), ex);
        return Result.error("主键重复，数据库中已存在该记录");
    }

    /**
     * 请求参数JSON格式异常
     *
     * @param ex
     * @return
     */
    @ExceptionHandler(value = HttpMessageNotReadableException.class)
    public Result handleHttpMessageNotReadableException(Exception ex) {
        log.error(ex.getMessage(), ex);
        return Result.error("请求参数值存在错误");
    }

    /**
     * 系统异常
     */
    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e) {
        log.error(e.getMessage(), e);
        return Result.error("请求不合法，拒绝请求");
    }

    /**
     * 拦截未知的运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public Result notFount(RuntimeException ex) {
        log.error(ex.getMessage(), ex);
        return Result.error("请求不合法，拒绝请求");
    }
}
