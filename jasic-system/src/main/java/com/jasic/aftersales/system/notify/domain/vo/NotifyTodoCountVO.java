package com.jasic.aftersales.system.notify.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 通知待办数量返回对象。
 *
 * @author Codex
 * @date 2026/04/18
 */
@ApiModel(description = "通知待办数量返回对象")
@Data
public class NotifyTodoCountVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 待办数量 */
    @ApiModelProperty(value = "待办数量")
    private Long count;
}
