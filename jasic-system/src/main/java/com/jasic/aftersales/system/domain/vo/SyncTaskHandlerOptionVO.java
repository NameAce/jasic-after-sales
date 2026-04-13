package com.jasic.aftersales.system.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 同步任务处理器选项
 *
 * @author Codex
 * @date 2026/04/12
 */
@ApiModel(description = "同步任务处理器选项")
@Data
public class SyncTaskHandlerOptionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 处理器编码 */
    @ApiModelProperty(value = "处理器编码")
    private String handlerCode;

    /** 处理器名称 */
    @ApiModelProperty(value = "处理器名称")
    private String handlerName;
}
