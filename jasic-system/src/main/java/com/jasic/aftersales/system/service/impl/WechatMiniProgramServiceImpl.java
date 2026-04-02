package com.jasic.aftersales.system.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.jasic.aftersales.common.constant.CacheConstants;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.framework.web.ResultCode;
import com.jasic.aftersales.system.domain.enums.WechatMiniProgramScene;
import com.jasic.aftersales.system.domain.model.WechatAuthSession;
import com.jasic.aftersales.system.domain.model.WechatPhoneInfo;
import com.jasic.aftersales.system.service.ISysConfigService;
import com.jasic.aftersales.system.service.WechatMiniProgramService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 微信小程序能力服务实现
 *
 * @author Codex
 * @date 2026/04/02
 */
@Slf4j
@Service
public class WechatMiniProgramServiceImpl implements WechatMiniProgramService {

    private static final String CODE_2_SESSION_URL = "https://api.weixin.qq.com/sns/jscode2session";
    private static final String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token";
    private static final String PHONE_NUMBER_URL = "https://api.weixin.qq.com/wxa/business/getuserphonenumber";
    private static final String SUBSCRIBE_SEND_URL = "https://api.weixin.qq.com/cgi-bin/message/subscribe/send";
    private static final int HTTP_TIMEOUT_MS = 5000;

    @Resource
    private ISysConfigService sysConfigService;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public WechatAuthSession code2Session(WechatMiniProgramScene scene, String code) {
        if (StrUtil.isBlank(code)) {
            throw new ServiceException("微信登录 code 不能为空");
        }
        WechatMiniProgramConfig config = loadRequiredConfig(scene);

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("appid", config.getAppId());
        params.put("secret", config.getSecret());
        params.put("js_code", code);
        params.put("grant_type", "authorization_code");

        String responseBody;
        try {
            responseBody = HttpUtil.get(CODE_2_SESSION_URL, params, HTTP_TIMEOUT_MS);
        } catch (Exception ex) {
            log.error("调用微信 code2Session 失败，scene={}", scene.getCode(), ex);
            throw new ServiceException(ResultCode.THIRD_PARTY_ERROR, "微信登录服务调用失败，请稍后重试");
        }

        JSONObject response = parseResponse(responseBody, "微信登录失败");
        WechatAuthSession session = new WechatAuthSession();
        session.setOpenid(response.getStr("openid"));
        session.setUnionid(response.getStr("unionid"));
        session.setSessionKey(response.getStr("session_key"));
        if (StrUtil.isBlank(session.getOpenid())) {
            throw new ServiceException(ResultCode.THIRD_PARTY_ERROR, "微信登录失败，未获取到用户标识");
        }
        return session;
    }

    @Override
    public WechatPhoneInfo getPhoneNumber(WechatMiniProgramScene scene, String phoneCode) {
        if (StrUtil.isBlank(phoneCode)) {
            throw new ServiceException("微信手机号 code 不能为空");
        }
        String accessToken = getAccessToken(scene);
        JSONObject body = JSONUtil.createObj().set("code", phoneCode);

        String responseBody;
        try {
            responseBody = HttpRequest.post(PHONE_NUMBER_URL + "?access_token=" + accessToken)
                    .timeout(HTTP_TIMEOUT_MS)
                    .body(body.toString(), ContentType.JSON.getValue())
                    .execute()
                    .body();
        } catch (Exception ex) {
            log.error("调用微信获取手机号失败，scene={}", scene.getCode(), ex);
            throw new ServiceException(ResultCode.THIRD_PARTY_ERROR, "微信手机号服务调用失败，请稍后重试");
        }

        JSONObject response = parseResponse(responseBody, "获取微信手机号失败");
        JSONObject phoneInfoObj = response.getJSONObject("phone_info");
        if (phoneInfoObj == null) {
            throw new ServiceException(ResultCode.THIRD_PARTY_ERROR, "获取微信手机号失败，返回结果异常");
        }
        WechatPhoneInfo phoneInfo = new WechatPhoneInfo();
        phoneInfo.setPhoneNumber(phoneInfoObj.getStr("phoneNumber"));
        phoneInfo.setPurePhoneNumber(phoneInfoObj.getStr("purePhoneNumber"));
        phoneInfo.setCountryCode(phoneInfoObj.getStr("countryCode"));
        if (StrUtil.isBlank(phoneInfo.getPhoneNumber())) {
            throw new ServiceException(ResultCode.THIRD_PARTY_ERROR, "获取微信手机号失败，未获取到手机号");
        }
        return phoneInfo;
    }

