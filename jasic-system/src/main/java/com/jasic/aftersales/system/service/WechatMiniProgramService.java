package com.jasic.aftersales.system.service;

import cn.hutool.json.JSONObject;
import com.jasic.aftersales.system.domain.enums.WechatMiniProgramScene;
import com.jasic.aftersales.system.domain.model.WechatAuthSession;
import com.jasic.aftersales.system.domain.model.WechatPhoneInfo;

/**
 * 微信小程序能力服务
 *
 * @author Codex
 * @date 2026/04/02
 */
public interface WechatMiniProgramService {

    /**
     * 通过登录 code 换取 openid/unionid
     *
     * @param scene 小程序场景
     * @param code  登录 code
     * @return 会话结果
     */
    WechatAuthSession code2Session(WechatMiniProgramScene scene, String code);

    /**
     * 通过手机号 code 换取手机号
     *
     * @param scene     小程序场景
     * @param phoneCode 手机号 code
     * @return 手机号结果
     */
    WechatPhoneInfo getPhoneNumber(WechatMiniProgramScene scene, String phoneCode);

    /**
     * 发送小程序订阅消息
     *
     * @param scene      小程序场景
     * @param openid     接收人 openid
     * @param templateId 模板 ID
     * @param pagePath   跳转页
     * @param data       模板数据
     */
    void sendSubscribeMessage(WechatMiniProgramScene scene, String openid, String templateId, String pagePath,
                              JSONObject data);
}
