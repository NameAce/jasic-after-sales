package com.jasic.aftersales.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.domain.entity.WorkOrder;
import com.jasic.aftersales.system.mapper.WorkOrderMapper;
import com.jasic.aftersales.system.notify.domain.dto.NotifyReadByBizDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyTodoCompleteDTO;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyMessage;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyMessageLog;
import com.jasic.aftersales.system.notify.domain.enums.NotifyActionTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyBizTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyTodoStatusEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyTypeEnum;
import com.jasic.aftersales.system.notify.domain.query.NotifyMessageQuery;
import com.jasic.aftersales.system.notify.domain.vo.NotifyMessagePageVO;
import com.jasic.aftersales.system.notify.mapper.SysNotifyMessageMapper;
import com.jasic.aftersales.system.notify.service.NotifyMessageLogService;
import com.jasic.aftersales.system.notify.service.impl.NotifyMessageServiceImpl;
import com.jasic.aftersales.system.notify.support.NotifyConstants;
import com.jasic.aftersales.system.service.WorkOrderPermissionService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/*** NotifyMessageServiceImpl tests.

@author Zoro*/
public class NotifyMessageServiceImplTest {

    /**验证ResolveTodoStatusesAsPendingAndRead，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldResolveTodoStatusesAsPendingAndRead() throws Exception {
        NotifyMessageServiceImpl service = new NotifyMessageServiceImpl();

        List<String> statuses = invokeResolveBoxStatuses(service, "TODO");

        Assert.assertEquals(Arrays.asList(
                NotifyTodoStatusEnum.PENDING.getCode(),
                NotifyTodoStatusEnum.READ.getCode()
        ), statuses);
    }

    /**验证ResolveHistoryStatusesAsDoneAndInvalid，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldResolveHistoryStatusesAsDoneAndInvalid() throws Exception {
        NotifyMessageServiceImpl service = new NotifyMessageServiceImpl();

        List<String> statuses = invokeResolveBoxStatuses(service, "HISTORY");

        Assert.assertEquals(Arrays.asList(
                NotifyTodoStatusEnum.DONE.getCode(),
                NotifyTodoStatusEnum.INVALID.getCode()
        ), statuses);
    }

    /**验证FilterPageQueryByCurrentReceiverAndMapRows，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldFilterPageQueryByCurrentReceiverAndMapRows() throws Exception {
        NotifyMessageServiceImpl service = new NotifyMessageServiceImpl();
        MessageMapperState mapperState = new MessageMapperState();
        mapperState.pageResult = new Page<>(1, 10);
        mapperState.pageResult.setTotal(1L);
        mapperState.pageResult.setRecords(Collections.singletonList(buildMessage(11L, 200L, NotifyTodoStatusEnum.PENDING.getCode())));
        setField(service, "sysNotifyMessageMapper", createMapperProxy(mapperState));
        setField(service, "notifyMessageLogService", new MemoryNotifyMessageLogService());
        initTableInfo();

        NotifyMessageQuery query = new NotifyMessageQuery();
        query.setReceiverId(200L);
        query.setReceiverCompanyId(300L);
        query.setBox("TODO");
        query.setPageNum(1);
        query.setPageSize(10);

        List<NotifyMessagePageVO> rows = service.listPage(query).getRecords();

        Assert.assertEquals(1, rows.size());
        Assert.assertEquals(Long.valueOf(11L), rows.get(0).getId());
        Assert.assertNotNull(mapperState.pageWrapper);
        Assert.assertTrue(mapperState.pageWrapper.getSqlSegment().contains("ORDER BY create_time DESC,id DESC"));
    }

    /**验证CountActiveTodo，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldCountActiveTodo() throws Exception {
        NotifyMessageServiceImpl service = new NotifyMessageServiceImpl();
        MessageMapperState mapperState = new MessageMapperState();
        mapperState.countResult = 3L;
        setField(service, "sysNotifyMessageMapper", createMapperProxy(mapperState));
        setField(service, "notifyMessageLogService", new MemoryNotifyMessageLogService());
        initTableInfo();

        Long count = service.countTodo(500L, 600L);

        Assert.assertEquals(Long.valueOf(3L), count);
        Assert.assertNotNull(mapperState.countWrapper);
        Assert.assertTrue(mapperState.countWrapper.getSqlSegment().contains("todo_status"));
        Assert.assertTrue(mapperState.countWrapper.getSqlSegment().contains("receiver_company_id"));
    }

    /**验证RejectReadingOtherUsersMessage，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldRejectReadingOtherUsersMessage() throws Exception {
        NotifyMessageServiceImpl service = new NotifyMessageServiceImpl();
        MessageMapperState mapperState = new MessageMapperState();
        mapperState.messageStore.put(1L, buildMessage(1L, 999L, NotifyTodoStatusEnum.PENDING.getCode()));
        setField(service, "sysNotifyMessageMapper", createMapperProxy(mapperState));
        MemoryNotifyMessageLogService logService = new MemoryNotifyMessageLogService();
        setField(service, "notifyMessageLogService", logService);

        try {
            service.markRead(1L, 100L, 300L);
            Assert.fail("expected permission isolation");
        } catch (ServiceException ex) {
            Assert.assertEquals("消息不存在", ex.getMessage());
        }

        Assert.assertEquals(0, mapperState.updateCount);
        Assert.assertEquals(0, logService.logs.size());
    }

    /**验证MarkOwnPendingMessageReadAndWriteLog，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldMarkOwnPendingMessageReadAndWriteLog() throws Exception {
        NotifyMessageServiceImpl service = new NotifyMessageServiceImpl();
        MessageMapperState mapperState = new MessageMapperState();
        mapperState.messageStore.put(2L, buildMessage(2L, 100L, NotifyTodoStatusEnum.PENDING.getCode()));
        setField(service, "sysNotifyMessageMapper", createMapperProxy(mapperState));
        MemoryNotifyMessageLogService logService = new MemoryNotifyMessageLogService();
        setField(service, "notifyMessageLogService", logService);
        setField(service, "workOrderMapper", createWorkOrderMapperProxy(true));
        setField(service, "workOrderPermissionService", new TestWorkOrderPermissionService(true));
        initTableInfo();

        service.markRead(2L, 100L, 300L);

        Assert.assertEquals(1, mapperState.updateCount);
        Assert.assertEquals(NotifyTodoStatusEnum.READ.getCode(), mapperState.messageStore.get(2L).getTodoStatus());
        Assert.assertNotNull(mapperState.messageStore.get(2L).getReadTime());
        Assert.assertEquals(1, logService.logs.size());
        Assert.assertEquals(NotifyActionTypeEnum.READ.getCode(), logService.logs.get(0).getActionType());
    }

    /**验证RejectReadingMessageWhenReceiverCompanyMismatch，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldRejectReadingMessageWhenReceiverCompanyMismatch() throws Exception {
        NotifyMessageServiceImpl service = new NotifyMessageServiceImpl();
        MessageMapperState mapperState = new MessageMapperState();
        mapperState.messageStore.put(3L, buildMessage(3L, 100L, NotifyTodoStatusEnum.PENDING.getCode()));
        setField(service, "sysNotifyMessageMapper", createMapperProxy(mapperState));
        setField(service, "notifyMessageLogService", new MemoryNotifyMessageLogService());

        try {
            service.markRead(3L, 100L, 301L);
            Assert.fail("expected receiver company isolation");
        } catch (ServiceException ex) {
            Assert.assertEquals("消息不存在", ex.getMessage());
        }

        Assert.assertEquals(0, mapperState.updateCount);
    }

    /**验证RejectReadingMessageWhenWorkOrderIsNotViewable，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldRejectReadingMessageWhenWorkOrderIsNotViewable() throws Exception {
        NotifyMessageServiceImpl service = new NotifyMessageServiceImpl();
        MessageMapperState mapperState = new MessageMapperState();
        mapperState.messageStore.put(4L, buildMessage(4L, 100L, NotifyTodoStatusEnum.PENDING.getCode()));
        setField(service, "sysNotifyMessageMapper", createMapperProxy(mapperState));
        setField(service, "notifyMessageLogService", new MemoryNotifyMessageLogService());
        setField(service, "workOrderMapper", createWorkOrderMapperProxy(true));
        setField(service, "workOrderPermissionService", new TestWorkOrderPermissionService(false));

        try {
            service.markRead(4L, 100L, 300L);
            Assert.fail("expected business object permission check");
        } catch (ServiceException ex) {
            Assert.assertEquals("无权查看该工单", ex.getMessage());
        }

        Assert.assertEquals(0, mapperState.updateCount);
    }

    /**验证MarkPendingTodoReadByBusinessWhenOpeningWorkOrderDetail，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldMarkPendingTodoReadByBusinessWhenOpeningWorkOrderDetail() throws Exception {
        NotifyMessageServiceImpl service = new NotifyMessageServiceImpl();
        MessageMapperState mapperState = new MessageMapperState();
        mapperState.messageStore.put(5L, buildMessage(5L, 100L, NotifyTodoStatusEnum.PENDING.getCode()));
        MemoryNotifyMessageLogService logService = new MemoryNotifyMessageLogService();
        setField(service, "sysNotifyMessageMapper", createMapperProxy(mapperState));
        setField(service, "notifyMessageLogService", logService);
        setField(service, "workOrderMapper", createWorkOrderMapperProxy(true));
        setField(service, "workOrderPermissionService", new TestWorkOrderPermissionService(true));
        initTableInfo();

        NotifyReadByBizDTO dto = new NotifyReadByBizDTO();
        dto.setBizType(NotifyBizTypeEnum.WORK_ORDER.getCode());
        dto.setBizId(88L);
        dto.setReceiverId(100L);
        dto.setReceiverCompanyId(300L);
        service.markReadByBiz(dto);

        Assert.assertEquals(1, mapperState.updateCount);
        Assert.assertEquals(NotifyTodoStatusEnum.READ.getCode(), mapperState.messageStore.get(5L).getTodoStatus());
        Assert.assertNotNull(mapperState.messageStore.get(5L).getReadTime());
        Assert.assertEquals(1, logService.logs.size());
        Assert.assertEquals(NotifyActionTypeEnum.READ.getCode(), logService.logs.get(0).getActionType());
    }

    /**验证CompleteActiveTodoByBusinessWhenTechnicianAccepts，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldCompleteActiveTodoByBusinessWhenTechnicianAccepts() throws Exception {
        NotifyMessageServiceImpl service = new NotifyMessageServiceImpl();
        MessageMapperState mapperState = new MessageMapperState();
        mapperState.messageStore.put(6L, buildMessage(6L, 100L, NotifyTodoStatusEnum.READ.getCode()));
        MemoryNotifyMessageLogService logService = new MemoryNotifyMessageLogService();
        setField(service, "sysNotifyMessageMapper", createMapperProxy(mapperState));
        setField(service, "notifyMessageLogService", logService);
        setField(service, "workOrderMapper", createWorkOrderMapperProxy(true));
        setField(service, "workOrderPermissionService", new TestWorkOrderPermissionService(true));
        initTableInfo();

        NotifyTodoCompleteDTO dto = new NotifyTodoCompleteDTO();
        dto.setBizType(NotifyBizTypeEnum.WORK_ORDER.getCode());
        dto.setBizId(88L);
        dto.setReceiverId(100L);
        dto.setReceiverCompanyId(300L);
        dto.setActionCode(NotifyConstants.ACTION_TECH_ACCEPT);
        service.completeTodoByBizAndReceiver(dto);

        Assert.assertEquals(1, mapperState.updateCount);
        Assert.assertEquals(NotifyTodoStatusEnum.DONE.getCode(), mapperState.messageStore.get(6L).getTodoStatus());
        Assert.assertNotNull(mapperState.messageStore.get(6L).getDoneTime());
        Assert.assertEquals(1, logService.logs.size());
        Assert.assertEquals(NotifyActionTypeEnum.DONE.getCode(), logService.logs.get(0).getActionType());
    }

    /**initTableInfo 处理逻辑，服务于当前类的业务编排和数据转换。*/
    private void initTableInfo() {
        if (TableInfoHelper.getTableInfo(SysNotifyMessage.class) != null) {
            return;
        }
        Configuration configuration = new Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "test");
        assistant.setCurrentNamespace(SysNotifyMessageMapper.class.getName());
        TableInfoHelper.initTableInfo(assistant, SysNotifyMessage.class);
    }

    /**invokeResolveBoxStatuses 处理逻辑，服务于当前类的业务编排和数据转换。
@param service service 字段参数。
@param box box 字段参数。
@return 查询或组装后的业务数据集合。*/
    @SuppressWarnings("unchecked")
    private List<String> invokeResolveBoxStatuses(NotifyMessageServiceImpl service, String box) throws Exception {
        Method method = NotifyMessageServiceImpl.class.getDeclaredMethod("resolveBoxStatuses", String.class);
        method.setAccessible(true);
        return (List<String>) method.invoke(service, box);
    }

    /**createMapperProxy 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@param state state 字段参数。
@return 新增或保存后的业务标识或处理结果。*/
    private SysNotifyMessageMapper createMapperProxy(MessageMapperState state) {
        InvocationHandler handler = new InvocationHandler() {
            /**invoke 处理逻辑，服务于当前类的业务编排和数据转换。
@param proxy proxy 字段参数。
@param method method 字段参数。
@param args args 字段参数。
@return 处理后的业务结果。*/
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                String name = method.getName();
                if ("selectPage".equals(name)) {
                    state.pageWrapper = (LambdaQueryWrapper<SysNotifyMessage>) args[1];
                    return state.pageResult;
                }
                if ("selectCount".equals(name)) {
                    state.countWrapper = (LambdaQueryWrapper<SysNotifyMessage>) args[0];
                    return state.countResult;
                }
                if ("selectById".equals(name)) {
                    state.lastMessageId = (Long) args[0];
                    return state.messageStore.get(args[0]);
                }
                if ("update".equals(name)) {
                    state.updateCount++;
                    applyMessageUpdate(state, (LambdaUpdateWrapper<SysNotifyMessage>) args[1]);
                    return 1;
                }
                if ("selectList".equals(name)) {
                    return new ArrayList<>(state.messageStore.values());
                }
                if ("insert".equals(name)) {
                    return 1;
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

    /**applyMessageUpdate 处理逻辑，服务于当前类的业务编排和数据转换。
@param state state 字段参数。
@param wrapper wrapper 字段参数。*/
    private void applyMessageUpdate(MessageMapperState state, LambdaUpdateWrapper<SysNotifyMessage> wrapper) {
        Long messageId = resolveMessageId(state, wrapper);
        SysNotifyMessage message = state.messageStore.get(messageId);
        if (message == null) {
            return;
        }
        Map<String, Object> params = wrapper.getParamNameValuePairs();
        if (params.containsValue(NotifyTodoStatusEnum.READ.getCode())) {
            message.setTodoStatus(NotifyTodoStatusEnum.READ.getCode());
            message.setReadTime(LocalDateTime.now());
        } else if (params.containsValue(NotifyTodoStatusEnum.DONE.getCode())) {
            message.setTodoStatus(NotifyTodoStatusEnum.DONE.getCode());
            message.setDoneTime(LocalDateTime.now());
        } else if (params.containsValue(NotifyTodoStatusEnum.INVALID.getCode())) {
            message.setTodoStatus(NotifyTodoStatusEnum.INVALID.getCode());
            message.setInvalidTime(LocalDateTime.now());
        }
    }

    /**resolveMessageId 业务数据，统一收口字段清洗、默认值处理和返回对象组装规则。
@param state state 字段参数。
@param wrapper wrapper 字段参数。
@return 查询或解析得到的业务对象。*/
    private Long resolveMessageId(MessageMapperState state, LambdaUpdateWrapper<SysNotifyMessage> wrapper) {
        if (state.lastMessageId != null) {
            return state.lastMessageId;
        }
        for (Object value : wrapper.getParamNameValuePairs().values()) {
            if (value instanceof Long && state.messageStore.containsKey(value)) {
                return (Long) value;
            }
        }
        return null;
    }

    /**createWorkOrderMapperProxy 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@param found found 字段参数。
@return 新增或保存后的业务标识或处理结果。*/
    private WorkOrderMapper createWorkOrderMapperProxy(boolean found) {
        InvocationHandler handler = new InvocationHandler() {
            /**invoke 处理逻辑，服务于当前类的业务编排和数据转换。
@param proxy proxy 字段参数。
@param method method 字段参数。
@param args args 字段参数。
@return 处理后的业务结果。*/
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectById".equals(method.getName())) {
                    if (!found) {
                        return null;
                    }
                    WorkOrder workOrder = new WorkOrder();
                    workOrder.setId((Long) args[0]);
                    return workOrder;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (WorkOrderMapper) Proxy.newProxyInstance(
                WorkOrderMapper.class.getClassLoader(),
                new Class[]{WorkOrderMapper.class},
                handler
        );
    }

    /**defaultValue 处理逻辑，服务于当前类的业务编排和数据转换。
@param type type 字段参数。
@return 处理后的业务结果。*/
    private Object defaultValue(Class<?> type) {
        if (type == null || !type.isPrimitive()) {
            return null;
        }
        if (boolean.class.equals(type)) {
            return false;
        }
        if (long.class.equals(type)) {
            return 0L;
        }
        if (int.class.equals(type)) {
            return 0;
        }
        if (short.class.equals(type)) {
            return (short) 0;
        }
        if (byte.class.equals(type)) {
            return (byte) 0;
        }
        if (float.class.equals(type)) {
            return 0F;
        }
        if (double.class.equals(type)) {
            return 0D;
        }
        if (char.class.equals(type)) {
            return '\0';
        }
        return null;
    }

    /**buildMessage 业务数据，统一收口字段清洗、默认值处理和返回对象组装规则。
@param id 主键ID。
@param receiverId receiverId 字段。
@param todoStatus 业务状态编码，用于判断或更新当前流程节点。
@return 处理后的业务结果。*/
    private SysNotifyMessage buildMessage(Long id, Long receiverId, String todoStatus) {
        SysNotifyMessage message = new SysNotifyMessage();
        message.setId(id);
        message.setReceiverId(receiverId);
        message.setReceiverCompanyId(300L);
        message.setTargetType(NotifyTypeEnum.IN_APP_TODO.getCode());
        message.setMessageType(NotifyTypeEnum.IN_APP_TODO.getCode());
        message.setTodoStatus(todoStatus);
        message.setTitle("message-" + id);
        message.setSummary("summary-" + id);
        message.setBizType("WORK_ORDER");
        message.setBizId(88L);
        message.setBizNo("WO202604180001");
        message.setRouteType("WORK_ORDER_DETAIL");
        message.setRouteValue("88");
        message.setCreateTime(LocalDateTime.of(2026, 4, 18, 10, 20, 30));
        return message;
    }

    /**TestWorkOrderPermissionService 测试类，用于验证对应业务规则、边界条件和回归场景。

@author Zoro*/
    private static class TestWorkOrderPermissionService extends WorkOrderPermissionService {
        /**allowed 字段，用于当前类内部业务处理。*/
        private final boolean allowed;

        /**构造 TestWorkOrderPermissionService 实例，初始化当前对象在业务流程中需要持有的基础数据。
@param allowed allowed 字段参数。*/
        private TestWorkOrderPermissionService(boolean allowed) {
            this.allowed = allowed;
        }

        /**canView 业务条件，用于决定后续流程是否允许继续执行。
@param workOrder workOrder 字段参数。
@return true 表示满足业务条件，false 表示不满足。*/
        @Override
        public boolean canView(WorkOrder workOrder) {
            return allowed;
        }
    }

    /**setField 处理逻辑，服务于当前类的业务编排和数据转换。
@param target target 字段参数。
@param fieldName 名称文本，用于展示、匹配或保存业务对象名称。
@param value value 字段参数。*/
    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /**MessageMapperState 测试类，用于验证对应业务规则、边界条件和回归场景。

@author Zoro*/
    private static class MessageMapperState {
        /**messageStore 字段，用于当前类内部业务处理。*/
        private final Map<Long, SysNotifyMessage> messageStore = new LinkedHashMap<>();
        /**pageResult 字段，用于当前类内部业务处理。*/
        private Page<SysNotifyMessage> pageResult;
        /**countResult 字段，用于当前类内部业务处理。*/
        private Long countResult;
        /**pageWrapper 字段，用于当前类内部业务处理。*/
        private LambdaQueryWrapper<SysNotifyMessage> pageWrapper;
        /**countWrapper 字段，用于当前类内部业务处理。*/
        private LambdaQueryWrapper<SysNotifyMessage> countWrapper;
        /**updateCount 字段，用于当前类内部业务处理。*/
        private int updateCount;
        /**lastMessageId 字段，用于当前类内部业务处理。*/
        private Long lastMessageId;
    }

    /**MemoryNotifyMessageLogService 测试类，用于验证对应业务规则、边界条件和回归场景。

@author Zoro*/
    private static class MemoryNotifyMessageLogService implements NotifyMessageLogService {
        /**logs 字段，用于当前类内部业务处理。*/
        private final List<SysNotifyMessageLog> logs = new ArrayList<>();

        /**createLog 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@param notifyMessageLog notifyMessageLog 字段参数。
@return 新增或保存后的业务标识或处理结果。*/
        @Override
        public Long createLog(SysNotifyMessageLog notifyMessageLog) {
            logs.add(notifyMessageLog);
            return (long) logs.size();
        }

        /**listByQuery 业务数据，按查询条件和数据权限返回可见范围内的结果。
@param query 查询条件，包含分页、筛选和权限收口所需字段。
@return 查询或组装后的业务数据集合。*/
        @Override
        public List<SysNotifyMessageLog> listByQuery(com.jasic.aftersales.system.notify.domain.query.NotifyMessageLogQuery query) {
            return Collections.emptyList();
        }
    }
}


