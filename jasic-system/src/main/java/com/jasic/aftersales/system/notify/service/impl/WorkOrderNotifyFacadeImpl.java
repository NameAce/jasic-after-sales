package com.jasic.aftersales.system.notify.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.notify.domain.dto.NotifyAssignedEventDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyEvaluationInviteEventDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyReadByBizDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyTodoCompleteDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyTodoInvalidateDTO;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyEvent;
import com.jasic.aftersales.system.notify.domain.enums.NotifyBizTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyEventStatusEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyEventTypeEnum;
import com.jasic.aftersales.system.notify.service.NotifyEventService;
import com.jasic.aftersales.system.notify.service.NotifyMessageService;
import com.jasic.aftersales.system.notify.service.WorkOrderNotifyFacade;
import com.jasic.aftersales.system.notify.support.NotifyConstants;
import com.jasic.aftersales.system.notify.support.NotifySceneCode;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * Work order notification facade implementation.
 *
 * @author Codex
 * @date 2026/04/18
 */
@Service
public class WorkOrderNotifyFacadeImpl implements WorkOrderNotifyFacade {

    /**
     * 通知消息服务服务依赖。
     *
     * @param dto 参数
     */
    @Resource
    private NotifyMessageService notifyMessageService;

    @Resource
    private NotifyEventService notifyEventService;

    /**
     * 处理publishAssignedEvent业务逻辑。
     *
     * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
     * @param dto 参数
     */
    @Override
    public void publishAssignedEvent(NotifyAssignedEventDTO dto) {
        // 说明：执行该步骤以保证业务流程正确。
        validateAssignedEvent(dto);
        if (dto.getOldAssignedUserId() != null && dto.getOldAssignedUserId().equals(dto.getNewAssignedUserId())) {
            return;
        }
        // 调用buildAssignedEventKey方法，复用统一能力并保证业务规则一致。
        String eventKey = buildAssignedEventKey(dto);
        // 说明：执行该步骤以保证业务流程正确。
        if (notifyEventService.getByEventKey(eventKey) != null) {
            return;
        }
        SysNotifyEvent notifyEvent = buildEvent(
                eventKey,
                NotifyEventTypeEnum.WORK_ORDER_ASSIGNED.getCode(),
                NotifySceneCode.WORK_ORDER_ASSIGNED_TODO.getCode(),
                dto.getWorkOrderId(),
                dto.getOrderNo(),
                dto.getOperatorId(),
                dto.getNewAssignedUserId(),
                JSONUtil.toJsonStr(dto)
        );
        // 调用createEventSafely方法，复用统一能力并保证业务规则一致。
        createEventSafely(notifyEvent);
    }

    /**
     * publish评价Invite事件。
     *
     * @param dto 参数
     */
    @Override
    public void publishEvaluationInviteEvent(NotifyEvaluationInviteEventDTO dto) {
        // 说明：执行该步骤以保证业务流程正确。
        validateEvaluationInviteEvent(dto);
        // 调用buildEvaluationInviteEventKey方法，复用统一能力并保证业务规则一致。
        String eventKey = buildEvaluationInviteEventKey(dto);
        // 说明：执行该步骤以保证业务流程正确。
        if (notifyEventService.getByEventKey(eventKey) != null) {
            return;
        }
        SysNotifyEvent notifyEvent = buildEvent(
                eventKey,
                NotifyEventTypeEnum.WORK_ORDER_EVALUATION_INVITE.getCode(),
                NotifySceneCode.WORK_ORDER_EVALUATION_INVITE_MP_C.getCode(),
                dto.getWorkOrderId(),
                dto.getOrderNo(),
                null,
                dto.getCustomerId(),
                JSONUtil.toJsonStr(dto)
        );
        // 调用createEventSafely方法，复用统一能力并保证业务规则一致。
        createEventSafely(notifyEvent);
    }

    /**
     * mark读取By业务。
     *
     * @param dto 参数
     */
    @Override
    public void markReadByBiz(NotifyReadByBizDTO dto) {
        // 调用markReadByBiz方法，复用统一能力并保证业务规则一致。
        notifyMessageService.markReadByBiz(dto);
    }

    /**
     * 完成待办By业务And接收人。
     *
     * @param dto 参数
     */
    @Override
    public void completeTodoByBizAndReceiver(NotifyTodoCompleteDTO dto) {
        // 调用completeTodoByBizAndReceiver方法，复用统一能力并保证业务规则一致。
        notifyMessageService.completeTodoByBizAndReceiver(dto);
    }

    /**
     * 作废待办By业务。
     *
     * @param dto 参数
     */
    @Override
    public void invalidateTodoByBiz(NotifyTodoInvalidateDTO dto) {
        // 调用invalidateTodoByBiz方法，复用统一能力并保证业务规则一致。
        notifyMessageService.invalidateTodoByBiz(dto);
    }