    @Override
    public void sendSubscribeMessage(WechatMiniProgramScene scene, String openid, String templateId, String pagePath,
                                     JSONObject data) {
        if (StrUtil.isBlank(openid)) {
            throw new ServiceException("消息接收人 openid 不能为空");
        }
        if (StrUtil.isBlank(templateId)) {
            throw new ServiceException("微信配置未完成");
        }
        String accessToken = getAccessToken(scene);
        JSONObject body = JSONUtil.createObj()
                .set("touser", openid)
                .set("template_id", templateId)
                .set("data", data == null ? JSONUtil.createObj() : data);
        if (StrUtil.isNotBlank(pagePath)) {
            body.set("page", pagePath);
        }

        String responseBody;
        try {
            responseBody = HttpRequest.post(SUBSCRIBE_SEND_URL + "?access_token=" + accessToken)
                    .timeout(HTTP_TIMEOUT_MS)
                    .body(body.toString(), ContentType.JSON.getValue())
                    .execute()
                    .body();
        } catch (Exception ex) {
            log.error("调用微信订阅消息发送失败，scene={}, openid={}", scene.getCode(), openid, ex);
            throw new ServiceException(ResultCode.THIRD_PARTY_ERROR, "微信通知发送失败，请稍后重试");
        }
        parseResponse(responseBody, "微信通知发送失败");
    }

    private String getAccessToken(WechatMiniProgramScene scene) {
        String cacheKey = CacheConstants.WECHAT_ACCESS_TOKEN_KEY + scene.getCode();
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null && StrUtil.isNotBlank(String.valueOf(cached))) {
            return String.valueOf(cached);
        }

        WechatMiniProgramConfig config = loadRequiredConfig(scene);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("grant_type", "client_credential");
        params.put("appid", config.getAppId());
        params.put("secret", config.getSecret());

        String responseBody;
        try {
            responseBody = HttpUtil.get(ACCESS_TOKEN_URL, params, HTTP_TIMEOUT_MS);
        } catch (Exception ex) {
            log.error("调用微信 access_token 接口失败，scene={}", scene.getCode(), ex);
            throw new ServiceException(ResultCode.THIRD_PARTY_ERROR, "微信服务调用失败，请稍后重试");
        }

        JSONObject response = parseResponse(responseBody, "获取微信 access_token 失败");
        String accessToken = response.getStr("access_token");
        Integer expiresIn = response.getInt("expires_in");
        if (StrUtil.isBlank(accessToken) || expiresIn == null || expiresIn <= 0) {
            throw new ServiceException(ResultCode.THIRD_PARTY_ERROR, "获取微信 access_token 失败，返回结果异常");
        }
        long ttlSeconds = Math.max(60L, expiresIn - 200L);
        redisTemplate.opsForValue().set(cacheKey, accessToken, ttlSeconds, TimeUnit.SECONDS);
        return accessToken;
    }

    private WechatMiniProgramConfig loadRequiredConfig(WechatMiniProgramScene scene) {
        String appId = sysConfigService.getValueByKey(scene.getAppIdKey());
        String secret = sysConfigService.getValueByKey(scene.getSecretKey());
        if (StrUtil.isBlank(appId) || StrUtil.isBlank(secret)) {
            throw new ServiceException(ResultCode.THIRD_PARTY_ERROR, "微信配置未完成，请联系管理员");
        }
        return new WechatMiniProgramConfig(appId, secret);
    }

    private JSONObject parseResponse(String responseBody, String defaultMessage) {
        try {
            JSONObject response = JSONUtil.parseObj(responseBody);
            Integer errCode = response.getInt("errcode");
            if (errCode != null && errCode != 0) {
                String errMsg = StrUtil.blankToDefault(response.getStr("errmsg"), defaultMessage);
                throw new ServiceException(ResultCode.THIRD_PARTY_ERROR, defaultMessage + "：" + errMsg);
            }
            return response;
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("解析微信接口响应失败，response={}", responseBody, ex);
            throw new ServiceException(ResultCode.THIRD_PARTY_ERROR, defaultMessage);
        }
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
}
