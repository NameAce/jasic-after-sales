package com.jasic.aftersales.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单详情视图
 *
 * @author Codex
 * @date 2026/03/26
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WorkOrderDetailVO extends WorkOrderListVO {

    private static final long serialVersionUID = 1L;

    /** 客户ID */
    private Long customerId;

    /** 物料编码 */
    private String productCode;

    /** 商品名称 */
    private String productName;

    /** 机器小号 */
    private String machineNo;

    /** 品牌编码 */
    private String brandCode;

    /** 服务方式 */
    private String serviceMode;

    /** 质保状态 */
    private String warrantyStatus;

    /** 客户报修描述 */
    private String faultDesc;

    /** 客户故障备注 */
    private String faultRemark;

    /** 寄件人姓名 */
    private String senderName;

    /** 寄件人手机号 */
    private String senderMobile;

    /** 寄件地址 */
    private String senderAddress;

    /** 寄件快递单号 */
    private String sendExpressNo;

    /** 评价状态 */
    private String evaluateStatus;

    /** 评价状态名称 */
    private String evaluateStatusLabel;

    /** 当前受理主体类型 */
    private String currentAcceptSubjectType;

    /** 建单来源公司ID */
    private Long createCompanyId;

    /** 建单来源公司名称 */
    private String createCompanyName;

    /** 建单入口类型 */
    private String createEntryType;

    /** 归属总部ID */
    private Long hqCompanyId;

    /** 归属总部名称 */
    private String hqCompanyName;

    /** 机器返回方式 */
    private String returnMethod;

    /** 回寄快递单号 */
    private String returnExpressNo;

    /** 关闭原因 */
    private String closeReason;

    /** 完成时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completedTime;

    /** 关闭时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime closedTime;

    /** 参与方列表 */
    private List<WorkOrderParticipantVO> participants;

    /** 报价记录列表 */
    private List<WorkOrderQuoteVO> quotes;

    /** 维修登记列表 */
    private List<WorkOrderRepairVO> repairs;

    /** 复检记录列表 */
    private List<WorkOrderReviewVO> reviews;

    /** 娴佽浆鍘嗗彶鍒楄〃 */
    private List<WorkOrderFlowVO> flows;

    /** 客户评价 */
    private WorkOrderEvaluationVO evaluation;

    /** 通知事件列表 */
    private List<WorkOrderNotifyEventVO> notifyEvents;

    /** 当前可执行动作 */
    private List<String> availableActions;
}
