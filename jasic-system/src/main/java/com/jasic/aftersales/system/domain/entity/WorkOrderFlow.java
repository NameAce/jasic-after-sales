package com.jasic.aftersales.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jasic.aftersales.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工单流转历史实体
 *
 * @author Codex
 * @date 2026/03/26
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("work_order_flow")
public class WorkOrderFlow extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单ID */
    private Long workOrderId;

    /** 动作类型 */
    private String actionType;

    /** 动作前主状态 */
    private String beforeStatus;

    /** 动作后主状态 */
    private String afterStatus;

    /** 来源公司ID */
    private Long fromCompanyId;

    /** 目标公司ID */
    private Long toCompanyId;

    /** 操作公司ID */
    private Long operatorCompanyId;

    /** 操作人ID */
    private Long operatorUserId;

    /** 备注 */
    private String remark;
}
