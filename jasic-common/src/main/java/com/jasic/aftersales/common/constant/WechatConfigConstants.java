package com.jasic.aftersales.common.constant;

/**
 * 微信配置参数键常量
 *
 * @author Codex
 * @date 2026/04/02
 */
public class WechatConfigConstants {

    /**
     * 禁止实例化微信配置常量类。
     */
    private WechatConfigConstants() {
    }

    /** B 端小程序 appid */
    public static final String B_APP_ID = "wechat.mp.b.appid";

    /** B 端小程序 secret */
    public static final String B_APP_SECRET = "wechat.mp.b.secret";

    /** C 端小程序 appid */
    public static final String C_APP_ID = "wechat.mp.c.appid";

    /** C 端小程序 secret */
    public static final String C_APP_SECRET = "wechat.mp.c.secret";

    /** B 端微信绑定小程序页面路径 */
    public static final String B_BIND_PAGE_PATH = "wechat.mp.b.bind.pagePath";
}
