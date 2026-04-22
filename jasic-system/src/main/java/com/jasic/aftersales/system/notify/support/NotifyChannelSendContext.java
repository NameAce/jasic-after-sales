package com.jasic.aftersales.system.notify.support;

import com.jasic.aftersales.system.notify.domain.entity.SysNotifyDispatch;
import lombok.Data;

import java.io.Serializable;

/**
 * Notify channel send context.
 *
 * @author Codex
 * @date 2026/04/21
 */
@Data
public class NotifyChannelSendContext implements Serializable {

    private static final long serialVersionUID = 1L;

    private SysNotifyDispatch dispatch;

    private NotifyDispatchPayload payload;
}
