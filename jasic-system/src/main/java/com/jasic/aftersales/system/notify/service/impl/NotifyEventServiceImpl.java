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
     * 系统通知事件Mapper数据访问接口。
     *
     * @param notifyEvent 参数
     * @return 处理结果
     */
    @Resource
    private SysNotifyEventMapper sysNotifyEventMapper;

    /**
     * 执行createEvent相关新增业务。
     *
     * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
     * @param notifyEvent 参数
     * @return 处理结果
     */
    @Override
    public Long createEvent(SysNotifyEvent notifyEvent) {
        // 调用insert方法，复用统一能力并保证业务规则一致。
        sysNotifyEventMapper.insert(notifyEvent);
        return notifyEvent.getId();
    }

    /**
     * 根据ID查询通知事件详情。
     *
     * @return 处理结果
     */
    @Override
    public SysNotifyEvent getById(Long id) {
        return sysNotifyEventMapper.selectById(id);
    }

    /**
     * 获取By事件Key。
     *
     * @param eventKey 参数
     * @return 处理结果
     */
    @Override
    public SysNotifyEvent getByEventKey(String eventKey) {
        if (StrUtil.isBlank(eventKey)) {
            return null;
        }
        // 说明：执行该步骤以保证业务流程正确。
        LambdaQueryWrapper<SysNotifyEvent> wrapper = new LambdaQueryWrapper<>();
        // 调用eq方法，复用统一能力并保证业务规则一致。
        wrapper.eq(SysNotifyEvent::getEventKey, eventKey);
        // 调用last方法，复用统一能力并保证业务规则一致。
        wrapper.last("limit 1");
        return sysNotifyEventMapper.selectOne(wrapper);
    }

    /**
     * 分页查询ConsumableEvents列表。
     *
     * @param now 参数
     * @param limit 参数
     * @return 处理结果
     */
    @Override
    public List<SysNotifyEvent> listConsumableEvents(LocalDateTime now, Integer limit) {
        // 调用now方法，复用统一能力并保证业务规则一致。
        LocalDateTime targetTime = now == null ? LocalDateTime.now() : now;
        // 说明：执行该步骤以保证业务流程正确。
        LambdaQueryWrapper<SysNotifyEvent> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(condition -> condition
                .eq(SysNotifyEvent::getStatus, NotifyEventStatusEnum.NEW.getCode())
                .or(failed -> failed.eq(SysNotifyEvent::getStatus, NotifyEventStatusEnum.FAILED.getCode())
                        .and(retry -> retry.isNull(SysNotifyEvent::getNextRetryTime)
                                .or()
                                // 调用le方法，复用统一能力并保证业务规则一致。
                                .le(SysNotifyEvent::getNextRetryTime, targetTime))));
        // 调用orderByAsc方法，复用统一能力并保证业务规则一致。
        wrapper.orderByAsc(SysNotifyEvent::getId);
        if (limit != null && limit > 0) {
            // 调用last方法，复用统一能力并保证业务规则一致。
            wrapper.last("limit " + limit);
        }
        return sysNotifyEventMapper.selectList(wrapper);
    }

    /**
     * 分页查询By查询列表。
     *
     * @param query 参数
     * @return 处理结果
     */
    @Override
    public List<SysNotifyEvent> listByQuery(NotifyEventQuery query) {
        if (query == null) {
            return Collections.emptyList();
        }
        // 说明：执行该步骤以保证业务流程正确。
        LambdaQueryWrapper<SysNotifyEvent> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(query.getEventKey())) {
            // 调用getEventKey方法，复用统一能力并保证业务规则一致。
            wrapper.eq(SysNotifyEvent::getEventKey, query.getEventKey());
        }
        if (StrUtil.isNotBlank(query.getEventType())) {
            // 调用getEventType方法，复用统一能力并保证业务规则一致。
            wrapper.eq(SysNotifyEvent::getEventType, query.getEventType());
        }
        if (StrUtil.isNotBlank(query.getBizType())) {
            // 调用getBizType方法，复用统一能力并保证业务规则一致。
            wrapper.eq(SysNotifyEvent::getBizType, query.getBizType());
        }
        if (query.getBizId() != null) {
            // 调用getBizId方法，复用统一能力并保证业务规则一致。
            wrapper.eq(SysNotifyEvent::getBizId, query.getBizId());
        }
        if (query.getReceiverId() != null) {
            // 调用getReceiverId方法，复用统一能力并保证业务规则一致。
            wrapper.eq(SysNotifyEvent::getReceiverId, query.getReceiverId());
        }
        if (StrUtil.isNotBlank(query.getStatus())) {
            // 调用getStatus方法，复用统一能力并保证业务规则一致。
            wrapper.eq(SysNotifyEvent::getStatus, query.getStatus());
        }
        // 调用orderByDesc方法，复用统一能力并保证业务规则一致。
        wrapper.orderByDesc(SysNotifyEvent::getId);
        return sysNotifyEventMapper.selectList(wrapper);
    }

    /**
     * 更新状态。
     *
     * @param eventId event ID
     * @param status 参数
     */
    @Override
    public void updateStatus(Long eventId, String status) {
        // 说明：执行该步骤以保证业务流程正确。
        SysNotifyEvent notifyEvent = new SysNotifyEvent();
        // 调用setId方法，复用统一能力并保证业务规则一致。
        notifyEvent.setId(eventId);
        // 调用setStatus方法，复用统一能力并保证业务规则一致。
        notifyEvent.setStatus(status);
        // 调用updateById方法，复用统一能力并保证业务规则一致。
        sysNotifyEventMapper.updateById(notifyEvent);
    }

    /**
     * markProcessing。
     *
     * @param eventId event ID
     */
    @Override
    public boolean markProcessing(Long eventId) {
        // 说明：执行该步骤以保证业务流程正确。
        LambdaUpdateWrapper<SysNotifyEvent> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysNotifyEvent::getId, eventId)
                .and(condition -> condition
                        .eq(SysNotifyEvent::getStatus, NotifyEventStatusEnum.NEW.getCode())
                        .or()
                        .eq(SysNotifyEvent::getStatus, NotifyEventStatusEnum.FAILED.getCode()))
                .set(SysNotifyEvent::getStatus, NotifyEventStatusEnum.PROCESSING.getCode())
                .set(SysNotifyEvent::getNextRetryTime, null)
                // 调用set方法，复用统一能力并保证业务规则一致。
                .set(SysNotifyEvent::getErrorMessage, null);
        return sysNotifyEventMapper.update(null, wrapper) > 0;
    }

    /**
     * markSuccess。
     *
     * @param eventId event ID
     */
    @Override
    public void markSuccess(Long eventId) {
        // 说明：执行该步骤以保证业务流程正确。
        LambdaUpdateWrapper<SysNotifyEvent> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysNotifyEvent::getId, eventId)
                .eq(SysNotifyEvent::getStatus, NotifyEventStatusEnum.PROCESSING.getCode())
                .set(SysNotifyEvent::getStatus, NotifyEventStatusEnum.SUCCESS.getCode())
                .set(SysNotifyEvent::getNextRetryTime, null)
                // 调用set方法，复用统一能力并保证业务规则一致。
                .set(SysNotifyEvent::getErrorMessage, null);
        // 调用update方法，复用统一能力并保证业务规则一致。
        sysNotifyEventMapper.update(null, wrapper);
    }

    /**
     * markFailed。
     *
     * @param eventId event ID
     * @param retryCount 参数
     * @param nextRetryTime 参数
     * @param errorMessage 参数
     */
    @Override
    public void markFailed(Long eventId, Integer retryCount, LocalDateTime nextRetryTime, String errorMessage) {
        // 说明：执行该步骤以保证业务流程正确。
        LambdaUpdateWrapper<SysNotifyEvent> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(SysNotifyEvent::getId, eventId)
                .set(SysNotifyEvent::getStatus, NotifyEventStatusEnum.FAILED.getCode())
                .set(SysNotifyEvent::getRetryCount, retryCount)
                .set(SysNotifyEvent::getNextRetryTime, nextRetryTime)
                // 调用set方法，复用统一能力并保证业务规则一致。
                .set(SysNotifyEvent::getErrorMessage, errorMessage);
        // 调用update方法，复用统一能力并保证业务规则一致。
        sysNotifyEventMapper.update(null, wrapper);
    }

    /**
     * 更新RetryInfo。
     *
     * @param eventId event ID
     * @param retryCount 参数
     * @param nextRetryTime 参数
     * @param errorMessage 参数
     */
    @Override
    public void updateRetryInfo(Long eventId, Integer retryCount, LocalDateTime nextRetryTime, String errorMessage) {
        // 说明：执行该步骤以保证业务流程正确。
        SysNotifyEvent notifyEvent = new SysNotifyEvent();
        // 调用setId方法，复用统一能力并保证业务规则一致。
        notifyEvent.setId(eventId);
        // 调用setRetryCount方法，复用统一能力并保证业务规则一致。
        notifyEvent.setRetryCount(retryCount);
        // 调用setNextRetryTime方法，复用统一能力并保证业务规则一致。
        notifyEvent.setNextRetryTime(nextRetryTime);
        // 调用setErrorMessage方法，复用统一能力并保证业务规则一致。
        notifyEvent.setErrorMessage(errorMessage);
        // 调用updateById方法，复用统一能力并保证业务规则一致。
        sysNotifyEventMapper.updateById(notifyEvent);
    }
}


