package com.jasic.aftersales.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jasic.aftersales.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 故障与维修配置维修项实体
 *
 * @author Codex
 * @date 2026/04/01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fault_repair_config_option")
public class FaultRepairConfigOption extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 故障项ID */
    private Long faultId;

    /** 维修说明 */
    private String repairDesc;

    /** 排序号 */
    private Integer sortNum;
}
