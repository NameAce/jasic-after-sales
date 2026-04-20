package com.jasic.aftersales.system.notify.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.notify.domain.dto.NotifyAssignedEventDTO;
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

    @Resource
    private NotifyMessageService notifyMessageService;

    @Resource
    private NotifyEventService notifyEventService;

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
        SysNotifyEvent notifyEvent = new SysNotifyEvent();
        notifyEvent.setEventKey(eventKey);
        notifyEvent.setEventType(NotifyEventTypeEnum.WORK_ORDER_ASSIGNED.getCode());
        notifyEvent.setBizType(NotifyBizTypeEnum.WORK_ORDER.getCode());
        notifyEvent.setBizId(dto.getWorkOrderId());
        notifyEvent.setBizNo(dto.getOrderNo());
        notifyEvent.setOperatorId(dto.getOperatorId());
        notifyEvent.setReceiverId(dto.getNewAssignedUserId());
        notifyEvent.setPayloadJson(JSONUtil.toJsonStr(dto));
        notifyEvent.setStatus(NotifyEventStatusEnum.NEW.getCode());
        notifyEvent.setRetryCount(0);
        try {
            notifyEventService.createEvent(notifyEvent);
        } catch (DuplicateKeyException ignored) {
            // Ignore duplicate inserts for the same event_key.
        }
    }

    @Override
    public void markReadByBiz(NotifyReadByBizDTO dto) {
        notifyMessageService.markReadByBiz(dto);
    }

    @Override
    public void completeTodoByBizAndReceiver(NotifyTodoCompleteDTO dto) {
        notifyMessageService.completeTodoByBizAndReceiver(dto);
    }

    @Override
    public void invalidateTodoByBiz(NotifyTodoInvalidateDTO dto) {
        notifyMessageService.invalidateTodoByBiz(dto);
    }

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
        if (StrUtil.isBlank(dto.getAssignType())) {
            throw new ServiceException("Assigned event missing assignType");
        }
        if (StrUtil.isBlank(dto.getOperationId())) {
            throw new ServiceException("Assigned event missing operationId");
        }
    }

    private String buildAssignedEventKey(NotifyAssignedEventDTO dto) {
        return String.format("%s:%s:%s:%s",
                NotifyConstants.EVENT_KEY_PREFIX_WORK_ORDER_ASSIGNED,
                dto.getWorkOrderId(),
                dto.getNewAssignedUserId(),
                dto.getOperationId()
        );
    }
}
