package com.jasic.aftersales.system.notify.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyEvent;
import com.jasic.aftersales.system.notify.domain.enums.NotifyEventStatusEnum;
import com.jasic.aftersales.system.notify.domain.query.NotifyEventQuery;
import com.jasic.aftersales.system.notify.mapper.SysNotifyEventMapper;
import com.jasic.aftersales.system.notify.service.NotifyEventService;
import com.jasic.aftersales.system.notify.support.NotifyConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 通知事件服务实现。
 *
 * <p>该实现负责事件状态机落库，包括自动消费抢占、失败重试、死信和超时恢复。
 * 业务 handler 不直接拼接更新 SQL，统一通过这里维护状态边界，避免不同线程写出不一致口径。</p>
 *
 * @author Codex
 * @date 2026/04/18
 */
@Service
public class NotifyEventServiceImpl implements NotifyEventService {

    @Resource
    private SysNotifyEventMapper sysNotifyEventMapper;

    @Value("${jasic.notify.event-retry-max-count:" + NotifyConstants.EVENT_RETRY_MAX_COUNT + "}")
    private int eventRetryMaxCount = NotifyConstants.EVENT_RETRY_MAX_COUNT;

    /**
     * 超时恢复原因文案。
     */
    private static final String TIMEOUT_RECOVER_ERROR_MESSAGE = "通知事件处理超时，系统已恢复为待重试";

    /**
     * 事件进入死信但未传具体原因时的兜底文案。
     */
    private static final String DEFAULT_DEAD_ERROR_MESSAGE = "通知事件进入死信，等待人工处理";

