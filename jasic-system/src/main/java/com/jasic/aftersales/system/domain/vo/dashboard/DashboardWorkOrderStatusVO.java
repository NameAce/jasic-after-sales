package com.jasic.aftersales.system.domain.vo.dashboard;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 首页工单状态统计结构。
 *
 * <p>该对象把原有状态计数列表收敛成首页固定字段，
 * 避免前端继续根据列表编码自己二次映射。</p>
 *
 * @author Codex
 * @date 2026/05/20
 */
@ApiModel(description = "首页工单状态统计结构")
@Data
public class DashboardWorkOrderStatusVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 工单总数。
     */
    @ApiModelProperty(value = "工单总数")
    private Long all;

    /**
     * 待派单数。
     */
    @ApiModelProperty(value = "待派单数")
    private Long pendingAssign;

    /**
     * 待接单数。
     */
    @ApiModelProperty(value = "待接单数")
    private Long pendingTechAccept;

    /**
     * 处理中数。
     */
    @ApiModelProperty(value = "处理中数")
    private Long inProgress;

    /**
     * 已完成数。
     */
    @ApiModelProperty(value = "已完成数")
    private Long completed;

    /**
     * 已关闭数。
     */
    @ApiModelProperty(value = "已关闭数")
    private Long closed;
}