    /**
     * 构建事件。
     *
     * @param eventKey 参数
     * @param eventType 参数
     * @param sceneCode 通知场景编码
     * @param bizNo 参数
     * @param operatorId operator ID
     * @param receiverId receiver ID
     * @param payloadJson 参数
     * @return 处理结果
     */
    private SysNotifyEvent buildEvent(String eventKey, String eventType, String sceneCode, Long bizId, String bizNo,
                                      Long operatorId, Long receiverId, String payloadJson) {
        // 说明：执行该步骤以保证业务流程正确。
        SysNotifyEvent notifyEvent = new SysNotifyEvent();
        // 调用setEventKey方法，复用统一能力并保证业务规则一致。
        notifyEvent.setEventKey(eventKey);
        // 调用setEventType方法，复用统一能力并保证业务规则一致。
        notifyEvent.setEventType(eventType);
        // 显式固化 sceneCode，保证事件消费阶段直接按场景查询多个目标，而不是再从 eventType 反推。
        notifyEvent.setSceneCode(sceneCode);
        // 调用getCode方法，复用统一能力并保证业务规则一致。
        notifyEvent.setBizType(NotifyBizTypeEnum.WORK_ORDER.getCode());
        // 调用setBizId方法，复用统一能力并保证业务规则一致。
        notifyEvent.setBizId(bizId);
        // 调用setBizNo方法，复用统一能力并保证业务规则一致。
        notifyEvent.setBizNo(bizNo);
        // 调用setOperatorId方法，复用统一能力并保证业务规则一致。
        notifyEvent.setOperatorId(operatorId);
        // 调用setReceiverId方法，复用统一能力并保证业务规则一致。
        notifyEvent.setReceiverId(receiverId);
        // 调用setPayloadJson方法，复用统一能力并保证业务规则一致。
        notifyEvent.setPayloadJson(payloadJson);
        // 调用getCode方法，复用统一能力并保证业务规则一致。
        notifyEvent.setStatus(NotifyEventStatusEnum.NEW.getCode());
        // 调用setRetryCount方法，复用统一能力并保证业务规则一致。
        notifyEvent.setRetryCount(0);
        return notifyEvent;
    }

    /**
     * 创建事件Safely。
     *
     * @param notifyEvent 参数
     */
    private void createEventSafely(SysNotifyEvent notifyEvent) {
        try {
            // 说明：执行该步骤以保证业务流程正确。
            notifyEventService.createEvent(notifyEvent);
        } catch (DuplicateKeyException ignored) {
            // Ignore duplicate inserts for the same event_key.
        }
    }

    /**
     * 校验Assigned事件。
     *
     * @param dto 参数
     */
    private void validateAssignedEvent(NotifyAssignedEventDTO dto) {
        if (dto == null) {
            throw new ServiceException("Assigned event payload cannot be null");
        }
        if (dto.getWorkOrderId() == null) {
            throw new ServiceException("Assigned event missing workOrderId");
        }
        if (StrUtil.isBlank(dto.getOrderNo())) {
            throw new ServiceException("Assigned event missing orderNo");
        }
        if (dto.getNewAssignedUserId() == null) {
            throw new ServiceException("Assigned event missing newAssignedUserId");
        }
        if (dto.getReceiverCompanyId() == null) {
            throw new ServiceException("Assigned event missing receiverCompanyId");
        }
        if (StrUtil.isBlank(dto.getAssignType())) {
            throw new ServiceException("Assigned event missing assignType");
        }
        if (StrUtil.isBlank(dto.getOperationId())) {
            throw new ServiceException("Assigned event missing operationId");
        }
    }

    /**
     * 校验评价Invite事件。
     *
     * @param dto 参数
     */
    private void validateEvaluationInviteEvent(NotifyEvaluationInviteEventDTO dto) {
        if (dto == null) {
            throw new ServiceException("Evaluation invite event payload cannot be null");
        }
        if (dto.getWorkOrderId() == null) {
            throw new ServiceException("Evaluation invite event missing workOrderId");
        }
        if (StrUtil.isBlank(dto.getOrderNo())) {
            throw new ServiceException("Evaluation invite event missing orderNo");
        }
        if (dto.getCustomerId() == null) {
            throw new ServiceException("Evaluation invite event missing customerId");
        }
    }

    /**
     * 构建Assigned事件Key。
     *
     * @param dto 参数
     * @return 处理结果
     */
    private String buildAssignedEventKey(NotifyAssignedEventDTO dto) {
        return String.format("%s:%s:%s:%s",
                NotifyConstants.EVENT_KEY_PREFIX_WORK_ORDER_ASSIGNED,
                dto.getWorkOrderId(),
                dto.getNewAssignedUserId(),
                dto.getOperationId()
        );
    }

    /**
     * 构建评价Invite事件Key。
     *
     * @param dto 参数
     * @return 处理结果
     */
    private String buildEvaluationInviteEventKey(NotifyEvaluationInviteEventDTO dto) {
        return String.format("%s:%s",
                NotifyConstants.EVENT_KEY_PREFIX_WORK_ORDER_EVALUATION_INVITE,
                dto.getWorkOrderId()
        );
    }
}

