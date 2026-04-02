package com.jasic.aftersales.system.domain.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 工单通知接收人信息
 *
 * @author Codex
 * @date 2026/04/02
 */
@Data
public class WorkOrderNotifyReceiverInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 微信 openid */
    private String openid;

    /** 失败原因 */
    private String failReason;
}
