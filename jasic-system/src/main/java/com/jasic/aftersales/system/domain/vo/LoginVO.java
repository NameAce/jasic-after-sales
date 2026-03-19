package com.jasic.aftersales.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 登录返回结果
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Data
public class LoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Token */
    private String token;

    /** 用户信息 */
    private SysUserVO userInfo;

    /** 关联公司列表（多公司时需要选择） */
    private List<SysCompanySimpleVO> companies;

    /** 是否需要选择公司 */
    private Boolean needChooseCompany;
}
