package com.jasic.aftersales.customer.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * C端客户信息
 *
 * @author Codex
 * @date 2026/04/06
 */
@Data
public class CustomerUserInfoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 客户ID */
    private Long userId;

    /** 手机号 */
    private String phone;

    /** 昵称 */
    private String nickname;

    /** 头像URL */
    private String avatar;

    /** 是否需要完善资料 */
    private Boolean needProfileComplete;
}
