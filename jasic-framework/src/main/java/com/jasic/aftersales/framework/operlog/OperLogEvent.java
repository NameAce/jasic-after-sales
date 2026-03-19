package com.jasic.aftersales.framework.operlog;

import org.springframework.context.ApplicationEvent;

import java.util.Map;

/**
 * 操作日志事件，由 OperLogAspect 发布，由 jasic-system 模块的监听器消费
 *
 * @author Zoro
 * @date 2026/03/18
 */
public class OperLogEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    /** 日志数据 */
    private final Map<String, Object> logData;

    /**
     * 构造操作日志事件
     *
     * @param source  事件源
     * @param logData 日志数据
     */
    public OperLogEvent(Object source, Map<String, Object> logData) {
        super(source);
        this.logData = logData;
    }

    /**
     * 获取日志数据
     *
     * @return 日志数据Map
     */
    public Map<String, Object> getLogData() {
        return logData;
    }
}
