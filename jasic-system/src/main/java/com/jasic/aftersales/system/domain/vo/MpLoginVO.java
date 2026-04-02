package com.jasic.aftersales.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * B端小程序登录结果
 *
 * @author Codex
 * @date 2026/04/02
 */
@Data
public class MpLoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 登录状态，BOUND/UNBOUND */
    private String status;

    /** token */
    private String token;

    /** 用户信息 */
    private SysUserVO userInfo;

    /** 关联公司列表 */
    private List<SysCompanySimpleVO> companies;

    /** 是否需要选公司 */
    private Boolean needChooseCompany;
}
