package com.jasic.aftersales.system.notify.service;

import com.jasic.aftersales.system.notify.domain.dto.NotifyAssignedEventDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyEvaluationInviteEventDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyReadByBizDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyTodoCompleteDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyTodoInvalidateDTO;

/**
 * Work order notification facade.
 *
 * @author Codex
 * @date 2026/04/18
 */
public interface WorkOrderNotifyFacade {

    /**
     * publishAssigned事件。
     *
     * @param dto 参数
     */
    void publishAssignedEvent(NotifyAssignedEventDTO dto);

    /**
     * publish评价Invite事件。
     *
     * @param dto 参数
     */
    void publishEvaluationInviteEvent(NotifyEvaluationInviteEventDTO dto);

    /**
     * 标记读取By业务。
     *
     * @param dto 参数
     */
    void markReadByBiz(NotifyReadByBizDTO dto);

    /**
     * 完成待办By业务And接收人。
     *
     * @param dto 参数
     */
    void completeTodoByBizAndReceiver(NotifyTodoCompleteDTO dto);

    /**
     * 作废待办By业务。
     *
     * @param dto 参数
     */
    void invalidateTodoByBiz(NotifyTodoInvalidateDTO dto);
}




