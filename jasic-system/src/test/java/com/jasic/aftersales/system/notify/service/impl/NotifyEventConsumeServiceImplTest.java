package com.jasic.aftersales.system.notify.service.impl;

import cn.hutool.json.JSONUtil;
import com.jasic.aftersales.system.domain.entity.SysUser;
import com.jasic.aftersales.system.mapper.SysUserMapper;
import com.jasic.aftersales.system.notify.domain.dto.NotifyAssignedEventDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyEvaluationInviteEventDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyTemplateChannelDTO;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyDispatch;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyEvent;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyMessage;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyMessageLog;
import com.jasic.aftersales.system.notify.domain.enums.NotifyActionTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyBizTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyChannelTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyDispatchResultCodeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyDispatchStatusEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyEventStatusEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyEventTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyInvalidReasonEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyTodoStatusEnum;
import com.jasic.aftersales.system.notify.service.support.NotifyEventHandler;
import com.jasic.aftersales.system.notify.service.support.NotifyEventHandlerRegistry;
import com.jasic.aftersales.system.notify.service.support.WorkOrderAssignedNotifyEventHandler;
import com.jasic.aftersales.system.notify.service.support.WorkOrderEvaluationInviteNotifyEventHandler;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTemplateChannelVO;
import com.jasic.aftersales.system.notify.service.NotifyDispatchService;
import com.jasic.aftersales.system.notify.service.NotifyEventService;
import com.jasic.aftersales.system.notify.service.NotifyMessageLogService;
import com.jasic.aftersales.system.notify.service.NotifyMessageService;
import com.jasic.aftersales.system.notify.service.NotifyChannelConfigService;
import com.jasic.aftersales.system.notify.service.NotifyTemplateRenderService;
import com.jasic.aftersales.system.notify.support.NotifyConstants;
import com.jasic.aftersales.system.notify.support.NotifyDispatchPayload;
import com.jasic.aftersales.system.notify.support.NotifySceneCode;
import com.jasic.aftersales.system.notify.support.NotifyTemplateRenderResult;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 通知事件消费服务测试。
 *
 * <p>阶段六重点覆盖工单派单站内待办与客户评价邀请外部分发两条正式链路，
 * 同时验证模板停用、渠道停用、渠道缺失和消费异常时的兜底结果，
 * 避免事件消费成功后缺少可解释的消息或 dispatch 记录。</p>
 *
 * @author Codex
 * @date 2026/05/16
 */
public class NotifyEventConsumeServiceImplTest {

    /**
     * 首次派单时应给新维修员生成一条待处理站内待办。
     *
     * @throws Exception 反射异常
     */
    @Test
    public void shouldCreatePendingMessageForFirstAssign() throws Exception {
        FakeNotifyEventService eventService = new FakeNotifyEventService();
        FakeNotifyMessageService messageService = new FakeNotifyMessageService();
        FakeNotifyMessageLogService logService = new FakeNotifyMessageLogService();
        FakeNotifyDispatchService dispatchService = new FakeNotifyDispatchService();

        SysNotifyEvent event = buildAssignedEvent(1L, 88L, 200L, 100L, NotifyConstants.ASSIGN_TYPE_ASSIGN, null, 200L);
        eventService.events.put(event.getId(), event);
        eventService.pendingEventIds.add(event.getId());

        NotifyEventConsumeServiceImpl service = createService(
                eventService,
                messageService,
                logService,
                new FakeNotifyTemplateRenderService(),
                new FakeNotifyChannelConfigService(),
                dispatchService,
                buildUserMapper(200L, "维修员A")
        );

        int successCount = service.consumePendingEvents();

        Assert.assertEquals(1, successCount);
        Assert.assertEquals(NotifyEventStatusEnum.SUCCESS.getCode(), eventService.events.get(1L).getStatus());
        Assert.assertEquals(1, messageService.createdMessages.size());
        Assert.assertEquals(Long.valueOf(2002L), messageService.createdMessages.get(0).getReceiverCompanyId());
        Assert.assertEquals(FakeNotifyTemplateRenderService.ASSIGNED_TEMPLATE_CODE,
                messageService.createdMessages.get(0).getTemplateCode());
        Assert.assertEquals(1, logService.logs.size());
        Assert.assertTrue(dispatchService.createdDispatches.isEmpty());
    }

    /**
     * 转派时应先失效旧维修员待办，再为新维修员生成新待办。
     *
     * @throws Exception 反射异常
     */
    @Test
    public void shouldInvalidateOldTodosAndCreateNewTodoOnTransfer() throws Exception {
        FakeNotifyEventService eventService = new FakeNotifyEventService();
        FakeNotifyMessageService messageService = new FakeNotifyMessageService();
        FakeNotifyMessageLogService logService = new FakeNotifyMessageLogService();
        FakeNotifyDispatchService dispatchService = new FakeNotifyDispatchService();

        SysNotifyEvent event = buildAssignedEvent(2L, 89L, 201L, 101L, NotifyConstants.ASSIGN_TYPE_TRANSFER, 200L, 201L);
        eventService.events.put(event.getId(), event);
        eventService.pendingEventIds.add(event.getId());

        SysNotifyMessage oldPending = buildActiveMessage(11L, 89L, 200L, NotifyTodoStatusEnum.PENDING.getCode());
        SysNotifyMessage oldRead = buildActiveMessage(12L, 89L, 200L, NotifyTodoStatusEnum.READ.getCode());
        messageService.activeTodosByReceiver.put(200L, new ArrayList<SysNotifyMessage>() {{
            add(oldPending);
            add(oldRead);
        }});

        NotifyEventConsumeServiceImpl service = createService(
                eventService,
                messageService,
                logService,
                new FakeNotifyTemplateRenderService(),
                new FakeNotifyChannelConfigService(),
                dispatchService,
                buildUserMapper(201L, "维修员B")
        );

        int successCount = service.consumePendingEvents();

        Assert.assertEquals(1, successCount);
        Assert.assertEquals(NotifyTodoStatusEnum.INVALID.getCode(), oldPending.getTodoStatus());
        Assert.assertEquals(NotifyTodoStatusEnum.INVALID.getCode(), oldRead.getTodoStatus());
        Assert.assertEquals(3, logService.logs.size());
    }

