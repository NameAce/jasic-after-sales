package com.jasic.aftersales.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jasic.aftersales.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色实体
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 归属公司ID */
    private Long companyId;

    /** 角色名称 */
    private String roleName;

    /** 角色标识 */
    private String roleKey;

    /** 数据范围（ALL/REGION/SELF） */
    private String dataScope;

    /** 角色类型（0=自定义角色，1=公司管理员角色，2=模板角色） */
    private Integer roleType;

    /** 是否系统角色（1=是，不可删除） */
    private Integer isSystem;

    /** 状态（1=正常，0=停用） */
    private Integer status;

    /** 排序 */
    private Integer orderNum;

    /** 备注 */
    private String remark;
}
