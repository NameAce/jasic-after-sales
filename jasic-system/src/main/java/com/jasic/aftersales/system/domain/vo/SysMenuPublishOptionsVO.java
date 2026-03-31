package com.jasic.aftersales.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 菜单发布可选项
 *
 * @author Zoro
 * @date 2026/03/31
 */
@Data
public class SysMenuPublishOptionsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 公司类型选项 */
    private List<SysMenuPublishTypeOptionVO> typeOptions;

    /** 角色模板选项 */
    private List<SysMenuPublishTemplateOptionVO> templateOptions;
}
