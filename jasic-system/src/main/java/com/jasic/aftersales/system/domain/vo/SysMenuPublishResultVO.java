package com.jasic.aftersales.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 菜单发布结果
 *
 * @author Zoro
 * @date 2026/03/31
 */
@ApiModel(description = "菜单发布结果")
@Data
public class SysMenuPublishResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 菜单ID */
    @ApiModelProperty(value = "菜单ID")
    private Long menuId;

    /** 新增公司类型菜单上限数量 */
    @ApiModelProperty(value = "新增公司类型菜单上限数量")
    private Integer addedTypeCodeCount;

    /** 新增角色模板菜单数量 */
    @ApiModelProperty(value = "新增角色模板菜单数量")
    private Integer addedTemplateCount;

    /** 新增角色菜单数量 */
    @ApiModelProperty(value = "新增角色菜单数量")
    private Integer updatedRoleCount;

    /** 踢下线用户数量 */
    @ApiModelProperty(value = "踢下线用户数量")
    private Integer kickedUserCount;

    /** 跳过公司数量 */
    @ApiModelProperty(value = "跳过公司数量")
    private Integer skippedCompanyCount;
}
