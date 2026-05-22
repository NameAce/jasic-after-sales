package com.jasic.aftersales.system.notify.service;

import com.jasic.aftersales.system.notify.support.NotifyChannelSendContext;
import com.jasic.aftersales.system.notify.support.NotifyChannelSendResult;

/**
 * Notify channel sender abstraction.
 *
 * @author Zoro
 * @date 2026/04/21
 */
public interface NotifyChannelSender {

    /**
     * supports。
     *
     * @param channelType channelType，当前业务处理所需的输入值。
     * @return 业务处理结果
     */
    boolean supports(String channelType);

    /**
     * 发送通知渠道发送。
     *
     * @param context 上下文对象，承载当前操作人、公司和数据范围。
     * @return 业务处理结果
     */
    NotifyChannelSendResult send(NotifyChannelSendContext context);
}




