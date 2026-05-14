package com.jasic.aftersales.system.domain.enums;

import com.jasic.aftersales.common.constant.WechatConfigConstants;

/**
 * 小程序场景枚举
 *
 * @author Codex
 * @date 2026/04/02
 */
public enum WechatMiniProgramScene {

    /** B 端承修方/总部小程序 */
    B("B", WechatConfigConstants.B_APP_ID, WechatConfigConstants.B_APP_SECRET),

    /** C 端终端用户小程序 */
    C("C", WechatConfigConstants.C_APP_ID, WechatConfigConstants.C_APP_SECRET);

    /**
     * ?? WechatMiniProgramScene ?????
     *
     * @param code ??
     * @param appIdKey ??
     * @param secretKey ??
     * @return ????
     */
    private final String code;
    private final String appIdKey;
    private final String secretKey;

    WechatMiniProgramScene(String code, String appIdKey, String secretKey) {
        this.code = code;
        this.appIdKey = appIdKey;
        this.secretKey = secretKey;
    }

    /**
     * ?????
     *
     * @return ?????
     */
    public String getCode() {
        return code;
    }

    /**
     * ??App Id Key?
     *
     * @return ?????
     */
    public String getAppIdKey() {
        return appIdKey;
    }

    /**
     * ??Secret Key?
     *
     * @return ?????
     */
    public String getSecretKey() {
        return secretKey;
    }
}
