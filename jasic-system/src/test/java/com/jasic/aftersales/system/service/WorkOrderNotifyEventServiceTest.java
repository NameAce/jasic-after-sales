package com.jasic.aftersales.system.service;

import com.jasic.aftersales.system.domain.entity.SysCompany;
import com.jasic.aftersales.system.domain.entity.WorkOrder;
import com.jasic.aftersales.system.domain.entity.WorkOrderNotifyEvent;
import com.jasic.aftersales.system.domain.model.WorkOrderNotifyReceiverInfo;
import com.jasic.aftersales.system.mapper.SysUserCompanyMapper;
import com.jasic.aftersales.system.mapper.WorkOrderMapper;
import com.jasic.aftersales.system.mapper.WorkOrderNotifyEventMapper;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工单通知事件服务测试
 *
 * @author Codex
 * @date 2026/04/02
 */
public class WorkOrderNotifyEventServiceTest {

    @Test
    public void shouldRecordCompanyFailureWhenNoManagerReceiver() throws Exception {
        WorkOrderNotifyEventService service = new WorkOrderNotifyEventService();
        EventStore store = new EventStore();
        setField(service, "workOrderNotifyEventMapper", createEventMapperProxy(store));
        setField(service, "sysUserCompanyMapper", createUserCompanyMapperProxy(Collections.emptyList()));

        service.recordCustomerEvaluated(buildWorkOrder(), 5, 4, 5, "服务很好");

        Assert.assertEquals(1, store.events.size());
        WorkOrderNotifyEvent event = store.events.values().iterator().next();
        Assert.assertEquals("COMPANY", event.getReceiverType());
        Assert.assertEquals("FAILED", event.getSendStatus());
        Assert.assertEquals("当前公司无可用接收人", event.getFailReason());
        Assert.assertTrue(event.getContentSnapshot().contains("时效5/质量4/满意5"));
    }

    @Test
    public void shouldMarkPendingEventFailedWhenWechatConfigMissing() throws Exception {
        WorkOrderNotifyEventService service = new WorkOrderNotifyEventService();
        EventStore store = new EventStore();
        WorkOrder workOrder = buildWorkOrder();

        setField(service, "workOrderNotifyEventMapper", createEventMapperProxy(store));
        setField(service, "workOrderMapper", createWorkOrderMapperProxy(workOrder));
        setField(service, "sysCompanyMapper", createCompanyMapperProxy(buildCompany()));
        setField(service, "sysConfigService", createConfigServiceProxy(""));
        setField(service, "receiverResolvers", Collections.singletonList(createCustomerResolver()));

        service.recordRepairFinished(workOrder, "更换配件");

        Assert.assertEquals(1, store.events.size());
        WorkOrderNotifyEvent event = store.events.values().iterator().next();
        Assert.assertEquals("CUSTOMER", event.getReceiverType());
        Assert.assertEquals("FAILED", event.getSendStatus());
        Assert.assertEquals("微信配置未完成", event.getFailReason());
    }

    @Test
    public void shouldSkipCustomerNotifyEventWhenCustomerIdMissing() throws Exception {
        WorkOrderNotifyEventService service = new WorkOrderNotifyEventService();
        EventStore store = new EventStore();
        WorkOrder workOrder = buildWorkOrder();
        workOrder.setCustomerId(null);

        setField(service, "workOrderNotifyEventMapper", createEventMapperProxy(store));

        service.recordRepairFinished(workOrder, "更换配件");
        service.recordEvaluationInvite(workOrder);

        Assert.assertTrue(store.events.isEmpty());
    }

    private WorkOrder buildWorkOrder() {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(100L);
        workOrder.setOrderNo("WO-20260402-001");
        workOrder.setCurrentAcceptCompanyId(200L);
        workOrder.setCustomerId(300L);
        return workOrder;
    }

    private SysCompany buildCompany() {
        SysCompany company = new SysCompany();
        company.setId(200L);
        company.setCompanyName("测试网点");
        return company;
    }

    private WorkOrderNotifyReceiverResolver createCustomerResolver() {
        return new WorkOrderNotifyReceiverResolver() {
            @Override
            public boolean supports(String receiverType) {
                return "CUSTOMER".equals(receiverType);
            }

            @Override
            public WorkOrderNotifyReceiverInfo resolve(Long receiverId) {
                WorkOrderNotifyReceiverInfo info = new WorkOrderNotifyReceiverInfo();
                info.setOpenid("customer-openid");
                return info;
            }
        };
    }

