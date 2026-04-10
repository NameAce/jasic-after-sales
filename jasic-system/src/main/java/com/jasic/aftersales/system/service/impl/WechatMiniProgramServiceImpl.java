package com.jasic.aftersales.system.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.bean.WxMaCodeLineColor;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.binarywang.wx.miniapp.bean.WxMaPhoneNumberInfo;
import cn.binarywang.wx.miniapp.bean.WxMaSubscribeMessage;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.framework.web.ResultCode;
import com.jasic.aftersales.system.domain.enums.WechatMiniProgramScene;
import com.jasic.aftersales.system.domain.model.WechatAuthSession;
import com.jasic.aftersales.system.domain.model.WechatPhoneInfo;
import com.jasic.aftersales.system.service.ISysConfigService;
import com.jasic.aftersales.system.service.WechatMiniProgramService;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 微信小程序能力服务实现
 *
 * @author Codex
 * @date 2026/04/02
 */
@Slf4j
@Service
public class WechatMiniProgramServiceImpl implements WechatMiniProgramService {

    private final ConcurrentMap<String, WxMaServiceHolder> serviceHolderMap = new ConcurrentHashMap<>();

    @Resource
    private ISysConfigService sysConfigService;

    @Override
    public WechatAuthSession code2Session(WechatMiniProgramScene scene, String code) {
        if (StrUtil.isBlank(code)) {
            throw new ServiceException("微信登录凭证不能为空");
        }

        WxMaJscode2SessionResult result;
        try {
            result = getMaService(scene).jsCode2SessionInfo(code);
        } catch (WxErrorException ex) {
            log.error("调用微信 code2Session 失败，scene={}", scene.getCode(), ex);
            throw buildWxServiceException("微信登录失败", ex);
        } catch (Exception ex) {
            log.error("调用微信 code2Session 失败，scene={}", scene.getCode(), ex);
            throw new ServiceException(ResultCode.THIRD_PARTY_ERROR, "微信登录服务调用失败，请稍后重试");
        }

        WechatAuthSession session = new WechatAuthSession();
        session.setOpenid(result.getOpenid());
        session.setSessionKey(result.getSessionKey());
        if (StrUtil.isBlank(session.getOpenid())) {
            throw new ServiceException(ResultCode.THIRD_PARTY_ERROR, "微信登录失败，未获取到用户标识");
        }
        return session;
    }

    @Override
    public WechatPhoneInfo getPhoneNumber(WechatMiniProgramScene scene, String phoneCode) {
        if (StrUtil.isBlank(phoneCode)) {
            throw new ServiceException("微信手机号凭证不能为空");
        }

        WxMaPhoneNumberInfo result;
        try {
            result = getMaService(scene).getUserService().getPhoneNumber(phoneCode);
        } catch (WxErrorException ex) {
            log.error("调用微信获取手机号失败，scene={}", scene.getCode(), ex);
            throw buildWxServiceException("获取微信手机号失败", ex);
        } catch (Exception ex) {
            log.error("调用微信获取手机号失败，scene={}", scene.getCode(), ex);
            throw new ServiceException(ResultCode.THIRD_PARTY_ERROR, "微信手机号服务调用失败，请稍后重试");
        }

        WechatPhoneInfo phoneInfo = new WechatPhoneInfo();
        phoneInfo.setPhoneNumber(result.getPhoneNumber());
        phoneInfo.setPurePhoneNumber(result.getPurePhoneNumber());
        phoneInfo.setCountryCode(result.getCountryCode());
        if (StrUtil.isBlank(phoneInfo.getPhoneNumber())) {
            throw new ServiceException(ResultCode.THIRD_PARTY_ERROR, "获取微信手机号失败，未获取到手机号");
        }
        return phoneInfo;
    }

    @Override
    public String createQrcodeBase64(WechatMiniProgramScene scene, String sceneValue, String pagePath) {
        if (StrUtil.isBlank(sceneValue)) {
            throw new ServiceException("绑定票据不能为空");
        }
        if (StrUtil.isBlank(pagePath)) {
            throw new ServiceException("微信绑定页面未配置，请联系管理员");
        }

        byte[] qrcodeBytes;
        try {
            qrcodeBytes = getMaService(scene).getQrcodeService()
                    .createWxaCodeUnlimitBytes(sceneValue, pagePath, true, "release", 430, true,
                            (WxMaCodeLineColor) null, false);
        } catch (WxErrorException ex) {
            log.error("调用微信生成二维码失败，scene={}", scene.getCode(), ex);
            throw buildWxServiceException("生成微信绑定二维码失败", ex);
        } catch (Exception ex) {
            log.error("调用微信生成二维码失败，scene={}", scene.getCode(), ex);
            throw new ServiceException(ResultCode.THIRD_PARTY_ERROR, "生成微信绑定二维码失败，请稍后重试");
        }
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(qrcodeBytes);
    }

