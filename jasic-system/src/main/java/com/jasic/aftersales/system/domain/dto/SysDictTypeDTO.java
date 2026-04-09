package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 字典类型新增/修改参数
 *
 * @author Codex
 * @date 2026/03/19
 */
@ApiModel(description = "字典类型新增/修改参数")
@Data
public class SysDictTypeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 字典名称 */
    @ApiModelProperty(value = "字典名称", required = true)
    @NotBlank(message = "字典名称不能为空")
    private String dictName;

    /** 字典类型 */
    @ApiModelProperty(value = "字典类型", required = true)
    @NotBlank(message = "字典类型不能为空")
    private String dictType;

    /** 状态 */
    @ApiModelProperty(value = "状态", required = true)
    @NotNull(message = "状态不能为空")
    private Integer status;

    /** 备注 */
    @ApiModelProperty(value = "备注")
    private String remark;
}
