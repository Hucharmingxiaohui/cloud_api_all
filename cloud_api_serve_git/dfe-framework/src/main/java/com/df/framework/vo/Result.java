package com.df.framework.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一返回数据
 *
 * @author Created by lyc on 2021/11/04
 */
@Data
public class Result<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 返回码
     */
    private Integer code;
    /**
     * 提示信息
     */
    private String msg;
    /**
     * 返回数据
     */
    private T data;

    /**
     * 初始化Result对象
     *
     * @param code 返回码
     * @param msg  提示信息
     * @param data 返回数据
     */
    public Result(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /**
     * 响应结果枚举类
     */
    public enum Type {
        SUCCESS(0, "请求成功"),
        ERROR(500, "请求失败"),
        WARN(600, "请求告警");

        private final int value;
        private final String name;

        Type(int value, String name) {
            this.value = value;
            this.name = name;
        }
    }

    // 返回成功结果，code默认，msg默认，data=null
    public static Result success() {
        return new Result(Type.SUCCESS.value, Type.SUCCESS.name, null);
    }

    // 返回成功结果，code默认，msg默认, data!={}
    public static Result success(Object data) {
        return new Result(Type.SUCCESS.value, Type.SUCCESS.name, data);
    }

    //返回成功结果, code默认, msg自定义，data=null
    public static Result success(String msg) {
        return new Result(Type.SUCCESS.value, msg, null);
    }

    // 返回成功结果, code默认，msg自定义，data!=null
    public static Result success(Object data, String msg) {
        return new Result(Type.SUCCESS.value, msg, data);
    }

    // 返回告警结果, code默认, msg自定义，data=null
    public static Result warn(String msg) {
        return new Result(Type.WARN.value, msg, null);
    }

    // 返回告警结果, code默认, msg自定义，data=null
    public static Result warn(String msg, Integer code) {
        return new Result(code, msg, null);
    }

    // 返回失败结果, code默认, msg自定义，data=null
    public static Result error(String msg) {
        return new Result(Type.ERROR.value, msg, null);
    }

    //返回失败结果code自定义, msg自定义，data=null
    public static Result error(String msg, Integer code) {
        return new Result(code, msg, null);
    }
}