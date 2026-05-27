package com.jasic.aftersales.system.notify.service.impl;

import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.notify.domain.dto.NotifyWorkOrderAcceptEventDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyWorkOrderAcceptedEventDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyWorkOrderEvaluatedEventDTO;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyEvent;
import com.jasic.aftersales.system.notify.service.NotifyEventService;
import com.jasic.aftersales.system.notify.service.NotifyMessageService;
import com.jasic.aftersales.system.notify.support.NotifyConstants;
import com.jasic.aftersales.system.notify.support.NotifySceneCode;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/*** WorkOrderNotifyFacadeImpl 单测。

@author Zoro*/
public class WorkOrderNotifyFacadeImplTest {

    /**
     * 当事件本身没有单一接收人时，仍应写入兼容占位值，
     * 避免历史库 `receiver_id NOT NULL` 约束导致主流程失败。
     *
     * @throws Exception 反射异常
     */
    @Test
    public void shouldUsePlaceholderReceiverIdWhenAcceptEventHasNoConcreteReceiver() throws Exception {
        WorkOrderNotifyFacadeImpl facade = new WorkOrderNotifyFacadeImpl();
        EventServiceState eventServiceState = new EventServiceState();
        setField(facade, "notifyEventService", createNotifyEventServiceProxy(eventServiceState));
        setField(facade, "notifyMessageService", createNotifyMessageServiceProxy());

        NotifyWorkOrderAcceptEventDTO dto = new NotifyWorkOrderAcceptEventDTO();
        dto.setWorkOrderId(101L);
        dto.setOrderNo("GD202605180001");
        dto.setCurrentAcceptCompanyId(501L);

        facade.publishAcceptEvent(dto);

        Assert.assertNotNull(eventServiceState.createdEvent);
        Assert.assertEquals("WORK_ORDER_ACCEPT:101:GD202605180001:501",
                eventServiceState.createdEvent.getEventKey());
        Assert.assertEquals(Long.valueOf(NotifyConstants.EVENT_RECEIVER_ID_PLACEHOLDER),
                eventServiceState.createdEvent.getReceiverId());
    }

    /**
     * 待派单通知幂等命中旧事件时，如果事件快照和当前工单不一致，应显式失败。
     *
     * <p>该用例覆盖测试环境清理不完整、旧通知运行数据残留等场景，
     * 防止新工单因为命中旧事件键而被静默跳过通知发布。</p>
     *
     * @throws Exception 反射异常
     */
    @Test
    public void shouldRejectAcceptEventKeyConflictWhenSnapshotMismatch() throws Exception {
        WorkOrderNotifyFacadeImpl facade = new WorkOrderNotifyFacadeImpl();
        EventServiceState eventServiceState = new EventServiceState();
        SysNotifyEvent existingEvent = new SysNotifyEvent();
        existingEvent.setId(9001L);
        existingEvent.setEventKey("WORK_ORDER_ACCEPT:101:GD202605180001:501");
        existingEvent.setEventType("WORK_ORDER_ACCEPT");
        existingEvent.setSceneCode(NotifySceneCode.WORK_ORDER_ACCEPT.getCode());
        existingEvent.setBizId(999L);
        existingEvent.setBizNo("GD202605170009");
        eventServiceState.existingEvent = existingEvent;
        setField(facade, "notifyEventService", createNotifyEventServiceProxy(eventServiceState));
        setField(facade, "notifyMessageService", createNotifyMessageServiceProxy());

        NotifyWorkOrderAcceptEventDTO dto = new NotifyWorkOrderAcceptEventDTO();
        dto.setWorkOrderId(101L);
        dto.setOrderNo("GD202605180001");
        dto.setCurrentAcceptCompanyId(501L);

        try {
            facade.publishAcceptEvent(dto);
            Assert.fail("待派单通知幂等键冲突时应抛出业务异常");
        } catch (ServiceException ex) {
            Assert.assertEquals("待派单通知幂等键冲突，请检查通知运行数据", ex.getMessage());
        }
        Assert.assertNull(eventServiceState.createdEvent);
    }

