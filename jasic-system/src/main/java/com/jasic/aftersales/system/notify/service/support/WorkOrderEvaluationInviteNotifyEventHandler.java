package com.jasic.aftersales.system.notify.service.support;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.notify.domain.dto.NotifyEvaluationInviteEventDTO;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyEvent;
import com.jasic.aftersales.system.notify.domain.enums.NotifyEventTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyReceiverTypeEnum;
import com.jasic.aftersales.system.notify.support.NotifyEventExecutionContext;
import com.jasic.aftersales.system.notify.support.NotifyReceiverSnapshot;
import com.jasic.aftersales.system.notify.support.NotifySceneCode;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 客户评价邀请通知事件处理器。
 *
 * <p>该处理器只负责把 `WORK_ORDER_EVALUATION_INVITE` 事件解析成统一执行上下文，
 * 包括客户接收对象、openid 快照和模板变量快照。
 * 外部分发表创建、跳过结果回写和 sender 真实发送由统一链路负责。</p>
 *
 * @author Zoro
 * @date 2026/05/16
 */
@Component
public class WorkOrderEvaluationInviteNotifyEventHandler implements NotifyEventHandler {

    /** 评价通知完成时间统一快照成完整时间字符串，避免分发重试时被序列化成时间戳。 */
    private static final DateTimeFormatter CLOSED_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supports(String eventType) {
        return NotifyEventTypeEnum.WORK_ORDER_EVALUATION_INVITE.getCode().equals(eventType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NotifyEventExecutionContext buildExecutionContext(SysNotifyEvent event) {
        NotifyEvaluationInviteEventDTO payload = parseEvaluationInvitePayload(event);

        NotifyEventExecutionContext context = new NotifyEventExecutionContext();
        context.setSceneCode(resolveSceneCode(event));
        context.setReceiverType(NotifyReceiverTypeEnum.CUSTOMER.getCode());
        context.setReceiverId(payload.getCustomerId());
        context.setReceiverName(resolveReceiverName(payload));
        context.setReceiverAddress(StrUtil.trimToNull(payload.getCustomerOpenid()));
        context.addReceiverSnapshot(NotifyReceiverSnapshot.of(
                NotifyReceiverTypeEnum.CUSTOMER.getCode(),
                payload.getCustomerId(),
                null,
                resolveReceiverName(payload),
                StrUtil.trimToNull(payload.getCustomerOpenid())
        ));
        context.setTemplateVariables(buildEvaluationVariables(payload));
        context.setMessageExtJson(null);
        return context;
    }

    /**
     * 解析并校验评价邀请事件快照。
     *
     * @param event 通知事件
     * @return 评价邀请快照
     */
    private NotifyEvaluationInviteEventDTO parseEvaluationInvitePayload(SysNotifyEvent event) {
        if (StrUtil.isBlank(event.getPayloadJson())) {
            throw new ServiceException("客户评价邀请通知事件载荷不能为空");
        }
        NotifyEvaluationInviteEventDTO payload;
        try {
            payload = JSONUtil.toBean(event.getPayloadJson(), NotifyEvaluationInviteEventDTO.class);
        } catch (Exception ex) {
            throw new ServiceException("客户评价邀请通知事件载荷解析失败");
        }
        if (payload == null) {
            throw new ServiceException("客户评价邀请通知事件载荷解析结果不能为空");
        }
        if (payload.getWorkOrderId() == null) {
            throw new ServiceException("客户评价邀请通知事件缺少工单ID");
        }
        if (payload.getCustomerId() == null) {
            throw new ServiceException("客户评价邀请通知事件缺少客户ID");
        }
        if (!Objects.equals(payload.getWorkOrderId(), event.getBizId())) {
            throw new ServiceException("客户评价邀请通知事件工单ID与事件主表不一致");
        }
        return payload;
    }

    /**
     * 解析通知场景编码。
     *
     * @param event 通知事件
     * @return 场景编码
     */
    private String resolveSceneCode(SysNotifyEvent event) {
        return StrUtil.blankToDefault(event.getSceneCode(), NotifySceneCode.WORK_ORDER_EVALUATION_INVITE.getCode());
    }

    /**
     * 构建接收人名称快照。
     *
     * <p>评价邀请当前没有独立的客户姓名字段，
     * 因此优先展示手机号，缺失时回退客户ID，保证排障记录仍有可识别接收对象。</p>
     *
     * @param payload 评价邀请快照
     * @return 接收人名称
     */
    private String resolveReceiverName(NotifyEvaluationInviteEventDTO payload) {
        String mobile = StrUtil.trim(payload.getCustomerMobile());
        return StrUtil.isNotBlank(mobile) ? mobile : String.valueOf(payload.getCustomerId());
    }

    /**
     * 组装评价邀请模板变量。
     *
     * @param payload 评价邀请快照
     * @return 模板变量快照
     */
    private Map<String, Object> buildEvaluationVariables(NotifyEvaluationInviteEventDTO payload) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("workOrderId", payload.getWorkOrderId());
        variables.put("orderNo", payload.getOrderNo());
        variables.put("customerId", payload.getCustomerId());
        variables.put("customerMobile", payload.getCustomerMobile());
        variables.put("customerOpenid", payload.getCustomerOpenid());
        variables.put("companyId", payload.getCompanyId());
        variables.put("companyName", payload.getCompanyName());
        // 评价邀请模板中的联系电话统一解释为服务网点对外联系电话，
        // 这里要把业务层已经按统一规则兜底后的电话快照写入变量，避免模板字段为空。
        variables.put("companyPhone", payload.getCompanyPhone());
        // 评价邀请的完成时间会跨越 dispatch payload 快照和自动重试链路，
        // 这里直接固化成模板最终展示值，避免 LocalDateTime 在反序列化后退化成时间戳。
        variables.put("closedTime", formatClosedTime(payload.getClosedTime()));
        return variables;
    }

    /**
     * 把工单关闭时间转换成模板变量最终展示值。
     *
     * @param closedTime 工单关闭时间
     * @return 模板可直接渲染的时间字符串；缺失时返回 null
     */
    private String formatClosedTime(LocalDateTime closedTime) {
        if (closedTime == null) {
            return null;
        }
        return closedTime.format(CLOSED_TIME_FORMATTER);
    }
}