    /**
     * 渠道配置不完整时，应落一条可解释的跳过分发记录。
     *
     * @throws Exception 反射异常
     */
    @Test
    public void shouldCreateSkippedDispatchWhenEvaluationInviteChannelConfigIsIncomplete() throws Exception {
        FakeNotifyEventService eventService = new FakeNotifyEventService();
        FakeNotifyMessageService messageService = new FakeNotifyMessageService();
        FakeNotifyMessageLogService logService = new FakeNotifyMessageLogService();
        FakeNotifyDispatchService dispatchService = new FakeNotifyDispatchService();
        FakeNotifyTemplateRenderService templateService = new FakeNotifyTemplateRenderService();
        FakeNotifyChannelConfigService channelConfigService = new FakeNotifyChannelConfigService();
        channelConfigService.channelConfigs.add(buildEvaluationChannel(null));

        SysNotifyEvent event = buildEvaluationEvent(3L, 90L, 9001L);
        eventService.events.put(event.getId(), event);
        eventService.pendingEventIds.add(event.getId());

        NotifyEventConsumeServiceImpl service = createService(
                eventService,
                messageService,
                logService,
                templateService,
                channelConfigService,
                dispatchService,
                buildUserMapper(null, null)
        );

        int successCount = service.consumePendingEvents();

        Assert.assertEquals(1, successCount);
        Assert.assertEquals(1, dispatchService.createdDispatches.size());
        SysNotifyDispatch dispatch = dispatchService.createdDispatches.get(0);
        Assert.assertEquals(NotifyDispatchStatusEnum.SKIPPED.getCode(), dispatch.getDispatchStatus());
        Assert.assertEquals(NotifyDispatchResultCodeEnum.SKIPPED_CHANNEL_CONFIG_MISSING.getCode(), dispatch.getResultCode());
    }

    /**
     * 客户缺少 openid 时，不应继续生成待发送任务。
     *
     * @throws Exception 反射异常
     */
    @Test
    public void shouldCreateSkippedDispatchWhenEvaluationInviteOpenidIsMissing() throws Exception {
        FakeNotifyEventService eventService = new FakeNotifyEventService();
        FakeNotifyMessageService messageService = new FakeNotifyMessageService();
        FakeNotifyMessageLogService logService = new FakeNotifyMessageLogService();
        FakeNotifyDispatchService dispatchService = new FakeNotifyDispatchService();
        FakeNotifyTemplateRenderService templateService = new FakeNotifyTemplateRenderService();
        FakeNotifyChannelConfigService channelConfigService = new FakeNotifyChannelConfigService();
        channelConfigService.channelConfigs.add(buildEvaluationChannel("wx-template-001"));

        SysNotifyEvent event = buildEvaluationEvent(7L, 94L, 9003L);
        NotifyEvaluationInviteEventDTO payload = JSONUtil.toBean(event.getPayloadJson(), NotifyEvaluationInviteEventDTO.class);
        payload.setCustomerOpenid(null);
        event.setPayloadJson(JSONUtil.toJsonStr(payload));
        eventService.events.put(event.getId(), event);
        eventService.pendingEventIds.add(event.getId());

        NotifyEventConsumeServiceImpl service = createService(
                eventService,
                messageService,
                logService,
                templateService,
                channelConfigService,
                dispatchService,
                buildUserMapper(null, null)
        );

        int successCount = service.consumePendingEvents();

        Assert.assertEquals(1, successCount);
        Assert.assertEquals(1, dispatchService.createdDispatches.size());
        SysNotifyDispatch dispatch = dispatchService.createdDispatches.get(0);
        Assert.assertEquals(NotifyDispatchStatusEnum.SKIPPED.getCode(), dispatch.getDispatchStatus());
        Assert.assertEquals(NotifyDispatchResultCodeEnum.SKIPPED_OPENID_MISSING.getCode(), dispatch.getResultCode());
        Assert.assertTrue(dispatch.getResultMessage().contains("openid"));
    }

