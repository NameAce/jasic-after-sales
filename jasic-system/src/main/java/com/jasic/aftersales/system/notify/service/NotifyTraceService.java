package com.jasic.aftersales.system.notify.service;

import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.system.notify.domain.query.NotifyTraceQuery;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTraceDispatchDetailVO;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTraceEventDetailVO;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTracePageVO;

/**
 * 通知记录排障服务。
 *
 * @author Codex
 * @date 2026/05/14
 */
public interface NotifyTraceService {

    /**
     * 分页查询通知记录。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<NotifyTracePageVO> listPage(NotifyTraceQuery query);

    /**
     * 查询通知事件详情。
     *
     * @param eventId 事件ID
     * @return 事件详情
     */
    NotifyTraceEventDetailVO getEventDetail(Long eventId);

    /**
     * 查询通知分发详情。
     *
     * @param dispatchId 分发任务ID
     * @return 分发详情
     */
    NotifyTraceDispatchDetailVO getDispatchDetail(Long dispatchId);

    /**
     * 人工重试事件。
     *
     * @param eventId 事件ID
     */
    void retryEvent(Long eventId);

    /**
     * 人工重试分发任务。
     *
     * @param dispatchId 分发任务ID
     */
    void retryDispatch(Long dispatchId);

    /**
     * 人工标记事件不再处理。
     *
     * @param eventId 事件ID
     * @param reason 处理原因
     */
    void markEventDead(Long eventId, String reason);

    /**
     * 人工标记分发任务不再处理。
     *
     * @param dispatchId 分发任务ID
     * @param reason 处理原因
     */
    void markDispatchDead(Long dispatchId, String reason);
}
