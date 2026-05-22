package com.jasic.aftersales.system.domain.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 微信手机号结果
 *
 * @author Zoro
 * @date 2026/04/02
 */
@Data
public class WechatPhoneInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 完整手机号 */
    private String phoneNumber;

    /** 纯手机号 */
    private String purePhoneNumber;

    /** 国家区号 */
    private String countryCode;
}