    /**
     * 模板和渠道都可用时，应生成待发送的小程序分发任务。
     *
     * @throws Exception 反射异常
     */
    @Test
    public void shouldCreatePendingDispatchWhenEvaluationInviteReady() throws Exception {
        FakeNotifyEventService eventService = new FakeNotifyEventService();
        FakeNotifyMessageService messageService = new FakeNotifyMessageService();
        FakeNotifyMessageLogService logService = new FakeNotifyMessageLogService();
        FakeNotifyDispatchService dispatchService = new FakeNotifyDispatchService();
        FakeNotifyTemplateRenderService templateService = new FakeNotifyTemplateRenderService();
        FakeNotifyChannelConfigService channelConfigService = new FakeNotifyChannelConfigService();
        channelConfigService.channelConfigs.add(buildEvaluationChannel("wx-template-001"));

        SysNotifyEvent event = buildEvaluationEvent(4L, 91L, 9002L);
        eventService.events.put(event.getId(), event);
        eventService.pendingEventIds.add(event.getId());

        NotifyEventConsumeServiceImpl service = createService(
                eventService,
                messageService,
                logService,
                templateService,
                channelConfigService,
                dispatchService,
                buildUserMapper(null, null)
        );

        int successCount = service.consumePendingEvents();

        Assert.assertEquals(1, successCount);
        Assert.assertEquals(1, dispatchService.createdDispatches.size());
        SysNotifyDispatch dispatch = dispatchService.createdDispatches.get(0);
        Assert.assertEquals(NotifyDispatchStatusEnum.PENDING.getCode(), dispatch.getDispatchStatus());
        Assert.assertEquals(NotifyChannelTypeEnum.MP_SUBSCRIBE.getCode(), dispatch.getChannelType());
        Assert.assertTrue(dispatch.getPayloadJson().contains("pages/order/evaluate"));
        NotifyDispatchPayload dispatchPayload = JSONUtil.toBean(dispatch.getPayloadJson(), NotifyDispatchPayload.class);
        Assert.assertEquals(NotifySceneCode.WORK_ORDER_EVALUATION_INVITE_MP_C.getCode(),
                dispatchPayload.getSceneCode());
        Assert.assertEquals("客户评价邀请订阅消息", dispatchPayload.getTemplateName());
        Assert.assertEquals(Integer.valueOf(1), dispatchPayload.getChannelEnabled());
    }

    /**
     * 模板启用但渠道停用时，应记录渠道停用的跳过结果。
     *
     * @throws Exception 反射异常
     */
    @Test
    public void shouldNotCreateDispatchWhenEvaluationInviteChannelDisabled() throws Exception {
        FakeNotifyEventService eventService = new FakeNotifyEventService();
        FakeNotifyMessageService messageService = new FakeNotifyMessageService();
        FakeNotifyMessageLogService logService = new FakeNotifyMessageLogService();
        FakeNotifyDispatchService dispatchService = new FakeNotifyDispatchService();
        FakeNotifyTemplateRenderService templateService = new FakeNotifyTemplateRenderService();
        FakeNotifyChannelConfigService channelConfigService = new FakeNotifyChannelConfigService();
        NotifyTemplateChannelVO channel = buildEvaluationChannel("wx-template-001");
        channel.setChannelEnabled(0);
        channelConfigService.channelConfigs.add(channel);
        channelConfigService.runtimeChannels.clear();

        SysNotifyEvent event = buildEvaluationEvent(8L, 95L, 9004L);
        eventService.events.put(event.getId(), event);
        eventService.pendingEventIds.add(event.getId());

        NotifyEventConsumeServiceImpl service = createService(
                eventService,
                messageService,
                logService,
                templateService,
                channelConfigService,
                dispatchService,
                buildUserMapper(null, null)
        );

        int successCount = service.consumePendingEvents();

        Assert.assertEquals(1, successCount);
        Assert.assertEquals(1, dispatchService.createdDispatches.size());
        Assert.assertEquals(NotifyDispatchStatusEnum.SKIPPED.getCode(),
                dispatchService.createdDispatches.get(0).getDispatchStatus());
        Assert.assertEquals(NotifyDispatchResultCodeEnum.SKIPPED_CHANNEL_DISABLED.getCode(),
                dispatchService.createdDispatches.get(0).getResultCode());
        NotifyDispatchPayload dispatchPayload = JSONUtil.toBean(
                dispatchService.createdDispatches.get(0).getPayloadJson(),
                NotifyDispatchPayload.class);
        Assert.assertEquals(NotifySceneCode.WORK_ORDER_EVALUATION_INVITE_MP_C.getCode(),
                dispatchPayload.getSceneCode());
        Assert.assertEquals(Integer.valueOf(0), dispatchPayload.getChannelEnabled());
    }

    /**
     * 派单模板停用时，应跳过待办生成且不影响事件消费成功。
     *
     * @throws Exception 反射异常
     */
    @Test
    public void shouldSkipAssignedTodoWhenTemplateDisabled() throws Exception {
        FakeNotifyEventService eventService = new FakeNotifyEventService();
        FakeNotifyMessageService messageService = new FakeNotifyMessageService();
        FakeNotifyMessageLogService logService = new FakeNotifyMessageLogService();
        FakeNotifyDispatchService dispatchService = new FakeNotifyDispatchService();
        FakeNotifyTemplateRenderService templateService = new FakeNotifyTemplateRenderService();
        templateService.notifyEnabled = false;

        SysNotifyEvent event = buildAssignedEvent(9L, 96L, 206L, 106L, NotifyConstants.ASSIGN_TYPE_ASSIGN, null, 206L);
        eventService.events.put(event.getId(), event);
        eventService.pendingEventIds.add(event.getId());

        NotifyEventConsumeServiceImpl service = createService(
                eventService,
                messageService,
                logService,
                templateService,
                new FakeNotifyChannelConfigService(),
                dispatchService,
                buildUserMapper(206L, "维修员C")
        );

        int successCount = service.consumePendingEvents();

        Assert.assertEquals(1, successCount);
        Assert.assertTrue(messageService.createdMessages.isEmpty());
        Assert.assertTrue(dispatchService.createdDispatches.isEmpty());
    }

