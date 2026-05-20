package com.jasic.aftersales.system.notify.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.system.domain.entity.SysUser;
import com.jasic.aftersales.system.mapper.SysUserCompanyMapper;
import com.jasic.aftersales.system.mapper.SysUserMapper;
import com.jasic.aftersales.system.notify.domain.dto.NotifyAssignedEventDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyEvaluationInviteEventDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyWorkOrderEvaluatedEventDTO;
import com.jasic.aftersales.system.notify.domain.entity.NotifyScene;
import com.jasic.aftersales.system.notify.domain.entity.NotifySceneTarget;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyDispatch;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyEvent;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyMessage;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyMessageLog;
import com.jasic.aftersales.system.notify.domain.enums.NotifyBizTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyDispatchResultCodeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyDispatchStatusEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyEventStatusEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyEventTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyInvalidReasonEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyReceiverTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyTodoStatusEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyTypeEnum;
import com.jasic.aftersales.system.notify.domain.query.NotifyEventQuery;
import com.jasic.aftersales.system.notify.domain.query.NotifyMessageQuery;
import com.jasic.aftersales.system.notify.domain.vo.NotifyMessagePageVO;
import com.jasic.aftersales.system.notify.mapper.NotifySceneMapper;
import com.jasic.aftersales.system.notify.mapper.NotifySceneTargetMapper;
import com.jasic.aftersales.system.notify.mapper.SysNotifyEventMapper;
import com.jasic.aftersales.system.notify.service.NotifyDispatchService;
import com.jasic.aftersales.system.notify.service.NotifyEventService;
import com.jasic.aftersales.system.notify.service.NotifyMessageLogService;
import com.jasic.aftersales.system.notify.service.NotifyMessageService;
import com.jasic.aftersales.system.notify.service.NotifyTemplateRenderService;
import com.jasic.aftersales.system.notify.service.support.NotifyEventHandler;
import com.jasic.aftersales.system.notify.service.support.NotifyEventHandlerRegistry;
import com.jasic.aftersales.system.notify.service.support.WorkOrderAssignedNotifyEventHandler;
import com.jasic.aftersales.system.notify.service.support.WorkOrderEvaluatedNotifyEventHandler;
import com.jasic.aftersales.system.notify.service.support.WorkOrderEvaluationInviteNotifyEventHandler;
import com.jasic.aftersales.system.notify.support.NotifyConstants;
import com.jasic.aftersales.system.notify.support.NotifyDispatchPayload;
import com.jasic.aftersales.system.notify.support.NotifyEventExecutionContext;
import com.jasic.aftersales.system.notify.support.NotifyReceiverSnapshot;
import com.jasic.aftersales.system.notify.support.NotifySceneCode;
import com.jasic.aftersales.system.notify.support.NotifySceneRegistry;
import com.jasic.aftersales.system.notify.support.NotifyTemplateChannelConfig;
import com.jasic.aftersales.system.notify.support.NotifyTemplateRenderResult;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
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

/**
 * 通知事件消费链路测试。
 *
 * <p>本测试重点覆盖阶段二新增的“单事件多目标分流”能力：
 * 1. 同一事件同时生成站内消息、站内待办和小程序分发表
 * 2. 转派前旧待办先失效，再创建新的多目标产物
 * 3. 小程序目标统一写入分发表并固化发送快照
 * 4. 事件异常仍走统一重试与死信规则</p>
 *
 * @author Codex
 * @date 2026/05/16
 */
public class NotifyEventConsumeServiceImplTest {

    /**
     * 工单派单场景同时启用多个目标时，应一次性产出消息、待办和分发任务。
     *
     * @throws Exception 反射异常
     */
    @Test
    public void shouldCreateMultiTargetsForAssignedScene() throws Exception {
        FakeNotifyEventService eventService = new FakeNotifyEventService();
        FakeNotifyMessageService messageService = new FakeNotifyMessageService();
        FakeNotifyMessageLogService logService = new FakeNotifyMessageLogService();
        FakeNotifyDispatchService dispatchService = new FakeNotifyDispatchService();
        TargetMapperState targetState = new TargetMapperState();
        targetState.targets.add(buildInAppTarget(1L, NotifyTypeEnum.IN_APP_MESSAGE.getCode()));
        targetState.targets.add(buildInAppTarget(2L, NotifyTypeEnum.IN_APP_TODO.getCode()));
        targetState.targets.add(buildMpTarget(3L, NotifySceneCode.WORK_ORDER_ASSIGNED.getCode()));

        SysNotifyEvent event = buildAssignedEvent(1L, 88L, 200L, 100L, NotifyConstants.ASSIGN_TYPE_ASSIGN, null, 200L);
        eventService.events.put(event.getId(), event);
        eventService.pendingEventIds.add(event.getId());

        NotifyEventConsumeServiceImpl service = createService(
                eventService,
                messageService,
                logService,
                dispatchService,
                targetState,
                buildUserMapper(200L, "维修员A", "openid-200")
        );

        int successCount = service.consumePendingEvents();

        Assert.assertEquals(1, successCount);
        Assert.assertEquals(NotifyEventStatusEnum.SUCCESS.getCode(), eventService.events.get(1L).getStatus());
        Assert.assertEquals(2, messageService.createdMessages.size());
        Assert.assertNotNull(messageService.getByEventIdAndTargetType(event.getId(), NotifyTypeEnum.IN_APP_MESSAGE.getCode()));
        Assert.assertNotNull(messageService.getByEventIdAndTargetType(event.getId(), NotifyTypeEnum.IN_APP_TODO.getCode()));

        SysNotifyDispatch dispatch = dispatchService.createdDispatches.get(0);
        Assert.assertEquals(NotifyDispatchStatusEnum.PENDING.getCode(), dispatch.getDispatchStatus());
        Assert.assertEquals(NotifySceneCode.WORK_ORDER_ASSIGNED.getCode(), dispatch.getSceneCode());
        Assert.assertEquals(NotifyTypeEnum.MP_SUBSCRIBE_B.getCode(), dispatch.getTargetType());
        Assert.assertEquals(NotifyReceiverTypeEnum.REPAIRER.getCode(), dispatch.getReceiverType());

        NotifyDispatchPayload payload = JSONUtil.toBean(dispatch.getPayloadJson(), NotifyDispatchPayload.class);
        Assert.assertEquals(NotifySceneCode.WORK_ORDER_ASSIGNED.getCode(), payload.getSceneCode());
        Assert.assertEquals(NotifyTypeEnum.MP_SUBSCRIBE_B.getCode(), payload.getTargetType());
        Assert.assertEquals("pages/order/detail?workOrderId=${workOrderId}", payload.getChannelConfig().getPagePathTemplate());
        Assert.assertEquals("客户A", payload.getVariables().get("customerName"));
        Assert.assertEquals("13800138000", payload.getVariables().get("customerMobile"));
        Assert.assertEquals(2, logService.logs.size());
    }

