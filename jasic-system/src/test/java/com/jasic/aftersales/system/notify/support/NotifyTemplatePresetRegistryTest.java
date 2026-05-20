package com.jasic.aftersales.system.notify.support;

import com.jasic.aftersales.system.notify.domain.dto.NotifyChannelFieldMappingDTO;
import com.jasic.aftersales.system.notify.domain.enums.NotifyBizTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyChannelSceneEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyChannelTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyReceiverTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyTypeEnum;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * 通知场景注册表测试。
 *
 * <p>该测试聚焦当前通知场景注册表暴露的默认元数据，
 * 确保后台默认配置、模板字段映射和渠道归属与现行代码口径一致。</p>
 *
 * @author Codex
 * @date 2026/05/20
 */
public class NotifyTemplatePresetRegistryTest {

    /**
     * 基础枚举应能解析当前通知场景依赖的业务和渠道语义。
     */
    @Test
    public void shouldResolvePhaseOneEnums() {
        Assert.assertEquals("工单", NotifyBizTypeEnum.fromCode("WORK_ORDER").getDesc());
        Assert.assertEquals("B端派单用户", NotifyReceiverTypeEnum.fromCode("ASSIGN_USER").getDesc());
        Assert.assertEquals("B端接单用户", NotifyReceiverTypeEnum.fromCode("ACCEPT_USER").getDesc());
        Assert.assertEquals("B端评价提醒接收人", NotifyReceiverTypeEnum.fromCode("EVALUATED_B_USER").getDesc());
        Assert.assertEquals("小程序订阅消息(B端)", NotifyTypeEnum.fromCode("MP_SUBSCRIBE_B").getDesc());
        Assert.assertEquals("小程序订阅消息", NotifyChannelTypeEnum.fromCode("MP_SUBSCRIBE").getDesc());
    }

    /**
     * 注册表应完整暴露当前 7 个通知场景，并携带默认模板配置。
     */
    @Test
    public void shouldExposeStageOneSceneMetas() {
        NotifySceneRegistry registry = new NotifySceneRegistry();

        List<NotifySceneMeta> sceneMetas = registry.listScenes();
        Assert.assertEquals(7, sceneMetas.size());

        assertAcceptScene(registry.getRequiredScene(NotifySceneCode.WORK_ORDER_ACCEPT.getCode()));
        assertTransferInScene(registry.getRequiredScene(NotifySceneCode.WORK_ORDER_TRANSFER_IN.getCode()));
        assertAssignedScene(registry.getRequiredScene(NotifySceneCode.WORK_ORDER_ASSIGNED.getCode()));
        assertEvaluatedScene(registry.getRequiredScene(NotifySceneCode.WORK_ORDER_EVALUATED.getCode()));
        assertAcceptedScene(registry.getRequiredScene(NotifySceneCode.WORK_ORDER_ACCEPTED.getCode()));
        assertTransferNoticeScene(registry.getRequiredScene(NotifySceneCode.WORK_ORDER_TRANSFER_NOTICE.getCode()));
        assertEvaluationInviteScene(registry.getRequiredScene(NotifySceneCode.WORK_ORDER_EVALUATION_INVITE.getCode()));
    }

    /**
     * 系统级目标池应返回系统支持的全量目标候选项。
     */
    @Test
    public void shouldExposeSystemLevelTargetPool() {
        NotifySceneRegistry registry = new NotifySceneRegistry();

        List<NotifySceneTargetMeta> targetMetas = registry.listSystemTargetMetas();
        Assert.assertEquals(4, targetMetas.size());

        NotifySceneTargetMeta inAppMessage = registry.getRequiredSystemTargetMeta(NotifyTypeEnum.IN_APP_MESSAGE.getCode());
        Assert.assertEquals(NotifyReceiverTypeEnum.REPAIRER.getCode(), inAppMessage.getReceiverType());
        Assert.assertNull(inAppMessage.getChannelType());

        NotifySceneTargetMeta mpSubscribeB = registry.getRequiredSystemTargetMeta(NotifyTypeEnum.MP_SUBSCRIBE_B.getCode());
        Assert.assertNull(mpSubscribeB.getReceiverType());
        Assert.assertEquals(NotifyChannelTypeEnum.MP_SUBSCRIBE.getCode(), mpSubscribeB.getChannelType());
        Assert.assertNull(mpSubscribeB.getDefaultContentTemplate());
        Assert.assertNull(mpSubscribeB.getDefaultChannelConfig());

        NotifySceneTargetMeta mpSubscribeC = registry.getRequiredSystemTargetMeta(NotifyTypeEnum.MP_SUBSCRIBE_C.getCode());
        Assert.assertEquals(NotifyReceiverTypeEnum.CUSTOMER.getCode(), mpSubscribeC.getReceiverType());
        Assert.assertEquals(NotifyChannelTypeEnum.MP_SUBSCRIBE.getCode(), mpSubscribeC.getChannelType());
        Assert.assertNull(mpSubscribeC.getDefaultContentTemplate());
        Assert.assertNull(mpSubscribeC.getDefaultChannelConfig());
    }

