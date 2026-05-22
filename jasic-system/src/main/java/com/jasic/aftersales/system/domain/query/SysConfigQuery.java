package com.jasic.aftersales.system.domain.query;

import com.jasic.aftersales.common.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 参数设置查询参数。
 *
 * <p>该查询对象继续服务旧参数设置列表。新增 groupKey 为后续按组查询预留能力，
 * 旧页面不传该字段时仍按原有名称、键名和内置状态查询。</p>
 *
 * @author Zoro
 * @date 2026/03/19
 */
@ApiModel(description = "参数设置查询参数")
@Data
@EqualsAndHashCode(callSuper = true)
public class SysConfigQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 参数名称，支持按配置展示名称进行模糊查询。 */
    @ApiModelProperty(value = "参数名称")
    private String configName;

    /** 参数键名，支持按配置业务 key 进行模糊查询。 */
    @ApiModelProperty(value = "参数键名")
    private String configKey;

    /** 是否内置，用于筛选内置配置或普通配置。 */
    @ApiModelProperty(value = "是否内置")
    private Integer configType;

    /** 配置分组标识，用于后续配置聚合页按 org、wechat、work_order、legacy 查询。 */
    @ApiModelProperty(value = "配置分组标识")
    private String groupKey;
}