    /**
     * 评价邀请模板停用时，应写入模板停用的跳过分发记录。
     *
     * @throws Exception 反射异常
     */
    @Test
    public void shouldCreateSkippedDispatchWhenEvaluationInviteTemplateDisabled() throws Exception {
        FakeNotifyEventService eventService = new FakeNotifyEventService();
        FakeNotifyMessageService messageService = new FakeNotifyMessageService();
        FakeNotifyMessageLogService logService = new FakeNotifyMessageLogService();
        FakeNotifyDispatchService dispatchService = new FakeNotifyDispatchService();
        FakeNotifyTemplateRenderService templateService = new FakeNotifyTemplateRenderService();
        FakeNotifyChannelConfigService channelConfigService = new FakeNotifyChannelConfigService();
        templateService.notifyEnabled = false;
        channelConfigService.channelConfigs.add(buildEvaluationChannel("wx-template-001"));

        SysNotifyEvent event = buildEvaluationEvent(10L, 97L, 9005L);
        eventService.events.put(event.getId(), event);
        eventService.pendingEventIds.add(event.getId());

        NotifyEventConsumeServiceImpl service = createService(
                eventService,
                messageService,
                logService,
                templateService,
                channelConfigService,
                dispatchService,
                buildUserMapper(null, null)
        );

        int successCount = service.consumePendingEvents();

        Assert.assertEquals(1, successCount);
        Assert.assertEquals(1, dispatchService.createdDispatches.size());
        Assert.assertEquals(NotifyDispatchStatusEnum.SKIPPED.getCode(),
                dispatchService.createdDispatches.get(0).getDispatchStatus());
        Assert.assertEquals(NotifyDispatchResultCodeEnum.SKIPPED_TEMPLATE_DISABLED.getCode(),
                dispatchService.createdDispatches.get(0).getResultCode());
    }

    /**
     * 消费抛错且未达到重试上限时，应标记失败并安排下一次重试。
     *
     * @throws Exception 反射异常
     */
    @Test
    public void shouldMarkEventFailedAndScheduleRetryWhenConsumptionThrows() throws Exception {
        FakeNotifyEventService eventService = new FakeNotifyEventService();
        FakeNotifyMessageService messageService = new FakeNotifyMessageService();
        FakeNotifyMessageLogService logService = new FakeNotifyMessageLogService();
        FakeNotifyDispatchService dispatchService = new FakeNotifyDispatchService();
        FakeNotifyTemplateRenderService templateService = new FakeNotifyTemplateRenderService();
        FakeNotifyChannelConfigService channelConfigService = new FakeNotifyChannelConfigService();

        SysNotifyEvent event = buildAssignedEvent(5L, 92L, 204L, 104L, NotifyConstants.ASSIGN_TYPE_ASSIGN, null, null);
        event.setRetryCount(1);
        eventService.events.put(event.getId(), event);
        eventService.pendingEventIds.add(event.getId());

        NotifyEventConsumeServiceImpl service = createService(
                eventService,
                messageService,
                logService,
                templateService,
                channelConfigService,
                dispatchService,
                buildUserMapper(null, null)
        );

        int successCount = service.consumePendingEvents();

        Assert.assertEquals(0, successCount);
        Assert.assertEquals(NotifyEventStatusEnum.FAILED.getCode(), eventService.events.get(5L).getStatus());
        Assert.assertEquals(Integer.valueOf(2), eventService.events.get(5L).getRetryCount());
        Assert.assertNotNull(eventService.events.get(5L).getNextRetryTime());
    }

    /**
     * 消费抛错且已达到重试上限时，应直接落为死信。
     *
     * @throws Exception 反射异常
     */
    @Test
    public void shouldMarkEventDeadWhenConsumptionRetryReachesMax() throws Exception {
        FakeNotifyEventService eventService = new FakeNotifyEventService();
        FakeNotifyMessageService messageService = new FakeNotifyMessageService();
        FakeNotifyMessageLogService logService = new FakeNotifyMessageLogService();
        FakeNotifyDispatchService dispatchService = new FakeNotifyDispatchService();
        FakeNotifyTemplateRenderService templateService = new FakeNotifyTemplateRenderService();
        FakeNotifyChannelConfigService channelConfigService = new FakeNotifyChannelConfigService();

        SysNotifyEvent event = buildAssignedEvent(6L, 93L, 205L, 105L, NotifyConstants.ASSIGN_TYPE_ASSIGN, null, null);
        event.setRetryCount(2);
        eventService.events.put(event.getId(), event);
        eventService.pendingEventIds.add(event.getId());

        NotifyEventConsumeServiceImpl service = createService(
                eventService,
                messageService,
                logService,
                templateService,
                channelConfigService,
                dispatchService,
                buildUserMapper(null, null)
        );

        int successCount = service.consumePendingEvents();

        Assert.assertEquals(0, successCount);
        Assert.assertEquals(NotifyEventStatusEnum.DEAD.getCode(), eventService.events.get(6L).getStatus());
        Assert.assertEquals(Integer.valueOf(3), eventService.events.get(6L).getRetryCount());
        Assert.assertTrue(eventService.events.get(6L).getErrorMessage().contains("死信"));
    }