    /**
     * B 端网点级通知命中多个可接单用户时，应为每个用户分别写入分发表。
     *
     * @throws Exception 反射异常
     */
    @Test
    public void shouldCreateOneDispatchPerAssignUserForCreateScene() throws Exception {
        FakeNotifyEventService eventService = new FakeNotifyEventService();
        FakeNotifyMessageService messageService = new FakeNotifyMessageService();
        FakeNotifyMessageLogService logService = new FakeNotifyMessageLogService();
        FakeNotifyDispatchService dispatchService = new FakeNotifyDispatchService();
        TargetMapperState targetState = new TargetMapperState();
        NotifySceneTarget target = buildMpTarget(1L, NotifySceneCode.WORK_ORDER_ACCEPT.getCode());
        target.setTargetType(NotifyTypeEnum.MP_SUBSCRIBE_B.getCode());
        targetState.targets.add(target);

        SysNotifyEvent event = new SysNotifyEvent();
        event.setId(6L);
        event.setEventKey("event-6");
        event.setEventType(NotifyEventTypeEnum.WORK_ORDER_ACCEPT.getCode());
        event.setSceneCode(NotifySceneCode.WORK_ORDER_ACCEPT.getCode());
        event.setBizType(NotifyBizTypeEnum.WORK_ORDER.getCode());
        event.setBizId(96L);
        event.setBizNo("WO-96");
        event.setStatus(NotifyEventStatusEnum.NEW.getCode());
        event.setRetryCount(0);
        eventService.events.put(event.getId(), event);
        eventService.pendingEventIds.add(event.getId());

        NotifyEventConsumeServiceImpl service = createService(
                eventService,
                messageService,
                logService,
                dispatchService,
                targetState,
                buildUserMapper(null, null, null)
        );
        setField(service, "notifyEventHandlerRegistry", buildRegistryWithHandlers(Collections.singletonList(new NotifyEventHandler() {
            @Override
            public boolean supports(String eventType) {
                return NotifyEventTypeEnum.WORK_ORDER_ACCEPT.getCode().equals(eventType);
            }

            @Override
            public NotifyEventExecutionContext buildExecutionContext(SysNotifyEvent sourceEvent) {
                NotifyEventExecutionContext context = new NotifyEventExecutionContext();
                context.setSceneCode(NotifySceneCode.WORK_ORDER_ACCEPT.getCode());
                context.setTemplateVariables(new LinkedHashMap<String, Object>() {{
                    put("workOrderId", 96L);
                    put("orderNo", "WO-96");
                    put("customerName", "张三");
                }});
                context.addReceiverSnapshots(
                        NotifyReceiverTypeEnum.ASSIGN_USER.getCode(),
                        new ArrayList<NotifyReceiverSnapshot>() {{
                            add(NotifyReceiverSnapshot.of(
                                    NotifyReceiverTypeEnum.ASSIGN_USER.getCode(),
                                    301L,
                                    2002L,
                                    "工程师A",
                                    "openid-301"
                            ));
                            add(NotifyReceiverSnapshot.of(
                                    NotifyReceiverTypeEnum.ASSIGN_USER.getCode(),
                                    302L,
                                    2002L,
                                    "工程师B",
                                    "openid-302"
                            ));
                        }}
                );
                return context;
            }
        })));

        int successCount = service.consumePendingEvents();

        Assert.assertEquals(1, successCount);
        Assert.assertEquals(2, dispatchService.createdDispatches.size());
        Assert.assertEquals(Long.valueOf(301L), dispatchService.createdDispatches.get(0).getReceiverId());
        Assert.assertEquals("openid-301", dispatchService.createdDispatches.get(0).getReceiverAddress());
        Assert.assertEquals(Long.valueOf(302L), dispatchService.createdDispatches.get(1).getReceiverId());
        Assert.assertEquals("openid-302", dispatchService.createdDispatches.get(1).getReceiverAddress());
    }

