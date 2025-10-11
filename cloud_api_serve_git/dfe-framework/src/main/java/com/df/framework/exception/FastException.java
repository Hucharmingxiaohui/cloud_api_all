package com.df.framework.exception;

/**
 * 自定义异常
 *
 * @author lyc
 **/
public class FastException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private String msg = "";
    private Integer code = 500;

    public FastException() {
        super();
    }

    public FastException(String msg) {
        super(msg);
        this.msg = msg;
    }

    public FastException(String msg, Throwable e) {
        super(msg, e);
        this.msg = msg;
    }

    public FastException(Integer code, String msg) {
        super(msg);
        this.msg = msg;
        this.code = code;
    }

    public FastException(Integer code, String msg, Throwable e) {
        super(msg, e);
        this.msg = msg;
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

}
