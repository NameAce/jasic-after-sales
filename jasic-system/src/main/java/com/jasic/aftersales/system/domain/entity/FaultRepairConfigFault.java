package com.jasic.aftersales.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jasic.aftersales.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 故障与维修配置故障项实体
 *
 * @author Zoro
 * @date 2026/04/01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fault_repair_config_fault")
public class FaultRepairConfigFault extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 配置ID */
    private Long configId;

    /** 故障描述 */
    private String faultDesc;

    /** 排序号 */
    private Integer sortNum;
}
