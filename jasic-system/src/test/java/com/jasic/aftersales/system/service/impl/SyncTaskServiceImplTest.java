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

public class SyncTaskServiceImplTest {

    private Set<String> permissionCodes;
    private StpInterface previousStpInterface;

    @Before
    public void setUp() {
        SaManager.setSaTokenContext(new SaTokenContextForThreadLocal());
        SaTokenContextForThreadLocalStorage.setBox(new MockSaRequest(), new MockSaResponse(), new MockSaStorage());
        StpUtil.login(101L);
        permissionCodes = new LinkedHashSet<>();
        previousStpInterface = SaManager.getStpInterface();
        SaManager.setStpInterface(new StpInterface() {
            @Override
            public List<String> getPermissionList(Object loginId, String loginType) {
                return new ArrayList<>(permissionCodes);
            }

            @Override
            public List<String> getRoleList(Object loginId, String loginType) {
                return Collections.emptyList();
            }
        });
    }

    @After
    public void tearDown() {
        try {
            StpUtil.logout();
        } finally {
            SaManager.setStpInterface(previousStpInterface);
            SaTokenContextForThreadLocalStorage.clearBox();
        }
    }

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

    private SyncTaskServiceImpl buildService(SyncTask task) throws Exception {
        SyncTaskServiceImpl service = new SyncTaskServiceImpl();
        setField(service, "syncTaskMapper", createSyncTaskMapper(task));
        setField(service, "syncTaskExecutionService", new StubExecutionService());
        return service;
    }

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

    private void switchContext(Long companyId, String subjectType, String typeCode) {
        SecurityContext.setCurrentCompanyId(companyId);
        SecurityContext.setCurrentSubjectType(subjectType);
        SecurityContext.setCurrentTypeCode(typeCode);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Object getField(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private static class StubExecutionService implements ISyncTaskExecutionService {
        private Long submittedTaskId;

        @Override
        public Long submitManualExecution(Long taskId) {
            submittedTaskId = taskId;
            return 9001L;
        }

        @Override
        public void executeScheduled(Long taskId) {
        }
    }

    private static class MockSaRequest implements SaRequest {
        @Override
        public Object getSource() {
            return this;
        }

        @Override
        public String getParam(String name) {
            return null;
        }

        @Override
        public List<String> getParamNames() {
            return Collections.emptyList();
        }

        @Override
        public Map<String, String> getParamMap() {
            return Collections.emptyMap();
        }

        @Override
        public String getHeader(String name) {
            return null;
        }

        @Override
        public String getCookieValue(String name) {
            return null;
        }

        @Override
        public String getRequestPath() {
            return "/";
        }

        @Override
        public String getUrl() {
            return "http://localhost/test";
        }

        @Override
        public String getMethod() {
            return "GET";
        }

        @Override
        public Object forward(String path) {
            return null;
        }
    }

    private static class MockSaResponse implements SaResponse {
        @Override
        public Object getSource() {
            return this;
        }

        @Override
        public SaResponse setStatus(int sc) {
            return this;
        }

        @Override
        public SaResponse setHeader(String name, String value) {
            return this;
        }

        @Override
        public SaResponse addHeader(String name, String value) {
            return this;
        }

        @Override
        public Object redirect(String url) {
            return null;
        }
    }

    private static class MockSaStorage implements SaStorage {
        private final Map<String, Object> storage = new LinkedHashMap<>();

        @Override
        public Object getSource() {
            return this;
        }

        @Override
        public Object get(String key) {
            return storage.get(key);
        }

        @Override
        public SaStorage set(String key, Object value) {
            storage.put(key, value);
            return this;
        }

        @Override
        public SaStorage delete(String key) {
            storage.remove(key);
            return this;
        }
    }
}
