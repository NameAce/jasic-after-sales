package com.jasic.aftersales.system.domain.query;

import com.jasic.aftersales.common.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 故障与维修配置查询参数
 *
 * @author Codex
 * @date 2026/04/01
 */
@ApiModel(description = "故障与维修配置查询参数")
@Data
@EqualsAndHashCode(callSuper = true)
public class FaultRepairConfigQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 归属总部ID */
    @ApiModelProperty(value = "归属总部ID")
    private Long companyId;

    /** 物料编码 */
    @ApiModelProperty(value = "物料编码")
    private String productCode;

    /** 产品型号 */
    @ApiModelProperty(value = "产品型号")
    private String productModel;

    /** 故障描述 */
    @ApiModelProperty(value = "故障描述")
    private String faultDesc;

    /** 状态 */
    @ApiModelProperty(value = "状态")
    private Integer status;
}
