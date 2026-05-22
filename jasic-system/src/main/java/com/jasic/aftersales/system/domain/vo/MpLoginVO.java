package com.jasic.aftersales.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * B端小程序登录结果
 *
 * @author Zoro
 * @date 2026/04/02
 */
@ApiModel(description = "B端小程序登录结果")
@Data
public class MpLoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 登录状态，BIND/UNBIND */
    @ApiModelProperty(value = "登录状态，BIND/UNBIND")
    private String status;

    /** token */
    @ApiModelProperty(value = "token")
    private String token;

    /** 用户信息 */
    @ApiModelProperty(value = "用户信息")
    private SysUserVO userInfo;

    /** 关联公司列表 */
    @ApiModelProperty(value = "关联公司列表")
    private List<SysCompanySimpleVO> companies;

    /** 是否需要选公司 */
    @ApiModelProperty(value = "是否需要选公司")
    private Boolean needChooseCompany;
}
