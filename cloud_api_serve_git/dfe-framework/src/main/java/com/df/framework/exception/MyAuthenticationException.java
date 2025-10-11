package com.df.framework.exception;


/**
 * @author GaoYi
 * @className MyAuthenticationException
 * @date 2022/5/8 23:11
 */
public class MyAuthenticationException extends Exception {

    private static final long serialVersionUID = -1574668724136378727L;

    private String message;

    public MyAuthenticationException(String message) {
        super(message);
        this.message = message;
    }
}
