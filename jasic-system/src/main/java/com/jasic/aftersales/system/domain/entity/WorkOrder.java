package com.jasic.aftersales.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jasic.aftersales.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 工单主表实体
 *
 * @author Codex
 * @date 2026/03/26
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("work_order")
public class WorkOrder extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单号 */
    private String orderNo;

    /** 客户ID */
    private Long customerId;

    /** 客户姓名 */
    private String customerName;

    /** 客户手机号 */
    private String customerMobile;

    /** 机器条码 */
    private String barcode;

    /** 物料编码 */
    private String productCode;

    /** 商品名称 */
    private String productName;

    /** 机器型号 */
    private String productModel;

    /** 机器小号 */
    private String machineNo;

    /** 品牌编码 */
    private String brandCode;

    /** 服务方式（寄修/到店） */
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

    /** 主状态 */
    private String mainStatus;

    /** 评价状态 */
    private String evaluateStatus;

    /** 当前受理主体类型（SERVICE/HQ） */
    private String currentAcceptSubjectType;

    /** 当前受理公司ID */
    private Long currentAcceptCompanyId;

    /** 当前维修员ID */
    private Long assignedUserId;

    /** 建单来源公司ID */
    private Long createCompanyId;

    /** 建单入口类型 */
    private String createEntryType;

    /** 归属总部ID */
    private Long hqCompanyId;

    /** 是否发生过转单（1=是，0=否） */
    private Integer hasTransfer;

    /** 转单次数 */
    private Integer transferCount;

    /** 机器返回方式（回寄/自提） */
    private String returnMethod;

    /** 回寄快递单号 */
    private String returnExpressNo;

    /** 关闭原因 */
    private String closeReason;

    /** 完成时间 */
    private LocalDateTime completedTime;

    /** 关闭时间 */
    private LocalDateTime closedTime;
}
