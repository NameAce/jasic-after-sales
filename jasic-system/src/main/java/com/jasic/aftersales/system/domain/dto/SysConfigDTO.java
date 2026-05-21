package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 参数设置新增/修改参数。
 *
 * <p>该 DTO 继续兼容旧“参数设置”页的入参结构。旧页面不会提交 groupKey 时，
 * 服务层会根据配置 key 自动归组或保留原分组，避免本次新增字段破坏现有新增、修改能力。</p>
 *
 * @author Codex
 * @date 2026/03/19
 */
@ApiModel(description = "参数设置新增/修改参数")
@Data
public class SysConfigDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键，修改参数时用于定位已有配置记录。 */
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 参数名称，用于说明配置项在旧参数设置页中的展示名称。 */
    @ApiModelProperty(value = "参数名称", required = true)
    @NotBlank(message = "参数名称不能为空")
    private String configName;

    /** 参数键名，作为业务代码读取配置值和缓存配置值的稳定标识。 */
    @ApiModelProperty(value = "参数键名", required = true)
    @NotBlank(message = "参数键名不能为空")
    private String configKey;

    /** 参数键值，允许空字符串以支持环境相关配置先初始化占位再按环境填写。 */
    @ApiModelProperty(value = "参数键值", required = true)
    @NotNull(message = "参数键值不能为空")
    private String configValue;

    /** 是否内置，内置配置不允许通过旧参数设置页删除。 */
    @ApiModelProperty(value = "是否内置", required = true)
    @NotNull(message = "是否内置不能为空")
    private Integer configType;

    /** 配置分组标识，未传时由服务层根据参数键名按兼容规则归组。 */
    @ApiModelProperty(value = "配置分组标识")
    private String groupKey;

    /** 备注，用于补充配置项业务用途、默认值口径或环境维护要求。 */
    @ApiModelProperty(value = "备注")
    private String remark;
}