    private WorkOrderNotifyEventMapper createEventMapperProxy(EventStore store) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("insert".equals(method.getName())) {
                    WorkOrderNotifyEvent event = (WorkOrderNotifyEvent) args[0];
                    event.setId(store.nextId++);
                    store.events.put(event.getId(), copyEvent(event));
                    return 1;
                }
                if ("selectById".equals(method.getName())) {
                    return store.events.get(args[0]);
                }
                if ("updateById".equals(method.getName())) {
                    WorkOrderNotifyEvent update = (WorkOrderNotifyEvent) args[0];
                    WorkOrderNotifyEvent current = store.events.get(update.getId());
                    if (current != null) {
                        if (update.getSendStatus() != null) {
                            current.setSendStatus(update.getSendStatus());
                        }
                        if (update.getSendTime() != null) {
                            current.setSendTime(update.getSendTime());
                        }
                        if (update.getFailReason() != null || "FAILED".equals(update.getSendStatus())) {
                            current.setFailReason(update.getFailReason());
                        }
                    }
                    return 1;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (WorkOrderNotifyEventMapper) Proxy.newProxyInstance(
                WorkOrderNotifyEventMapper.class.getClassLoader(),
                new Class<?>[]{WorkOrderNotifyEventMapper.class},
                handler
        );
    }

    private SysUserCompanyMapper createUserCompanyMapperProxy(List<Object> relations) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectList".equals(method.getName())) {
                    return relations;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (SysUserCompanyMapper) Proxy.newProxyInstance(
                SysUserCompanyMapper.class.getClassLoader(),
                new Class<?>[]{SysUserCompanyMapper.class},
                handler
        );
    }

    private WorkOrderMapper createWorkOrderMapperProxy(WorkOrder workOrder) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectById".equals(method.getName())) {
                    return workOrder;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (WorkOrderMapper) Proxy.newProxyInstance(
                WorkOrderMapper.class.getClassLoader(),
                new Class<?>[]{WorkOrderMapper.class},
                handler
        );
    }

    private Object createCompanyMapperProxy(SysCompany company) throws Exception {
        Class<?> mapperClass = Class.forName("com.jasic.aftersales.system.mapper.SysCompanyMapper");
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectById".equals(method.getName())) {
                    return company;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return Proxy.newProxyInstance(mapperClass.getClassLoader(), new Class<?>[]{mapperClass}, handler);
    }

    private Object createConfigServiceProxy(String value) throws Exception {
        Class<?> serviceClass = Class.forName("com.jasic.aftersales.system.service.ISysConfigService");
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("getValueByKey".equals(method.getName())) {
                    return value;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return Proxy.newProxyInstance(serviceClass.getClassLoader(), new Class<?>[]{serviceClass}, handler);
    }

    private WorkOrderNotifyEvent copyEvent(WorkOrderNotifyEvent source) {
        WorkOrderNotifyEvent target = new WorkOrderNotifyEvent();
        target.setId(source.getId());
        target.setWorkOrderId(source.getWorkOrderId());
        target.setCompanyId(source.getCompanyId());
        target.setEventType(source.getEventType());
        target.setTriggerNode(source.getTriggerNode());
        target.setReceiverType(source.getReceiverType());
        target.setReceiverId(source.getReceiverId());
        target.setTitleSnapshot(source.getTitleSnapshot());
        target.setContentSnapshot(source.getContentSnapshot());
        target.setSendStatus(source.getSendStatus());
        target.setSendTime(source.getSendTime());
        target.setFailReason(source.getFailReason());
        return target;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = WorkOrderNotifyEventService.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (boolean.class.equals(returnType)) {
            return false;
        }
        if (char.class.equals(returnType)) {
            return '\0';
        }
        if (byte.class.equals(returnType) || short.class.equals(returnType)
                || int.class.equals(returnType) || long.class.equals(returnType)) {
            return 0;
        }
        if (float.class.equals(returnType)) {
            return 0F;
        }
        if (double.class.equals(returnType)) {
            return 0D;
        }
        return null;
    }

    private static class EventStore {

        private long nextId = 1L;
        private final Map<Long, WorkOrderNotifyEvent> events = new LinkedHashMap<>();
    }
}