    /**
     * 当事件已有真实接收人时，不应被兼容占位值覆盖。
     *
     * @throws Exception 反射异常
     */
    @Test
    public void shouldKeepRealReceiverIdWhenAcceptedEventHasCustomer() throws Exception {
        WorkOrderNotifyFacadeImpl facade = new WorkOrderNotifyFacadeImpl();
        EventServiceState eventServiceState = new EventServiceState();
        setField(facade, "notifyEventService", createNotifyEventServiceProxy(eventServiceState));
        setField(facade, "notifyMessageService", createNotifyMessageServiceProxy());

        NotifyWorkOrderAcceptedEventDTO dto = new NotifyWorkOrderAcceptedEventDTO();
        dto.setWorkOrderId(102L);
        dto.setOrderNo("GD202605180002");
        dto.setCustomerId(9001L);

        facade.publishAcceptedEvent(dto);

        Assert.assertNotNull(eventServiceState.createdEvent);
        Assert.assertEquals(Long.valueOf(9001L), eventServiceState.createdEvent.getReceiverId());
    }

    /**
     * B 端客户评价完成通知虽然会扩展成多接收人，但事件主表仍应固化当前责任维修员作为主接收快照。
     *
     * @throws Exception 反射异常
     */
    @Test
    public void shouldUseAssignedUserAsPrimaryReceiverWhenPublishingEvaluatedEvent() throws Exception {
        WorkOrderNotifyFacadeImpl facade = new WorkOrderNotifyFacadeImpl();
        EventServiceState eventServiceState = new EventServiceState();
        setField(facade, "notifyEventService", createNotifyEventServiceProxy(eventServiceState));
        setField(facade, "notifyMessageService", createNotifyMessageServiceProxy());

        NotifyWorkOrderEvaluatedEventDTO dto = new NotifyWorkOrderEvaluatedEventDTO();
        dto.setWorkOrderId(103L);
        dto.setOrderNo("GD202605180003");
        dto.setAssignedUserId(3001L);

        facade.publishEvaluatedEvent(dto);

        Assert.assertNotNull(eventServiceState.createdEvent);
        Assert.assertEquals(Long.valueOf(3001L), eventServiceState.createdEvent.getReceiverId());
        Assert.assertEquals(NotifySceneCode.WORK_ORDER_EVALUATED.getCode(), eventServiceState.createdEvent.getSceneCode());
    }

    /**
     * 创建通知事件服务代理。
     *
     * @param state 测试状态
     * @return 服务代理
     */
    private NotifyEventService createNotifyEventServiceProxy(EventServiceState state) {
        InvocationHandler handler = new InvocationHandler() {
            /**invoke 处理逻辑，服务于当前类的业务编排和数据转换。
@param proxy proxy 字段参数。
@param method method 字段参数。
@param args args 字段参数。
@return 处理后的业务结果。*/
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                String name = method.getName();
                if ("getByEventKey".equals(name)) {
                    return state.existingEvent;
                }
                if ("createEvent".equals(name)) {
                    state.createdEvent = (SysNotifyEvent) args[0];
                    return 1L;
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
     * 创建通知消息服务代理。
     *
     * @return 服务代理
     */
    private NotifyMessageService createNotifyMessageServiceProxy() {
        InvocationHandler handler = new InvocationHandler() {
            /**invoke 处理逻辑，服务于当前类的业务编排和数据转换。
@param proxy proxy 字段参数。
@param method method 字段参数。
@param args args 字段参数。
@return 处理后的业务结果。*/
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                return defaultValue(method.getReturnType());
            }
        };
        return (NotifyMessageService) Proxy.newProxyInstance(
                NotifyMessageService.class.getClassLoader(),
                new Class[]{NotifyMessageService.class},
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
     * 事件服务调用状态。
     */
    private static class EventServiceState {
        /**existingEvent 字段，用于模拟数据库中已存在的通知事件。*/
        private SysNotifyEvent existingEvent;

        /**createdEvent 字段，用于当前类内部业务处理。*/
        private SysNotifyEvent createdEvent;
    }
}
