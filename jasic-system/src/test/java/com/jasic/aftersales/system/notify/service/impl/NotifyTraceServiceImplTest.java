package com.jasic.aftersales.system.notify.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyDispatch;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyEvent;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyMessage;
import com.jasic.aftersales.system.notify.domain.enums.NotifyDispatchResultCodeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyDispatchStatusEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyEventStatusEnum;
import com.jasic.aftersales.system.notify.domain.query.NotifyTraceQuery;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTracePageVO;
import com.jasic.aftersales.system.notify.mapper.NotifyTraceMapper;
import com.jasic.aftersales.system.notify.mapper.SysNotifyDispatchMapper;
import com.jasic.aftersales.system.notify.mapper.SysNotifyMessageMapper;
import com.jasic.aftersales.system.notify.service.NotifyDispatchService;
import com.jasic.aftersales.system.notify.service.NotifyEventService;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;

/**
 * NotifyTraceServiceImpl 单测。
 */
public class NotifyTraceServiceImplTest {

    /**
     * 分页查询应返回 Mapper 聚合结果。
     *
     * @throws Exception 反射异常
     */
    @Test
    public void shouldReturnPagedTraceRows() throws Exception {
        NotifyTraceServiceImpl service = new NotifyTraceServiceImpl();
        TraceMapperState traceMapperState = new TraceMapperState();
        NotifyTracePageVO row = new NotifyTracePageVO();
        row.setEventId(100L);
        row.setSceneCode("WORK_ORDER_ASSIGNED");
        row.setEventStatus("FAILED");
        traceMapperState.pageResult = new Page<>(2, 5, 8);
        traceMapperState.pageResult.setRecords(Collections.singletonList(row));

        MessageMapperState messageMapperState = new MessageMapperState();
        SysNotifyMessage message = new SysNotifyMessage();
        message.setId(201L);
        message.setEventId(100L);
        message.setTargetType("IN_APP_TODO");
        message.setTodoStatus("READ");
        messageMapperState.messages = Collections.singletonList(message);

        DispatchMapperState dispatchMapperState = new DispatchMapperState();
        SysNotifyDispatch dispatch = new SysNotifyDispatch();
        dispatch.setId(301L);
        dispatch.setEventId(100L);
        dispatch.setTargetType("MP_SUBSCRIBE_C");
        dispatch.setDispatchStatus("FAILED");
        dispatchMapperState.dispatches = Collections.singletonList(dispatch);

        setField(service, "notifyTraceMapper", createTraceMapperProxy(traceMapperState));
        setField(service, "notifyEventService", createNotifyEventServiceProxy(new EventServiceState()));
        setField(service, "notifyDispatchService", createNotifyDispatchServiceProxy(new DispatchServiceState()));
        setField(service, "sysNotifyMessageMapper", createNotifyMessageMapperProxy(messageMapperState));
        setField(service, "sysNotifyDispatchMapper", createSysNotifyDispatchMapperProxy(dispatchMapperState));

        NotifyTraceQuery query = new NotifyTraceQuery();
        query.setPageNum(2);
        query.setPageSize(5);
        PageResult<NotifyTracePageVO> pageResult = service.listPage(query);

        Assert.assertEquals(Long.valueOf(8L), pageResult.getTotal());
        Assert.assertEquals(Integer.valueOf(2), pageResult.getPageNum());
        Assert.assertEquals(Integer.valueOf(5), pageResult.getPageSize());
        Assert.assertEquals(1, pageResult.getRecords().size());
        Assert.assertEquals(Long.valueOf(100L), pageResult.getRecords().get(0).getEventId());
        Assert.assertEquals(Integer.valueOf(1), pageResult.getRecords().get(0).getMessageCount());
        Assert.assertEquals(Integer.valueOf(1), pageResult.getRecords().get(0).getDispatchCount());
        Assert.assertEquals(1, pageResult.getRecords().get(0).getMessageTargetSummaries().size());
        Assert.assertEquals(1, pageResult.getRecords().get(0).getDispatchTargetSummaries().size());
        Assert.assertSame(query, traceMapperState.lastQuery);
    }

