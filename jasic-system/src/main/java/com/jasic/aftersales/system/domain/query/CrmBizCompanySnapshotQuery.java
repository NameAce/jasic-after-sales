package com.jasic.aftersales.system.domain.query;

import com.jasic.aftersales.common.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * CRM 公司快照查询参数
 *
 * @author Zoro
 * @date 2026/04/12
 */
@ApiModel(description = "CRM 公司快照查询参数")
@Data
@EqualsAndHashCode(callSuper = true)
public class CrmBizCompanySnapshotQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 客户编码 */
    @ApiModelProperty(value = "客户编码")
    private String companyCode;

    /** 客户名称 */
    @ApiModelProperty(value = "客户名称")
    private String companyName;

    /** CRM 客户状态 */
    @ApiModelProperty(value = "CRM 客户状态")
    private Integer custState;
}
