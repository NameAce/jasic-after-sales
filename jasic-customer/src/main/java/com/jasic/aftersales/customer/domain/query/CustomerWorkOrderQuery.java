package com.jasic.aftersales.customer.domain.query;

import com.jasic.aftersales.common.core.domain.PageQuery;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * C端工单查询参数
 *
 * @author Zoro
 * @date 2026/03/26
 */
@ApiModel(description = "C端工单查询参数")
@Data
@EqualsAndHashCode(callSuper = true)
public class CustomerWorkOrderQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 标签页状态（WAIT_ACCEPT/IN_PROGRESS/COMPLETED/CLOSED） */
    @ApiModelProperty(value = "标签页状态（WAIT_ACCEPT/IN_PROGRESS/COMPLETED/CLOSED）")
    private String tabStatus;

    /** 当前受理网点关键字，单个输入框同时匹配网点名称和网点电话 */
    @ApiModelProperty(value = "当前受理网点关键字，单个输入框同时匹配网点名称和网点电话")
    private String keyword;
}
