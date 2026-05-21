package com.jasic.aftersales.system.domain.vo.dashboard;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 首页最新动态项。
 *
 * <p>该对象是首页专用返回结构，
 * 与通知列表页分页项解耦，避免首页继续直接依赖列表页对象语义。</p>
 *
 * @author Codex
 * @date 2026/05/20
 */
@ApiModel(description = "首页最新动态项")
@Data
public class DashboardHistoryTodoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息ID。
     */
    @ApiModelProperty(value = "消息ID")
    private Long id;

    /**
     * 标题。
     */
    @ApiModelProperty(value = "标题")
    private String title;

    /**
     * 摘要。
     */
    @ApiModelProperty(value = "摘要")
    private String summary;

    /**
     * 业务类型。
     */
    @ApiModelProperty(value = "业务类型")
    private String bizType;

    /**
     * 业务ID。
     */
    @ApiModelProperty(value = "业务ID")
    private Long bizId;

    /**
     * 跳转类型。
     */
    @ApiModelProperty(value = "跳转类型")
    private String routeType;

    /**
     * 跳转值。
     */
    @ApiModelProperty(value = "跳转值")
    private String routeValue;

    /**
     * 待办状态。
     */
    @ApiModelProperty(value = "待办状态")
    private String todoStatus;

    /**
     * 创建时间。
     */
    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
