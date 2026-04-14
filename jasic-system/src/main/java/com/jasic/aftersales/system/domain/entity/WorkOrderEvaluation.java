package com.jasic.aftersales.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jasic.aftersales.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工单评价实体
 *
 * @author Codex
 * @date 2026/03/26
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("work_order_evaluation")
public class WorkOrderEvaluation extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 工单ID */
    private Long workOrderId;

    /** 客户ID */
    private Long customerId;

    /** 被评价服务方公司ID */
    private Long companyId;

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
}