    /**
     * 转派场景应先失效旧维修员待办，再创建新待办。
     *
     * @throws Exception 反射异常
     */
    @Test
    public void shouldInvalidateTransferredTodoBeforeCreatingNewTodo() throws Exception {
        FakeNotifyEventService eventService = new FakeNotifyEventService();
        FakeNotifyMessageService messageService = new FakeNotifyMessageService();
        FakeNotifyMessageLogService logService = new FakeNotifyMessageLogService();
        FakeNotifyDispatchService dispatchService = new FakeNotifyDispatchService();
        TargetMapperState targetState = new TargetMapperState();
        targetState.targets.add(buildInAppTarget(1L, NotifyTypeEnum.IN_APP_TODO.getCode()));

        SysNotifyEvent event = buildAssignedEvent(2L, 89L, 201L, 101L, NotifyConstants.ASSIGN_TYPE_TRANSFER, 200L, 201L);
        eventService.events.put(event.getId(), event);
        eventService.pendingEventIds.add(event.getId());

        SysNotifyMessage oldTodo = buildActiveTodo(11L, 89L, 200L, NotifyTodoStatusEnum.PENDING.getCode());
        messageService.activeTodosByReceiver.put(200L, new ArrayList<SysNotifyMessage>() {{
            add(oldTodo);
        }});

        NotifyEventConsumeServiceImpl service = createService(
                eventService,
                messageService,
                logService,
                dispatchService,
                targetState,
                buildUserMapper(201L, "维修员B", "openid-201")
        );

        int successCount = service.consumePendingEvents();

        Assert.assertEquals(1, successCount);
        Assert.assertEquals(NotifyTodoStatusEnum.INVALID.getCode(), oldTodo.getTodoStatus());
        Assert.assertEquals(NotifyInvalidReasonEnum.TRANSFERRED.getCode(), oldTodo.getInvalidReason());
        Assert.assertNotNull(messageService.getByEventIdAndTargetType(event.getId(), NotifyTypeEnum.IN_APP_TODO.getCode()));
        Assert.assertEquals(2, logService.logs.size());
    }

    /**
     * 维修员缺少 openid 时，小程序目标应写入跳过分发记录，而不是直接发送。
     *
     * @throws Exception 反射异常
     */
    @Test
    public void shouldCreateSkippedDispatchWhenAssignedReceiverOpenidMissing() throws Exception {
        FakeNotifyEventService eventService = new FakeNotifyEventService();
        FakeNotifyMessageService messageService = new FakeNotifyMessageService();
        FakeNotifyMessageLogService logService = new FakeNotifyMessageLogService();
        FakeNotifyDispatchService dispatchService = new FakeNotifyDispatchService();
        TargetMapperState targetState = new TargetMapperState();
        targetState.targets.add(buildMpTarget(1L, NotifySceneCode.WORK_ORDER_ASSIGNED.getCode()));

        SysNotifyEvent event = buildAssignedEvent(3L, 90L, 202L, 102L, NotifyConstants.ASSIGN_TYPE_ASSIGN, null, 202L);
        eventService.events.put(event.getId(), event);
        eventService.pendingEventIds.add(event.getId());

        NotifyEventConsumeServiceImpl service = createService(
                eventService,
                messageService,
                logService,
                dispatchService,
                targetState,
                buildUserMapper(202L, "维修员C", null)
        );

        int successCount = service.consumePendingEvents();

        Assert.assertEquals(1, successCount);
        Assert.assertEquals(1, dispatchService.createdDispatches.size());
        Assert.assertEquals(NotifyDispatchStatusEnum.SKIPPED.getCode(), dispatchService.createdDispatches.get(0).getDispatchStatus());
        Assert.assertEquals(NotifyDispatchResultCodeEnum.SKIPPED_OPENID_MISSING.getCode(),
                dispatchService.createdDispatches.get(0).getResultCode());
    }

    /**
     * 客户评价邀请场景应统一写入待发送的小程序分发表。
     *
     * @throws Exception 反射异常
     */
    @Test
    public void shouldCreatePendingDispatchForEvaluationInvite() throws Exception {
        FakeNotifyEventService eventService = new FakeNotifyEventService();
        FakeNotifyMessageService messageService = new FakeNotifyMessageService();
        FakeNotifyMessageLogService logService = new FakeNotifyMessageLogService();
        FakeNotifyDispatchService dispatchService = new FakeNotifyDispatchService();
        TargetMapperState targetState = new TargetMapperState();
        targetState.targets.add(buildMpTarget(1L, NotifySceneCode.WORK_ORDER_EVALUATION_INVITE.getCode()));

        SysNotifyEvent event = buildEvaluationEvent(4L, 91L, 9001L);
        eventService.events.put(event.getId(), event);
        eventService.pendingEventIds.add(event.getId());

        NotifyEventConsumeServiceImpl service = createService(
                eventService,
                messageService,
                logService,
                dispatchService,
                targetState,
                buildUserMapper(null, null, null)
        );

        int successCount = service.consumePendingEvents();

        Assert.assertEquals(1, successCount);
        Assert.assertEquals(1, dispatchService.createdDispatches.size());
        SysNotifyDispatch dispatch = dispatchService.createdDispatches.get(0);
        Assert.assertEquals(NotifyDispatchStatusEnum.PENDING.getCode(), dispatch.getDispatchStatus());
        Assert.assertEquals(NotifyReceiverTypeEnum.CUSTOMER.getCode(), dispatch.getReceiverType());
        Assert.assertEquals("openid-9001", dispatch.getReceiverAddress());
        NotifyDispatchPayload payload = JSONUtil.toBean(dispatch.getPayloadJson(), NotifyDispatchPayload.class);
        Assert.assertEquals("客户评价邀请-MP_SUBSCRIBE_C", payload.getTemplateName());
    }

