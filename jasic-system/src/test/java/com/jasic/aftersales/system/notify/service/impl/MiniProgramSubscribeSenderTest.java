package com.jasic.aftersales.system.notify.service.impl;

import cn.hutool.json.JSONObject;
import com.jasic.aftersales.system.domain.enums.WechatMiniProgramScene;
import com.jasic.aftersales.system.notify.domain.dto.NotifyChannelFieldMappingDTO;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyDispatch;
import com.jasic.aftersales.system.notify.domain.enums.NotifyChannelTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyDispatchStatusEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyReceiverTypeEnum;
import com.jasic.aftersales.system.notify.support.NotifyChannelSendContext;
import com.jasic.aftersales.system.notify.support.NotifyChannelSendResult;
import com.jasic.aftersales.system.notify.support.NotifyDispatchPayload;
import com.jasic.aftersales.system.notify.support.NotifyTemplateChannelConfig;
import com.jasic.aftersales.system.service.WechatMiniProgramService;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 小程序 sender 测试。
 *
 * <p>小程序 B/C 端归属必须由场景目标配置中的 `channelScene` 明确声明，
 * 避免发送阶段再次根据接收对象或旧场景后缀猜测目标小程序。</p>
 *
 * @author Zoro
 * @date 2026/05/16
 */
public class MiniProgramSubscribeSenderTest {

    /**
     * 维修员通知应按显式渠道场景发送到 B 端小程序。
     *
     * @throws Exception 反射异常
     */
    @Test
    public void shouldRouteRepairerDispatchToMiniProgramB() throws Exception {
        MiniProgramSubscribeSender sender = new MiniProgramSubscribeSender();
        SendCapture capture = new SendCapture();
        setField(sender, "wechatMiniProgramService", createWechatService(capture));

        NotifyChannelSendResult result = sender.send(buildContext(
                "WORK_ORDER_ASSIGNED",
                NotifyReceiverTypeEnum.REPAIRER.getCode(),
                "openid-repairer",
                "B"
        ));

        Assert.assertEquals(NotifyDispatchStatusEnum.SUCCESS.getCode(), result.getDispatchStatus());
        Assert.assertEquals(WechatMiniProgramScene.B, capture.scene);
    }

    /**
     * 客户通知应按显式渠道场景发送到 C 端小程序。
     *
     * @throws Exception 反射异常
     */
    @Test
    public void shouldRouteCustomerDispatchToMiniProgramC() throws Exception {
        MiniProgramSubscribeSender sender = new MiniProgramSubscribeSender();
        SendCapture capture = new SendCapture();
        setField(sender, "wechatMiniProgramService", createWechatService(capture));

        NotifyChannelSendResult result = sender.send(buildContext(
                "WORK_ORDER_EVALUATION_INVITE",
                NotifyReceiverTypeEnum.CUSTOMER.getCode(),
                "openid-customer",
                "C"
        ));

        Assert.assertEquals(NotifyDispatchStatusEnum.SUCCESS.getCode(), result.getDispatchStatus());
        Assert.assertEquals(WechatMiniProgramScene.C, capture.scene);
    }

    /**
     * 显式配置了小程序场景时，应直接按配置发送。
     *
     * @throws Exception 反射异常
     */
    @Test
    public void shouldPreferConfiguredChannelScene() throws Exception {
        MiniProgramSubscribeSender sender = new MiniProgramSubscribeSender();
        SendCapture capture = new SendCapture();
        setField(sender, "wechatMiniProgramService", createWechatService(capture));

        NotifyChannelSendResult result = sender.send(buildContext(
                "WORK_ORDER_ASSIGNED",
                NotifyReceiverTypeEnum.REPAIRER.getCode(),
                "openid-repairer",
                "C"
        ));

        Assert.assertEquals(NotifyDispatchStatusEnum.SUCCESS.getCode(), result.getDispatchStatus());
        Assert.assertEquals(WechatMiniProgramScene.C, capture.scene);
    }

