package com.jasic.aftersales.system.notify.service.support;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.notify.domain.dto.NotifyWorkOrderAcceptEventDTO;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyEvent;
import com.jasic.aftersales.system.notify.domain.enums.NotifyEventTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyReceiverTypeEnum;
import com.jasic.aftersales.system.notify.support.NotifyEventExecutionContext;
import com.jasic.aftersales.system.notify.support.NotifySceneCode;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * B 端待派单通知事件处理器。
 *
 * @author Zoro
 * @date 2026/05/16
 */
@Component
public class WorkOrderAcceptNotifyEventHandler implements NotifyEventHandler {

    /**workOrderAssignUserResolver 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private WorkOrderAssignUserResolver workOrderAssignUserResolver;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supports(String eventType) {
        return NotifyEventTypeEnum.WORK_ORDER_ACCEPT.getCode().equals(eventType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NotifyEventExecutionContext buildExecutionContext(SysNotifyEvent event) {
        NotifyWorkOrderAcceptEventDTO payload = parsePayload(event);

        NotifyEventExecutionContext context = new NotifyEventExecutionContext();
        context.setSceneCode(resolveSceneCode(event));
        context.setReceiverType(NotifyReceiverTypeEnum.ASSIGN_USER.getCode());
        context.addReceiverSnapshots(
                NotifyReceiverTypeEnum.ASSIGN_USER.getCode(),
                workOrderAssignUserResolver.resolveAssignUserSnapshots(payload.getCurrentAcceptCompanyId())
        );
        context.setTemplateVariables(buildTemplateVariables(payload));
        context.setMessageExtJson(null);
        return context;
    }

    /**
     * 解析并校验事件载荷。
     *
     * @param event 通知事件
     * @return 事件载荷
     */
    private NotifyWorkOrderAcceptEventDTO parsePayload(SysNotifyEvent event) {
        if (StrUtil.isBlank(event.getPayloadJson())) {
            throw new ServiceException("工单接单通知事件载荷不能为空");
        }
        NotifyWorkOrderAcceptEventDTO payload;
        try {
            payload = JSONUtil.toBean(event.getPayloadJson(), NotifyWorkOrderAcceptEventDTO.class);
        } catch (Exception ex) {
            throw new ServiceException("工单接单通知事件载荷解析失败");
        }
        if (payload == null) {
            throw new ServiceException("工单接单通知事件载荷解析结果不能为空");
        }
        if (payload.getWorkOrderId() == null) {
            throw new ServiceException("工单接单通知事件缺少工单ID");
        }
        if (payload.getCurrentAcceptCompanyId() == null) {
            throw new ServiceException("工单接单通知事件缺少当前承接网点ID");
        }
        if (!Objects.equals(payload.getWorkOrderId(), event.getBizId())) {
            throw new ServiceException("工单接单通知事件工单ID与事件主表不一致");
        }
        return payload;
    }

    /**
     * 解析场景编码。
     *
     * @param event 通知事件
     * @return 场景编码
     */
    private String resolveSceneCode(SysNotifyEvent event) {
        return StrUtil.blankToDefault(event.getSceneCode(), NotifySceneCode.WORK_ORDER_ACCEPT.getCode());
    }

    /**
     * 组装模板变量快照。
     *
     * @param payload 事件载荷
     * @return 模板变量
     */
    private Map<String, Object> buildTemplateVariables(NotifyWorkOrderAcceptEventDTO payload) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("workOrderId", payload.getWorkOrderId());
        variables.put("orderNo", payload.getOrderNo());
        variables.put("currentAcceptCompanyId", payload.getCurrentAcceptCompanyId());
        variables.put("currentAcceptCompanyName", payload.getCurrentAcceptCompanyName());
        variables.put("customerName", payload.getCustomerName());
        // 待派单通知模板已经允许直接展示客户联系电话，
        // 这里需要把建单时固化下来的手机号快照一并透传给模板渲染层。
        variables.put("customerMobile", payload.getCustomerMobile());
        return variables;
    }
}
