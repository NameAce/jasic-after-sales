package com.jasic.aftersales.system.notify.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 通知模板枚举选项。
 *
 * <p>用于统一返回通知模板配置页所需的编码和中文说明，
 * 避免前端自行维护业务类型、触发场景、通知类型等常量表。</p>
 *
 * @author Zoro
 * @date 2026/05/15
 */
@ApiModel(description = "通知模板枚举选项")
@Data
public class NotifyTemplateEnumOptionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 选项编码。
     */
    @ApiModelProperty(value = "选项编码")
    private String code;

    /**
     * 选项说明。
     */
    @ApiModelProperty(value = "选项说明")
    private String desc;
}