    /**
     * 事件上下文解析失败时，应进入失败并安排重试。
     *
     * @throws Exception 反射异常
     */
    /**
     * B 端客户评价完成提醒应按责任维修员、最后派单人和公司主账号分别生成分发表。
     *
     * @throws Exception 反射异常
     */
    @Test
    public void shouldCreateOneDispatchPerResolvedReceiverForEvaluatedScene() throws Exception {
        FakeNotifyEventService eventService = new FakeNotifyEventService();
        FakeNotifyMessageService messageService = new FakeNotifyMessageService();
        FakeNotifyMessageLogService logService = new FakeNotifyMessageLogService();
        FakeNotifyDispatchService dispatchService = new FakeNotifyDispatchService();
        TargetMapperState targetState = new TargetMapperState();
        targetState.targets.add(buildMpTarget(1L, NotifySceneCode.WORK_ORDER_EVALUATED.getCode()));

        SysNotifyEvent event = buildEvaluatedEvent(7L, 93L, 9002L, 501L, 3003L);
        eventService.events.put(event.getId(), event);
        eventService.pendingEventIds.add(event.getId());

        SysNotifyEvent latestAssignedEvent = new SysNotifyEvent();
        latestAssignedEvent.setOperatorId(601L);

        Map<Long, SysUser> users = new LinkedHashMap<>();
        users.put(501L, buildUserSnapshot(501L, "李四", "openid-501"));
        users.put(601L, buildUserSnapshot(601L, "派单员甲", "openid-601"));
        users.put(701L, buildUserSnapshot(701L, "主账号", "openid-701"));

        NotifyEventConsumeServiceImpl service = createService(
                eventService,
                messageService,
                logService,
                dispatchService,
                targetState,
                buildUserMapper(users),
                buildNotifyEventMapper(latestAssignedEvent),
                buildPrimaryAccountMapper(701L)
        );

        int successCount = service.consumePendingEvents();

        Assert.assertEquals(1, successCount);
        Assert.assertEquals(3, dispatchService.createdDispatches.size());
        Assert.assertEquals(Long.valueOf(501L), dispatchService.createdDispatches.get(0).getReceiverId());
        Assert.assertEquals(Long.valueOf(601L), dispatchService.createdDispatches.get(1).getReceiverId());
        Assert.assertEquals(Long.valueOf(701L), dispatchService.createdDispatches.get(2).getReceiverId());
        Assert.assertEquals("openid-701", dispatchService.createdDispatches.get(2).getReceiverAddress());
        Assert.assertEquals(NotifyReceiverTypeEnum.EVALUATED_B_USER.getCode(),
                dispatchService.createdDispatches.get(0).getReceiverType());

        NotifyDispatchPayload payload = JSONUtil.toBean(dispatchService.createdDispatches.get(0).getPayloadJson(),
                NotifyDispatchPayload.class);
        Assert.assertEquals(NotifySceneCode.WORK_ORDER_EVALUATED.getCode(), payload.getSceneCode());
        Assert.assertEquals("李四", payload.getVariables().get("assignedUserName"));
        Assert.assertEquals("张三", payload.getVariables().get("customerName"));
    }

    @Test
    public void shouldMarkEventFailedWhenContextBuildThrows() throws Exception {
        FakeNotifyEventService eventService = new FakeNotifyEventService();
        FakeNotifyMessageService messageService = new FakeNotifyMessageService();
        FakeNotifyMessageLogService logService = new FakeNotifyMessageLogService();
        FakeNotifyDispatchService dispatchService = new FakeNotifyDispatchService();
        TargetMapperState targetState = new TargetMapperState();
        targetState.targets.add(buildInAppTarget(1L, NotifyTypeEnum.IN_APP_TODO.getCode()));

        SysNotifyEvent event = buildAssignedEvent(5L, 92L, 203L, 103L, NotifyConstants.ASSIGN_TYPE_ASSIGN, null, 203L);
        NotifyAssignedEventDTO payload = JSONUtil.toBean(event.getPayloadJson(), NotifyAssignedEventDTO.class);
        payload.setNewAssignedUserId(null);
        event.setPayloadJson(JSONUtil.toJsonStr(payload));
        event.setRetryCount(1);
        eventService.events.put(event.getId(), event);
        eventService.pendingEventIds.add(event.getId());

        NotifyEventConsumeServiceImpl service = createService(
                eventService,
                messageService,
                logService,
                dispatchService,
                targetState,
                buildUserMapper(null, null, null)
        );

        int successCount = service.consumePendingEvents();

        Assert.assertEquals(0, successCount);
        Assert.assertEquals(NotifyEventStatusEnum.FAILED.getCode(), eventService.events.get(5L).getStatus());
        Assert.assertEquals(Integer.valueOf(2), eventService.events.get(5L).getRetryCount());
        Assert.assertNotNull(eventService.events.get(5L).getNextRetryTime());
    }

