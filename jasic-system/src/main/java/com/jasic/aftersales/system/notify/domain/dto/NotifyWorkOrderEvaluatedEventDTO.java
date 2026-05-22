package com.jasic.aftersales.system.notify.domain.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * B端客户评价完成通知事件载荷。
 *
 * <p>该事件用于在客户提交评价成功后，固化后续 B 端通知所需的关键快照，
 * 包括工单基础信息、客户展示信息、最终责任维修员信息以及最终处理公司信息。
 * 事件消费阶段不再回查客户评价表拼装模板字段，避免重试时受到业务数据变更影响。</p>
 *
 * @author Zoro
 * @date 2026/05/20
 */
@Data
public class NotifyWorkOrderEvaluatedEventDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工单ID。 */
    private Long workOrderId;

    /** 工单号。 */
    private String orderNo;

    /** 客户ID。 */
    private Long customerId;

    /** 客户展示名称，按“客户姓名 -> 客户手机号 -> 客户”顺序兜底。 */
    private String customerName;

    /** 客户联系电话。 */
    private String customerMobile;

    /** 最终责任维修员ID。 */
    private Long assignedUserId;

    /** 最终责任维修员展示名称，按“真实姓名 -> 用户名 -> 用户ID”顺序兜底。 */
    private String assignedUserName;

    /** 客户评价时工单的最终处理公司ID。 */
    private Long currentAcceptCompanyId;
}
