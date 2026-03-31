package com.jasic.aftersales.customer.domain.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * C端工单评价参数
 *
 * @author Codex
 * @date 2026/03/26
 */
@Data
public class CustomerWorkOrderEvaluateDTO {

    /** 工单ID */
    @NotNull(message = "工单ID不能为空")
    private Long workOrderId;

    /** 评分 */
    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最小为1分")
    @Max(value = 5, message = "评分最大为5分")
    private Integer score;

    /** 标签 */
    private String tags;

    /** 评价内容 */
    private String content;
}