    /**
     * 构造待测消费服务。
     *
     * @param eventService 事件服务桩
     * @param messageService 消息服务桩
     * @param logService 日志服务桩
     * @param dispatchService 分发服务桩
     * @param targetState 目标配置桩
     * @param userMapper 用户Mapper桩
     * @return 待测服务
     * @throws Exception 反射异常
     */
    private NotifyEventConsumeServiceImpl createService(FakeNotifyEventService eventService,
                                                        FakeNotifyMessageService messageService,
                                                        FakeNotifyMessageLogService logService,
                                                        FakeNotifyDispatchService dispatchService,
                                                        TargetMapperState targetState,
                                                        SysUserMapper userMapper) throws Exception {
        return createService(
                eventService,
                messageService,
                logService,
                dispatchService,
                targetState,
                userMapper,
                buildNotifyEventMapper(null),
                buildPrimaryAccountMapper(null)
        );
    }

    /**
     * 构造待测服务，并注入评价提醒处理器所需的额外依赖。
     *
     * @param eventService 事件服务桩
     * @param messageService 消息服务桩
     * @param logService 日志服务桩
     * @param dispatchService 分发服务桩
     * @param targetState 目标配置桩
     * @param userMapper 用户 Mapper 桩
     * @param notifyEventMapper 通知事件 Mapper 桩
     * @param userCompanyMapper 用户公司关系 Mapper 桩
     * @return 待测服务
     * @throws Exception 反射异常
     */
    private NotifyEventConsumeServiceImpl createService(FakeNotifyEventService eventService,
                                                        FakeNotifyMessageService messageService,
                                                        FakeNotifyMessageLogService logService,
                                                        FakeNotifyDispatchService dispatchService,
                                                        TargetMapperState targetState,
                                                        SysUserMapper userMapper,
                                                        SysNotifyEventMapper notifyEventMapper,
                                                        SysUserCompanyMapper userCompanyMapper) throws Exception {
        NotifyEventConsumeServiceImpl service = new NotifyEventConsumeServiceImpl();
        setField(service, "notifyEventService", eventService);
        setField(service, "notifyEventHandlerRegistry",
                buildRegistry(messageService, logService, userMapper, notifyEventMapper, userCompanyMapper));
        setField(service, "notifySceneTargetMapper", createTargetMapper(targetState));
        setField(service, "notifySceneMapper", createSceneMapper());
        setField(service, "notifySceneRegistry", new NotifySceneRegistry());
        setField(service, "notifyTemplateRenderService", new FakeNotifyTemplateRenderService());
        setField(service, "notifyMessageService", messageService);
        setField(service, "notifyMessageLogService", logService);
        setField(service, "notifyDispatchService", dispatchService);
        setField(service, "transactionTemplate", new TransactionTemplate(new NoopTransactionManager()));
        initTargetTableInfo();
        return service;
    }

    /**
     * 构造事件处理器注册表。
     *
     * @param messageService 消息服务桩
     * @param logService 日志服务桩
     * @param userMapper 用户Mapper桩
     * @return 注册表
     * @throws Exception 反射异常
     */
    private NotifyEventHandlerRegistry buildRegistry(FakeNotifyMessageService messageService,
                                                     FakeNotifyMessageLogService logService,
                                                     SysUserMapper userMapper,
                                                     SysNotifyEventMapper notifyEventMapper,
                                                     SysUserCompanyMapper userCompanyMapper) throws Exception {
        WorkOrderAssignedNotifyEventHandler assignedHandler = new WorkOrderAssignedNotifyEventHandler();
        setField(assignedHandler, "notifyMessageService", messageService);
        setField(assignedHandler, "notifyMessageLogService", logService);
        setField(assignedHandler, "sysUserMapper", userMapper);

        WorkOrderEvaluationInviteNotifyEventHandler evaluationHandler = new WorkOrderEvaluationInviteNotifyEventHandler();
        WorkOrderEvaluatedNotifyEventHandler evaluatedHandler = new WorkOrderEvaluatedNotifyEventHandler();
        setField(evaluatedHandler, "sysUserMapper", userMapper);
        setField(evaluatedHandler, "sysNotifyEventMapper", notifyEventMapper);
        setField(evaluatedHandler, "sysUserCompanyMapper", userCompanyMapper);

        NotifyEventHandlerRegistry registry = new NotifyEventHandlerRegistry();
        setField(registry, "notifyEventHandlers", new ArrayList<Object>() {{
            add(assignedHandler);
            add(evaluatedHandler);
            add(evaluationHandler);
        }});
        return registry;
    }

    /**
     * 按指定 handler 列表构造事件处理器注册表。
     *
     * @param handlers handler 列表
     * @return 注册表
     * @throws Exception 反射异常
     */
    private NotifyEventHandlerRegistry buildRegistryWithHandlers(List<?> handlers) throws Exception {
        NotifyEventHandlerRegistry registry = new NotifyEventHandlerRegistry();
        setField(registry, "notifyEventHandlers", handlers);
        return registry;
    }

    /**
     * 构造站内目标配置。
     *
     * @param id 主键
     * @param targetType 目标类型
     * @return 目标配置
     */
    private NotifySceneTarget buildInAppTarget(Long id, String targetType) {
        NotifySceneTarget target = new NotifySceneTarget();
        target.setId(id);
        target.setSceneCode(NotifySceneCode.WORK_ORDER_ASSIGNED.getCode());
        target.setTargetType(targetType);
        target.setEnabled(1);
        return target;
    }