    /**
     * 失败事件应允许人工重试。
     *
     * @throws Exception 反射异常
     */
    @Test
    public void shouldRetryFailedEvent() throws Exception {
        NotifyTraceServiceImpl service = new NotifyTraceServiceImpl();
        EventServiceState eventServiceState = new EventServiceState();
        SysNotifyEvent event = new SysNotifyEvent();
        event.setId(10L);
        event.setStatus(NotifyEventStatusEnum.FAILED.getCode());
        eventServiceState.event = event;
        setField(service, "notifyTraceMapper", createTraceMapperProxy(new TraceMapperState()));
        setField(service, "notifyEventService", createNotifyEventServiceProxy(eventServiceState));
        setField(service, "notifyDispatchService", createNotifyDispatchServiceProxy(new DispatchServiceState()));
        setField(service, "sysNotifyMessageMapper", createNotifyMessageMapperProxy(new MessageMapperState()));
        setField(service, "sysNotifyDispatchMapper", createSysNotifyDispatchMapperProxy(new DispatchMapperState()));

        service.retryEvent(10L);

        Assert.assertEquals(Long.valueOf(10L), eventServiceState.resetEventId);
    }

    /**
     * 成功事件不应允许人工重试。
     *
     * @throws Exception 反射异常
     */
    @Test
    public void shouldRejectRetryWhenEventStatusIsSuccess() throws Exception {
        NotifyTraceServiceImpl service = new NotifyTraceServiceImpl();
        EventServiceState eventServiceState = new EventServiceState();
        SysNotifyEvent event = new SysNotifyEvent();
        event.setId(11L);
        event.setStatus(NotifyEventStatusEnum.SUCCESS.getCode());
        eventServiceState.event = event;
        setField(service, "notifyTraceMapper", createTraceMapperProxy(new TraceMapperState()));
        setField(service, "notifyEventService", createNotifyEventServiceProxy(eventServiceState));
        setField(service, "notifyDispatchService", createNotifyDispatchServiceProxy(new DispatchServiceState()));
        setField(service, "sysNotifyMessageMapper", createNotifyMessageMapperProxy(new MessageMapperState()));
        setField(service, "sysNotifyDispatchMapper", createSysNotifyDispatchMapperProxy(new DispatchMapperState()));

        try {
            service.retryEvent(11L);
            Assert.fail("Expected ServiceException");
        } catch (ServiceException ex) {
            Assert.assertEquals("仅 FAILED/DEAD 状态的通知事件允许人工重试", ex.getMessage());
        }
    }

    /**
     * 失败分发任务应允许人工重试并重新进入待发送。
     *
     * @throws Exception 反射异常
     */
    @Test
    public void shouldRetryFailedDispatch() throws Exception {
        NotifyTraceServiceImpl service = new NotifyTraceServiceImpl();
        DispatchServiceState dispatchServiceState = new DispatchServiceState();
        SysNotifyDispatch dispatch = new SysNotifyDispatch();
        dispatch.setId(20L);
        dispatch.setDispatchStatus(NotifyDispatchStatusEnum.FAILED.getCode());
        dispatchServiceState.dispatch = dispatch;
        setField(service, "notifyTraceMapper", createTraceMapperProxy(new TraceMapperState()));
        setField(service, "notifyEventService", createNotifyEventServiceProxy(new EventServiceState()));
        setField(service, "notifyDispatchService", createNotifyDispatchServiceProxy(dispatchServiceState));
        setField(service, "sysNotifyMessageMapper", createNotifyMessageMapperProxy(new MessageMapperState()));
        setField(service, "sysNotifyDispatchMapper", createSysNotifyDispatchMapperProxy(new DispatchMapperState()));

        service.retryDispatch(20L);

        Assert.assertEquals(Long.valueOf(20L), dispatchServiceState.resetDispatchId);
    }

    /**
     * 新建事件可由人工标记为死信，避免异常事件继续被自动消费。
     *
     * @throws Exception 反射异常
     */
    @Test
    public void shouldMarkNewEventDeadWithManualReason() throws Exception {
        NotifyTraceServiceImpl service = new NotifyTraceServiceImpl();
        EventServiceState eventServiceState = new EventServiceState();
        SysNotifyEvent event = new SysNotifyEvent();
        event.setId(12L);
        event.setStatus(NotifyEventStatusEnum.NEW.getCode());
        eventServiceState.event = event;
        setField(service, "notifyTraceMapper", createTraceMapperProxy(new TraceMapperState()));
        setField(service, "notifyEventService", createNotifyEventServiceProxy(eventServiceState));
        setField(service, "notifyDispatchService", createNotifyDispatchServiceProxy(new DispatchServiceState()));
        setField(service, "sysNotifyMessageMapper", createNotifyMessageMapperProxy(new MessageMapperState()));
        setField(service, "sysNotifyDispatchMapper", createSysNotifyDispatchMapperProxy(new DispatchMapperState()));

        service.markEventDead(12L, "重复事件已人工关闭");

        Assert.assertEquals(Long.valueOf(12L), eventServiceState.markDeadEventId);
        Assert.assertEquals("人工标记不再处理：重复事件已人工关闭", eventServiceState.markDeadMessage);
    }

