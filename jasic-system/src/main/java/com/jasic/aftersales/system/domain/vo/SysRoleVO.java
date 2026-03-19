package com.jasic.aftersales.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色VO
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Data
public class SysRoleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 角色ID */
    private Long id;

    /** 归属公司ID */
    private Long companyId;

    /** 角色名称 */
    private String roleName;

    /** 角色标识 */
    private String roleKey;

    /** 数据范围 */
    private String dataScope;

    /** 角色类型（0=自定义角色，1=公司管理员角色，2=模板角色） */
    private Integer roleType;

    /** 是否系统角色 */
    private Integer isSystem;

    /** 状态 */
    private Integer status;

    /** 排序 */
    private Integer orderNum;

    /** 备注 */
    private String remark;

    /** 已分配菜单ID列表 */
    private List<Long> menuIds;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
