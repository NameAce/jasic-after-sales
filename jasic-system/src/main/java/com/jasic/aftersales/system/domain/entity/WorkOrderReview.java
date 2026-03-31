package com.jasic.aftersales.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jasic.aftersales.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工单复检记录实体
 *
 * @author Codex
 * @date 2026/03/26
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("work_order_review")
public class WorkOrderReview extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单ID */
    private Long workOrderId;

    /** 复检公司ID */
    private Long companyId;

    /** 复检人ID */
    private Long reviewUserId;

    /** 复检结果 */
    private String reviewResult;

    /** 复检说明 */
    private String reviewDesc;

    /** 是否继续维修（1=是，0=否） */
    private Integer isContinueRepair;
}
