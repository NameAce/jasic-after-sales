package com.jasic.aftersales.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 微信绑定状态
 *
 * @author Codex
 * @date 2026/04/02
 */
@Data
public class WechatBindStatusVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否已绑定 */
    private Boolean bound;

    /** 当前待使用绑定码 */
    private String bindCode;

    /** 绑定微信 openid 脱敏展示 */
    private String maskedOpenid;

    /** 微信授权手机号快照 */
    private String wechatPhone;

    /** 绑定码过期时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireAt;
}
