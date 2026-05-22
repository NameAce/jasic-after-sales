package com.jasic.aftersales.system.notify.support;

import lombok.Data;

import java.io.Serializable;

/**
 * 通知接收人快照。
 *
 * <p>同一个通知场景下可能同时存在多个通知目标，
 * 且不同目标对应的接收对象并不相同。
 * 该快照用于在事件消费阶段固化每一类接收人的身份、名称和外部地址，
 * 供后续目标分发时按 `receiverType` 精确选择。</p>
 *
 * @author Zoro
 * @date 2026/05/16
 */
@Data
public class NotifyReceiverSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 接收对象类型。
     */
    private String receiverType;

    /**
     * 接收对象ID。
     */
    private Long receiverId;

    /**
     * 接收公司ID。
     *
     * <p>站内消息和待办等内部目标会依赖该字段做权限归属。</p>
     */
    private Long receiverCompanyId;

    /**
     * 接收对象名称快照。
     */
    private String receiverName;

    /**
     * 外部地址快照。
     *
     * <p>当前小程序订阅消息场景下存储微信 openid。</p>
     */
    private String receiverAddress;

    /**
     * 构建接收人快照。
     *
     * @param receiverType 接收对象类型
     * @param receiverId 接收对象ID
     * @param receiverCompanyId 接收公司ID
     * @param receiverName 接收对象名称
     * @param receiverAddress 外部地址
     * @return 接收人快照
     */
    public static NotifyReceiverSnapshot of(String receiverType, Long receiverId, Long receiverCompanyId,
                                            String receiverName, String receiverAddress) {
        NotifyReceiverSnapshot snapshot = new NotifyReceiverSnapshot();
        snapshot.setReceiverType(receiverType);
        snapshot.setReceiverId(receiverId);
        snapshot.setReceiverCompanyId(receiverCompanyId);
        snapshot.setReceiverName(receiverName);
        snapshot.setReceiverAddress(receiverAddress);
        return snapshot;
    }
}
