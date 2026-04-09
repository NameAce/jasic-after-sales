package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.List;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 角色模板新增/修改参数
 *
 * @author Zoro
 * @date 2026/03/18
 */
@ApiModel(description = "角色模板新增/修改参数")
@Data
public class SysRoleTemplateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 模板ID（修改时必传） */
    @ApiModelProperty(value = "模板ID（修改时必传）")
    private Long id;

    /** 公司类型编码 */
    @ApiModelProperty(value = "公司类型编码", required = true)
    @NotBlank(message = "公司类型编码不能为空")
    private String typeCode;

    /** 角色名称 */
    @ApiModelProperty(value = "角色名称", required = true)
    @NotBlank(message = "角色名称不能为空")
    private String roleName;

    /** 角色标识 */
    @ApiModelProperty(value = "角色标识", required = true)
    @NotBlank(message = "角色标识不能为空")
    private String roleKey;

    /** 数据范围 */
    @ApiModelProperty(value = "数据范围", required = true)
    @NotBlank(message = "数据范围不能为空")
    private String dataScope;

    /** 是否管理员角色模板（1=是，每种类型最多一个） */
    @ApiModelProperty(value = "是否管理员角色模板（1=是，每种类型最多一个）")
    private Integer isAdmin;

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
