package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * 角色新增/修改参数
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Data
public class SysRoleDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 角色ID（修改时必传） */
    private Long id;

    /** 角色名称 */
    @NotBlank(message = "角色名称不能为空")
    private String roleName;

    /** 角色标识 */
    @NotBlank(message = "角色标识不能为空")
    private String roleKey;

    /** 数据范围（ALL/REGION/SELF） */
    private String dataScope;

    /** 排序 */
    private Integer orderNum;

    /** 备注 */
    private String remark;

    /** 菜单ID列表 */
    private List<Long> menuIds;
}
