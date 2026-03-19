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
            Map<String, Object> data = event.getLogData();
            SysOperLog operLog = new SysOperLog();
            operLog.setTitle((String) data.get("title"));
            operLog.setOperType((Integer) data.get("operType"));
            operLog.setMethod((String) data.get("method"));
            operLog.setRequestMethod((String) data.get("requestMethod"));
            operLog.setRequestUrl((String) data.get("requestUrl"));
            operLog.setRequestParam((String) data.get("requestParam"));
            operLog.setResponseResult((String) data.get("responseResult"));
            operLog.setUserId((Long) data.get("userId"));
            operLog.setCompanyId((Long) data.get("companyId"));
            operLog.setIp((String) data.get("ip"));
            operLog.setStatus((Integer) data.get("status"));
            operLog.setErrorMsg((String) data.get("errorMsg"));
            operLog.setOperTime((LocalDateTime) data.get("operTime"));
            operLog.setCostTime((Long) data.get("costTime"));
            operLogService.save(operLog);
        } catch (Exception e) {
            log.error("异步写入操作日志失败", e);
        }
    }
}