    /**
     * 校验 B 端待派单通知默认配置。
     */
    private void assertAcceptScene(NotifySceneMeta sceneMeta) {
        Assert.assertEquals("B端待派单通知", sceneMeta.getSceneName());
        Assert.assertEquals(NotifyTypeEnum.MP_SUBSCRIBE_B.getCode(), sceneMeta.getDefaultTargetType());
        Assert.assertTrue(containsVariable(sceneMeta, "customerName", "客户姓名"));

        NotifySceneTargetMeta targetMeta = sceneMeta.getTargetMeta(NotifyTypeEnum.MP_SUBSCRIBE_B.getCode());
        Assert.assertEquals(NotifyReceiverTypeEnum.ASSIGN_USER.getCode(), targetMeta.getReceiverType());
        Assert.assertEquals(Integer.valueOf(1), targetMeta.getDefaultEnabled());
        Assert.assertEquals("WORK_ORDER_DETAIL", targetMeta.getDefaultRouteType());
        Assert.assertEquals("${workOrderId}", targetMeta.getDefaultRouteValueTemplate());
        Assert.assertEquals(NotifyChannelSceneEnum.B.getCode(), targetMeta.getDefaultChannelConfig().getChannelScene());
        Assert.assertEquals("JEO-zVGuWBQPIhU0ck7e3I97Tlr1tNk1ouxbbLovCCE",
                targetMeta.getDefaultChannelConfig().getTemplateId());
        Assert.assertEquals("pages/order/detail?workOrderId=${workOrderId}",
                targetMeta.getDefaultChannelConfig().getPagePathTemplate());
        assertFieldMapping(targetMeta.getDefaultChannelConfig().getFieldMapping(), 0, "character_string14", "${orderNo}");
        assertFieldMapping(targetMeta.getDefaultChannelConfig().getFieldMapping(), 1, "thing15", "${customerName}");
    }

    /**
     * 校验 B 端工单转入通知默认配置。
     */
    private void assertTransferInScene(NotifySceneMeta sceneMeta) {
        Assert.assertEquals("B端工单转入通知", sceneMeta.getSceneName());
        Assert.assertTrue(containsVariable(sceneMeta, "fromCompanyName", "转出网点名称"));

        NotifySceneTargetMeta targetMeta = sceneMeta.getTargetMeta(NotifyTypeEnum.MP_SUBSCRIBE_B.getCode());
        Assert.assertEquals("WORK_ORDER_DETAIL", targetMeta.getDefaultRouteType());
        Assert.assertEquals("${workOrderId}", targetMeta.getDefaultRouteValueTemplate());
        Assert.assertEquals(NotifyChannelSceneEnum.B.getCode(), targetMeta.getDefaultChannelConfig().getChannelScene());
        Assert.assertEquals("mw7ebqsdXbJxdQf-A_9161z0CdEVRGSi_I-gQY3dONw",
                targetMeta.getDefaultChannelConfig().getTemplateId());
        assertFieldMapping(targetMeta.getDefaultChannelConfig().getFieldMapping(), 3, "thing4", "${fromCompanyName}");
    }

