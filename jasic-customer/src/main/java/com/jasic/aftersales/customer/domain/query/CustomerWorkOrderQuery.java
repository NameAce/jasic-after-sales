package com.jasic.aftersales.customer.domain.query;

import com.jasic.aftersales.common.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * C端工单查询参数
 *
 * @author Codex
 * @date 2026/03/26
 */
@ApiModel(description = "C端工单查询参数")
@Data
@EqualsAndHashCode(callSuper = true)
public class CustomerWorkOrderQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 页签状态（WAIT_ACCEPT/IN_PROGRESS/COMPLETED/CLOSED） */
    @ApiModelProperty(value = "页签状态（WAIT_ACCEPT/IN_PROGRESS/COMPLETED/CLOSED）")
    private String tabStatus;
}
