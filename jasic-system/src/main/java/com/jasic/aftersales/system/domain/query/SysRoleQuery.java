package com.jasic.aftersales.system.domain.query;

import com.jasic.aftersales.common.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 角色查询参数
 *
 * @author Zoro
 * @date 2026/03/18
 */
@ApiModel(description = "角色查询参数")
@Data
@EqualsAndHashCode(callSuper = true)
public class SysRoleQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 角色名称（模糊） */
    @ApiModelProperty(value = "角色名称（模糊）")
    private String roleName;

    /** 角色标识 */
    @ApiModelProperty(value = "角色标识")
    private String roleKey;

    /** 状态 */
    @ApiModelProperty(value = "状态")
    private Integer status;

    /** 目标公司ID */
    @ApiModelProperty(value = "目标公司ID")
    private Long targetCompanyId;
}
