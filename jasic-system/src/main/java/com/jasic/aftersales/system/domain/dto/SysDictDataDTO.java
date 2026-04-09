package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 字典数据新增/修改参数
 *
 * @author Codex
 * @date 2026/03/19
 */
@ApiModel(description = "字典数据新增/修改参数")
@Data
public class SysDictDataDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 字典类型 */
    @ApiModelProperty(value = "字典类型", required = true)
    @NotBlank(message = "字典类型不能为空")
    private String dictType;

    /** 字典标签 */
    @ApiModelProperty(value = "字典标签", required = true)
    @NotBlank(message = "字典标签不能为空")
    private String dictLabel;

    /** 字典键值 */
    @ApiModelProperty(value = "字典键值", required = true)
    @NotBlank(message = "字典键值不能为空")
    private String dictValue;

    /** 排序 */
    @ApiModelProperty(value = "排序", required = true)
    @NotNull(message = "排序不能为空")
    private Integer dictSort;

    /** 自定义样式 */
    @ApiModelProperty(value = "自定义样式")
    private String cssClass;

    /** 标签样式 */
    @ApiModelProperty(value = "标签样式")
    private String listClass;

    /** 是否默认 */
    @ApiModelProperty(value = "是否默认", required = true)
    @NotNull(message = "是否默认不能为空")
    private Integer isDefault;

    /** 状态 */
    @ApiModelProperty(value = "状态", required = true)
    @NotNull(message = "状态不能为空")
    private Integer status;

    /** 备注 */
    @ApiModelProperty(value = "备注")
    private String remark;
}
