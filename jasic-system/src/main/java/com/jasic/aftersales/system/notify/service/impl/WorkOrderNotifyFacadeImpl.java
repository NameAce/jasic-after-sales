package com.jasic.aftersales.system.notify.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.notify.domain.dto.NotifyAssignedEventDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyEvaluationInviteEventDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyReadByBizDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyTodoCompleteDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyTodoInvalidateDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyWorkOrderAcceptEventDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyWorkOrderAcceptedEventDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyWorkOrderEvaluatedEventDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyWorkOrderTransferInEventDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyWorkOrderTransferNoticeEventDTO;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyEvent;
import com.jasic.aftersales.system.notify.domain.enums.NotifyBizTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyEventStatusEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyEventTypeEnum;
import com.jasic.aftersales.system.notify.service.NotifyEventService;
import com.jasic.aftersales.system.notify.service.NotifyMessageService;
import com.jasic.aftersales.system.notify.service.WorkOrderNotifyFacade;
import com.jasic.aftersales.system.notify.support.NotifyConstants;
import com.jasic.aftersales.system.notify.support.NotifySceneCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * Work order notification facade implementation.
 *
 * @author Zoro
 * @date 2026/04/18
 */
@Slf4j
@Service
public class WorkOrderNotifyFacadeImpl implements WorkOrderNotifyFacade {

    /**
     * 通知消息服务服务依赖。
     *
     * @param dto 接口请求参数，承载本次业务操作需要的字段。
     */
    @Resource
    private NotifyMessageService notifyMessageService;

