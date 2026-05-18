package com.jasic.aftersales.system.notify.service.support;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.notify.domain.dto.NotifyWorkOrderAcceptedEventDTO;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyEvent;
import com.jasic.aftersales.system.notify.domain.enums.NotifyEventTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyReceiverTypeEnum;
import com.jasic.aftersales.system.notify.support.NotifyEventExecutionContext;
import com.jasic.aftersales.system.notify.support.NotifyReceiverSnapshot;
import com.jasic.aftersales.system.notify.support.NotifySceneCode;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * C 端接单成功提醒事件处理器。
 *
 * @author Codex
 * @date 2026/05/16
 */
@Component
public class WorkOrderAcceptedNotifyEventHandler implements NotifyEventHandler {

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supports(String eventType) {
        return NotifyEventTypeEnum.WORK_ORDER_ACCEPTED.getCode().equals(eventType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NotifyEventExecutionContext buildExecutionContext(SysNotifyEvent event) {
        NotifyWorkOrderAcceptedEventDTO payload = parsePayload(event);

        NotifyEventExecutionContext context = new NotifyEventExecutionContext();
        context.setSceneCode(resolveSceneCode(event));
        context.setReceiverType(NotifyReceiverTypeEnum.CUSTOMER.getCode());
        context.setReceiverId(payload.getCustomerId());
        context.setReceiverName(String.valueOf(payload.getCustomerId()));
        context.setReceiverAddress(StrUtil.trimToNull(payload.getCustomerOpenid()));
        context.addReceiverSnapshot(NotifyReceiverSnapshot.of(
                NotifyReceiverTypeEnum.CUSTOMER.getCode(),
                payload.getCustomerId(),
                null,
                String.valueOf(payload.getCustomerId()),
                StrUtil.trimToNull(payload.getCustomerOpenid())
        ));
        context.setTemplateVariables(buildTemplateVariables(payload));
        context.setMessageExtJson(null);
        return context;
    }

    private NotifyWorkOrderAcceptedEventDTO parsePayload(SysNotifyEvent event) {
        if (StrUtil.isBlank(event.getPayloadJson())) {
            throw new ServiceException("工单接单成功提醒事件载荷不能为空");
        }
        NotifyWorkOrderAcceptedEventDTO payload;
        try {
            payload = JSONUtil.toBean(event.getPayloadJson(), NotifyWorkOrderAcceptedEventDTO.class);
        } catch (Exception ex) {
            throw new ServiceException("工单接单成功提醒事件载荷解析失败");
        }
        if (payload == null) {
            throw new ServiceException("工单接单成功提醒事件载荷解析结果不能为空");
        }
        if (payload.getWorkOrderId() == null) {
            throw new ServiceException("工单接单成功提醒事件缺少工单ID");
        }
        if (payload.getCustomerId() == null) {
            throw new ServiceException("工单接单成功提醒事件缺少客户ID");
        }
        if (!Objects.equals(payload.getWorkOrderId(), event.getBizId())) {
            throw new ServiceException("工单接单成功提醒事件工单ID与事件主表不一致");
        }
        return payload;
    }

    private String resolveSceneCode(SysNotifyEvent event) {
        return StrUtil.blankToDefault(event.getSceneCode(), NotifySceneCode.WORK_ORDER_ACCEPTED.getCode());
    }

    private Map<String, Object> buildTemplateVariables(NotifyWorkOrderAcceptedEventDTO payload) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("workOrderId", payload.getWorkOrderId());
        variables.put("orderNo", payload.getOrderNo());
        variables.put("companyId", payload.getCompanyId());
        variables.put("companyName", payload.getCompanyName());
        variables.put("companyPhone", payload.getCompanyPhone());
        return variables;
    }
}