    /**
     * {@inheritDoc}
     */
    @Override
    public Long createEvent(SysNotifyEvent notifyEvent) {
        sysNotifyEventMapper.insert(notifyEvent);
        return notifyEvent.getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SysNotifyEvent getById(Long id) {
        return sysNotifyEventMapper.selectById(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SysNotifyEvent getByEventKey(String eventKey) {
        if (StrUtil.isBlank(eventKey)) {
            return null;
        }
        LambdaQueryWrapper<SysNotifyEvent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotifyEvent::getEventKey, eventKey).last("limit 1");
        return sysNotifyEventMapper.selectOne(wrapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SysNotifyEvent> listConsumableEvents(LocalDateTime now, Integer limit) {
        LocalDateTime targetTime = now == null ? LocalDateTime.now() : now;
        LambdaQueryWrapper<SysNotifyEvent> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(condition -> condition
                        .eq(SysNotifyEvent::getStatus, NotifyEventStatusEnum.NEW.getCode())
                        .or(failed -> failed.eq(SysNotifyEvent::getStatus, NotifyEventStatusEnum.FAILED.getCode())
                                .lt(SysNotifyEvent::getRetryCount, eventRetryMaxCount)
                                .and(next -> next.isNull(SysNotifyEvent::getNextRetryTime)
                                        .or()
                                        .le(SysNotifyEvent::getNextRetryTime, targetTime))))
                .orderByAsc(SysNotifyEvent::getId);
        if (limit != null && limit > 0) {
            wrapper.last("limit " + limit);
        }
        return sysNotifyEventMapper.selectList(wrapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SysNotifyEvent> listTimeoutProcessingEvents(LocalDateTime timeoutBefore, Integer limit) {
        if (timeoutBefore == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<SysNotifyEvent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotifyEvent::getStatus, NotifyEventStatusEnum.PROCESSING.getCode())
                .le(SysNotifyEvent::getProcessingTime, timeoutBefore)
                .orderByAsc(SysNotifyEvent::getId);
        if (limit != null && limit > 0) {
            wrapper.last("limit " + limit);
        }
        return sysNotifyEventMapper.selectList(wrapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SysNotifyEvent> listByQuery(NotifyEventQuery query) {
        if (query == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<SysNotifyEvent> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(query.getEventKey())) {
            wrapper.eq(SysNotifyEvent::getEventKey, query.getEventKey());
        }
        if (StrUtil.isNotBlank(query.getEventType())) {
            wrapper.eq(SysNotifyEvent::getEventType, query.getEventType());
        }
        if (StrUtil.isNotBlank(query.getSceneCode())) {
            wrapper.eq(SysNotifyEvent::getSceneCode, query.getSceneCode());
        }
        if (StrUtil.isNotBlank(query.getBizType())) {
            wrapper.eq(SysNotifyEvent::getBizType, query.getBizType());
        }
        if (query.getBizId() != null) {
            wrapper.eq(SysNotifyEvent::getBizId, query.getBizId());
        }
        if (query.getReceiverId() != null) {
            wrapper.eq(SysNotifyEvent::getReceiverId, query.getReceiverId());
        }
        if (StrUtil.isNotBlank(query.getStatus())) {
            wrapper.eq(SysNotifyEvent::getStatus, query.getStatus());
        }
        wrapper.orderByDesc(SysNotifyEvent::getId);
        return sysNotifyEventMapper.selectList(wrapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateStatus(Long eventId, String status) {
        SysNotifyEvent notifyEvent = new SysNotifyEvent();
        notifyEvent.setId(eventId);
        notifyEvent.setStatus(status);
        sysNotifyEventMapper.updateById(notifyEvent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean markProcessing(Long eventId) {
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<SysNotifyEvent> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysNotifyEvent::getId, eventId)
                .and(condition -> condition
                        .eq(SysNotifyEvent::getStatus, NotifyEventStatusEnum.NEW.getCode())
                        .or(failed -> failed.eq(SysNotifyEvent::getStatus, NotifyEventStatusEnum.FAILED.getCode())
                                .lt(SysNotifyEvent::getRetryCount, eventRetryMaxCount)
                                .and(next -> next.isNull(SysNotifyEvent::getNextRetryTime)
                                        .or()
                                        .le(SysNotifyEvent::getNextRetryTime, now))))
                .set(SysNotifyEvent::getStatus, NotifyEventStatusEnum.PROCESSING.getCode())
                // 进入处理中时必须记录抢占时间，后续超时恢复据此判断是否出现卡死。
                .set(SysNotifyEvent::getProcessingTime, now)
                .set(SysNotifyEvent::getNextRetryTime, null)
                .set(SysNotifyEvent::getErrorMessage, null);
        return sysNotifyEventMapper.update(null, wrapper) > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void markSuccess(Long eventId) {
        LambdaUpdateWrapper<SysNotifyEvent> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysNotifyEvent::getId, eventId)
                .eq(SysNotifyEvent::getStatus, NotifyEventStatusEnum.PROCESSING.getCode())
                .set(SysNotifyEvent::getStatus, NotifyEventStatusEnum.SUCCESS.getCode())
                .set(SysNotifyEvent::getProcessingTime, null)
                .set(SysNotifyEvent::getNextRetryTime, null)
                .set(SysNotifyEvent::getErrorMessage, null);
        sysNotifyEventMapper.update(null, wrapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void markFailed(Long eventId, Integer retryCount, LocalDateTime nextRetryTime, String errorMessage) {
        LambdaUpdateWrapper<SysNotifyEvent> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysNotifyEvent::getId, eventId)
                .eq(SysNotifyEvent::getStatus, NotifyEventStatusEnum.PROCESSING.getCode())
                .set(SysNotifyEvent::getStatus, NotifyEventStatusEnum.FAILED.getCode())
                .set(SysNotifyEvent::getRetryCount, retryCount)
                .set(SysNotifyEvent::getProcessingTime, null)
                .set(SysNotifyEvent::getNextRetryTime, nextRetryTime)
                .set(SysNotifyEvent::getErrorMessage, trimErrorMessage(errorMessage));
        sysNotifyEventMapper.update(null, wrapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateRetryInfo(Long eventId, Integer retryCount, LocalDateTime nextRetryTime, String errorMessage) {
        SysNotifyEvent notifyEvent = new SysNotifyEvent();
        notifyEvent.setId(eventId);
        notifyEvent.setRetryCount(retryCount);
        notifyEvent.setNextRetryTime(nextRetryTime);
        notifyEvent.setErrorMessage(trimErrorMessage(errorMessage));
        sysNotifyEventMapper.updateById(notifyEvent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int recoverTimeoutProcessingEvents(LocalDateTime timeoutBefore) {
        if (timeoutBefore == null) {
            return 0;
        }
        LambdaUpdateWrapper<SysNotifyEvent> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysNotifyEvent::getStatus, NotifyEventStatusEnum.PROCESSING.getCode())
                .le(SysNotifyEvent::getProcessingTime, timeoutBefore)
                // 宕机或线程异常中断后，先恢复为 FAILED，让同一个消费任务后半段重新捞起处理。
                .set(SysNotifyEvent::getStatus, NotifyEventStatusEnum.FAILED.getCode())
                .set(SysNotifyEvent::getProcessingTime, null)
                .set(SysNotifyEvent::getNextRetryTime, null)
                .set(SysNotifyEvent::getErrorMessage, TIMEOUT_RECOVER_ERROR_MESSAGE);
        return sysNotifyEventMapper.update(null, wrapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void markDead(Long eventId, String errorMessage) {
        LambdaUpdateWrapper<SysNotifyEvent> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysNotifyEvent::getId, eventId)
                .and(condition -> condition
                        .eq(SysNotifyEvent::getStatus, NotifyEventStatusEnum.NEW.getCode())
                        .or()
                        .eq(SysNotifyEvent::getStatus, NotifyEventStatusEnum.FAILED.getCode())
                        .or()
                        .eq(SysNotifyEvent::getStatus, NotifyEventStatusEnum.PROCESSING.getCode()))
                // 死信是自动任务的终态，进入后只能等待人工排障或人工重试。
                .set(SysNotifyEvent::getStatus, NotifyEventStatusEnum.DEAD.getCode())
                .set(SysNotifyEvent::getProcessingTime, null)
                .set(SysNotifyEvent::getNextRetryTime, null)
                .set(SysNotifyEvent::getErrorMessage, trimErrorMessage(resolveDeadErrorMessage(errorMessage)));
        sysNotifyEventMapper.update(null, wrapper);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resetForRetry(Long eventId) {
        LambdaUpdateWrapper<SysNotifyEvent> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysNotifyEvent::getId, eventId)
                .and(condition -> condition
                        .eq(SysNotifyEvent::getStatus, NotifyEventStatusEnum.FAILED.getCode())
                        .or()
                        .eq(SysNotifyEvent::getStatus, NotifyEventStatusEnum.DEAD.getCode()))
                // 人工重试需要彻底清空失败上下文，避免旧失败原因污染新一轮处理结果。
                .set(SysNotifyEvent::getStatus, NotifyEventStatusEnum.NEW.getCode())
                .set(SysNotifyEvent::getRetryCount, 0)
                .set(SysNotifyEvent::getProcessingTime, null)
                .set(SysNotifyEvent::getNextRetryTime, null)
                .set(SysNotifyEvent::getErrorMessage, null);
        sysNotifyEventMapper.update(null, wrapper);
    }

    /**
     * 解析死信原因。
     *
     * @param errorMessage 传入原因
     * @return 可落库原因
     */
    private String resolveDeadErrorMessage(String errorMessage) {
        if (StrUtil.isBlank(errorMessage)) {
            return DEFAULT_DEAD_ERROR_MESSAGE;
        }
        return errorMessage;
    }

    /**
     * 裁剪失败原因文本，避免超长报错内容导致落库失败。
     *
     * @param errorMessage 原始原因
     * @return 裁剪后的原因
     */
    private String trimErrorMessage(String errorMessage) {
        if (errorMessage == null) {
            return null;
        }
        String actual = errorMessage.trim();
        if (actual.isEmpty()) {
            return null;
        }
        return actual.length() > 500 ? StrUtil.sub(actual, 0, 500) : actual;
    }
}
