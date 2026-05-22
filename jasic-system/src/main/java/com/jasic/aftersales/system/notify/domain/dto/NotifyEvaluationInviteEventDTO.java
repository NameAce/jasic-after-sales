package com.jasic.aftersales.system.notify.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Work order evaluation invite event payload.
 *
 * @author Zoro
 * @date 2026/04/21
 */
@Data
public class NotifyEvaluationInviteEventDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**工单ID，由接口调用方提交并参与服务层业务校验。*/
    private Long workOrderId;

    /**工单号，由接口调用方提交并参与服务层业务校验。*/
    private String orderNo;

    /**客户ID，由接口调用方提交并参与服务层业务校验。*/
    private Long customerId;

    /**customerMobile 字段，由接口调用方提交并参与服务层业务校验。*/
    private String customerMobile;

    /**customerOpenid 字段，由接口调用方提交并参与服务层业务校验。*/
    private String customerOpenid;

    /**公司ID，由接口调用方提交并参与服务层业务校验。*/
    private Long companyId;

    /**companyName 字段，由接口调用方提交并参与服务层业务校验。*/
    private String companyName;

    /**companyPhone 字段，由接口调用方提交并参与服务层业务校验。*/
    private String companyPhone;

    /**closedTime 字段，由接口调用方提交并参与服务层业务校验。*/
    private LocalDateTime closedTime;
}