    @Override
    public void sendSubscribeMessage(WechatMiniProgramScene scene, String openid, String templateId, String pagePath,
                                     JSONObject data) {
        if (StrUtil.isBlank(openid)) {
            throw new ServiceException("消息接收人微信标识不能为空");
        }
        if (StrUtil.isBlank(templateId)) {
            throw new ServiceException("微信配置未完成");
        }

        WxMaSubscribeMessage message = new WxMaSubscribeMessage();
        message.setToUser(openid);
        message.setTemplateId(templateId);
        if (StrUtil.isNotBlank(pagePath)) {
            message.setPage(pagePath);
        }
        if (data != null && !data.isEmpty()) {
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                message.addData(buildMsgData(entry.getKey(), entry.getValue()));
            }
        }

        try {
            getMaService(scene).getMsgService().sendSubscribeMsg(message);
        } catch (WxErrorException ex) {
            log.error("调用微信订阅消息发送失败，scene={}, openid={}", scene.getCode(), openid, ex);
            throw buildWxServiceException("微信通知发送失败", ex);
        } catch (Exception ex) {
            log.error("调用微信订阅消息发送失败，scene={}, openid={}", scene.getCode(), openid, ex);
            throw new ServiceException(ResultCode.THIRD_PARTY_ERROR, "微信通知发送失败，请稍后重试");
        }
    }

    /**
     * 获取当前场景对应的小程序服务实例。
     */
    private WxMaService getMaService(WechatMiniProgramScene scene) {
        WechatMiniProgramConfig config = loadRequiredConfig(scene);
        WxMaServiceHolder holder = serviceHolderMap.get(scene.getCode());
        if (holder != null && holder.matches(config)) {
            return holder.getService();
        }

        synchronized (this) {
            holder = serviceHolderMap.get(scene.getCode());
            if (holder != null && holder.matches(config)) {
                return holder.getService();
            }
            WxMaService service = buildService(config);
            serviceHolderMap.put(scene.getCode(), new WxMaServiceHolder(config.getAppId(), config.getSecret(), service));
            return service;
        }
    }

    /**
     * 根据系统参数表配置构建 WxJava 小程序服务。
     */
    private WxMaService buildService(WechatMiniProgramConfig config) {
        WxMaDefaultConfigImpl wxConfig = new WxMaDefaultConfigImpl();
        wxConfig.setAppid(config.getAppId());
        wxConfig.setSecret(config.getSecret());

        WxMaService service = new WxMaServiceImpl();
        service.setWxMaConfig(wxConfig);
        return service;
    }

    private WechatMiniProgramConfig loadRequiredConfig(WechatMiniProgramScene scene) {
        String appId = sysConfigService.getValueByKey(scene.getAppIdKey());
        String secret = sysConfigService.getValueByKey(scene.getSecretKey());
        if (StrUtil.isBlank(appId) || StrUtil.isBlank(secret)) {
            throw new ServiceException(ResultCode.THIRD_PARTY_ERROR, "微信配置未完成，请联系管理员");
        }
        return new WechatMiniProgramConfig(appId, secret);
    }

    /**
     * 优先透传微信侧错误信息，便于区分是配置问题还是三方调用故障。
     *
     * @param defaultMessage 默认错误提示
     * @param ex 微信 SDK 异常
     * @return 统一业务异常
     */
    private ServiceException buildWxServiceException(String defaultMessage, WxErrorException ex) {
        String errorMessage = defaultMessage;
        if (ex != null && ex.getError() != null && StrUtil.isNotBlank(ex.getError().getErrorMsg())) {
            errorMessage = defaultMessage + "：" + ex.getError().getErrorMsg();
        }
        return new ServiceException(ResultCode.THIRD_PARTY_ERROR, errorMessage);
    }

    /**
     * 组装订阅消息字段，统一做空值兜底。
     *
     * @param name 字段名
     * @param valueObj 字段值
     * @return 订阅消息字段
     */
    private WxMaSubscribeMessage.MsgData buildMsgData(String name, Object valueObj) {
        WxMaSubscribeMessage.MsgData data = new WxMaSubscribeMessage.MsgData();
        data.setName(name);
        data.setValue(resolveTemplateValue(valueObj));
        return data;
    }

    /**
     * 微信模板字段为空时使用短横线占位，避免模板校验失败。
     *
     * @param valueObj 原始字段值
     * @return 模板可接受的字符串值
     */
    private String resolveTemplateValue(Object valueObj) {
        if (valueObj instanceof JSONObject) {
            return StrUtil.blankToDefault(((JSONObject) valueObj).getStr("value"), "-");
        }
        return StrUtil.blankToDefault(valueObj == null ? null : String.valueOf(valueObj), "-");
    }

    /**
     * 小程序配置快照
     */
    private static class WechatMiniProgramConfig {

        private final String appId;
        private final String secret;

        private WechatMiniProgramConfig(String appId, String secret) {
            this.appId = appId;
            this.secret = secret;
        }

        public String getAppId() {
            return appId;
        }

        public String getSecret() {
            return secret;
        }
    }

    /**
     * 场景级小程序服务缓存
     */
    private static class WxMaServiceHolder {

        private final String appId;
        private final String secret;
        private final WxMaService service;

        private WxMaServiceHolder(String appId, String secret, WxMaService service) {
            this.appId = appId;
            this.secret = secret;
            this.service = service;
        }

        public WxMaService getService() {
            return service;
        }

        public boolean matches(WechatMiniProgramConfig config) {
            return StrUtil.equals(appId, config.getAppId()) && StrUtil.equals(secret, config.getSecret());
        }
    }
}
