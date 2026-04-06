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

    /** 服务时效评分 */
    @NotNull(message = "服务时效评分不能为空")
    @Min(value = 1, message = "服务时效评分最小为1分")
    @Max(value = 5, message = "服务时效评分最大为5分")
    private Integer timelinessScore;

    /** 维修质量评分 */
    @NotNull(message = "维修质量评分不能为空")
    @Min(value = 1, message = "维修质量评分最小为1分")
    @Max(value = 5, message = "维修质量评分最大为5分")
    private Integer qualityScore;

    /** 服务满意度评分 */
    @NotNull(message = "服务满意度评分不能为空")
    @Min(value = 1, message = "服务满意度评分最小为1分")
    @Max(value = 5, message = "服务满意度评分最大为5分")
    private Integer satisfactionScore;

    /** 标签 */
    private String tags;

    /** 评价内容 */
    private String content;
}
