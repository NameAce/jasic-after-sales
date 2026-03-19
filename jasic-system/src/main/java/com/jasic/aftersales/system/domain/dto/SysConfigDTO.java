package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 参数设置新增/修改参数
 *
 * @author Codex
 * @date 2026/03/19
 */
@Data
public class SysConfigDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 参数名称 */
    @NotBlank(message = "参数名称不能为空")
    private String configName;

    /** 参数键名 */
    @NotBlank(message = "参数键名不能为空")
    private String configKey;

    /** 参数键值 */
    @NotBlank(message = "参数键值不能为空")
    private String configValue;

    /** 是否内置 */
    @NotNull(message = "是否内置不能为空")
    private Integer configType;

    /** 备注 */
    private String remark;
}
