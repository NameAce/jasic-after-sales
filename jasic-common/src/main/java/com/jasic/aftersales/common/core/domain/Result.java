package com.jasic.aftersales.common.core.domain;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应结果封装
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态码 */
    private String code;

    /** 提示信息 */
    private String msg;

    /** 数据 */
    private T data;

    private static final String SUCCESS_CODE = "00000";
    private static final String SUCCESS_MSG = "操作成功";

    /**
     * 成功返回（无数据）
     *
     * @param <T> 数据类型
     * @return 成功结果
     */
    public static <T> Result<T> ok() {
        return ok(null);
    }

    /**
     * 成功返回（带数据）
     *
     * @param data 返回数据
     * @param <T>  数据类型
     * @return 成功结果
     */
    public static <T> Result<T> ok(T data) {
        Result<T> result = new Result<>();
        result.setCode(SUCCESS_CODE);
        result.setMsg(SUCCESS_MSG);
        result.setData(data);
        return result;
    }

    /**
     * 失败返回
     *
     * @param code 错误码
     * @param msg  错误信息
     * @param <T>  数据类型
     * @return 失败结果
     */
    public static <T> Result<T> fail(String code, String msg) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMsg(msg);
        return result;
    }

    /**
     * 失败返回（默认用户端错误码）
     *
     * @param msg 错误信息
     * @param <T> 数据类型
     * @return 失败结果
     */
    public static <T> Result<T> fail(String msg) {
        return fail("A0001", msg);
    }
}