    /**
     * 构造小程序目标配置。
     *
     * @param id 主键
     * @param sceneCode 场景编码
     * @return 目标配置
     */
    private NotifySceneTarget buildMpTarget(Long id, String sceneCode) {
        NotifySceneTarget target = new NotifySceneTarget();
        target.setId(id);
        target.setSceneCode(sceneCode);
        boolean bSideScene = NotifySceneCode.WORK_ORDER_ASSIGNED.getCode().equals(sceneCode)
                || NotifySceneCode.WORK_ORDER_EVALUATED.getCode().equals(sceneCode);
        target.setTargetType(bSideScene
                ? NotifyTypeEnum.MP_SUBSCRIBE_B.getCode()
                : NotifyTypeEnum.MP_SUBSCRIBE_C.getCode());
        target.setEnabled(1);
        NotifyTemplateChannelConfig config = new NotifyTemplateChannelConfig();
        config.setTemplateId("wx-template-001");
        config.setChannelScene(bSideScene ? "B" : "C");
        config.setPagePathTemplate(bSideScene
                ? "pages/order/detail?workOrderId=${workOrderId}"
                : "pages/order/evaluate?workOrderId=${workOrderId}");
        config.setFieldMapping(Collections.singletonList(new com.jasic.aftersales.system.notify.domain.dto.NotifyChannelFieldMappingDTO() {{
            setField("thing1");
            setValue("${orderNo}");
        }}));
        target.setConfigJson(JSONUtil.toJsonStr(config));
        return target;
    }

