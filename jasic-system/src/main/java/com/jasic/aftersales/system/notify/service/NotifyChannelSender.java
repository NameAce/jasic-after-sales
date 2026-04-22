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

    boolean supports(String channelType);

    NotifyChannelSendResult send(NotifyChannelSendContext context);
}
