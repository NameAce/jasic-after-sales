package com.jasic.aftersales.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 菜单发布公司类型选项
 *
 * @author Zoro
 * @date 2026/03/31
 */
@ApiModel(description = "菜单发布公司类型选项")
@Data
public class SysMenuPublishTypeOptionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 公司类型编码 */
    @ApiModelProperty(value = "公司类型编码")
    private String typeCode;

    /** 公司类型名称 */
    @ApiModelProperty(value = "公司类型名称")
    private String typeName;
}
