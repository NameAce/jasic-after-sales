package com.jasic.aftersales.customer.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * C端工单列表视图
 *
 * @author Codex
 * @date 2026/03/26
 */
@Data
public class CustomerWorkOrderListVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String orderNo;

    private String customerName;

    private String customerMobile;

    private String barcode;

    private String productModel;

    private String mainStatus;

    private String displayStatus;

    private String evaluateStatus;

    private String currentAcceptCompanyName;

    private String assignedUserName;

    private Integer hasTransfer;

    private Boolean canEvaluate;

    private LocalDateTime createTime;

    private LocalDateTime closedTime;
}
