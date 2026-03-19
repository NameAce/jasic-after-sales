package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.List;

/**
 * 角色模板新增/修改参数
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Data
public class SysRoleTemplateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 模板ID（修改时必传） */
    private Long id;

    /** 公司类型编码 */
    @NotBlank(message = "公司类型编码不能为空")
    private String typeCode;

    /** 角色名称 */
    @NotBlank(message = "角色名称不能为空")
    private String roleName;

    /** 角色标识 */
    @NotBlank(message = "角色标识不能为空")
    private String roleKey;

    /** 数据范围 */
    private String dataScope;

    /** 是否管理员角色模板（1=是，每种类型最多一个） */
    private Integer isAdmin;

    /** 排序 */
    private Integer orderNum;

    /** 备注 */
    private String remark;

    /** 菜单ID列表 */
    private List<Long> menuIds;
}
