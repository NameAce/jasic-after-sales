package com.jasic.aftersales.system.domain.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 微信 code2Session 结果
 *
 * @author Codex
 * @date 2026/04/02
 */
@Data
public class WechatAuthSession implements Serializable {

    private static final long serialVersionUID = 1L;

    /** openid */
    private String openid;

    /** session_key */
    private String sessionKey;
}
