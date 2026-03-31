package com.jasic.aftersales.system.service;

import com.jasic.aftersales.system.domain.entity.WorkOrder;
import com.jasic.aftersales.system.domain.entity.WorkOrderNotifyEvent;
import com.jasic.aftersales.system.mapper.WorkOrderNotifyEventMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 工单通知事件服务
 *
 * @author Codex
 * @date 2026/03/26
 */
@Service
public class WorkOrderNotifyEventService {

    @Resource
    private WorkOrderNotifyEventMapper workOrderNotifyEventMapper;

    /**
     * 记录维修完成通知事件
     *
     * @param workOrder 工单主表
     * @param summary   维修摘要
     */
    public void recordRepairFinished(WorkOrder workOrder, String summary) {
        saveEvent(workOrder, workOrder.getCurrentAcceptCompanyId(), "REPAIR_FINISHED_NOTICE", "REPAIR_FINISH",
                "CUSTOMER", workOrder.getCustomerId(), "维修完成通知",
                buildContent("工单已维修完成", workOrder, summary));
    }

    /**
     * 记录客户评价邀请通知事件
     *
     * @param workOrder 工单主表
     */
    public void recordEvaluationInvite(WorkOrder workOrder) {
        saveEvent(workOrder, workOrder.getCurrentAcceptCompanyId(), "EVALUATION_INVITE_NOTICE", "CLOSE",
                "CUSTOMER", workOrder.getCustomerId(), "客户满意度评价通知",
                buildContent("工单已关闭，请进行满意度评价", workOrder, null));
    }

    /**
     * 记录客户评价结果通知事件
     *
     * @param workOrder 工单主表
     * @param score     评分
     * @param content   评价内容
     */
    public void recordCustomerEvaluated(WorkOrder workOrder, Integer score, String content) {
        saveEvent(workOrder, workOrder.getCurrentAcceptCompanyId(), "CUSTOMER_EVALUATED_NOTICE", "EVALUATE",
                "COMPANY", workOrder.getCurrentAcceptCompanyId(), "客户评价结果通知",
                buildContent("客户已完成评价，评分：" + score, workOrder, content));
    }

    private void saveEvent(WorkOrder workOrder, Long companyId, String eventType, String triggerNode,
                           String receiverType, Long receiverId, String title, String content) {
        if (workOrder == null || receiverId == null) {
            return;
        }
        WorkOrderNotifyEvent event = new WorkOrderNotifyEvent();
        event.setWorkOrderId(workOrder.getId());
        event.setCompanyId(companyId);
        event.setEventType(eventType);
        event.setTriggerNode(triggerNode);
        event.setReceiverType(receiverType);
        event.setReceiverId(receiverId);
        event.setTitleSnapshot(title);
        event.setContentSnapshot(content);
        event.setSendStatus("PENDING");
        workOrderNotifyEventMapper.insert(event);
    }

    private String buildContent(String prefix, WorkOrder workOrder, String detail) {
        StringBuilder builder = new StringBuilder(prefix)
                .append("，工单号：")
                .append(workOrder.getOrderNo());
        if (detail != null && !detail.trim().isEmpty()) {
            builder.append("，说明：").append(detail);
        }
        return builder.toString();
    }
}