    /**
     * 构造待测事件消费服务。
     *
     * @param eventService 事件服务桩
     * @param messageService 消息服务桩
     * @param logService 消息日志服务桩
     * @param templateService 模板渲染服务桩
     * @param channelConfigService 渠道配置服务桩
     * @param dispatchService 分发服务桩
     * @param userMapper 用户查询桩
     * @return 待测服务
     * @throws Exception 反射异常
     */
    private NotifyEventConsumeServiceImpl createService(FakeNotifyEventService eventService,
                                                        FakeNotifyMessageService messageService,
                                                        FakeNotifyMessageLogService logService,
                                                        FakeNotifyTemplateRenderService templateService,
                                                        FakeNotifyChannelConfigService channelConfigService,
                                                        FakeNotifyDispatchService dispatchService,
                                                        SysUserMapper userMapper) throws Exception {
        NotifyEventConsumeServiceImpl service = new NotifyEventConsumeServiceImpl();
        setField(service, "notifyEventService", eventService);
        setField(service, "notifyEventHandlerRegistry",
                buildRegistry(messageService, logService, templateService, channelConfigService, dispatchService, userMapper));
        setField(service, "transactionTemplate", new TransactionTemplate(new NoopTransactionManager()));
        return service;
    }

    /**
     * 构造包含派单和评价邀请 handler 的注册表。
     *
     * @param messageService 消息服务桩
     * @param logService 消息日志服务桩
     * @param templateService 模板渲染服务桩
     * @param channelConfigService 渠道配置服务桩
     * @param dispatchService 分发服务桩
     * @param userMapper 用户查询桩
     * @return handler 注册表
     * @throws Exception 反射异常
     */
    private NotifyEventHandlerRegistry buildRegistry(FakeNotifyMessageService messageService,
                                                     FakeNotifyMessageLogService logService,
                                                     FakeNotifyTemplateRenderService templateService,
                                                     FakeNotifyChannelConfigService channelConfigService,
                                                     FakeNotifyDispatchService dispatchService,
                                                     SysUserMapper userMapper) throws Exception {
        WorkOrderAssignedNotifyEventHandler assignedHandler = new WorkOrderAssignedNotifyEventHandler();
        setField(assignedHandler, "notifyMessageService", messageService);
        setField(assignedHandler, "notifyMessageLogService", logService);
        setField(assignedHandler, "notifyTemplateRenderService", templateService);
        setField(assignedHandler, "sysUserMapper", userMapper);

        WorkOrderEvaluationInviteNotifyEventHandler evaluationInviteHandler =
                new WorkOrderEvaluationInviteNotifyEventHandler();
        setField(evaluationInviteHandler, "notifyTemplateRenderService", templateService);
        setField(evaluationInviteHandler, "notifyChannelConfigService", channelConfigService);
        setField(evaluationInviteHandler, "notifyDispatchService", dispatchService);

        NotifyEventHandlerRegistry registry = new NotifyEventHandlerRegistry();
        List<NotifyEventHandler> handlers = new ArrayList<>();
        handlers.add(assignedHandler);
        handlers.add(evaluationInviteHandler);
        setField(registry, "notifyEventHandlers", handlers);
        return registry;
    }

    /**
     * 构造工单派单事件。
     *
     * @param eventId 事件ID
     * @param bizId 工单ID
     * @param receiverId 接收人ID
     * @param operatorId 操作人ID
     * @param assignType 派单类型
     * @param oldAssignedUserId 旧维修员ID
     * @param newAssignedUserId 新维修员ID
     * @return 工单派单事件
     */
    private SysNotifyEvent buildAssignedEvent(Long eventId, Long bizId, Long receiverId, Long operatorId,
                                              String assignType, Long oldAssignedUserId, Long newAssignedUserId) {
        NotifyAssignedEventDTO payload = new NotifyAssignedEventDTO();
        payload.setWorkOrderId(bizId);
        payload.setOrderNo("WO-" + bizId);
        payload.setOldAssignedUserId(oldAssignedUserId);
        payload.setNewAssignedUserId(newAssignedUserId);
        payload.setReceiverCompanyId(2002L);
        payload.setOperatorId(operatorId);
        payload.setAssignType(assignType);
        payload.setOperationId("op-" + eventId);

        SysNotifyEvent event = new SysNotifyEvent();
        event.setId(eventId);
        event.setEventKey("event-" + eventId);
        event.setEventType(NotifyEventTypeEnum.WORK_ORDER_ASSIGNED.getCode());
        event.setBizType(NotifyBizTypeEnum.WORK_ORDER.getCode());
        event.setBizId(bizId);
        event.setBizNo("WO-" + bizId);
        event.setOperatorId(operatorId);
        event.setReceiverId(receiverId);
        event.setPayloadJson(JSONUtil.toJsonStr(payload));
        event.setStatus(NotifyEventStatusEnum.NEW.getCode());
        event.setRetryCount(0);
        return event;
    }

