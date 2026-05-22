package com.jasic.aftersales.system.service.impl;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaTokenContextForThreadLocal;
import cn.dev33.satoken.context.SaTokenContextForThreadLocalStorage;
import cn.dev33.satoken.context.model.SaRequest;
import cn.dev33.satoken.context.model.SaResponse;
import cn.dev33.satoken.context.model.SaStorage;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import com.jasic.aftersales.common.enums.SubjectTypeEnum;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.framework.security.SecurityContext;
import com.jasic.aftersales.system.domain.dto.SyncTaskDTO;
import com.jasic.aftersales.system.domain.entity.SyncTask;
import com.jasic.aftersales.system.domain.query.SyncTaskLogQuery;
import com.jasic.aftersales.system.domain.query.SyncTaskQuery;
import com.jasic.aftersales.system.domain.vo.SyncTaskLogVO;
import com.jasic.aftersales.system.service.ISyncTaskExecutionService;
import com.jasic.aftersales.system.mapper.SyncTaskMapper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**SyncTaskServiceImplTest 测试类，用于验证对应业务规则、边界条件和回归场景。

@author Zoro*/
public class SyncTaskServiceImplTest {

    /**permissionCodes 字段，用于当前类内部业务处理。*/
    private Set<String> permissionCodes;
    /**previousStpInterface 字段，用于当前类内部业务处理。*/
    private StpInterface previousStpInterface;

    /**setUp 处理逻辑，服务于当前类的业务编排和数据转换。*/
    @Before
    public void setUp() {
        SaManager.setSaTokenContext(new SaTokenContextForThreadLocal());
        SaTokenContextForThreadLocalStorage.setBox(new MockSaRequest(), new MockSaResponse(), new MockSaStorage());
        StpUtil.login(101L);
        permissionCodes = new LinkedHashSet<>();
        previousStpInterface = SaManager.getStpInterface();
        SaManager.setStpInterface(new StpInterface() {
            /**getPermissionList 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@param loginId loginId 字段。
@param loginType loginType 字段参数。
@return 查询或组装后的业务数据集合。*/
            @Override
            public List<String> getPermissionList(Object loginId, String loginType) {
                return new ArrayList<>(permissionCodes);
            }

            /**getRoleList 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@param loginId loginId 字段。
@param loginType loginType 字段参数。
@return 查询或组装后的业务数据集合。*/
            @Override
            public List<String> getRoleList(Object loginId, String loginType) {
                return Collections.emptyList();
            }
        });
    }

    /**tearDown 处理逻辑，服务于当前类的业务编排和数据转换。*/
    @After
    public void tearDown() {
        try {
            StpUtil.logout();
        } finally {
            SaManager.setStpInterface(previousStpInterface);
            SaTokenContextForThreadLocalStorage.clearBox();
        }
    }

