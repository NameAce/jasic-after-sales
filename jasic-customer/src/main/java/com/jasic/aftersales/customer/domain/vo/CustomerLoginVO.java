package com.jasic.aftersales.customer.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * C端登录结果
 *
 * @author Codex
 * @date 2026/04/06
 */
@Data
public class CustomerLoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** token */
    private String token;

    /** 用户信息 */
    private CustomerUserInfoVO userInfo;
}
