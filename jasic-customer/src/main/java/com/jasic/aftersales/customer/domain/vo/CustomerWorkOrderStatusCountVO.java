package com.jasic.aftersales.customer.domain.vo;

import lombok.Data;

import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * C端工单状态计数
 *
 * @author Zoro
 * @date 2026/03/26
 */
@ApiModel(description = "C端工单状态计数")
@Data
public class CustomerWorkOrderStatusCountVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 全部工单数量 */
    @ApiModelProperty(value = "全部工单数量")
    private Long allCount;

    /** 待接单数量 */
    @ApiModelProperty(value = "待接单数量")
    private Long waitAcceptCount;

    /** 处理中数量 */
    @ApiModelProperty(value = "处理中数量")
    private Long inProgressCount;

    /** 已完成数量 */
    @ApiModelProperty(value = "已完成数量")
    private Long completedCount;

    /** 已关闭数量 */
    @ApiModelProperty(value = "已关闭数量")
    private Long closedCount;
}
