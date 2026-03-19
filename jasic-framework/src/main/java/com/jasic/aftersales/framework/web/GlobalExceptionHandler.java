package com.jasic.aftersales.framework.web;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import com.jasic.aftersales.common.core.domain.Result;
import com.jasic.aftersales.common.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;

/**
 * 全局异常处理器
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理未登录异常
     *
     * @param e       异常
     * @param request 请求
     * @return 错误响应
     */
    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLoginException(NotLoginException e, HttpServletRequest request) {
        log.warn("请求地址 [{}]，未登录：{}", request.getRequestURI(), e.getMessage());
        return Result.fail(ResultCode.NOT_LOGIN, "未登录或登录已过期，请重新登录");
    }

    /**
     * 处理无权限异常
     *
     * @param e       异常
     * @param request 请求
     * @return 错误响应
     */
    @ExceptionHandler(NotPermissionException.class)
    public Result<Void> handleNotPermissionException(NotPermissionException e, HttpServletRequest request) {
        log.warn("请求地址 [{}]，权限不足：{}", request.getRequestURI(), e.getMessage());
        return Result.fail(ResultCode.NOT_PERMISSION, "没有操作权限");
    }

    /**
     * 处理业务异常
     *
     * @param e       异常
     * @param request 请求
     * @return 错误响应
     */
    @ExceptionHandler(ServiceException.class)
    public Result<Void> handleServiceException(ServiceException e, HttpServletRequest request) {
        log.warn("请求地址 [{}]，业务异常：{}", request.getRequestURI(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常（@RequestBody）
     *
     * @param e 异常
     * @return 错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "参数校验失败";
        log.warn("参数校验失败：{}", message);
        return Result.fail(ResultCode.PARAM_ERROR, message);
    }

    /**
     * 处理参数绑定异常（@ModelAttribute）
     *
     * @param e 异常
     * @return 错误响应
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "参数校验失败";
        log.warn("参数绑定失败：{}", message);
        return Result.fail(ResultCode.PARAM_ERROR, message);
    }

    /**
     * 处理请求方法不支持异常
     *
     * @param e       异常
     * @param request 请求
     * @return 错误响应
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        log.warn("请求地址 [{}]，不支持 [{}] 请求", request.getRequestURI(), e.getMethod());
        return Result.fail(ResultCode.USER_ERROR, "不支持" + e.getMethod() + "请求");
    }

    /**
     * 处理未知异常
     *
     * @param e       异常
     * @param request 请求
     * @return 错误响应
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("请求地址 [{}]，系统异常", request.getRequestURI(), e);
        return Result.fail(ResultCode.SYSTEM_ERROR, "系统内部错误，请联系管理员");
    }
}