    /**验证nonPlatformUserShouldNotTriggerSyncTaskEvenWithPermission，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void nonPlatformUserShouldNotTriggerSyncTaskEvenWithPermission() throws Exception {
        switchContext(11L, SubjectTypeEnum.SERVICE.getCode(), "SITE_FIRST");
        permissionCodes.add("system:syncTask:execute");
        SyncTaskServiceImpl service = buildService(buildTask(7L));

        try {
            service.execute(7L);
            Assert.fail("非平台用户不应允许手动触发同步任务");
        } catch (ServiceException ex) {
            Assert.assertEquals("仅平台用户可以手动触发同步任务", ex.getMessage());
        }
    }

    /**验证platformUserShouldFailClosedWhenExecutePermissionMissing，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void platformUserShouldFailClosedWhenExecutePermissionMissing() throws Exception {
        switchContext(1L, SubjectTypeEnum.PLATFORM.getCode(), "PLATFORM");
        SyncTaskServiceImpl service = buildService(buildTask(7L));

        try {
            service.execute(7L);
            Assert.fail("缺少执行权限点应拒绝");
        } catch (ServiceException ex) {
            Assert.assertEquals("无权手动触发同步任务", ex.getMessage());
        }
    }

    /**验证platformUserShouldSubmitSyncTaskWithExecutePermission，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void platformUserShouldSubmitSyncTaskWithExecutePermission() throws Exception {
        switchContext(1L, SubjectTypeEnum.PLATFORM.getCode(), "PLATFORM");
        permissionCodes.add("system:syncTask:execute");
        SyncTaskServiceImpl service = buildService(buildTask(42L));
        StubExecutionService executionService = (StubExecutionService) getField(service, "syncTaskExecutionService");

        Long logId = service.execute(42L);

        Assert.assertEquals(Long.valueOf(9001L), logId);
        Assert.assertEquals(Long.valueOf(42L), executionService.submittedTaskId);
    }

    /**验证machineBarcodeQuickTriggerShouldUseMachineBarcodePermission，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void machineBarcodeQuickTriggerShouldUseMachineBarcodePermission() throws Exception {
        switchContext(1L, SubjectTypeEnum.PLATFORM.getCode(), "PLATFORM");
        permissionCodes.add("system:machineBarcode:sync");
        SyncTaskServiceImpl service = buildService(buildTask(88L));
        StubExecutionService executionService = (StubExecutionService) getField(service, "syncTaskExecutionService");

        Long logId = service.executeDefaultMachineBarcodeTask();

        Assert.assertEquals(Long.valueOf(9001L), logId);
        Assert.assertEquals(Long.valueOf(88L), executionService.submittedTaskId);
    }

    /**验证machineBarcodeQuickTriggerShouldRejectSyncTaskPermissionOnly，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void machineBarcodeQuickTriggerShouldRejectSyncTaskPermissionOnly() throws Exception {
        switchContext(1L, SubjectTypeEnum.PLATFORM.getCode(), "PLATFORM");
        permissionCodes.add("system:syncTask:execute");
        SyncTaskServiceImpl service = buildService(buildTask(88L));

        try {
            service.executeDefaultMachineBarcodeTask();
            Assert.fail("条码快捷同步不应复用同步任务页执行权限");
        } catch (ServiceException ex) {
            Assert.assertEquals("无权手动触发同步任务", ex.getMessage());
        }
    }

    /**buildService 业务数据，统一收口字段清洗、默认值处理和返回对象组装规则。
@param task task 字段参数。
@return 处理后的业务结果。*/
    private SyncTaskServiceImpl buildService(SyncTask task) throws Exception {
        SyncTaskServiceImpl service = new SyncTaskServiceImpl();
        setField(service, "syncTaskMapper", createSyncTaskMapper(task));
        setField(service, "syncTaskExecutionService", new StubExecutionService());
        return service;
    }

    /**buildTask 业务数据，统一收口字段清洗、默认值处理和返回对象组装规则。
@param id 主键ID。
@return 处理后的业务结果。*/
    private SyncTask buildTask(Long id) {
        SyncTask task = new SyncTask();
        task.setId(id);
        task.setTaskCode("MACHINE_BARCODE_SYNC");
        task.setTaskName("条码档案同步");
        task.setHandlerCode("machineBarcodeSync");
        task.setCronExpression("0 0 2 * * ?");
        task.setStatus(1);
        return task;
    }

    /**createSyncTaskMapper 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@param task task 字段参数。
@return 新增或保存后的业务标识或处理结果。*/
    private SyncTaskMapper createSyncTaskMapper(SyncTask task) {
        InvocationHandler handler = (proxy, method, args) -> {
            if ("selectOne".equals(method.getName()) || "selectById".equals(method.getName())) {
                return task;
            }
            return defaultValue(method.getReturnType());
        };
        return (SyncTaskMapper) Proxy.newProxyInstance(
                SyncTaskMapper.class.getClassLoader(),
                new Class[]{SyncTaskMapper.class},
                handler
        );
    }

    /**defaultValue 处理逻辑，服务于当前类的业务编排和数据转换。
@param returnType returnType 字段参数。
@return 处理后的业务结果。*/
    private Object defaultValue(Class<?> returnType) {
        if (returnType == Void.TYPE) {
            return null;
        }
        if (returnType == Boolean.TYPE) {
            return false;
        }
        if (returnType == Integer.TYPE || returnType == Long.TYPE || returnType == Short.TYPE || returnType == Byte.TYPE) {
            return 0;
        }
        return null;
    }

