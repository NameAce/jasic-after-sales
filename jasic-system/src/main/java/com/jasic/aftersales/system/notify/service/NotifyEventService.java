package com.jasic.aftersales.system.notify.service;

import com.jasic.aftersales.system.notify.domain.entity.SysNotifyEvent;
import com.jasic.aftersales.system.notify.domain.query.NotifyEventQuery;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知事件 Service。
 *
 * @author Codex
 * @date 2026/04/18
 */
public interface NotifyEventService {

    /**
     * 创建通知事件。
     *
     * @param notifyEvent 通知事件
     * @return 主键ID
     */
    Long createEvent(SysNotifyEvent notifyEvent);

    /**
     * 根据ID查询通知事件。
     *
     * @param id 主键ID
     * @return 通知事件
     */
    SysNotifyEvent getById(Long id);

    /**
     * 根据幂等键查询通知事件。
     *
     * @param eventKey 幂等键
     * @return 通知事件
     */
    SysNotifyEvent getByEventKey(String eventKey);

    /**
     * 查询可消费事件列表。
     *
     * @param now   当前时间
     * @param limit 查询上限
     * @return 待消费事件列表
     */
    List<SysNotifyEvent> listConsumableEvents(LocalDateTime now, Integer limit);

    /**
     * 按条件查询通知事件列表。
     *
     * @param query 查询参数
     * @return 通知事件列表
     */
    List<SysNotifyEvent> listByQuery(NotifyEventQuery query);

    /**
     * 更新事件状态。
     *
     * @param eventId 事件ID
     * @param status  事件状态
     */
    void updateStatus(Long eventId, String status);

    /**
     * 抢占事件为处理中状态。
     *
     * @param eventId 事件ID
     * @return 是否抢占成功
     */
    boolean markProcessing(Long eventId);

    /**
     * 标记事件消费成功。
     *
     * @param eventId 事件ID
     */
    void markSuccess(Long eventId);

    /**
     * 标记事件消费失败。
     *
     * @param eventId       事件ID
     * @param retryCount    重试次数
     * @param nextRetryTime 下次重试时间
     * @param errorMessage  最近一次失败信息
     */
    void markFailed(Long eventId, Integer retryCount, LocalDateTime nextRetryTime, String errorMessage);

    /**
     * 更新重试信息。
     *
     * @param eventId       事件ID
     * @param retryCount    重试次数
     * @param nextRetryTime 下次重试时间
     * @param errorMessage  最近一次失败信息
     */
    void updateRetryInfo(Long eventId, Integer retryCount, LocalDateTime nextRetryTime, String errorMessage);
}
