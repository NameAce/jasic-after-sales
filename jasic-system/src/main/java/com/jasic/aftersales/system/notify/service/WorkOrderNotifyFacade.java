package com.jasic.aftersales.system.notify.service;

import com.jasic.aftersales.system.notify.domain.dto.NotifyAssignedEventDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyEvaluationInviteEventDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyReadByBizDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyTodoCompleteDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyTodoInvalidateDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyWorkOrderAcceptEventDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyWorkOrderAcceptedEventDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyWorkOrderTransferInEventDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyWorkOrderTransferNoticeEventDTO;

/**
 * Work order notification facade.
 *
 * @author Codex
 * @date 2026/04/18
 */
public interface WorkOrderNotifyFacade {

    /**
     * 发布 B 端接单通知事件。
     *
     * @param dto 事件参数
     */
    void publishAcceptEvent(NotifyWorkOrderAcceptEventDTO dto);

    /**
     * 发布 B 端工单转入通知事件。
     *
     * @param dto 事件参数
     */
    void publishTransferInEvent(NotifyWorkOrderTransferInEventDTO dto);

    /**
     * publishAssigned事件。
     *
     * @param dto 参数
     */
    void publishAssignedEvent(NotifyAssignedEventDTO dto);

    /**
     * 发布 C 端接单成功提醒事件。
     *
     * @param dto 事件参数
     */
    void publishAcceptedEvent(NotifyWorkOrderAcceptedEventDTO dto);

    /**
     * 发布 C 端网点转单通知事件。
     *
     * @param dto 事件参数
     */
    void publishTransferNoticeEvent(NotifyWorkOrderTransferNoticeEventDTO dto);

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




