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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 小程序 sender 测试。
 *
 * <p>阶段二开始，同一 `sceneCode` 下可能存在维修员和客户两类小程序目标，
 * 因此 sender 必须优先按接收对象类型选择 B/C 端，而不是继续依赖旧的场景编码后缀。</p>
 *
 * @author Codex
 * @date 2026/05/16
 */
public class MiniProgramSubscribeSenderTest {

    /**
     * 维修员通知应发送到 B 端小程序。
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
                "openid-repairer"
        ));

        Assert.assertEquals(NotifyDispatchStatusEnum.SUCCESS.getCode(), result.getDispatchStatus());
        Assert.assertEquals(WechatMiniProgramScene.B, capture.scene);
    }

    /**
     * 客户通知应发送到 C 端小程序。
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
                "openid-customer"
        ));

        Assert.assertEquals(NotifyDispatchStatusEnum.SUCCESS.getCode(), result.getDispatchStatus());
        Assert.assertEquals(WechatMiniProgramScene.C, capture.scene);
    }

    /**
     * 构造发送上下文。
     *
     * @param sceneCode 场景编码
     * @param receiverType 接收对象类型
     * @param openid 接收人openid
     * @return 发送上下文
     */
    private NotifyChannelSendContext buildContext(String sceneCode, String receiverType, String openid) {
        NotifyTemplateChannelConfig config = new NotifyTemplateChannelConfig();
        config.setTemplateId("wx-template-001");
        config.setPagePathTemplate("pages/order/detail?workOrderId=${workOrderId}");
        NotifyChannelFieldMappingDTO fieldMapping = new NotifyChannelFieldMappingDTO();
        fieldMapping.setField("thing1");
        fieldMapping.setValue("${orderNo}");
        config.setFieldMapping(Collections.singletonList(fieldMapping));

        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("workOrderId", 88L);
        variables.put("orderNo", "WO-88");

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
        private WechatMiniProgramScene scene;
        private String openid;
        private String templateId;
        private String pagePath;
        private JSONObject data;
    }
}