    /**
     * 构造客户评价邀请事件。
     *
     * @param eventId 事件ID
     * @param bizId 工单ID
     * @param customerId 客户ID
     * @return 评价邀请事件
     */
    private SysNotifyEvent buildEvaluationEvent(Long eventId, Long bizId, Long customerId) {
        NotifyEvaluationInviteEventDTO payload = new NotifyEvaluationInviteEventDTO();
        payload.setWorkOrderId(bizId);
        payload.setOrderNo("WO-" + bizId);
        payload.setCustomerId(customerId);
        payload.setCustomerMobile("13800138000");
        payload.setCustomerOpenid("openid-" + customerId);
        payload.setCompanyId(3001L);
        payload.setCompanyName("深圳南山服务网点");
        payload.setClosedTime(LocalDateTime.of(2026, 4, 21, 18, 0, 0));

        SysNotifyEvent event = new SysNotifyEvent();
        event.setId(eventId);
        event.setEventKey("event-" + eventId);
        event.setEventType(NotifyEventTypeEnum.WORK_ORDER_EVALUATION_INVITE.getCode());
        event.setBizType(NotifyBizTypeEnum.WORK_ORDER.getCode());
        event.setBizId(bizId);
        event.setBizNo("WO-" + bizId);
        event.setReceiverId(customerId);
        event.setPayloadJson(JSONUtil.toJsonStr(payload));
        event.setStatus(NotifyEventStatusEnum.NEW.getCode());
        event.setRetryCount(0);
        return event;
    }

    /**
     * 构造评价邀请使用的小程序渠道配置。
     *
     * @param templateId 第三方订阅消息模板ID
     * @return 渠道配置
     */
    private NotifyTemplateChannelVO buildEvaluationChannel(String templateId) {
        NotifyTemplateChannelVO channel = new NotifyTemplateChannelVO();
        channel.setSceneCode(FakeNotifyTemplateRenderService.EVALUATION_TEMPLATE_CODE);
        channel.setChannelType(NotifyChannelTypeEnum.MP_SUBSCRIBE.getCode());
        channel.setChannelEnabled(1);
        channel.setTemplateId(templateId);
        channel.setPagePathTemplate("pages/order/evaluate?workOrderId=${workOrderId}");
        List<com.jasic.aftersales.system.notify.domain.dto.NotifyChannelFieldMappingDTO> fieldMappings = new ArrayList<>();
        com.jasic.aftersales.system.notify.domain.dto.NotifyChannelFieldMappingDTO mapping = new com.jasic.aftersales.system.notify.domain.dto.NotifyChannelFieldMappingDTO();
        mapping.setField("thing1");
        mapping.setValue("${orderNo}");
        fieldMappings.add(mapping);
        channel.setFieldMapping(fieldMappings);
        return channel;
    }

    /**
     * 构造仍处于活动中的旧待办消息。
     *
     * @param id 消息ID
     * @param bizId 工单ID
     * @param receiverId 接收人ID
     * @param todoStatus 待办状态
     * @return 活动待办消息
     */
    private SysNotifyMessage buildActiveMessage(Long id, Long bizId, Long receiverId, String todoStatus) {
        SysNotifyMessage message = new SysNotifyMessage();
        message.setId(id);
        message.setBizType(NotifyBizTypeEnum.WORK_ORDER.getCode());
        message.setBizId(bizId);
        message.setBizNo("WO-" + bizId);
        message.setReceiverId(receiverId);
        message.setReceiverCompanyId(2002L);
        message.setTodoStatus(todoStatus);
        return message;
    }

