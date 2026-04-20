package com.jasic.aftersales.system.notify.service;

import com.jasic.aftersales.system.notify.domain.dto.NotifyAssignedEventDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyReadByBizDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyTodoCompleteDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyTodoInvalidateDTO;

/**
 * 工单通知门面。
 *
 * @author Codex
 * @date 2026/04/18
 */
public interface WorkOrderNotifyFacade {

    /**
     * 发布工单派单事件。
     *
     * @param dto 派单事件参数
     */
    void publishAssignedEvent(NotifyAssignedEventDTO dto);

    /**
     * 按业务对象标记已读。
     *
     * @param dto 已读参数
     */
    void markReadByBiz(NotifyReadByBizDTO dto);

    /**
     * 按业务对象和接收人完成待办。
     *
     * @param dto 完成参数
     */
    void completeTodoByBizAndReceiver(NotifyTodoCompleteDTO dto);

    /**
     * 按业务对象失效待办。
     *
     * @param dto 失效参数
     */
    void invalidateTodoByBiz(NotifyTodoInvalidateDTO dto);
}
