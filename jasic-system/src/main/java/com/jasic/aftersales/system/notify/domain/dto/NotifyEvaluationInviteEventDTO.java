package com.jasic.aftersales.system.notify.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Work order evaluation invite event payload.
 *
 * @author Codex
 * @date 2026/04/21
 */
@Data
public class NotifyEvaluationInviteEventDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long workOrderId;

    private String orderNo;

    private Long customerId;

    private String customerMobile;

    private String customerOpenid;

    private Long companyId;

    private String companyName;

    private String companyPhone;

    private LocalDateTime closedTime;
}
