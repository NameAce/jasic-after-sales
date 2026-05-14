package com.jasic.aftersales.system.notify.service;

import com.jasic.aftersales.system.notify.support.NotifyChannelSendContext;
import com.jasic.aftersales.system.notify.support.NotifyChannelSendResult;

/**
 * Notify channel sender abstraction.
 *
 * @author Codex
 * @date 2026/04/21
 */
public interface NotifyChannelSender {

    /**
     * supports。
     *
     * @param channelType 参数
     * @return 处理结果
     */
    boolean supports(String channelType);

    /**
     * 发送通知渠道发送。
     *
     * @param context 参数
     * @return 处理结果
     */
    NotifyChannelSendResult send(NotifyChannelSendContext context);
}




