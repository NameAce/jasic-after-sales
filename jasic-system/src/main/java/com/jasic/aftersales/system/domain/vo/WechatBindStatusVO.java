package com.jasic.aftersales.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 微信绑定状态
 *
 * @author Codex
 * @date 2026/04/02
 */
@ApiModel(description = "微信绑定状态")
@Data
public class WechatBindStatusVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否已绑定 */
    @ApiModelProperty(value = "是否已绑定")
    private Boolean bound;

    /** 绑定微信 openid 脱敏展示 */
    @ApiModelProperty(value = "绑定微信 openid 脱敏展示")
    private String maskedOpenid;

    /** 微信授权手机号快照 */
    @ApiModelProperty(value = "微信授权手机号快照")
    private String wechatPhone;

    /** 绑定票据过期时间 */
    @ApiModelProperty(value = "绑定票据过期时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireAt;

    /** 是否存在有效绑定票据 */
    @ApiModelProperty(value = "是否存在有效绑定票据")
    private Boolean hasActiveTicket;

    /** 绑定二维码 base64 */
    @ApiModelProperty(value = "绑定二维码 base64")
    private String qrImageBase64;
}


