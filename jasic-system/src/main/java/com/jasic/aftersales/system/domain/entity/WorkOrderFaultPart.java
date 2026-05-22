package com.jasic.aftersales.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jasic.aftersales.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工单故障点配件明细实体
 *
 * @author Zoro
 * @date 2026/04/15
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("work_order_fault_part")
public class WorkOrderFaultPart extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单ID */
    private Long workOrderId;

    /** 故障点ID */
    private Long faultId;

    /** 登记公司ID */
    private Long companyId;

    /** 配件名称 */
    private String partName;

    /** 配件数量 */
    private Integer partQty;

    /** 排序号 */
    private Integer sortNum;

    /** 登记人ID */
    private Long createdBy;
}
