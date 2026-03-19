package com.jasic.aftersales.framework.operlog;

import cn.hutool.json.JSONUtil;
import com.jasic.aftersales.common.annotation.OperLog;
import com.jasic.aftersales.framework.security.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 操作日志 AOP 切面
 * <p>
 * 拦截 @OperLog 注解标记的方法，构建日志数据后通过 Spring 事件发布，
 * 由 jasic-system 模块的 OperLogEventListener 异步写入数据库。
 * </p>
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Slf4j
@Aspect
@Component
public class OperLogAspect {

    private static final ThreadLocal<Long> START_TIME = new ThreadLocal<>();

    @Resource
    private ApplicationEventPublisher eventPublisher;

    /**
     * 方法执行前记录开始时间
     *
     * @param joinPoint 切入点
     * @param operLog   操作日志注解
     */
    @Before("@annotation(operLog)")
    public void doBefore(JoinPoint joinPoint, OperLog operLog) {
        START_TIME.set(System.currentTimeMillis());
    }

    /**
     * 方法正常返回后记录日志
     *
     * @param joinPoint 切入点
     * @param operLog   操作日志注解
     * @param result    返回结果
     */
    @AfterReturning(pointcut = "@annotation(operLog)", returning = "result")
    public void doAfterReturning(JoinPoint joinPoint, OperLog operLog, Object result) {
        handleLog(joinPoint, operLog, null, result);
    }

    /**
     * 方法抛出异常后记录日志
     *
     * @param joinPoint 切入点
     * @param operLog   操作日志注解
     * @param e         异常
     */
    @AfterThrowing(pointcut = "@annotation(operLog)", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, OperLog operLog, Exception e) {
        handleLog(joinPoint, operLog, e, null);
    }

    /**
     * 处理日志记录，构建日志数据并发布事件
     *
     * @param joinPoint 切入点
     * @param operLog   注解
     * @param e         异常（正常返回时为null）
     * @param result    返回结果（异常时为null）
     */
    private void handleLog(JoinPoint joinPoint, OperLog operLog, Exception e, Object result) {
        try {
            long costTime = System.currentTimeMillis() - START_TIME.get();
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            String className = joinPoint.getTarget().getClass().getName();
            String methodName = method.getName();

            Map<String, Object> logData = new HashMap<>(16);
            logData.put("title", operLog.title());
            logData.put("operType", operLog.operType().getCode());
            logData.put("method", className + "." + methodName);
            logData.put("costTime", costTime);
            logData.put("operTime", LocalDateTime.now());
            logData.put("status", e == null ? 1 : 0);

            if (e != null) {
                String errorMsg = e.getMessage();
                logData.put("errorMsg", errorMsg != null && errorMsg.length() > 2000 ? errorMsg.substring(0, 2000) : errorMsg);
            }

            if (operLog.isSaveRequestData()) {
                Object[] args = joinPoint.getArgs();
                String requestParam = JSONUtil.toJsonStr(args);
                logData.put("requestParam", requestParam.length() > 2000 ? requestParam.substring(0, 2000) : requestParam);
            }

            if (operLog.isSaveResponseData() && result != null) {
                String responseResult = JSONUtil.toJsonStr(result);
                logData.put("responseResult", responseResult.length() > 2000 ? responseResult.substring(0, 2000) : responseResult);
            }

            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                logData.put("requestUrl", request.getRequestURI());
                logData.put("requestMethod", request.getMethod());
                logData.put("ip", getIpAddress(request));
            }

            try {
                logData.put("userId", SecurityContext.getCurrentUserId());
                logData.put("companyId", SecurityContext.getCurrentCompanyId());
            } catch (Exception ignored) {
                // 未登录场景（如登录接口本身）
            }

            eventPublisher.publishEvent(new OperLogEvent(this, logData));
        } catch (Exception ex) {
            log.error("记录操作日志异常", ex);
        } finally {
            START_TIME.remove();
        }
    }

    /**
     * 获取客户端IP地址
     *
     * @param request HTTP请求
     * @return IP地址
     */
    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
