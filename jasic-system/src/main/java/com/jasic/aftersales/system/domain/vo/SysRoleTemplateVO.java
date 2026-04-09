package com.jasic.aftersales.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 角色模板VO
 *
 * @author Zoro
 * @date 2026/03/18
 */
@ApiModel(description = "角色模板VO")
@Data
public class SysRoleTemplateVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 模板ID */
    @ApiModelProperty(value = "模板ID")
    private Long id;

    /** 公司类型编码 */
    @ApiModelProperty(value = "公司类型编码")
    private String typeCode;

    /** 角色名称 */
    @ApiModelProperty(value = "角色名称")
    private String roleName;

    /** 角色标识 */
    @ApiModelProperty(value = "角色标识")
    private String roleKey;

    /** 数据范围 */
    @ApiModelProperty(value = "数据范围")
    private String dataScope;

    /** 是否管理员角色模板（1=是） */
    @ApiModelProperty(value = "是否管理员角色模板（1=是）")
    private Integer isAdmin;

    /** 排序 */
    @ApiModelProperty(value = "排序")
    private Integer orderNum;

    /** 备注 */
    @ApiModelProperty(value = "备注")
    private String remark;

    /** 关联菜单ID列表 */
    @ApiModelProperty(value = "关联菜单ID列表")
    private List<Long> menuIds;

    /** 创建时间 */
    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
