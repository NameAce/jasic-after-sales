package com.jasic.aftersales.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jasic.aftersales.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色模板实体
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role_template")
public class SysRoleTemplate extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 公司类型编码 */
    private String typeCode;

    /** 角色名称 */
    private String roleName;

    /** 角色标识 */
    private String roleKey;

    /** 数据范围（ALL/REGION/SELF） */
    private String dataScope;

    /** 是否管理员角色模板（1=是，每种类型最多一个） */
    private Integer isAdmin;

    /** 排序 */
    private Integer orderNum;

    /** 备注 */
    private String remark;
}
