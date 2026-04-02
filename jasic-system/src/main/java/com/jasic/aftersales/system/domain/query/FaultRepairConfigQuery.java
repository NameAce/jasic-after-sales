package com.jasic.aftersales.system.domain.query;

import com.jasic.aftersales.common.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 故障与维修配置查询参数
 *
 * @author Codex
 * @date 2026/04/01
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FaultRepairConfigQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 归属总部ID */
    private Long companyId;

    /** 物料编码 */
    private String productCode;

    /** 产品型号 */
    private String productModel;

    /** 故障描述 */
    private String faultDesc;

    /** 状态 */
    private Integer status;
}
