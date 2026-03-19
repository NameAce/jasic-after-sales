package com.jasic.aftersales.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色模板VO
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Data
public class SysRoleTemplateVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 模板ID */
    private Long id;

    /** 公司类型编码 */
    private String typeCode;

    /** 角色名称 */
    private String roleName;

    /** 角色标识 */
    private String roleKey;

    /** 数据范围 */
    private String dataScope;

    /** 是否管理员角色模板（1=是） */
    private Integer isAdmin;

    /** 排序 */
    private Integer orderNum;

    /** 备注 */
    private String remark;

    /** 关联菜单ID列表 */
    private List<Long> menuIds;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
