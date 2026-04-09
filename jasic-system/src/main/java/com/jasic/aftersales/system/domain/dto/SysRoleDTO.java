package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 角色新增/修改参数
 *
 * @author Zoro
 * @date 2026/03/18
 */
@ApiModel(description = "角色新增/修改参数")
@Data
public class SysRoleDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 角色ID（修改时必传） */
    @ApiModelProperty(value = "角色ID（修改时必传）")
    private Long id;

    /** 角色名称 */
    @ApiModelProperty(value = "角色名称", required = true)
    @NotBlank(message = "角色名称不能为空")
    private String roleName;

    /** 角色标识 */
    @ApiModelProperty(value = "角色标识", required = true)
    @NotBlank(message = "角色标识不能为空")
    private String roleKey;

    /** 数据范围（ALL/REGION/SELF） */
    @ApiModelProperty(value = "数据范围（ALL/REGION/SELF）", required = true)
    @NotBlank(message = "数据范围不能为空")
    private String dataScope;

    /** 排序 */
    @ApiModelProperty(value = "排序")
    private Integer orderNum;

    /** 备注 */
    @ApiModelProperty(value = "备注")
    private String remark;

    /** 菜单ID列表 */
    @ApiModelProperty(value = "菜单ID列表")
    private List<Long> menuIds;
}
