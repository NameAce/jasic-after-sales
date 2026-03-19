package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.List;

/**
 * 菜单拷贝参数
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Data
public class SysMenuCopyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 源主体类型（PLATFORM/HQ/SERVICE） */
    @NotBlank(message = "源主体类型不能为空")
    private String sourceSubjectType;

    /** 目标主体类型（PLATFORM/HQ/SERVICE） */
    @NotBlank(message = "目标主体类型不能为空")
    private String targetSubjectType;

    /** 要拷贝的菜单ID列表（为空则拷贝全部） */
    private List<Long> menuIds;
}