    /**
     * time 类型字段应转换成微信 `time.DATA` 可接受的时间格式，避免 `LocalDateTime#toString()` 触发 47003。
     *
     * @throws Exception 反射异常
     */
    @Test
    public void shouldNormalizeWechatTimeFieldValue() throws Exception {
        MiniProgramSubscribeSender sender = new MiniProgramSubscribeSender();
        SendCapture capture = new SendCapture();
        setField(sender, "wechatMiniProgramService", createWechatService(capture));

        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("workOrderId", 88L);
        variables.put("closedTime", LocalDateTime.of(2026, 5, 27, 14, 30, 45));

        NotifyChannelSendResult result = sender.send(buildContext(
                "WORK_ORDER_EVALUATION_INVITE",
                NotifyReceiverTypeEnum.CUSTOMER.getCode(),
                "openid-customer",
                "C",
                "time4",
                "${closedTime}",
                variables
        ));

        Assert.assertEquals(NotifyDispatchStatusEnum.SUCCESS.getCode(), result.getDispatchStatus());
        Assert.assertEquals("2026-05-27 14:30:45", capture.data.getStr("time4"));
    }

    /**
     * 构造发送上下文。
     *
     * @param sceneCode 场景编码
     * @param receiverType 接收对象类型
     * @param openid 接收人openid
     * @param channelScene 显式小程序场景
     * @return 发送上下文
     */
    private NotifyChannelSendContext buildContext(String sceneCode, String receiverType, String openid, String channelScene) {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("workOrderId", 88L);
        variables.put("orderNo", "WO-88");
        return buildContext(sceneCode, receiverType, openid, channelScene, "thing1", "${orderNo}", variables);
    }

    /**
     * 构造支持自定义字段映射与变量的发送上下文，便于覆盖不同模板字段规则。
     *
     * @param sceneCode 场景编码
     * @param receiverType 接收对象类型
     * @param openid 接收人 openid
     * @param channelScene 显式小程序场景
     * @param fieldName 模板字段名
     * @param fieldValueTemplate 模板字段取值模板
     * @param variables 变量快照
     * @return 发送上下文
     */
    private NotifyChannelSendContext buildContext(String sceneCode, String receiverType, String openid, String channelScene,
                                                  String fieldName, String fieldValueTemplate, Map<String, Object> variables) {
        NotifyTemplateChannelConfig config = new NotifyTemplateChannelConfig();
        config.setTemplateId("wx-template-001");
        config.setChannelScene(channelScene);
        config.setPagePathTemplate("pages/order/detail?workOrderId=${workOrderId}");
        NotifyChannelFieldMappingDTO fieldMapping = new NotifyChannelFieldMappingDTO();
        fieldMapping.setField(fieldName);
        fieldMapping.setValue(fieldValueTemplate);
        config.setFieldMapping(Collections.singletonList(fieldMapping));

        NotifyDispatchPayload payload = new NotifyDispatchPayload();
        payload.setSceneCode(sceneCode);
        payload.setTemplateCode(sceneCode);
        payload.setChannelType(NotifyChannelTypeEnum.MP_SUBSCRIBE.getCode());
        payload.setChannelConfig(config);
        payload.setVariables(variables);

        SysNotifyDispatch dispatch = new SysNotifyDispatch();
        dispatch.setReceiverType(receiverType);
        dispatch.setReceiverAddress(openid);

        NotifyChannelSendContext context = new NotifyChannelSendContext();
        context.setDispatch(dispatch);
        context.setPayload(payload);
        return context;
    }

    /**
     * 构造微信服务代理。
     *
     * @param capture 调用捕获器
     * @return 微信服务代理
     */
    private WechatMiniProgramService createWechatService(SendCapture capture) {
        InvocationHandler handler = (proxy, method, args) -> {
            if ("sendSubscribeMessage".equals(method.getName())) {
                capture.scene = (WechatMiniProgramScene) args[0];
                capture.openid = (String) args[1];
                capture.templateId = (String) args[2];
                capture.pagePath = (String) args[3];
                capture.data = (JSONObject) args[4];
            }
            return null;
        };
        return (WechatMiniProgramService) Proxy.newProxyInstance(
                WechatMiniProgramService.class.getClassLoader(),
                new Class<?>[]{WechatMiniProgramService.class},
                handler
        );
    }

    /**
     * 反射设置字段。
     *
     * @param target 目标对象
     * @param fieldName 字段名
     * @param value 字段值
     * @throws Exception 反射异常
     */
    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /**
     * 发送调用捕获器。
     */
    private static class SendCapture {
        /**scene 字段，用于当前类内部业务处理。*/
        private WechatMiniProgramScene scene;
        /**openid 字段，用于当前类内部业务处理。*/
        private String openid;
        /**templateId 字段，用于当前类内部业务处理。*/
        private String templateId;
        /**pagePath 字段，用于当前类内部业务处理。*/
        private String pagePath;
        /**data 字段，用于当前类内部业务处理。*/
        private JSONObject data;
    }
}
