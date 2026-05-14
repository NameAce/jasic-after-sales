package com.jasic.aftersales.system.notify.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyEvent;
import com.jasic.aftersales.system.notify.domain.enums.NotifyEventStatusEnum;
import com.jasic.aftersales.system.notify.domain.query.NotifyEventQuery;
import com.jasic.aftersales.system.notify.mapper.SysNotifyEventMapper;
import com.jasic.aftersales.system.notify.service.NotifyEventService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 通知事件 Service 实现。
 *
 * @author Codex
 * @date 2026/04/18
 */
@Service
public class NotifyEventServiceImpl implements NotifyEventService {

    /**
     * ?????
     *
     * @param notifyEvent ??
     * @return ????
     */
    @Resource
    private SysNotifyEventMapper sysNotifyEventMapper;

    @Override
    public Long createEvent(SysNotifyEvent notifyEvent) {
        sysNotifyEventMapper.insert(notifyEvent);
        return notifyEvent.getId();
    }

    /**
     * ??By Id?
     *
     * @param id ??ID
     * @return ????
     */
    @Override
    public SysNotifyEvent getById(Long id) {
        return sysNotifyEventMapper.selectById(id);
    }

    /**
     * ??By Event Key?
     *
     * @param eventKey ??
     * @return ????
     */
    @Override
    public SysNotifyEvent getByEventKey(String eventKey) {
        if (StrUtil.isBlank(eventKey)) {
            return null;
        }
        // ????????????????????????
        LambdaQueryWrapper<SysNotifyEvent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysNotifyEvent::getEventKey, eventKey);
        wrapper.last("limit 1");
        return sysNotifyEventMapper.selectOne(wrapper);
    }

    /**
     * ???????
     *
     * @param now ??
     * @param limit ??
     * @return ????
     */
    @Override
    public List<SysNotifyEvent> listConsumableEvents(LocalDateTime now, Integer limit) {
        LocalDateTime targetTime = now == null ? LocalDateTime.now() : now;
        // ????????????????????????
        LambdaQueryWrapper<SysNotifyEvent> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(condition -> condition
                .eq(SysNotifyEvent::getStatus, NotifyEventStatusEnum.NEW.getCode())
                .or(failed -> failed.eq(SysNotifyEvent::getStatus, NotifyEventStatusEnum.FAILED.getCode())
                        .and(retry -> retry.isNull(SysNotifyEvent::getNextRetryTime)
                                .or()
                                .le(SysNotifyEvent::getNextRetryTime, targetTime))));
        wrapper.orderByAsc(SysNotifyEvent::getId);
        if (limit != null && limit > 0) {
            wrapper.last("limit " + limit);
        }
        return sysNotifyEventMapper.selectList(wrapper);
    }

    /**
     * ???????
     *
     * @param query ????
     * @return ????
     */
    @Override
    public List<SysNotifyEvent> listByQuery(NotifyEventQuery query) {
        if (query == null) {
            return Collections.emptyList();
        }
        // ????????????????????????
        LambdaQueryWrapper<SysNotifyEvent> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(query.getEventKey())) {
            wrapper.eq(SysNotifyEvent::getEventKey, query.getEventKey());
        }
        if (StrUtil.isNotBlank(query.getEventType())) {
            wrapper.eq(SysNotifyEvent::getEventType, query.getEventType());
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
     * ?????
     *
     * @param eventId event ID
     * @param status ??
     */
    @Override
    public void updateStatus(Long eventId, String status) {
        // ????????????????????????
        SysNotifyEvent notifyEvent = new SysNotifyEvent();
        notifyEvent.setId(eventId);
        notifyEvent.setStatus(status);
        sysNotifyEventMapper.updateById(notifyEvent);
    }

    /**
     * ?? markProcessing ?????
     *
     * @param eventId event ID
     * @return true ??????
     */
    @Override
    public boolean markProcessing(Long eventId) {
        // ????????????????????????
        LambdaUpdateWrapper<SysNotifyEvent> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysNotifyEvent::getId, eventId)
                .and(condition -> condition
                        .eq(SysNotifyEvent::getStatus, NotifyEventStatusEnum.NEW.getCode())
                        .or()
                        .eq(SysNotifyEvent::getStatus, NotifyEventStatusEnum.FAILED.getCode()))
                .set(SysNotifyEvent::getStatus, NotifyEventStatusEnum.PROCESSING.getCode())
                .set(SysNotifyEvent::getNextRetryTime, null)
                .set(SysNotifyEvent::getErrorMessage, null);
        return sysNotifyEventMapper.update(null, wrapper) > 0;
    }

    /**
     * ?? markSuccess ?????
     *
     * @param eventId event ID
     */
    @Override
    public void markSuccess(Long eventId) {
        // ????????????????????????
        LambdaUpdateWrapper<SysNotifyEvent> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysNotifyEvent::getId, eventId)
                .eq(SysNotifyEvent::getStatus, NotifyEventStatusEnum.PROCESSING.getCode())
                .set(SysNotifyEvent::getStatus, NotifyEventStatusEnum.SUCCESS.getCode())
                .set(SysNotifyEvent::getNextRetryTime, null)
                .set(SysNotifyEvent::getErrorMessage, null);
        sysNotifyEventMapper.update(null, wrapper);
    }

    /**
     * ?? markFailed ?????
     *
     * @param eventId event ID
     * @param retryCount ??
     * @param nextRetryTime ??
     * @param errorMessage ??
     */
    @Override
    public void markFailed(Long eventId, Integer retryCount, LocalDateTime nextRetryTime, String errorMessage) {
        // ????????????????????????
        LambdaUpdateWrapper<SysNotifyEvent> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysNotifyEvent::getId, eventId)
                .set(SysNotifyEvent::getStatus, NotifyEventStatusEnum.FAILED.getCode())
                .set(SysNotifyEvent::getRetryCount, retryCount)
                .set(SysNotifyEvent::getNextRetryTime, nextRetryTime)
                .set(SysNotifyEvent::getErrorMessage, errorMessage);
        sysNotifyEventMapper.update(null, wrapper);
    }

    /**
     * ?????
     *
     * @param eventId event ID
     * @param retryCount ??
     * @param nextRetryTime ??
     * @param errorMessage ??
     */
    @Override
    public void updateRetryInfo(Long eventId, Integer retryCount, LocalDateTime nextRetryTime, String errorMessage) {
        // ????????????????????????
        SysNotifyEvent notifyEvent = new SysNotifyEvent();
        notifyEvent.setId(eventId);
        notifyEvent.setRetryCount(retryCount);
        notifyEvent.setNextRetryTime(nextRetryTime);
        notifyEvent.setErrorMessage(errorMessage);
        sysNotifyEventMapper.updateById(notifyEvent);
    }
}
