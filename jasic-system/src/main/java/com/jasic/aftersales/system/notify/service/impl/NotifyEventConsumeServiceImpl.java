package com.jasic.aftersales.system.notify.service.impl;

import cn.hutool.core.util.StrUtil;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyEvent;
import com.jasic.aftersales.system.notify.domain.enums.NotifyEventStatusEnum;
import com.jasic.aftersales.system.notify.service.NotifyEventConsumeService;
import com.jasic.aftersales.system.notify.service.NotifyEventService;
import com.jasic.aftersales.system.notify.service.support.NotifyEventHandler;
import com.jasic.aftersales.system.notify.service.support.NotifyEventHandlerRegistry;
import com.jasic.aftersales.system.notify.support.NotifyConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知事件消费服务。
 *
 * <p>该类只负责消费编排：查询可处理事件、抢占事件、委派给对应 handler，
 * 并在成功或失败后统一回写事件状态。具体 payload 解析和业务动作由 handler 自己负责。</p>
 *
 * @author Codex
 * @date 2026/05/14
 */
@Slf4j
@Service
public class NotifyEventConsumeServiceImpl implements NotifyEventConsumeService {

    @Resource
    private NotifyEventService notifyEventService;

    @Resource
    private NotifyEventHandlerRegistry notifyEventHandlerRegistry;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Value("${jasic.notify.event-retry-max-count:" + NotifyConstants.EVENT_RETRY_MAX_COUNT + "}")
    private int eventRetryMaxCount = NotifyConstants.EVENT_RETRY_MAX_COUNT;

    @Value("${jasic.notify.event-retry-delay-minutes:" + NotifyConstants.EVENT_RETRY_DELAY_MINUTES + "}")
    private long eventRetryDelayMinutes = NotifyConstants.EVENT_RETRY_DELAY_MINUTES;

    /**
     * 自动重试耗尽后的兜底文案。
     */
    private static final String DEAD_RETRY_EXCEEDED_MESSAGE = "通知事件超过最大重试次数，已转入死信";

    /**
     * {@inheritDoc}
     */
    @Override
    public int consumePendingEvents() {
        List<SysNotifyEvent> events = notifyEventService.listConsumableEvents(
                LocalDateTime.now(),
                NotifyConstants.EVENT_CONSUME_BATCH_SIZE
        );
        int successCount = 0;
        for (SysNotifyEvent event : events) {
            if (!notifyEventService.markProcessing(event.getId())) {
                continue;
            }
            try {
                transactionTemplate.execute(status -> {
                    consumeSingleEvent(event.getId());
                    return null;
                });
                successCount++;
            } catch (Exception ex) {
                log.error("Consume notify event failed. eventId={}, eventKey={}", event.getId(), event.getEventKey(), ex);
                // 统一在这里做失败兜底，保证所有 handler 异常都会走同一套重试和死信规则。
                markEventFailed(event.getId(), ex);
            }
        }
        return successCount;
    }

    /**
     * 消费单条已抢占事件。
     *
     * @param eventId 事件ID
     */
    private void consumeSingleEvent(Long eventId) {
        SysNotifyEvent event = getRequiredProcessingEvent(eventId);
        // 统一通过注册表路由处理器，避免消费服务随通知场景增加继续膨胀。
        NotifyEventHandler notifyEventHandler = notifyEventHandlerRegistry.getRequiredHandler(event.getEventType());
        notifyEventHandler.handle(event);
        notifyEventService.markSuccess(event.getId());
    }

    /**
     * 查询并校验处理中事件。
     *
     * @param eventId 事件ID
     * @return 处理中事件
     */
    private SysNotifyEvent getRequiredProcessingEvent(Long eventId) {
        SysNotifyEvent event = notifyEventService.getById(eventId);
        if (event == null) {
            throw new ServiceException("Notify event not found");
        }
        if (!NotifyEventStatusEnum.PROCESSING.getCode().equals(event.getStatus())) {
            throw new ServiceException("Notify event is not processing");
        }
        return event;
    }

    /**
     * 按 Phase 3 规则回写失败状态。
     *
     * <p>未达到上限时进入 FAILED 并安排下一次自动重试；
     * 达到上限时进入 DEAD，后续只能依赖人工重试重新放回消费队列。</p>
     *
     * @param eventId 事件ID
     * @param ex 消费异常
     */
    private void markEventFailed(Long eventId, Exception ex) {
        SysNotifyEvent current = notifyEventService.getById(eventId);
        int currentRetryCount = current == null || current.getRetryCount() == null ? 0 : current.getRetryCount();
        int nextRetryCount = currentRetryCount + 1;
        String errorMessage = buildErrorMessage(ex);
        if (nextRetryCount >= eventRetryMaxCount) {
            // 进入死信前先补齐最后一次失败计数，避免排障时看不到“为何已经耗尽重试预算”。
            notifyEventService.updateRetryInfo(eventId, nextRetryCount, null, errorMessage);
            notifyEventService.markDead(eventId, buildDeadErrorMessage(errorMessage));
            return;
        }
        notifyEventService.markFailed(
                eventId,
                nextRetryCount,
                LocalDateTime.now().plusMinutes(eventRetryDelayMinutes),
                errorMessage
        );
    }

    /**
     * 构建死信原因。
     *
     * @param errorMessage 最近一次失败原因
     * @return 死信原因
     */
    private String buildDeadErrorMessage(String errorMessage) {
        if (StrUtil.isBlank(errorMessage)) {
            return DEAD_RETRY_EXCEEDED_MESSAGE;
        }
        String message = DEAD_RETRY_EXCEEDED_MESSAGE + "：" + errorMessage;
        return message.length() > 500 ? StrUtil.sub(message, 0, 500) : message;
    }

    /**
     * 构建失败信息文本。
     *
     * @param ex 消费异常
     * @return 裁剪后的失败信息
     */
    private String buildErrorMessage(Exception ex) {
        String message = ex == null ? null : StrUtil.trim(ex.getMessage());
        if (StrUtil.isBlank(message) && ex != null) {
            message = ex.getClass().getSimpleName();
        }
        if (message == null) {
            return null;
        }
        return message.length() > 500 ? StrUtil.sub(message, 0, 500) : message;
    }
}
