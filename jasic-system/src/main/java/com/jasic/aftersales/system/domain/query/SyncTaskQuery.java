package com.jasic.aftersales.system.domain.query;

import com.jasic.aftersales.common.core.domain.PageQuery;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 同步任务查询参数
 *
 * @author Codex
 * @date 2026/04/12
 */
@ApiModel(description = "同步任务查询参数")
@Data
@EqualsAndHashCode(callSuper = true)
public class SyncTaskQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 任务编码 */
    @ApiModelProperty(value = "任务编码")
    private String taskCode;

    /** 任务名称 */
    @ApiModelProperty(value = "任务名称")
    private String taskName;

    /** 处理器编码 */
    @ApiModelProperty(value = "处理器编码")
    private String handlerCode;

    /** 状态 */
    @ApiModelProperty(value = "状态（1=启用，0=停用）")
    private Integer status;
}