    /**
     * 校验 B 端维修员接单通知默认配置。
     */
    private void assertAssignedScene(NotifySceneMeta sceneMeta) {
        Assert.assertEquals("B端维修员接单通知", sceneMeta.getSceneName());
        Assert.assertEquals(NotifyTypeEnum.IN_APP_TODO.getCode(), sceneMeta.getDefaultTargetType());
        Assert.assertTrue(containsVariable(sceneMeta, "customerMobile", "客户联系电话"));

        NotifySceneTargetMeta mpTargetMeta = sceneMeta.getTargetMeta(NotifyTypeEnum.MP_SUBSCRIBE_B.getCode());
        Assert.assertEquals(Integer.valueOf(1), mpTargetMeta.getDefaultEnabled());
        Assert.assertEquals("WORK_ORDER_DETAIL", mpTargetMeta.getDefaultRouteType());
        Assert.assertEquals("${workOrderId}", mpTargetMeta.getDefaultRouteValueTemplate());
        Assert.assertEquals(NotifyChannelSceneEnum.B.getCode(), mpTargetMeta.getDefaultChannelConfig().getChannelScene());
        Assert.assertEquals("hhXhuNSWE4r98FbVMX8MfveAzBq3h7-QtfAMVOB2fTg",
                mpTargetMeta.getDefaultChannelConfig().getTemplateId());
        assertFieldMapping(mpTargetMeta.getDefaultChannelConfig().getFieldMapping(), 1, "thing15", "${customerName}");
        assertFieldMapping(mpTargetMeta.getDefaultChannelConfig().getFieldMapping(), 2, "phone_number16", "${customerMobile}");
    }

    /**
     * 校验 B 端客户评价完成提醒默认配置。
     */
    private void assertEvaluatedScene(NotifySceneMeta sceneMeta) {
        Assert.assertEquals("B端评价提醒", sceneMeta.getSceneName());
        Assert.assertEquals(NotifyTypeEnum.MP_SUBSCRIBE_B.getCode(), sceneMeta.getDefaultTargetType());
        Assert.assertTrue(containsVariable(sceneMeta, "assignedUserName", "最终责任维修员展示名称"));

        NotifySceneTargetMeta targetMeta = sceneMeta.getTargetMeta(NotifyTypeEnum.MP_SUBSCRIBE_B.getCode());
        Assert.assertEquals(NotifyReceiverTypeEnum.EVALUATED_B_USER.getCode(), targetMeta.getReceiverType());
        Assert.assertEquals(Integer.valueOf(1), targetMeta.getDefaultEnabled());
        Assert.assertEquals("评价提醒", targetMeta.getDefaultTitleTemplate());
        Assert.assertEquals("WORK_ORDER_DETAIL", targetMeta.getDefaultRouteType());
        Assert.assertEquals("${workOrderId}", targetMeta.getDefaultRouteValueTemplate());
        Assert.assertEquals(NotifyChannelSceneEnum.B.getCode(), targetMeta.getDefaultChannelConfig().getChannelScene());
        Assert.assertEquals("aW97dc0OyW40-vGbO9ekIT9DFfyS6JvR9UhPkPuaW_Q",
                targetMeta.getDefaultChannelConfig().getTemplateId());
        assertFieldMapping(targetMeta.getDefaultChannelConfig().getFieldMapping(), 0, "character_string8", "${orderNo}");
        assertFieldMapping(targetMeta.getDefaultChannelConfig().getFieldMapping(), 1, "thing9", "${customerName}");
        assertFieldMapping(targetMeta.getDefaultChannelConfig().getFieldMapping(), 2, "phone_number10", "${customerMobile}");
        assertFieldMapping(targetMeta.getDefaultChannelConfig().getFieldMapping(), 3, "thing11", "${assignedUserName}");
    }

    /**
     * 校验 C 端接单成功提醒默认配置。
     */
    private void assertAcceptedScene(NotifySceneMeta sceneMeta) {
        Assert.assertEquals("C端接单成功提醒", sceneMeta.getSceneName());
        Assert.assertTrue(containsVariable(sceneMeta, "companyPhone", "service_phone"));

        NotifySceneTargetMeta targetMeta = sceneMeta.getTargetMeta(NotifyTypeEnum.MP_SUBSCRIBE_C.getCode());
        Assert.assertEquals("WORK_ORDER_DETAIL", targetMeta.getDefaultRouteType());
        Assert.assertEquals("${workOrderId}", targetMeta.getDefaultRouteValueTemplate());
        Assert.assertEquals(NotifyChannelSceneEnum.C.getCode(), targetMeta.getDefaultChannelConfig().getChannelScene());
        Assert.assertEquals("_p97aAe9-FJ2c6lCcZjVMQgxDnvBz8q6IRdFnnjIyWg",
                targetMeta.getDefaultChannelConfig().getTemplateId());
        assertFieldMapping(targetMeta.getDefaultChannelConfig().getFieldMapping(), 2, "phone_number11", "${companyPhone}");
    }

