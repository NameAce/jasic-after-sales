package com.jasic.aftersales.common.exception;

import lombok.Getter;

/**
 * 业务异常
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Getter
public class ServiceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 错误码 */
    private final String code;

    /**
     * 构造业务异常（默认用户端错误码）
     *
     * @param message 错误信息
     */
    public ServiceException(String message) {
        this("A0001", message);
    }

    /**
     * 构造业务异常
     *
     * @param code    错误码
     * @param message 错误信息
     */
    public ServiceException(String code, String message) {
        super(message);
        this.code = code;
    }
}
