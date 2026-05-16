package com.jasic.aftersales.system.notify.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 排障页通知目标聚合摘要。
 *
 * <p>该对象围绕“一个事件下某个通知目标产出了什么”组织，
 * 统一承载站内消息、站内待办和外部分发任务的数量、状态分布和摘要文案。
 * 页面通过该对象可以直接展示“站内消息已生成”“外部分发失败”“sender 跳过”“死信”等状态口径。</p>
 *
 * @author Codex
 * @date 2026/05/16
 */
@ApiModel(description = "通知排障目标聚合摘要")
@Data
public class NotifyTraceTargetSummaryVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 通知目标类型 */
    @ApiModelProperty(value = "通知目标类型")
    private String targetType;

    /** 通知目标类型说明 */
    @ApiModelProperty(value = "通知目标类型说明")
    private String targetTypeDesc;

    /** 产物分类 */
    @ApiModelProperty(value = "产物分类，IN_APP/EXTERNAL")
    private String productCategory;

    /** 产物分类说明 */
    @ApiModelProperty(value = "产物分类说明")
    private String productCategoryDesc;

    /** 产物数量 */
    @ApiModelProperty(value = "产物数量")
    private Integer totalCount;

    /** 当前最需要关注的状态 */
    @ApiModelProperty(value = "当前最需要关注的状态")
    private String highlightStatus;

    /** 当前最需要关注的状态说明 */
    @ApiModelProperty(value = "当前最需要关注的状态说明")
    private String highlightStatusDesc;

    /** 状态分布 */
    @ApiModelProperty(value = "状态分布")
    private List<NotifyTraceStatusCountVO> statusCounts = new ArrayList<>();

    /** 目标摘要文案 */
    @ApiModelProperty(value = "目标摘要文案")
    private String summaryText;
}