    /**
     * 失败分发任务应带人工原因标记死信。
     *
     * @throws Exception 反射异常
     */
    @Test
    public void shouldMarkFailedDispatchDeadWithManualReason() throws Exception {
        NotifyTraceServiceImpl service = new NotifyTraceServiceImpl();
        DispatchServiceState dispatchServiceState = new DispatchServiceState();
        SysNotifyDispatch dispatch = new SysNotifyDispatch();
        dispatch.setId(21L);
        dispatch.setDispatchStatus(NotifyDispatchStatusEnum.FAILED.getCode());
        dispatchServiceState.dispatch = dispatch;
        setField(service, "notifyTraceMapper", createTraceMapperProxy(new TraceMapperState()));
        setField(service, "notifyEventService", createNotifyEventServiceProxy(new EventServiceState()));
        setField(service, "notifyDispatchService", createNotifyDispatchServiceProxy(dispatchServiceState));
        setField(service, "sysNotifyMessageMapper", createNotifyMessageMapperProxy(new MessageMapperState()));
        setField(service, "sysNotifyDispatchMapper", createSysNotifyDispatchMapperProxy(new DispatchMapperState()));

        service.markDispatchDead(21L, "第三方模板停用");

        Assert.assertEquals(Long.valueOf(21L), dispatchServiceState.markDeadDispatchId);
        Assert.assertEquals(NotifyDispatchResultCodeEnum.DEAD_MANUAL_CLOSED.getCode(), dispatchServiceState.markDeadResultCode);
        Assert.assertEquals("人工标记不再处理：第三方模板停用", dispatchServiceState.markDeadResultMessage);
    }

    /**
     * 事件详情应组装关联的站内消息和分发任务。
     *
     * @throws Exception 反射异常
     */
    @Test
    public void shouldAssembleEventDetailWithMessagesAndDispatches() throws Exception {
        NotifyTraceServiceImpl service = new NotifyTraceServiceImpl();
        EventServiceState eventServiceState = new EventServiceState();
        SysNotifyEvent event = new SysNotifyEvent();
        event.setId(31L);
        event.setEventKey("event-31");
        event.setStatus(NotifyEventStatusEnum.FAILED.getCode());
        eventServiceState.event = event;

        MessageMapperState messageMapperState = new MessageMapperState();
        SysNotifyMessage message = new SysNotifyMessage();
        message.setId(41L);
        message.setEventId(31L);
        message.setTodoStatus("READ");
        messageMapperState.messages = Collections.singletonList(message);

        DispatchMapperState dispatchMapperState = new DispatchMapperState();
        SysNotifyDispatch dispatch = new SysNotifyDispatch();
        dispatch.setId(51L);
        dispatch.setEventId(31L);
        dispatch.setDispatchStatus(NotifyDispatchStatusEnum.FAILED.getCode());
        dispatchMapperState.dispatches = Collections.singletonList(dispatch);

        setField(service, "notifyTraceMapper", createTraceMapperProxy(new TraceMapperState()));
        setField(service, "notifyEventService", createNotifyEventServiceProxy(eventServiceState));
        setField(service, "notifyDispatchService", createNotifyDispatchServiceProxy(new DispatchServiceState()));
        setField(service, "sysNotifyMessageMapper", createNotifyMessageMapperProxy(messageMapperState));
        setField(service, "sysNotifyDispatchMapper", createSysNotifyDispatchMapperProxy(dispatchMapperState));

        Assert.assertEquals(1, service.getEventDetail(31L).getMessages().size());
        Assert.assertEquals(1, service.getEventDetail(31L).getDispatches().size());
        Assert.assertEquals(1, service.getEventDetail(31L).getMessageTargetSummaries().size());
        Assert.assertEquals(1, service.getEventDetail(31L).getDispatchTargetSummaries().size());
    }

