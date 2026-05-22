package com.jasic.aftersales.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jasic.aftersales.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 工单报价记录实体
 *
 * @author Zoro
 * @date 2026/03/26
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("work_order_quote")
public class WorkOrderQuote extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单ID */
    private Long workOrderId;

    /** 报价公司ID */
    private Long companyId;

    /** 报价人ID */
    private Long quotedBy;

    /** 故障判定 */
    private String faultJudge;

    /** 报价金额 */
    private BigDecimal quoteAmount;

    /** 报价说明 */
    private String quoteDesc;

    /** 是否当前有效报价（1=是，0=否） */
    private Integer isCurrentValid;
}
