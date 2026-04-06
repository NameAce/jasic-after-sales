package com.jasic.aftersales.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工单评价视图
 *
 * @author Codex
 * @date 2026/03/26
 */
@Data
public class WorkOrderEvaluationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 评价ID */
    private Long id;

    /** 客户ID */
    private Long customerId;

    /** 被评价公司ID */
    private Long companyId;

    /** 评分 */
    /** 服务时效评分 */
    private Integer timelinessScore;

    /** 维修质量评分 */
    private Integer qualityScore;

    /** 服务满意度评分 */
    private Integer satisfactionScore;

    /** 标签集合 */
    private String tags;

    /** 评价内容 */
    private String content;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