    /**
     * 构造工单派单事件。
     *
     * @param eventId 事件ID
     * @param bizId 工单ID
     * @param receiverId 接收维修员ID
     * @param operatorId 操作人ID
     * @param assignType 派单类型
     * @param oldAssignedUserId 旧维修员ID
     * @param newAssignedUserId 新维修员ID
     * @return 事件
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
        payload.setCustomerName("客户A");
        payload.setCustomerMobile("13800138000");
        payload.setOperationId("op-" + eventId);

        SysNotifyEvent event = new SysNotifyEvent();
        event.setId(eventId);
        event.setEventKey("event-" + eventId);
        event.setEventType(NotifyEventTypeEnum.WORK_ORDER_ASSIGNED.getCode());
        event.setSceneCode(NotifySceneCode.WORK_ORDER_ASSIGNED.getCode());
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
     * 构造评价邀请事件。
     *
     * @param eventId 事件ID
     * @param bizId 工单ID
     * @param customerId 客户ID
     * @return 事件
     */
    private SysNotifyEvent buildEvaluationEvent(Long eventId, Long bizId, Long customerId) {
        NotifyEvaluationInviteEventDTO payload = new NotifyEvaluationInviteEventDTO();
        payload.setWorkOrderId(bizId);
        payload.setOrderNo("WO-" + bizId);
        payload.setCustomerId(customerId);
        payload.setCustomerMobile("13800138000");
        payload.setCustomerOpenid("openid-" + customerId);
        payload.setCompanyId(3001L);
        payload.setCompanyPhone("0755-99990000");
        payload.setCompanyName("深圳南山服务网点");
        payload.setClosedTime(LocalDateTime.of(2026, 5, 16, 18, 0, 0));

        SysNotifyEvent event = new SysNotifyEvent();
        event.setId(eventId);
        event.setEventKey("event-" + eventId);
        event.setEventType(NotifyEventTypeEnum.WORK_ORDER_EVALUATION_INVITE.getCode());
        event.setSceneCode(NotifySceneCode.WORK_ORDER_EVALUATION_INVITE.getCode());
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
     * 构造旧待办。
     *
     * @param id 消息ID
     * @param bizId 工单ID
     * @param receiverId 接收人ID
     * @param todoStatus 待办状态
     * @return 旧待办
     */
    /**
     * 构造 B 端客户评价完成提醒事件。
     *
     * @param eventId 事件ID
     * @param bizId 工单ID
     * @param customerId 客户ID
     * @param assignedUserId 责任维修员ID
     * @param companyId 最终处理公司ID
     * @return 事件
     */
    private SysNotifyEvent buildEvaluatedEvent(Long eventId, Long bizId, Long customerId,
                                               Long assignedUserId, Long companyId) {
        NotifyWorkOrderEvaluatedEventDTO payload = new NotifyWorkOrderEvaluatedEventDTO();
        payload.setWorkOrderId(bizId);
        payload.setOrderNo("WO-" + bizId);
        payload.setCustomerId(customerId);
        payload.setCustomerName("张三");
        payload.setCustomerMobile("18100005610");
        payload.setAssignedUserId(assignedUserId);
        payload.setAssignedUserName("李四");
        payload.setCurrentAcceptCompanyId(companyId);

        SysNotifyEvent event = new SysNotifyEvent();
        event.setId(eventId);
        event.setEventKey("event-" + eventId);
        event.setEventType(NotifyEventTypeEnum.WORK_ORDER_EVALUATED.getCode());
        event.setSceneCode(NotifySceneCode.WORK_ORDER_EVALUATED.getCode());
        event.setBizType(NotifyBizTypeEnum.WORK_ORDER.getCode());
        event.setBizId(bizId);
        event.setBizNo("WO-" + bizId);
        event.setReceiverId(assignedUserId);
        event.setPayloadJson(JSONUtil.toJsonStr(payload));
        event.setStatus(NotifyEventStatusEnum.NEW.getCode());
        event.setRetryCount(0);
        return event;
    }

    /**
     * 构造用户快照。
     *
     * @param userId 用户ID
     * @param realName 用户姓名
     * @param openid 用户 openid
     * @return 用户快照
     */
    private SysUser buildUserSnapshot(Long userId, String realName, String openid) {
        SysUser user = new SysUser();
        user.setId(userId);
        user.setRealName(realName);
        user.setOpenid(openid);
        user.setStatus(1);
        return user;
    }

    private SysNotifyMessage buildActiveTodo(Long id, Long bizId, Long receiverId, String todoStatus) {
        SysNotifyMessage message = new SysNotifyMessage();
        message.setId(id);
        message.setBizType(NotifyBizTypeEnum.WORK_ORDER.getCode());
        message.setBizId(bizId);
        message.setBizNo("WO-" + bizId);
        message.setReceiverId(receiverId);
        message.setReceiverCompanyId(2002L);
        message.setTargetType(NotifyTypeEnum.IN_APP_TODO.getCode());
        message.setMessageType(NotifyTypeEnum.IN_APP_TODO.getCode());
        message.setTodoStatus(todoStatus);
        return message;
    }

    /**
     * 构造用户Mapper桩。
     *
     * @param userId 用户ID
     * @param realName 用户姓名
     * @param openid 用户openid
     * @return Mapper桩
     */
    private SysUserMapper buildUserMapper(Long userId, String realName, String openid) {
        Map<Long, SysUser> users = new LinkedHashMap<>();
        if (userId != null) {
            SysUser user = new SysUser();
            user.setId(userId);
            user.setRealName(realName);
            user.setOpenid(openid);
            user.setStatus(1);
            users.put(userId, user);
        }
        return buildUserMapper(users);
    }

    /**
     * 构造支持多用户查询的用户 Mapper 桩。
     *
     * @param users 用户快照映射
     * @return Mapper 桩
     */
    private SysUserMapper buildUserMapper(Map<Long, SysUser> users) {
        InvocationHandler handler = (proxy, method, args) -> {
            if ("selectById".equals(method.getName())) {
                Long id = args == null || args.length == 0 ? null : (Long) args[0];
                return users == null ? null : users.get(id);
            }
            return null;
        };
        return (SysUserMapper) Proxy.newProxyInstance(
                SysUserMapper.class.getClassLoader(),
                new Class<?>[]{SysUserMapper.class},
                handler
        );
    }

    /**
     * 构造通知事件 Mapper 桩。
     *
     * @param latestAssignedEvent 最后一条派单事件快照
     * @return Mapper 桩
     */
    private SysNotifyEventMapper buildNotifyEventMapper(SysNotifyEvent latestAssignedEvent) {
        InvocationHandler handler = (proxy, method, args) -> {
            if ("selectOne".equals(method.getName())) {
                return latestAssignedEvent;
            }
            return defaultValue(method.getReturnType());
        };
        return (SysNotifyEventMapper) Proxy.newProxyInstance(
                SysNotifyEventMapper.class.getClassLoader(),
                new Class<?>[]{SysNotifyEventMapper.class},
                handler
        );
    }

    /**
     * 构造公司主账号查询 Mapper 桩。
     *
     * @param primaryAccountUserId 公司主账号用户ID
     * @return Mapper 桩
     */
    private SysUserCompanyMapper buildPrimaryAccountMapper(Long primaryAccountUserId) {
        InvocationHandler handler = (proxy, method, args) -> {
            if ("selectPrimaryAccountUserIdByCompanyId".equals(method.getName())) {
                return primaryAccountUserId;
            }
            return defaultValue(method.getReturnType());
        };
        return (SysUserCompanyMapper) Proxy.newProxyInstance(
                SysUserCompanyMapper.class.getClassLoader(),
                new Class<?>[]{SysUserCompanyMapper.class},
                handler
        );
    }

    /**
     * 构造目标配置Mapper桩。
     *
     * @param state 目标配置状态
     * @return Mapper桩
     */
    @SuppressWarnings("unchecked")
    private NotifySceneTargetMapper createTargetMapper(TargetMapperState state) {
        InvocationHandler handler = (proxy, method, args) -> {
            if ("selectList".equals(method.getName())) {
                LambdaQueryWrapper<NotifySceneTarget> wrapper = (LambdaQueryWrapper<NotifySceneTarget>) args[0];
                String sql = wrapper.getSqlSegment();
                Map<String, Object> params = wrapper.getParamNameValuePairs();
                List<NotifySceneTarget> result = new ArrayList<>();
                for (NotifySceneTarget target : state.targets) {
                    if (sql.contains("scene_code") && !params.containsValue(target.getSceneCode())) {
                        continue;
                    }
                    if (sql.contains("enabled") && !params.containsValue(target.getEnabled())) {
                        continue;
                    }
                    result.add(target);
                }
                return result;
            }
            return defaultValue(method.getReturnType());
        };
        return (NotifySceneTargetMapper) Proxy.newProxyInstance(
                NotifySceneTargetMapper.class.getClassLoader(),
                new Class<?>[]{NotifySceneTargetMapper.class},
                handler
        );
    }

    /**
     * 构造场景配置 Mapper 桩。
     *
     * <p>消费链路正式按 `notify_scene.status` 判断场景总开关，测试桩默认返回启用，
     * 让用例聚焦目标拆分、接收人展开和分发落表。</p>
     *
     * @return Mapper桩
     */
    private NotifySceneMapper createSceneMapper() {
        InvocationHandler handler = (proxy, method, args) -> {
            if ("selectOne".equals(method.getName())) {
                NotifyScene scene = new NotifyScene();
                scene.setStatus(1);
                return scene;
            }
            return defaultValue(method.getReturnType());
        };
        return (NotifySceneMapper) Proxy.newProxyInstance(
                NotifySceneMapper.class.getClassLoader(),
                new Class<?>[]{NotifySceneMapper.class},
                handler
        );
    }

    /**
     * 初始化目标配置实体元数据。
     */
    private void initTargetTableInfo() {
        if (TableInfoHelper.getTableInfo(NotifySceneTarget.class) != null) {
            return;
        }
        Configuration configuration = new Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "notify-scene-target-consume-test");
        assistant.setCurrentNamespace(NotifySceneTargetMapper.class.getName());
        TableInfoHelper.initTableInfo(assistant, NotifySceneTarget.class);
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
     * 返回基础类型默认值。
     *
     * @param type 返回值类型
     * @return 默认值
     */
    private Object defaultValue(Class<?> type) {
        if (type == null || !type.isPrimitive()) {
            return null;
        }
        if (boolean.class.equals(type)) {
            return false;
        }
        if (int.class.equals(type)) {
            return 0;
        }
        if (long.class.equals(type)) {
            return 0L;
        }
        return null;
    }

    /**
     * 事件服务桩。
     */
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
        public List<SysNotifyEvent> listByQuery(NotifyEventQuery query) {
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
            event.setNextRetryTime(nextRetryTime);
            event.setErrorMessage(errorMessage);
            event.setProcessingTime(null);
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
            event.setErrorMessage(errorMessage);
        }

        @Override
        public void resetForRetry(Long eventId) {
        }
    }

