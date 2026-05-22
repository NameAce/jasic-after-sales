package com.jasic.aftersales.system.domain.enums;

import com.jasic.aftersales.common.constant.WechatConfigConstants;

/**
 * 小程序场景枚举
 *
 * @author Zoro
 * @date 2026/04/02
 */
public enum WechatMiniProgramScene {

    /** B 端承修方/总部小程序 */
    B("B", WechatConfigConstants.B_APP_ID, WechatConfigConstants.B_APP_SECRET),

    /** C 端终端用户小程序 */
    C("C", WechatConfigConstants.C_APP_ID, WechatConfigConstants.C_APP_SECRET);

    /**
     * 微信小程序程序场景编码。
     */
    private final String code;
    /**appIdKey 字段，用于当前类内部业务处理。*/
    private final String appIdKey;
    /**secretKey 字段，用于当前类内部业务处理。*/
    private final String secretKey;

    /**
     * 构造微信小程序程序场景实例。
     *
     * @param code 业务编码，用于匹配枚举、配置或外部系统数据。
     * @param appIdKey appIdKey，当前业务处理所需的输入值。
     * @param secretKey secretKey，当前业务处理所需的输入值。
     */
    WechatMiniProgramScene(String code, String appIdKey, String secretKey) {
        this.code = code;
        this.appIdKey = appIdKey;
        this.secretKey = secretKey;
    }

    /**
     * 获取微信小程序程序场景编码。
     *
     * @return 业务处理结果
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取AppIDKey。
     *
     * @return 业务处理结果
     */
    public String getAppIdKey() {
        return appIdKey;
    }

    /**
     * 获取SecretKey。
     *
     * @return 业务处理结果
     */
    public String getSecretKey() {
        return secretKey;
    }
}




