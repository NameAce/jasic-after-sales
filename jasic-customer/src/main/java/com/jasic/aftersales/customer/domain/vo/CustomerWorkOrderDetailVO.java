package com.jasic.aftersales.customer.domain.vo;

import com.jasic.aftersales.system.domain.vo.WorkOrderEvaluationVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderQuoteVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderRepairVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderReviewVO;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * C端工单详情视图
 *
 * @author Codex
 * @date 2026/03/26
 */
@Data
public class CustomerWorkOrderDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String orderNo;

    private Long customerId;

    private String customerName;

    private String customerMobile;

    private String barcode;

    private String productCode;

    private String productModel;

    private String brandCode;

    private String serviceMode;

    private String warrantyStatus;

    private String faultDesc;

    private String senderName;

    private String senderMobile;

    private String senderAddress;

    private String sendExpressNo;

    private String mainStatus;

    private String displayStatus;

    private String evaluateStatus;

    private String currentAcceptCompanyName;

    private String assignedUserName;

    private Long hqCompanyId;

    private String returnMethod;

    private String returnExpressNo;

    private String closeReason;

    private Boolean canEvaluate;

    private Boolean canEditSendInfo;

    private LocalDateTime completedTime;

    private LocalDateTime closedTime;

    private LocalDateTime createTime;

    private List<WorkOrderQuoteVO> quotes;

    private List<WorkOrderRepairVO> repairs;

    private List<WorkOrderReviewVO> reviews;

    private WorkOrderEvaluationVO evaluation;
}
