package com.jasic.aftersales.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 工单人员选项
 *
 * @author Codex
 * @date 2026/03/26
 */
@ApiModel(description = "工单人员选项")
@Data
public class WorkOrderUserOptionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID */
    @ApiModelProperty(value = "用户ID")
    private Long id;

    /** 真实姓名 */
    @ApiModelProperty(value = "真实姓名")
    private String realName;

    /** 手机号 */
    @ApiModelProperty(value = "手机号")
    private String phone;
}
