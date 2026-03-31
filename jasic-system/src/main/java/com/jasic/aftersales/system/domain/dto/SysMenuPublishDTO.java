package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * 菜单发布参数
 *
 * @author Zoro
 * @date 2026/03/31
 */
@Data
public class SysMenuPublishDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 菜单参数 */
    @Valid
    @NotNull(message = "菜单信息不能为空")
    private SysMenuDTO menu;

    /** 目标公司类型编码列表 */
    @NotEmpty(message = "目标公司类型不能为空")
    private List<String> targetTypeCodes;

    /** 目标角色模板ID列表 */
    private List<Long> targetTemplateIds;

    /** 是否同步到已有公司 */
    private Boolean syncExistingCompanies;
}
