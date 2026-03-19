package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 字典数据新增/修改参数
 *
 * @author Codex
 * @date 2026/03/19
 */
@Data
public class SysDictDataDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 字典类型 */
    @NotBlank(message = "字典类型不能为空")
    private String dictType;

    /** 字典标签 */
    @NotBlank(message = "字典标签不能为空")
    private String dictLabel;

    /** 字典键值 */
    @NotBlank(message = "字典键值不能为空")
    private String dictValue;

    /** 排序 */
    @NotNull(message = "排序不能为空")
    private Integer dictSort;

    /** 自定义样式 */
    private String cssClass;

    /** 标签样式 */
    private String listClass;

    /** 是否默认 */
    @NotNull(message = "是否默认不能为空")
    private Integer isDefault;

    /** 状态 */
    @NotNull(message = "状态不能为空")
    private Integer status;

    /** 备注 */
    private String remark;
}
