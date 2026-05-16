package com.jasic.aftersales.system.notify.service.support;

import com.jasic.aftersales.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 通知事件处理器注册表。
 *
 * <p>负责根据 `eventType` 为事件消费服务定位唯一处理器。
 * 该注册表只做处理器路由，不承载任何具体业务规则，
 * 这样后续新增通知场景时只需要增加新的 handler，而不需要继续膨胀消费编排类。</p>
 *
 * @author Codex
 * @date 2026/05/14
 */
@Component
public class NotifyEventHandlerRegistry {

    @Autowired(required = false)
    private List<NotifyEventHandler> notifyEventHandlers = Collections.emptyList();

    /**
     * 根据事件类型获取唯一处理器。
     *
     * <p>如果未找到处理器，说明当前事件类型还没有完成消费实现；
     * 如果找到多个处理器，说明 Spring 装配存在配置错误，
     * 两种情况都必须明确抛错，避免事件被静默吞掉。</p>
     *
     * @param eventType 事件类型编码
     * @return 唯一匹配的处理器
     */
    public NotifyEventHandler getRequiredHandler(String eventType) {
        List<NotifyEventHandler> matchedHandlers = new ArrayList<>();
        // 逐个匹配处理器，确保每种事件类型最终只落到一个消费实现。
        for (NotifyEventHandler notifyEventHandler : notifyEventHandlers) {
            if (notifyEventHandler.supports(eventType)) {
                matchedHandlers.add(notifyEventHandler);
            }
        }
        if (matchedHandlers.isEmpty()) {
            throw new ServiceException("Unsupported notify eventType: " + eventType);
        }
        if (matchedHandlers.size() > 1) {
            throw new ServiceException("Multiple notify event handlers matched eventType: " + eventType);
        }
        return matchedHandlers.get(0);
    }
}
