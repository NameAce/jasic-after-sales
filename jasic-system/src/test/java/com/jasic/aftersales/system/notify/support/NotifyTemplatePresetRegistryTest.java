package com.jasic.aftersales.system.notify.support;

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
 * <p>沿用原测试类名，验证 Phase 1 已改为以 `NotifySceneRegistry` 作为唯一元数据来源，
 * 避免后续线程继续把旧预置组合注册表当成正式基线。</p>
 *
 * @author Codex
 * @date 2026/05/16
 */
public class NotifyTemplatePresetRegistryTest {

    @Test
    public void shouldResolvePhaseOneEnums() {
        Assert.assertEquals("工单", NotifyBizTypeEnum.fromCode("WORK_ORDER").getDesc());
        Assert.assertEquals("站内待办", NotifyTypeEnum.fromCode("IN_APP_TODO").getDesc());
        Assert.assertEquals("C端客户", NotifyReceiverTypeEnum.fromCode("CUSTOMER").getDesc());
        Assert.assertEquals("小程序订阅消息", NotifyChannelTypeEnum.fromCode("MP_SUBSCRIBE").getDesc());
    }

    @Test
    public void shouldExposePhaseOneSceneMetas() {
        NotifySceneRegistry registry = new NotifySceneRegistry();

        List<NotifySceneMeta> sceneMetas = registry.listScenes();
        Assert.assertEquals(2, sceneMetas.size());

        NotifySceneMeta assignedTodoScene = registry.getRequiredScene(
                NotifySceneCode.WORK_ORDER_ASSIGNED_TODO.getCode()
        );
        Assert.assertEquals("维修员", assignedTodoScene.getReceiverDesc());
        Assert.assertEquals("工单派单待办", assignedTodoScene.getDefaultTemplateName());
        Assert.assertEquals("${workOrderId}", assignedTodoScene.getDefaultRouteValueTemplate());
        Assert.assertFalse(assignedTodoScene.getVariables().isEmpty());

        NotifySceneMeta evaluationInviteScene = registry.getRequiredScene(
                NotifySceneCode.WORK_ORDER_EVALUATION_INVITE_MP_C.getCode()
        );
        Assert.assertEquals("C端客户", evaluationInviteScene.getReceiverDesc());
        Assert.assertEquals(NotifyChannelTypeEnum.MP_SUBSCRIBE.getCode(), evaluationInviteScene.getChannelType());
        Assert.assertEquals(
                NotifyChannelSceneEnum.C.getCode(),
                evaluationInviteScene.getTargetMeta(NotifyTypeEnum.MP_SUBSCRIBE_C.getCode())
                        .getDefaultChannelConfig()
                        .getChannelScene()
        );
        Assert.assertEquals("customerOpenid", evaluationInviteScene.getVariables().get(4).getName());
    }
}