    /**notifyEventService 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private NotifyEventService notifyEventService;

    /**
     * {@inheritDoc}
     */
    @Override
    public void publishAcceptEvent(NotifyWorkOrderAcceptEventDTO dto) {
        validateAcceptEvent(dto);
        String eventKey = buildAcceptEventKey(dto);
        SysNotifyEvent existingEvent = notifyEventService.getByEventKey(eventKey);
        if (existingEvent != null) {
            validateSameAcceptEventSnapshot(existingEvent, dto, eventKey);
            return;
        }
        SysNotifyEvent notifyEvent = buildEvent(
                eventKey,
                NotifyEventTypeEnum.WORK_ORDER_ACCEPT.getCode(),
                NotifySceneCode.WORK_ORDER_ACCEPT.getCode(),
                dto.getWorkOrderId(),
                dto.getOrderNo(),
                null,
                null,
                JSONUtil.toJsonStr(dto)
        );
        createEventSafely(notifyEvent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void publishTransferInEvent(NotifyWorkOrderTransferInEventDTO dto) {
        validateTransferInEvent(dto);
        String eventKey = buildTransferInEventKey(dto);
        if (notifyEventService.getByEventKey(eventKey) != null) {
            return;
        }
        SysNotifyEvent notifyEvent = buildEvent(
                eventKey,
                NotifyEventTypeEnum.WORK_ORDER_TRANSFER_IN.getCode(),
                NotifySceneCode.WORK_ORDER_TRANSFER_IN.getCode(),
                dto.getWorkOrderId(),
                dto.getOrderNo(),
                null,
                null,
                JSONUtil.toJsonStr(dto)
        );
        createEventSafely(notifyEvent);
    }

    /**
     * 处理publishAssignedEvent业务逻辑。
     *
     * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
     * @param dto 接口请求参数，承载本次业务操作需要的字段。
     */
    @Override
    public void publishAssignedEvent(NotifyAssignedEventDTO dto) {
        validateAssignedEvent(dto);
        if (dto.getOldAssignedUserId() != null && dto.getOldAssignedUserId().equals(dto.getNewAssignedUserId())) {
            return;
        }
        String eventKey = buildAssignedEventKey(dto);
        if (notifyEventService.getByEventKey(eventKey) != null) {
            return;
        }
        SysNotifyEvent notifyEvent = buildEvent(
                eventKey,
                NotifyEventTypeEnum.WORK_ORDER_ASSIGNED.getCode(),
                NotifySceneCode.WORK_ORDER_ASSIGNED.getCode(),
                dto.getWorkOrderId(),
                dto.getOrderNo(),
                dto.getOperatorId(),
                dto.getNewAssignedUserId(),
                JSONUtil.toJsonStr(dto)
        );
        createEventSafely(notifyEvent);
    }

    /**
     * publish评价Invite事件。
     *
     * @param dto 接口请求参数，承载本次业务操作需要的字段。
     */
    @Override
    public void publishAcceptedEvent(NotifyWorkOrderAcceptedEventDTO dto) {
        validateAcceptedEvent(dto);
        String eventKey = buildAcceptedEventKey(dto);
        if (notifyEventService.getByEventKey(eventKey) != null) {
            return;
        }
        SysNotifyEvent notifyEvent = buildEvent(
                eventKey,
                NotifyEventTypeEnum.WORK_ORDER_ACCEPTED.getCode(),
                NotifySceneCode.WORK_ORDER_ACCEPTED.getCode(),
                dto.getWorkOrderId(),
                dto.getOrderNo(),
                null,
                dto.getCustomerId(),
                JSONUtil.toJsonStr(dto)
        );
        createEventSafely(notifyEvent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void publishTransferNoticeEvent(NotifyWorkOrderTransferNoticeEventDTO dto) {
        validateTransferNoticeEvent(dto);
        String eventKey = buildTransferNoticeEventKey(dto);
        if (notifyEventService.getByEventKey(eventKey) != null) {
            return;
        }
        SysNotifyEvent notifyEvent = buildEvent(
                eventKey,
                NotifyEventTypeEnum.WORK_ORDER_TRANSFER_NOTICE.getCode(),
                NotifySceneCode.WORK_ORDER_TRANSFER_NOTICE.getCode(),
                dto.getWorkOrderId(),
                dto.getOrderNo(),
                null,
                dto.getCustomerId(),
                JSONUtil.toJsonStr(dto)
        );
        createEventSafely(notifyEvent);
    }

    /**
     * 发布评价邀请事件。
     *
     * @param dto 评价邀请通知参数，包含工单、客户和目标接收人信息。
     */
    @Override
    public void publishEvaluationInviteEvent(NotifyEvaluationInviteEventDTO dto) {
        validateEvaluationInviteEvent(dto);
        String eventKey = buildEvaluationInviteEventKey(dto);
        if (notifyEventService.getByEventKey(eventKey) != null) {
            return;
        }
        SysNotifyEvent notifyEvent = buildEvent(
                eventKey,
                NotifyEventTypeEnum.WORK_ORDER_EVALUATION_INVITE.getCode(),
                NotifySceneCode.WORK_ORDER_EVALUATION_INVITE.getCode(),
                dto.getWorkOrderId(),
                dto.getOrderNo(),
                null,
                dto.getCustomerId(),
                JSONUtil.toJsonStr(dto)
        );
        createEventSafely(notifyEvent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void publishEvaluatedEvent(NotifyWorkOrderEvaluatedEventDTO dto) {
        validateEvaluatedEvent(dto);
        String eventKey = buildEvaluatedEventKey(dto);
        if (notifyEventService.getByEventKey(eventKey) != null) {
            return;
        }
        SysNotifyEvent notifyEvent = buildEvent(
                eventKey,
                NotifyEventTypeEnum.WORK_ORDER_EVALUATED.getCode(),
                NotifySceneCode.WORK_ORDER_EVALUATED.getCode(),
                dto.getWorkOrderId(),
                dto.getOrderNo(),
                dto.getCustomerId(),
                dto.getAssignedUserId(),
                JSONUtil.toJsonStr(dto)
        );
        createEventSafely(notifyEvent);
    }

    /**
     * mark读取By业务。
     *
     * @param dto 接口请求参数，承载本次业务操作需要的字段。
     */
    @Override
    public void markReadByBiz(NotifyReadByBizDTO dto) {
        notifyMessageService.markReadByBiz(dto);
    }

    /**
     * 完成待办By业务And接收人。
     *
     * @param dto 接口请求参数，承载本次业务操作需要的字段。
     */
    @Override
    public void completeTodoByBizAndReceiver(NotifyTodoCompleteDTO dto) {
        notifyMessageService.completeTodoByBizAndReceiver(dto);
    }

    /**
     * 作废待办By业务。
     *
     * @param dto 接口请求参数，承载本次业务操作需要的字段。
     */
    @Override
    public void invalidateTodoByBiz(NotifyTodoInvalidateDTO dto) {
        notifyMessageService.invalidateTodoByBiz(dto);
    }

    /**
     * 构建事件。
     *
     * @param eventKey eventKey，当前业务处理所需的输入值。
     * @param eventType eventType，当前业务处理所需的输入值。
     * @param sceneCode 通知场景编码
     * @param bizNo bizNo，当前业务处理所需的输入值。
     * @param operatorId operator ID
     * @param receiverId receiver ID
     * @param payloadJson payloadJson，当前业务处理所需的输入值。
     * @return 业务处理结果
     */
    private SysNotifyEvent buildEvent(String eventKey, String eventType, String sceneCode, Long bizId, String bizNo,
                                      Long operatorId, Long receiverId, String payloadJson) {
        SysNotifyEvent notifyEvent = new SysNotifyEvent();
        notifyEvent.setEventKey(eventKey);
        notifyEvent.setEventType(eventType);
        // 显式固化 sceneCode，保证事件消费阶段直接按场景查询多个目标，而不是再从 eventType 反推。
        notifyEvent.setSceneCode(sceneCode);
        notifyEvent.setBizType(NotifyBizTypeEnum.WORK_ORDER.getCode());
        notifyEvent.setBizId(bizId);
        notifyEvent.setBizNo(bizNo);
        notifyEvent.setOperatorId(operatorId);
        // 兼容历史库里 `receiver_id NOT NULL` 约束：
        // 对于“只表达业务事实、不依赖单一接收人”的事件，这里写入占位值，
        // 避免代客户建单、转单等主事务因为通知事件落库失败而整体回滚。
        notifyEvent.setReceiverId(receiverId == null
                ? NotifyConstants.EVENT_RECEIVER_ID_PLACEHOLDER
                : receiverId);
        notifyEvent.setPayloadJson(payloadJson);
        notifyEvent.setStatus(NotifyEventStatusEnum.NEW.getCode());
        notifyEvent.setRetryCount(0);
        return notifyEvent;
    }

    /**
     * 创建事件Safely。
     *
     * @param notifyEvent notifyEvent，当前业务处理所需的输入值。
     */
    private void createEventSafely(SysNotifyEvent notifyEvent) {
        try {
            notifyEventService.createEvent(notifyEvent);
        } catch (DuplicateKeyException ignored) {
            // Ignore duplicate inserts for the same event_key.
        }
    }

    /**
     * 校验Assigned事件。
     *
     * @param dto 接口请求参数，承载本次业务操作需要的字段。
     */
    private void validateAcceptEvent(NotifyWorkOrderAcceptEventDTO dto) {
        if (dto == null) {
            throw new ServiceException("Accept event payload cannot be null");
        }
        if (dto.getWorkOrderId() == null) {
            throw new ServiceException("Accept event missing workOrderId");
        }
        if (StrUtil.isBlank(dto.getOrderNo())) {
            throw new ServiceException("Accept event missing orderNo");
        }
        if (dto.getCurrentAcceptCompanyId() == null) {
            throw new ServiceException("Accept event missing currentAcceptCompanyId");
        }
    }

    /**validateTransferInEvent 业务校验，提前阻断非法参数、越权访问或不允许的状态流转。
@param dto 业务请求参数，承载本次操作需要提交的字段。*/
    private void validateTransferInEvent(NotifyWorkOrderTransferInEventDTO dto) {
        if (dto == null) {
            throw new ServiceException("Transfer-in event payload cannot be null");
        }
        if (dto.getWorkOrderId() == null) {
            throw new ServiceException("Transfer-in event missing workOrderId");
        }
        if (StrUtil.isBlank(dto.getOrderNo())) {
            throw new ServiceException("Transfer-in event missing orderNo");
        }
        if (dto.getCurrentAcceptCompanyId() == null) {
            throw new ServiceException("Transfer-in event missing currentAcceptCompanyId");
        }
        if (dto.getTransferCount() == null) {
            throw new ServiceException("Transfer-in event missing transferCount");
        }
    }

    /**validateAssignedEvent 业务校验，提前阻断非法参数、越权访问或不允许的状态流转。
@param dto 业务请求参数，承载本次操作需要提交的字段。*/
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
     * @param dto 接口请求参数，承载本次业务操作需要的字段。
     */
    private void validateAcceptedEvent(NotifyWorkOrderAcceptedEventDTO dto) {
        if (dto == null) {
            throw new ServiceException("Accepted event payload cannot be null");
        }
        if (dto.getWorkOrderId() == null) {
            throw new ServiceException("Accepted event missing workOrderId");
        }
        if (StrUtil.isBlank(dto.getOrderNo())) {
            throw new ServiceException("Accepted event missing orderNo");
        }
        if (dto.getCustomerId() == null) {
            throw new ServiceException("Accepted event missing customerId");
        }
    }

    /**validateTransferNoticeEvent 业务校验，提前阻断非法参数、越权访问或不允许的状态流转。
@param dto 业务请求参数，承载本次操作需要提交的字段。*/
    private void validateTransferNoticeEvent(NotifyWorkOrderTransferNoticeEventDTO dto) {
        if (dto == null) {
            throw new ServiceException("Transfer-notice event payload cannot be null");
        }
        if (dto.getWorkOrderId() == null) {
            throw new ServiceException("Transfer-notice event missing workOrderId");
        }
        if (StrUtil.isBlank(dto.getOrderNo())) {
            throw new ServiceException("Transfer-notice event missing orderNo");
        }
        if (dto.getCustomerId() == null) {
            throw new ServiceException("Transfer-notice event missing customerId");
        }
        if (dto.getTransferCount() == null) {
            throw new ServiceException("Transfer-notice event missing transferCount");
        }
    }

    /**validateEvaluationInviteEvent 业务校验，提前阻断非法参数、越权访问或不允许的状态流转。
@param dto 业务请求参数，承载本次操作需要提交的字段。*/
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
     * 校验 B 端客户评价完成通知事件。
     *
     * @param dto 事件参数
     */
    private void validateEvaluatedEvent(NotifyWorkOrderEvaluatedEventDTO dto) {
        if (dto == null) {
            throw new ServiceException("Evaluated event payload cannot be null");
        }
        if (dto.getWorkOrderId() == null) {
            throw new ServiceException("Evaluated event missing workOrderId");
        }
        if (StrUtil.isBlank(dto.getOrderNo())) {
            throw new ServiceException("Evaluated event missing orderNo");
        }
        if (dto.getAssignedUserId() == null) {
            throw new ServiceException("Evaluated event missing assignedUserId");
        }
    }

    /**
     * 构建Assigned事件Key。
     *
     * @param dto 接口请求参数，承载本次业务操作需要的字段。
     * @return 业务处理结果
     */
    private String buildAcceptEventKey(NotifyWorkOrderAcceptEventDTO dto) {
        return String.format("%s:%s:%s:%s",
                NotifyConstants.EVENT_KEY_PREFIX_WORK_ORDER_ACCEPT,
                dto.getWorkOrderId(),
                StrUtil.trim(dto.getOrderNo()),
                dto.getCurrentAcceptCompanyId()
        );
    }

    /**
     * 校验待派单通知幂等命中的事件是否确实属于同一张工单。
     *
     * <p>待派单事件的幂等键已经包含工单ID、工单编号和当前承接公司。
     * 正常情况下，命中旧事件只能说明同一业务动作被重复触发，可以安全跳过。
     * 如果命中的事件主表和当前工单快照不一致，说明通知运行表存在脏数据或人工写入错误，
     * 此时必须显式失败，避免再次出现“新工单被旧事件吞掉”的静默漏通知。</p>
     *
     * @param existingEvent 已存在的事件
     * @param dto 当前待发布的待派单通知参数
     * @param eventKey 本次命中的幂等键
     */
    private void validateSameAcceptEventSnapshot(SysNotifyEvent existingEvent, NotifyWorkOrderAcceptEventDTO dto,
                                                 String eventKey) {
        boolean sameEvent = Objects.equals(existingEvent.getBizId(), dto.getWorkOrderId())
                && Objects.equals(StrUtil.trim(existingEvent.getBizNo()), StrUtil.trim(dto.getOrderNo()))
                && Objects.equals(existingEvent.getEventType(), NotifyEventTypeEnum.WORK_ORDER_ACCEPT.getCode())
                && Objects.equals(existingEvent.getSceneCode(), NotifySceneCode.WORK_ORDER_ACCEPT.getCode());
        if (sameEvent) {
            return;
        }
        log.error("待派单通知幂等键冲突。eventKey={}, existingEventId={}, existingBizId={}, existingBizNo={}, currentBizId={}, currentBizNo={}",
                eventKey,
                existingEvent.getId(),
                existingEvent.getBizId(),
                existingEvent.getBizNo(),
                dto.getWorkOrderId(),
                dto.getOrderNo());
        throw new ServiceException("待派单通知幂等键冲突，请检查通知运行数据");
    }

    /**buildTransferInEventKey 业务数据，统一收口字段清洗、默认值处理和返回对象组装规则。
@param dto 业务请求参数，承载本次操作需要提交的字段。
@return 处理后的业务结果。*/
    private String buildTransferInEventKey(NotifyWorkOrderTransferInEventDTO dto) {
        return String.format("%s:%s:%s",
                NotifyConstants.EVENT_KEY_PREFIX_WORK_ORDER_TRANSFER_IN,
                dto.getWorkOrderId(),
                dto.getTransferCount()
        );
    }

    /**buildAssignedEventKey 业务数据，统一收口字段清洗、默认值处理和返回对象组装规则。
@param dto 业务请求参数，承载本次操作需要提交的字段。
@return 处理后的业务结果。*/
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
     * @param dto 接口请求参数，承载本次业务操作需要的字段。
     * @return 业务处理结果
     */
    private String buildAcceptedEventKey(NotifyWorkOrderAcceptedEventDTO dto) {
        return String.format("%s:%s",
                NotifyConstants.EVENT_KEY_PREFIX_WORK_ORDER_ACCEPTED,
                dto.getWorkOrderId()
        );
    }

    /**buildTransferNoticeEventKey 业务数据，统一收口字段清洗、默认值处理和返回对象组装规则。
@param dto 业务请求参数，承载本次操作需要提交的字段。
@return 处理后的业务结果。*/
    private String buildTransferNoticeEventKey(NotifyWorkOrderTransferNoticeEventDTO dto) {
        return String.format("%s:%s:%s",
                NotifyConstants.EVENT_KEY_PREFIX_WORK_ORDER_TRANSFER_NOTICE,
                dto.getWorkOrderId(),
                dto.getTransferCount()
        );
    }

    /**buildEvaluationInviteEventKey 业务数据，统一收口字段清洗、默认值处理和返回对象组装规则。
@param dto 业务请求参数，承载本次操作需要提交的字段。
@return 处理后的业务结果。*/
    private String buildEvaluationInviteEventKey(NotifyEvaluationInviteEventDTO dto) {
        return String.format("%s:%s",
                NotifyConstants.EVENT_KEY_PREFIX_WORK_ORDER_EVALUATION_INVITE,
                dto.getWorkOrderId()
        );
    }

    /**
     * 构建 B 端客户评价完成通知事件幂等键。
     *
     * @param dto 事件参数
     * @return 事件幂等键
     */
    private String buildEvaluatedEventKey(NotifyWorkOrderEvaluatedEventDTO dto) {
        return String.format("%s:%s",
                NotifyConstants.EVENT_KEY_PREFIX_WORK_ORDER_EVALUATED,
                dto.getWorkOrderId()
        );
    }
}
