package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 选择/切换公司请求参数
 *
 * @author Zoro
 * @date 2026/03/18
 */
@ApiModel(description = "选择/切换公司请求参数")
@Data
public class ChooseCompanyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 公司ID */
    @ApiModelProperty(value = "公司ID", required = true)
    @NotNull(message = "公司ID不能为空")
    private Long companyId;
}
