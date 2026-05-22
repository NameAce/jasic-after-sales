package com.jasic.aftersales.system.notify.support;

import com.jasic.aftersales.system.notify.domain.entity.SysNotifyDispatch;
import lombok.Data;

import java.io.Serializable;

/**
 * Notify channel send context.
 *
 * @author Zoro
 * @date 2026/04/21
 */
@Data
public class NotifyChannelSendContext implements Serializable {

    private static final long serialVersionUID = 1L;

    /**dispatch 字段，用于当前类内部业务处理。*/
    private SysNotifyDispatch dispatch;

    /**payload 字段，用于当前类内部业务处理。*/
    private NotifyDispatchPayload payload;
}
