package com.jasic.aftersales.system.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 公司保存结果
 *
 * @author Zoro
 * @date 2026/04/17
 */
@ApiModel(description = "公司保存结果")
@Data
public class SysCompanySaveResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**主键ID，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "公司ID")
    private Long id;

    /**geocodeStatus 字段，用于向前端展示经过服务层组装后的业务值。*/
    @ApiModelProperty(value = "地理解析状态")
    private String geocodeStatus;
}
