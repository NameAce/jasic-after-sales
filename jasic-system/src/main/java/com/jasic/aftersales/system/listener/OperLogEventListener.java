package com.jasic.aftersales.system.listener;

import com.jasic.aftersales.framework.operlog.OperLogEvent;
import com.jasic.aftersales.system.domain.entity.SysOperLog;
import com.jasic.aftersales.system.service.ISysOperLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 操作日志事件监听器，异步写入数据库
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Slf4j
@Component
public class OperLogEventListener {

    /**
     * 系统操作日志服务服务依赖。
     *
     * @param event 参数
     */
    @Resource
    private ISysOperLogService operLogService;

    /**
     * 监听操作日志事件，异步写入数据库
     *
     * @param event 操作日志事件
     */
    @Async
    @EventListener
    public void onOperLogEvent(OperLogEvent event) {
        try {
            // 调用getLogData方法，复用统一能力并保证业务规则一致。
            Map<String, Object> data = event.getLogData();
            // 调用SysOperLog方法，复用统一能力并保证业务规则一致。
            SysOperLog operLog = new SysOperLog();
            // 调用get方法，复用统一能力并保证业务规则一致。
            operLog.setTitle((String) data.get("title"));
            // 调用get方法，复用统一能力并保证业务规则一致。
            operLog.setOperType((Integer) data.get("operType"));
            // 调用get方法，复用统一能力并保证业务规则一致。
            operLog.setMethod((String) data.get("method"));
            // 调用get方法，复用统一能力并保证业务规则一致。
            operLog.setRequestMethod((String) data.get("requestMethod"));
            // 调用get方法，复用统一能力并保证业务规则一致。
            operLog.setRequestUrl((String) data.get("requestUrl"));
            // 调用get方法，复用统一能力并保证业务规则一致。
            operLog.setRequestParam((String) data.get("requestParam"));
            // 调用get方法，复用统一能力并保证业务规则一致。
            operLog.setResponseResult((String) data.get("responseResult"));
            // 调用get方法，复用统一能力并保证业务规则一致。
            operLog.setUserId((Long) data.get("userId"));
            // 调用get方法，复用统一能力并保证业务规则一致。
            operLog.setCompanyId((Long) data.get("companyId"));
            // 调用get方法，复用统一能力并保证业务规则一致。
            operLog.setIp((String) data.get("ip"));
            // 调用get方法，复用统一能力并保证业务规则一致。
            operLog.setStatus((Integer) data.get("status"));
            // 调用get方法，复用统一能力并保证业务规则一致。
            operLog.setErrorMsg((String) data.get("errorMsg"));
            // 调用get方法，复用统一能力并保证业务规则一致。
            operLog.setOperTime((LocalDateTime) data.get("operTime"));
            // 调用get方法，复用统一能力并保证业务规则一致。
            operLog.setCostTime((Long) data.get("costTime"));
            // 调用save方法，复用统一能力并保证业务规则一致。
            operLogService.save(operLog);
        } catch (Exception e) {
            // 调用error方法，复用统一能力并保证业务规则一致。
            log.error("异步写入操作日志失败", e);
        }
    }
}


