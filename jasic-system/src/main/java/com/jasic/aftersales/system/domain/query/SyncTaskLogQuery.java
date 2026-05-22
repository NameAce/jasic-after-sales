package com.jasic.aftersales.system.domain.query;

import com.jasic.aftersales.common.core.domain.PageQuery;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 同步任务日志查询参数
 *
 * @author Zoro
 * @date 2026/04/12
 */
@ApiModel(description = "同步任务日志查询参数")
@Data
@EqualsAndHashCode(callSuper = true)
public class SyncTaskLogQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 任务ID */
    @ApiModelProperty(value = "任务ID")
    private Long taskId;

    /** 执行状态 */
    @ApiModelProperty(value = "执行状态")
    private String status;
}