    /**switchContext 处理逻辑，服务于当前类的业务编排和数据转换。
@param companyId 公司ID。
@param subjectType subjectType 字段参数。
@param typeCode 业务编码，用于匹配枚举、配置或外部系统数据。*/
    private void switchContext(Long companyId, String subjectType, String typeCode) {
        SecurityContext.setCurrentCompanyId(companyId);
        SecurityContext.setCurrentSubjectType(subjectType);
        SecurityContext.setCurrentTypeCode(typeCode);
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

    /**getField 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@param target target 字段参数。
@param fieldName 名称文本，用于展示、匹配或保存业务对象名称。
@return 查询或解析得到的业务对象。*/
    private Object getField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    /**StubExecutionService 测试类，用于验证对应业务规则、边界条件和回归场景。

@author Zoro*/
    private static class StubExecutionService implements ISyncTaskExecutionService {
        /**submittedTaskId 字段，用于当前类内部业务处理。*/
        private Long submittedTaskId;

        /**submitManualExecution 处理逻辑，服务于当前类的业务编排和数据转换。
@param taskId taskId 字段。
@return 处理后的业务结果。*/
        @Override
        public Long submitManualExecution(Long taskId) {
            submittedTaskId = taskId;
            return 9001L;
        }

        /**executeScheduled 处理逻辑，服务于当前类的业务编排和数据转换。
@param taskId taskId 字段。*/
        @Override
        public void executeScheduled(Long taskId) {
        }
    }

    /**MockSaRequest 测试类，用于验证对应业务规则、边界条件和回归场景。

@author Zoro*/
    private static class MockSaRequest implements SaRequest {
        /**getSource 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@return 查询或解析得到的业务对象。*/
        @Override
        public Object getSource() {
            return this;
        }

        /**getParam 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@param name 名称文本，用于展示、匹配或保存业务对象名称。
@return 查询或解析得到的业务对象。*/
        @Override
        public String getParam(String name) {
            return null;
        }

        /**getParamNames 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@return 查询或组装后的业务数据集合。*/
        @Override
        public List<String> getParamNames() {
            return Collections.emptyList();
        }

        /**getParamMap 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@return 查询或组装后的业务数据集合。*/
        @Override
        public Map<String, String> getParamMap() {
            return Collections.emptyMap();
        }

        /**getHeader 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@param name 名称文本，用于展示、匹配或保存业务对象名称。
@return 查询或解析得到的业务对象。*/
        @Override
        public String getHeader(String name) {
            return null;
        }

        /**getCookieValue 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@param name 名称文本，用于展示、匹配或保存业务对象名称。
@return 查询或解析得到的业务对象。*/
        @Override
        public String getCookieValue(String name) {
            return null;
        }

        /**getRequestPath 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@return 查询或解析得到的业务对象。*/
        @Override
        public String getRequestPath() {
            return "/";
        }

        /**getUrl 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@return 查询或解析得到的业务对象。*/
        @Override
        public String getUrl() {
            return "http://localhost/test";
        }

        /**getMethod 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@return 查询或解析得到的业务对象。*/
        @Override
        public String getMethod() {
            return "GET";
        }

        /**forward 处理逻辑，服务于当前类的业务编排和数据转换。
@param path path 字段参数。
@return 处理后的业务结果。*/
        @Override
        public Object forward(String path) {
            return null;
        }
    }

    /**MockSaResponse 测试类，用于验证对应业务规则、边界条件和回归场景。

@author Zoro*/
    private static class MockSaResponse implements SaResponse {
        /**getSource 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@return 查询或解析得到的业务对象。*/
        @Override
        public Object getSource() {
            return this;
        }

        /**setStatus 处理逻辑，服务于当前类的业务编排和数据转换。
@param sc sc 字段参数。
@return 处理后的业务结果。*/
        @Override
        public SaResponse setStatus(int sc) {
            return this;
        }

        /**setHeader 处理逻辑，服务于当前类的业务编排和数据转换。
@param name 名称文本，用于展示、匹配或保存业务对象名称。
@param value value 字段参数。
@return 处理后的业务结果。*/
        @Override
        public SaResponse setHeader(String name, String value) {
            return this;
        }

        /**addHeader 处理逻辑，服务于当前类的业务编排和数据转换。
@param name 名称文本，用于展示、匹配或保存业务对象名称。
@param value value 字段参数。
@return 处理后的业务结果。*/
        @Override
        public SaResponse addHeader(String name, String value) {
            return this;
        }

        /**redirect 处理逻辑，服务于当前类的业务编排和数据转换。
@param url url 字段参数。
@return 处理后的业务结果。*/
        @Override
        public Object redirect(String url) {
            return null;
        }
    }

    /**MockSaStorage 测试类，用于验证对应业务规则、边界条件和回归场景。

@author Zoro*/
    private static class MockSaStorage implements SaStorage {
        /**storage 字段，用于当前类内部业务处理。*/
        private final Map<String, Object> storage = new LinkedHashMap<>();

        /**getSource 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@return 查询或解析得到的业务对象。*/
        @Override
        public Object getSource() {
            return this;
        }

        /**get 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@param key key 字段参数。
@return 查询或解析得到的业务对象。*/
        @Override
        public Object get(String key) {
            return storage.get(key);
        }

        /**set 处理逻辑，服务于当前类的业务编排和数据转换。
@param key key 字段参数。
@param value value 字段参数。
@return 处理后的业务结果。*/
        @Override
        public SaStorage set(String key, Object value) {
            storage.put(key, value);
            return this;
        }

        /**delete 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@param key key 字段参数。
@return 处理后的业务结果。*/
        @Override
        public SaStorage delete(String key) {
            storage.remove(key);
            return this;
        }
    }
}
