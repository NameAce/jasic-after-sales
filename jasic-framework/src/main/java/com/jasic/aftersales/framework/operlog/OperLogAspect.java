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
        // 调用currentTimeMillis方法，复用统一能力并保证业务规则一致。
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
        // 调用handleLog方法，复用统一能力并保证业务规则一致。
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
        // 调用handleLog方法，复用统一能力并保证业务规则一致。
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
            // 调用get方法，复用统一能力并保证业务规则一致。
            long costTime = System.currentTimeMillis() - START_TIME.get();
            // 调用getSignature方法，复用统一能力并保证业务规则一致。
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            // 调用getMethod方法，复用统一能力并保证业务规则一致。
            Method method = signature.getMethod();
            // 调用getName方法，复用统一能力并保证业务规则一致。
            String className = joinPoint.getTarget().getClass().getName();
            // 调用getName方法，复用统一能力并保证业务规则一致。
            String methodName = method.getName();

            Map<String, Object> logData = new HashMap<>(16);
            // 调用title方法，复用统一能力并保证业务规则一致。
            logData.put("title", operLog.title());
            // 调用getCode方法，复用统一能力并保证业务规则一致。
            logData.put("operType", operLog.operType().getCode());
            // 调用put方法，复用统一能力并保证业务规则一致。
            logData.put("method", className + "." + methodName);
            // 调用put方法，复用统一能力并保证业务规则一致。
            logData.put("costTime", costTime);
            // 调用now方法，复用统一能力并保证业务规则一致。
            logData.put("operTime", LocalDateTime.now());
            // 调用put方法，复用统一能力并保证业务规则一致。
            logData.put("status", e == null ? 1 : 0);

            if (e != null) {
                // 调用getMessage方法，复用统一能力并保证业务规则一致。
                String errorMsg = e.getMessage();
                // 调用substring方法，复用统一能力并保证业务规则一致。
                logData.put("errorMsg", errorMsg != null && errorMsg.length() > 2000 ? errorMsg.substring(0, 2000) : errorMsg);
            }

            if (operLog.isSaveRequestData()) {
                // 调用getArgs方法，复用统一能力并保证业务规则一致。
                Object[] args = joinPoint.getArgs();
                // 调用toJsonStr方法，复用统一能力并保证业务规则一致。
                String requestParam = JSONUtil.toJsonStr(args);
                // 调用substring方法，复用统一能力并保证业务规则一致。
                logData.put("requestParam", requestParam.length() > 2000 ? requestParam.substring(0, 2000) : requestParam);
            }

            if (operLog.isSaveResponseData() && result != null) {
                // 调用toJsonStr方法，复用统一能力并保证业务规则一致。
                String responseResult = JSONUtil.toJsonStr(result);
                // 调用substring方法，复用统一能力并保证业务规则一致。
                logData.put("responseResult", responseResult.length() > 2000 ? responseResult.substring(0, 2000) : responseResult);
            }

            // 调用getRequestAttributes方法，复用统一能力并保证业务规则一致。
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                // 调用getRequest方法，复用统一能力并保证业务规则一致。
                HttpServletRequest request = attributes.getRequest();
                // 调用getRequestURI方法，复用统一能力并保证业务规则一致。
                logData.put("requestUrl", request.getRequestURI());
                // 调用getMethod方法，复用统一能力并保证业务规则一致。
                logData.put("requestMethod", request.getMethod());
                // 调用getIpAddress方法，复用统一能力并保证业务规则一致。
                logData.put("ip", getIpAddress(request));
            }

            try {
                // 调用getCurrentUserId方法，复用统一能力并保证业务规则一致。
                logData.put("userId", SecurityContext.getCurrentUserId());
                // 调用getCurrentCompanyId方法，复用统一能力并保证业务规则一致。
                logData.put("companyId", SecurityContext.getCurrentCompanyId());
            } catch (Exception ignored) {
                // 未登录场景（如登录接口本身）
            }

            // 调用OperLogEvent方法，复用统一能力并保证业务规则一致。
            eventPublisher.publishEvent(new OperLogEvent(this, logData));
        } catch (Exception ex) {
            // 调用error方法，复用统一能力并保证业务规则一致。
            log.error("记录操作日志异常", ex);
        } finally {
            // 调用remove方法，复用统一能力并保证业务规则一致。
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
        // 调用getHeader方法，复用统一能力并保证业务规则一致。
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            // 调用getHeader方法，复用统一能力并保证业务规则一致。
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            // 调用getRemoteAddr方法，复用统一能力并保证业务规则一致。
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            // 调用trim方法，复用统一能力并保证业务规则一致。
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
