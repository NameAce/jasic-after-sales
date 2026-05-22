package com.jasic.aftersales.system.notify.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 通知场景目标预览字段映射结果。
 *
 * @author Zoro
 * @date 2026/05/16
 */
@ApiModel(description = "通知场景目标预览字段映射结果")
@Data
public class NotifyScenePreviewFieldMappingVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**field 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "渠道字段")
    private String field;

    /**valueTemplate 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "值模板")
    private String valueTemplate;

    /**value 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "渲染结果")
    private String value;
}
