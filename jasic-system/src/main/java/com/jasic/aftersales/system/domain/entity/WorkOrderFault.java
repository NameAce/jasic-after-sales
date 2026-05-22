package com.jasic.aftersales.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jasic.aftersales.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工单故障点记录实体
 *
 * @author Zoro
 * @date 2026/03/26
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("work_order_fault")
public class WorkOrderFault extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单ID */
    private Long workOrderId;

    /** 维修登记ID */
    private Long repairId;

    /** 登记公司ID */
    private Long companyId;

    /** 故障描述 */
    private String faultDesc;

    /** 其它故障说明 */
    private String faultRemark;

    /** 维修说明 */
    private String repairDesc;

    /** 其他维修说明 */
    private String otherDesc;

    /** 排序号 */
    private Integer sortNum;

    /** 登记人ID */
    private Long createdBy;
}
