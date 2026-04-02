package com.jasic.aftersales.system.service;

import com.jasic.aftersales.system.domain.model.WorkOrderNotifyReceiverInfo;

/**
 * 工单通知接收人解析器
 *
 * @author Codex
 * @date 2026/04/02
 */
public interface WorkOrderNotifyReceiverResolver {

    /**
     * 是否支持当前接收人类型
     *
     * @param receiverType 接收人类型
     * @return true 表示支持
     */
    boolean supports(String receiverType);

    /**
     * 解析接收人微信信息
     *
     * @param receiverId 接收人ID
     * @return 接收人信息
     */
    WorkOrderNotifyReceiverInfo resolve(Long receiverId);
}
