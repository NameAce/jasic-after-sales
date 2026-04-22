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

    void publishAssignedEvent(NotifyAssignedEventDTO dto);

    void publishEvaluationInviteEvent(NotifyEvaluationInviteEventDTO dto);

    void markReadByBiz(NotifyReadByBizDTO dto);

    void completeTodoByBizAndReceiver(NotifyTodoCompleteDTO dto);

    void invalidateTodoByBiz(NotifyTodoInvalidateDTO dto);
}