    /**
     * 校验 C 端网点转单通知默认配置。
     */
    private void assertTransferNoticeScene(NotifySceneMeta sceneMeta) {
        Assert.assertEquals("C端网点转单通知", sceneMeta.getSceneName());
        Assert.assertTrue(containsVariable(sceneMeta, "toCompanyName", "当前处理网点名称"));
        Assert.assertTrue(containsVariable(sceneMeta, "transferTip", null));

        NotifySceneTargetMeta targetMeta = sceneMeta.getTargetMeta(NotifyTypeEnum.MP_SUBSCRIBE_C.getCode());
        Assert.assertEquals("WORK_ORDER_DETAIL", targetMeta.getDefaultRouteType());
        Assert.assertEquals("${workOrderId}", targetMeta.getDefaultRouteValueTemplate());
        Assert.assertEquals(NotifyChannelSceneEnum.C.getCode(), targetMeta.getDefaultChannelConfig().getChannelScene());
        Assert.assertEquals("0_vY_Wlie3dIuqmfpPAp_Hpbj-9yCso8yO1WSzWg3og",
                targetMeta.getDefaultChannelConfig().getTemplateId());
        assertFieldMapping(targetMeta.getDefaultChannelConfig().getFieldMapping(), 1, "thing2", "${toCompanyName}");
        assertFieldMapping(targetMeta.getDefaultChannelConfig().getFieldMapping(), 3, "thing4", "${transferTip}");
    }

    /**
     * 校验 C 端客户满意度评价邀请默认配置。
     */
    private void assertEvaluationInviteScene(NotifySceneMeta sceneMeta) {
        Assert.assertEquals("C端客户满意度评价通知", sceneMeta.getSceneName());

        NotifySceneTargetMeta targetMeta = sceneMeta.getTargetMeta(NotifyTypeEnum.MP_SUBSCRIBE_C.getCode());
        Assert.assertEquals(NotifyChannelTypeEnum.MP_SUBSCRIBE.getCode(), targetMeta.getChannelType());
        Assert.assertEquals("WORK_ORDER_EVALUATE", targetMeta.getDefaultRouteType());
        Assert.assertEquals("${workOrderId}", targetMeta.getDefaultRouteValueTemplate());
        Assert.assertEquals(NotifyChannelSceneEnum.C.getCode(), targetMeta.getDefaultChannelConfig().getChannelScene());
        Assert.assertEquals("01ZBgiyxkgui_wKWFtYsETnkSySMxeANaK2SoShvXkM",
                targetMeta.getDefaultChannelConfig().getTemplateId());
        assertFieldMapping(targetMeta.getDefaultChannelConfig().getFieldMapping(), 1, "phone_number2", "${companyPhone}");
        assertFieldMapping(targetMeta.getDefaultChannelConfig().getFieldMapping(), 3, "time4", "${closedTime}");
    }

    /**
     * 校验字段映射。
     *
     * @param mappings 字段映射列表
     * @param index 目标下标
     * @param expectedField 期望字段名
     * @param expectedValue 期望值模板
     */
    private void assertFieldMapping(List<NotifyChannelFieldMappingDTO> mappings, int index,
                                    String expectedField, String expectedValue) {
        Assert.assertTrue(mappings.size() > index);
        Assert.assertEquals(expectedField, mappings.get(index).getField());
        Assert.assertEquals(expectedValue, mappings.get(index).getValue());
    }

    /**
     * 校验场景变量是否包含指定说明片段。
     *
     * @param sceneMeta 场景元数据
     * @param variableName 变量名
     * @param expectedDescFragment 说明片段
     * @return 命中结果
     */
    private boolean containsVariable(NotifySceneMeta sceneMeta, String variableName, String expectedDescFragment) {
        for (NotifyTemplateVariableMeta variableMeta : sceneMeta.getVariables()) {
            if (!variableName.equals(variableMeta.getName())) {
                continue;
            }
            if (expectedDescFragment == null || expectedDescFragment.trim().isEmpty()) {
                return true;
            }
            if (variableMeta.getDesc() != null && variableMeta.getDesc().contains(expectedDescFragment)) {
                return true;
            }
        }
        return false;
    }
}