    /**
     * 构造按 ID 查询用户的 Mapper 代理。
     *
     * @param userId 用户ID
     * @param realName 用户姓名
     * @return 用户 Mapper 代理
     */
    private SysUserMapper buildUserMapper(Long userId, String realName) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectById".equals(method.getName())) {
                    if (userId == null || realName == null) {
                        return null;
                    }
                    SysUser user = new SysUser();
                    user.setId(userId);
                    user.setRealName(realName);
                    return user;
                }
                return null;
            }
        };
        return (SysUserMapper) Proxy.newProxyInstance(
                SysUserMapper.class.getClassLoader(),
                new Class<?>[]{SysUserMapper.class},
                handler
        );
    }

    /**
     * 反射设置待测对象字段。
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

    private static class FakeNotifyEventService implements NotifyEventService {
        private final Map<Long, SysNotifyEvent> events = new LinkedHashMap<>();
        private final List<Long> pendingEventIds = new ArrayList<>();

        @Override
        public Long createEvent(SysNotifyEvent notifyEvent) {
            events.put(notifyEvent.getId(), notifyEvent);
            return notifyEvent.getId();
        }

        @Override
        public SysNotifyEvent getById(Long id) {
            return events.get(id);
        }

        @Override
        public SysNotifyEvent getByEventKey(String eventKey) {
            return null;
        }

        @Override
        public List<SysNotifyEvent> listConsumableEvents(LocalDateTime now, Integer limit) {
            List<SysNotifyEvent> result = new ArrayList<>();
            for (Long eventId : pendingEventIds) {
                result.add(events.get(eventId));
            }
            return result;
        }

        @Override
        public List<SysNotifyEvent> listTimeoutProcessingEvents(LocalDateTime timeoutBefore, Integer limit) {
            return Collections.emptyList();
        }

        @Override
        public List<SysNotifyEvent> listByQuery(com.jasic.aftersales.system.notify.domain.query.NotifyEventQuery query) {
            return Collections.emptyList();
        }

        @Override
        public void updateStatus(Long eventId, String status) {
            events.get(eventId).setStatus(status);
        }

        @Override
        public boolean markProcessing(Long eventId) {
            events.get(eventId).setStatus(NotifyEventStatusEnum.PROCESSING.getCode());
            events.get(eventId).setProcessingTime(LocalDateTime.now());
            return true;
        }

        @Override
        public void markSuccess(Long eventId) {
            events.get(eventId).setStatus(NotifyEventStatusEnum.SUCCESS.getCode());
            events.get(eventId).setProcessingTime(null);
        }

        @Override
        public void markFailed(Long eventId, Integer retryCount, LocalDateTime nextRetryTime, String errorMessage) {
            SysNotifyEvent event = events.get(eventId);
            event.setStatus(NotifyEventStatusEnum.FAILED.getCode());
            event.setRetryCount(retryCount);
            event.setProcessingTime(null);
            event.setNextRetryTime(nextRetryTime);
            event.setErrorMessage(errorMessage);
        }

        @Override
        public void updateRetryInfo(Long eventId, Integer retryCount, LocalDateTime nextRetryTime, String errorMessage) {
            SysNotifyEvent event = events.get(eventId);
            event.setRetryCount(retryCount);
            event.setNextRetryTime(nextRetryTime);
            event.setErrorMessage(errorMessage);
        }

        @Override
        public int recoverTimeoutProcessingEvents(LocalDateTime timeoutBefore) {
            return 0;
        }

        @Override
        public void markDead(Long eventId, String errorMessage) {
            SysNotifyEvent event = events.get(eventId);
            event.setStatus(NotifyEventStatusEnum.DEAD.getCode());
            event.setProcessingTime(null);
            event.setNextRetryTime(null);
            event.setErrorMessage(errorMessage);
        }

        @Override
        public void resetForRetry(Long eventId) {
            SysNotifyEvent event = events.get(eventId);
            event.setStatus(NotifyEventStatusEnum.NEW.getCode());
            event.setRetryCount(0);
            event.setProcessingTime(null);
            event.setNextRetryTime(null);
            event.setErrorMessage(null);
        }
    }

    private static class FakeNotifyMessageService implements NotifyMessageService {
        private final Map<Long, SysNotifyMessage> messageByEventId = new LinkedHashMap<>();
        private final Map<Long, List<SysNotifyMessage>> activeTodosByReceiver = new LinkedHashMap<>();
        private final List<SysNotifyMessage> createdMessages = new ArrayList<>();
        private long nextMessageId = 1000L;

        @Override
        public Long createMessage(SysNotifyMessage notifyMessage) {
            notifyMessage.setId(nextMessageId++);
            createdMessages.add(notifyMessage);
            messageByEventId.put(notifyMessage.getEventId(), notifyMessage);
            return notifyMessage.getId();
        }

        @Override
        public SysNotifyMessage getById(Long id) {
            return null;
        }

        @Override
        public SysNotifyMessage getByEventId(Long eventId) {
            return messageByEventId.get(eventId);
        }

        @Override
        public List<SysNotifyMessage> listActiveTodoByBizAndReceiver(String bizType, Long bizId, Long receiverId,
                                                                     Long receiverCompanyId) {
            if (receiverCompanyId == null) {
                return Collections.emptyList();
            }
            return activeTodosByReceiver.getOrDefault(receiverId, Collections.emptyList());
        }

        @Override
        public boolean invalidateMessage(Long messageId, String invalidReason, LocalDateTime invalidTime) {
            for (List<SysNotifyMessage> messages : activeTodosByReceiver.values()) {
                for (SysNotifyMessage message : messages) {
                    if (messageId.equals(message.getId())) {
                        message.setTodoStatus(NotifyTodoStatusEnum.INVALID.getCode());
                        message.setInvalidReason(invalidReason);
                        message.setInvalidTime(invalidTime);
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public void markRead(Long id, Long receiverId, Long receiverCompanyId) {
        }

        @Override
        public void markReadByBiz(com.jasic.aftersales.system.notify.domain.dto.NotifyReadByBizDTO dto) {
        }

        @Override
        public void completeTodoByBizAndReceiver(com.jasic.aftersales.system.notify.domain.dto.NotifyTodoCompleteDTO dto) {
        }

        @Override
        public void invalidateTodoByBiz(com.jasic.aftersales.system.notify.domain.dto.NotifyTodoInvalidateDTO dto) {
        }

        @Override
        public com.jasic.aftersales.common.core.domain.PageResult<com.jasic.aftersales.system.notify.domain.vo.NotifyMessagePageVO> listPage(
                com.jasic.aftersales.system.notify.domain.query.NotifyMessageQuery query) {
            return null;
        }

        @Override
        public Long countTodo(Long receiverId, Long receiverCompanyId) {
            return 0L;
        }
    }

    private static class FakeNotifyTemplateRenderService implements NotifyTemplateRenderService {
        private static final String ASSIGNED_TEMPLATE_CODE = "TPL_WORK_ORDER_ASSIGNED_ACTIVE";
        private static final String EVALUATION_TEMPLATE_CODE = "TPL_WORK_ORDER_EVALUATION_ACTIVE";
        private boolean notifyEnabled = true;

        @Override
        public NotifyTemplateRenderResult render(String sceneCode, Map<String, Object> variables) {
            NotifyTemplateRenderResult result = new NotifyTemplateRenderResult();
            result.setNotifyEnabled(notifyEnabled);
            if (NotifySceneCode.WORK_ORDER_ASSIGNED_TODO.getCode().equals(sceneCode)) {
                result.setTemplateCode(ASSIGNED_TEMPLATE_CODE);
                result.setSceneCode(NotifySceneCode.WORK_ORDER_ASSIGNED_TODO.getCode());
                result.setSceneName(NotifySceneCode.WORK_ORDER_ASSIGNED_TODO.getDesc());
                result.setTemplateName("工单派单待办");
            } else if (NotifySceneCode.WORK_ORDER_EVALUATION_INVITE_MP_C.getCode().equals(sceneCode)) {
                result.setTemplateCode(EVALUATION_TEMPLATE_CODE);
                result.setSceneCode(NotifySceneCode.WORK_ORDER_EVALUATION_INVITE_MP_C.getCode());
                result.setSceneName(NotifySceneCode.WORK_ORDER_EVALUATION_INVITE_MP_C.getDesc());
                result.setTemplateName("客户评价邀请订阅消息");
            } else {
                result.setTemplateCode("UNKNOWN_TEMPLATE");
                result.setSceneCode(sceneCode);
            }
            Object orderNo = variables.get("orderNo");
            Object workOrderId = variables.get("workOrderId");
            result.setTitle("你有新的工单待处理");
            result.setSummary("工单" + orderNo + "已派发给你，请尽快处理");
            result.setRouteType(NotifyConstants.ROUTE_TYPE_WORK_ORDER_DETAIL);
            result.setRouteValue(String.valueOf(workOrderId));
            return result;
        }

    }

    private static class FakeNotifyChannelConfigService implements NotifyChannelConfigService {
        private final List<NotifyTemplateChannelVO> channelConfigs = new ArrayList<>();
        private final List<NotifyTemplateChannelVO> runtimeChannels = new ArrayList<>();

        @Override
        public List<NotifyTemplateChannelVO> listChannelConfigs(String sceneCode) {
            return channelConfigs;
        }

        @Override
        public List<NotifyTemplateChannelVO> listRuntimeChannelConfigs(String sceneCode) {
            if (!runtimeChannels.isEmpty()) {
                return runtimeChannels;
            }
            List<NotifyTemplateChannelVO> enabledChannels = new ArrayList<>();
            for (NotifyTemplateChannelVO channelConfig : channelConfigs) {
                if (channelConfig != null && Objects.equals(channelConfig.getChannelEnabled(), 1)) {
                    enabledChannels.add(channelConfig);
                }
            }
            return enabledChannels;
        }

        @Override
        public boolean hasRuntimeChannelConfigs(String sceneCode) {
            return !channelConfigs.isEmpty();
        }

        @Override
        public void saveChannelConfigs(String sceneCode, List<NotifyTemplateChannelDTO> channelConfigs) {
        }
    }

    private static class FakeNotifyDispatchService implements NotifyDispatchService {
        private final List<SysNotifyDispatch> createdDispatches = new ArrayList<>();

        @Override
        public Long createDispatch(SysNotifyDispatch dispatch) {
            if (dispatch.getId() == null) {
                dispatch.setId((long) (createdDispatches.size() + 1));
            }
            createdDispatches.add(dispatch);
            return dispatch.getId();
        }

        @Override
        public SysNotifyDispatch getById(Long id) {
            return null;
        }

        @Override
        public List<SysNotifyDispatch> listSendableDispatches(LocalDateTime now, Integer limit) {
            return Collections.emptyList();
        }

        @Override
        public List<SysNotifyDispatch> listTimeoutProcessingDispatches(LocalDateTime timeoutBefore, Integer limit) {
            return Collections.emptyList();
        }

        @Override
        public boolean markProcessing(Long dispatchId) {
            return false;
        }

        @Override
        public void markSuccess(Long dispatchId, String resultCode, String resultMessage, String channelResponseJson) {
        }

        @Override
        public void markFailed(Long dispatchId, Integer retryCount, LocalDateTime nextRetryTime, String resultCode,
                               String resultMessage, String channelResponseJson) {
        }

        @Override
        public void markSkipped(Long dispatchId, String resultCode, String resultMessage, String channelResponseJson) {
        }

        @Override
        public int recoverTimeoutProcessingDispatches(LocalDateTime timeoutBefore) {
            return 0;
        }

        @Override
        public void markDead(Long dispatchId, String resultCode, String resultMessage) {
        }

        @Override
        public void resetForRetry(Long dispatchId) {
        }

        @Override
        public int consumePendingDispatches() {
            return 0;
        }
    }

    private static class FakeNotifyMessageLogService implements NotifyMessageLogService {
        private final List<SysNotifyMessageLog> logs = new ArrayList<>();

        @Override
        public Long createLog(SysNotifyMessageLog notifyMessageLog) {
            logs.add(notifyMessageLog);
            return (long) logs.size();
        }

        @Override
        public List<SysNotifyMessageLog> listByQuery(com.jasic.aftersales.system.notify.domain.query.NotifyMessageLogQuery query) {
            return Collections.emptyList();
        }
    }

    private static class NoopTransactionManager implements PlatformTransactionManager {

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
        }

        @Override
        public void rollback(TransactionStatus status) {
        }
    }
}