    /**
     * 消息服务桩。
     */
    private static class FakeNotifyMessageService implements NotifyMessageService {
        private final List<SysNotifyMessage> createdMessages = new ArrayList<>();
        private final Map<String, SysNotifyMessage> messageByEventAndTarget = new LinkedHashMap<>();
        private final Map<Long, List<SysNotifyMessage>> activeTodosByReceiver = new LinkedHashMap<>();
        private long nextMessageId = 1000L;

        @Override
        public Long createMessage(SysNotifyMessage notifyMessage) {
            notifyMessage.setId(nextMessageId++);
            createdMessages.add(notifyMessage);
            messageByEventAndTarget.put(buildKey(notifyMessage.getEventId(), notifyMessage.getTargetType()), notifyMessage);
            return notifyMessage.getId();
        }

        @Override
        public SysNotifyMessage getById(Long id) {
            return null;
        }

        @Override
        public SysNotifyMessage getByEventId(Long eventId) {
            for (SysNotifyMessage createdMessage : createdMessages) {
                if (eventId.equals(createdMessage.getEventId())) {
                    return createdMessage;
                }
            }
            return null;
        }

        @Override
        public SysNotifyMessage getByEventIdAndTargetType(Long eventId, String targetType) {
            return messageByEventAndTarget.get(buildKey(eventId, targetType));
        }

        @Override
        public List<SysNotifyMessage> listActiveTodoByBizAndReceiver(String bizType, Long bizId, Long receiverId,
                                                                     Long receiverCompanyId) {
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
        public PageResult<NotifyMessagePageVO> listPage(NotifyMessageQuery query) {
            return null;
        }

        @Override
        public Long countTodo(Long receiverId, Long receiverCompanyId) {
            return 0L;
        }

        private String buildKey(Long eventId, String targetType) {
            return eventId + "#" + targetType;
        }
    }

    /**
     * 渲染服务桩。
     */
    private static class FakeNotifyTemplateRenderService implements NotifyTemplateRenderService {

        @Override
        public NotifyTemplateRenderResult render(String sceneCode, String targetType, Map<String, Object> variables) {
            NotifyTemplateRenderResult result = new NotifyTemplateRenderResult();
            result.setNotifyEnabled(true);
            result.setSceneCode(sceneCode);
            if (NotifySceneCode.WORK_ORDER_ASSIGNED.getCode().equals(sceneCode)) {
                result.setSceneName("工单派单");
            } else if (NotifySceneCode.WORK_ORDER_EVALUATED.getCode().equals(sceneCode)) {
                result.setSceneName("评价提醒");
            } else if (NotifySceneCode.WORK_ORDER_EVALUATION_INVITE.getCode().equals(sceneCode)) {
                result.setSceneName("客户评价邀请");
            } else {
                result.setSceneName(sceneCode);
            }
            result.setTemplateCode(sceneCode);
            result.setTemplateName(result.getSceneName() + "-" + targetType);
            result.setTitle("标题-" + targetType);
            result.setSummary("内容-" + variables.get("orderNo") + "-" + targetType);
            result.setRouteType(NotifyConstants.ROUTE_TYPE_WORK_ORDER_DETAIL);
            result.setRouteValue(String.valueOf(variables.get("workOrderId")));
            return result;
        }
    }

    /**
     * 分发服务桩。
     */
    private static class FakeNotifyDispatchService implements NotifyDispatchService {
        private final List<SysNotifyDispatch> createdDispatches = new ArrayList<>();

        @Override
        public Long createDispatch(SysNotifyDispatch dispatch) {
            dispatch.setId((long) (createdDispatches.size() + 1));
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

    /**
     * 消息日志服务桩。
     */
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

    /**
     * 目标配置状态桩。
     */
    private static class TargetMapperState {
        private final List<NotifySceneTarget> targets = new ArrayList<>();
    }

    /**
     * 空事务管理器。
     */
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
