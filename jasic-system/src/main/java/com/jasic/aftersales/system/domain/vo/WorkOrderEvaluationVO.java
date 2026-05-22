package com.jasic.aftersales.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 工单评价视图
 *
 * @author Zoro
 * @date 2026/03/26
 */
@ApiModel(description = "工单评价视图")
@Data
public class WorkOrderEvaluationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 评价ID */
    @ApiModelProperty(value = "评价ID")
    private Long id;

    /** 客户ID */
    @ApiModelProperty(value = "客户ID")
    private Long customerId;

    /** 被评价公司ID */
    @ApiModelProperty(value = "被评价公司ID")
    private Long companyId;

    /** 服务时效评分 */
    @ApiModelProperty(value = "服务时效评分")
    private Integer timelinessScore;

    /** 维修质量评分 */
    @ApiModelProperty(value = "维修质量评分")
    private Integer qualityScore;

    /** 服务满意度评分 */
    @ApiModelProperty(value = "服务满意度评分")
    private Integer satisfactionScore;

    /** 标签集合 */
    @ApiModelProperty(value = "标签集合")
    private String tags;

    /** 评价内容 */
    @ApiModelProperty(value = "评价内容")
    private String content;

    /** 创建时间 */
    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
