package com.jasic.aftersales.system.domain.model;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 微信绑定票据会话
 *
 * @author Codex
 * @date 2026/04/02
 */
@Data
public class WechatBindSession implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long userId;

    /** 绑定票据 */
    private String bindTicket;

    /** 过期时间 */
    private LocalDateTime expireAt;
}


