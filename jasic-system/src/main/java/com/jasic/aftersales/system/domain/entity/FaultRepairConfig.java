package com.jasic.aftersales.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jasic.aftersales.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 故障与维修配置实体
 *
 * @author Zoro
 * @date 2026/04/01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fault_repair_config")
public class FaultRepairConfig extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 归属总部ID */
    private Long companyId;

    /** 物料编码 */
    private String productCode;

    /** 产品型号 */
    private String productModel;

    /** 状态 */
    private Integer status;

    /** 备注 */
    private String remark;
}