    /**
     * 创建 Trace Mapper 代理。
     *
     * @param state 测试状态
     * @return Mapper 代理
     */
    @SuppressWarnings("unchecked")
    private NotifyTraceMapper createTraceMapperProxy(TraceMapperState state) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectTracePage".equals(method.getName())) {
                    state.lastQuery = (NotifyTraceQuery) args[1];
                    return state.pageResult;
                }
                return null;
            }
        };
        return (NotifyTraceMapper) Proxy.newProxyInstance(
                NotifyTraceMapper.class.getClassLoader(),
                new Class[]{NotifyTraceMapper.class},
                handler
        );
    }

    /**
     * 创建通知事件服务代理。
     *
     * @param state 测试状态
     * @return 服务代理
     */
    @SuppressWarnings("unchecked")
    private NotifyEventService createNotifyEventServiceProxy(EventServiceState state) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("getById".equals(method.getName())) {
                    return state.event;
                }
                if ("resetForRetry".equals(method.getName())) {
                    state.resetEventId = (Long) args[0];
                    return null;
                }
                if ("markDead".equals(method.getName())) {
                    state.markDeadEventId = (Long) args[0];
                    state.markDeadMessage = (String) args[1];
                    return null;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (NotifyEventService) Proxy.newProxyInstance(
                NotifyEventService.class.getClassLoader(),
                new Class[]{NotifyEventService.class},
                handler
        );
    }

    /**
     * 创建通知分发服务代理。
     *
     * @param state 测试状态
     * @return 服务代理
     */
    @SuppressWarnings("unchecked")
    private NotifyDispatchService createNotifyDispatchServiceProxy(DispatchServiceState state) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("getById".equals(method.getName())) {
                    return state.dispatch;
                }
                if ("resetForRetry".equals(method.getName())) {
                    state.resetDispatchId = (Long) args[0];
                    return null;
                }
                if ("markDead".equals(method.getName())) {
                    state.markDeadDispatchId = (Long) args[0];
                    state.markDeadResultCode = (String) args[1];
                    state.markDeadResultMessage = (String) args[2];
                    return null;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (NotifyDispatchService) Proxy.newProxyInstance(
                NotifyDispatchService.class.getClassLoader(),
                new Class[]{NotifyDispatchService.class},
                handler
        );
    }

    /**
     * 创建通知消息 Mapper 代理。
     *
     * @param state 测试状态
     * @return Mapper 代理
     */
    @SuppressWarnings("unchecked")
    private SysNotifyMessageMapper createNotifyMessageMapperProxy(MessageMapperState state) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectList".equals(method.getName())) {
                    state.lastWrapper = (LambdaQueryWrapper<SysNotifyMessage>) args[0];
                    return state.messages;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (SysNotifyMessageMapper) Proxy.newProxyInstance(
                SysNotifyMessageMapper.class.getClassLoader(),
                new Class[]{SysNotifyMessageMapper.class},
                handler
        );
    }

    /**
     * 创建通知分发 Mapper 代理。
     *
     * @param state 测试状态
     * @return Mapper 代理
     */
    @SuppressWarnings("unchecked")
    private SysNotifyDispatchMapper createSysNotifyDispatchMapperProxy(DispatchMapperState state) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectList".equals(method.getName())) {
                    state.lastWrapper = (LambdaQueryWrapper<SysNotifyDispatch>) args[0];
                    return state.dispatches;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (SysNotifyDispatchMapper) Proxy.newProxyInstance(
                SysNotifyDispatchMapper.class.getClassLoader(),
                new Class[]{SysNotifyDispatchMapper.class},
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
     * 返回基础类型默认值。
     *
     * @param type 返回类型
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
        if (double.class.equals(type)) {
            return 0D;
        }
        if (float.class.equals(type)) {
            return 0F;
        }
        if (short.class.equals(type)) {
            return (short) 0;
        }
        if (byte.class.equals(type)) {
            return (byte) 0;
        }
        if (char.class.equals(type)) {
            return '\0';
        }
        return null;
    }

    /**
     * Trace Mapper 测试状态。
     */
    private static class TraceMapperState {
        private Page<NotifyTracePageVO> pageResult = new Page<>();
        private NotifyTraceQuery lastQuery;
    }

    /**
     * 事件服务测试状态。
     */
    private static class EventServiceState {
        private SysNotifyEvent event;
        private Long resetEventId;
        private Long markDeadEventId;
        private String markDeadMessage;
    }

    /**
     * 分发服务测试状态。
     */
    private static class DispatchServiceState {
        private SysNotifyDispatch dispatch;
        private Long resetDispatchId;
        private Long markDeadDispatchId;
        private String markDeadResultCode;
        private String markDeadResultMessage;
    }

    /**
     * 通知消息 Mapper 测试状态。
     */
    private static class MessageMapperState {
        private List<SysNotifyMessage> messages = Collections.emptyList();
        private LambdaQueryWrapper<SysNotifyMessage> lastWrapper;
    }

    /**
     * 通知分发 Mapper 测试状态。
     */
    private static class DispatchMapperState {
        private List<SysNotifyDispatch> dispatches = Collections.emptyList();
        private LambdaQueryWrapper<SysNotifyDispatch> lastWrapper;
    }
}
