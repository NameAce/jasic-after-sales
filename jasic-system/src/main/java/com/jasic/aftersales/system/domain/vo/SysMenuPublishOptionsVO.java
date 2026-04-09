package com.jasic.aftersales.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 菜单发布可选项
 *
 * @author Zoro
 * @date 2026/03/31
 */
@ApiModel(description = "菜单发布可选项")
@Data
public class SysMenuPublishOptionsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 公司类型选项 */
    @ApiModelProperty(value = "公司类型选项")
    private List<SysMenuPublishTypeOptionVO> typeOptions;

    /** 角色模板选项 */
    @ApiModelProperty(value = "角色模板选项")
    private List<SysMenuPublishTemplateOptionVO> templateOptions;
}
