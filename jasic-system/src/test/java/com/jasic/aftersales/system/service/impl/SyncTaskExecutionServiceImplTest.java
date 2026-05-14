package com.jasic.aftersales.system.service.impl;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaTokenContextForThreadLocal;
import cn.dev33.satoken.context.SaTokenContextForThreadLocalStorage;
import cn.dev33.satoken.context.model.SaRequest;
import cn.dev33.satoken.context.model.SaResponse;
import cn.dev33.satoken.context.model.SaStorage;
import cn.dev33.satoken.stp.StpUtil;
import com.jasic.aftersales.system.domain.entity.SyncTask;
import com.jasic.aftersales.system.domain.entity.SyncTaskLog;
import com.jasic.aftersales.system.mapper.SyncTaskLogMapper;
import com.jasic.aftersales.system.mapper.SyncTaskMapper;
import com.jasic.aftersales.system.service.support.SyncTaskAsyncExecutor;
import com.jasic.aftersales.system.service.support.SyncTaskExecutionContext;
import com.jasic.aftersales.system.service.support.SyncTaskExecutionResult;
import com.jasic.aftersales.system.service.support.SyncTaskExecutionRunner;
import com.jasic.aftersales.system.service.support.SyncTaskHandler;
import com.jasic.aftersales.system.service.support.SyncTaskRunningRegistry;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SyncTaskExecutionServiceImplTest {

    @Before
    public void setUp() {
        SaManager.setSaTokenContext(new SaTokenContextForThreadLocal());
        SaTokenContextForThreadLocalStorage.setBox(new MockSaRequest(), new MockSaResponse(), new MockSaStorage());
        StpUtil.login(101L);
    }

    @After
    public void tearDown() {
        try {
            StpUtil.logout();
        } finally {
            SaTokenContextForThreadLocalStorage.clearBox();
        }
    }

    @Test
    public void manualExecutionShouldRecordCurrentTriggerUser() throws Exception {
        CapturingLogMapper logMapper = new CapturingLogMapper();
        CapturingAsyncExecutor asyncExecutor = new CapturingAsyncExecutor();
        SyncTaskExecutionServiceImpl service = buildService(logMapper, asyncExecutor, new CapturingRunner());

        Long logId = service.submitManualExecution(5L);

        Assert.assertEquals(Long.valueOf(501L), logId);
        Assert.assertEquals("MANUAL", logMapper.insertedLog.getTriggerType());
        Assert.assertEquals(Long.valueOf(101L), logMapper.insertedLog.getTriggerUserId());
        Assert.assertEquals(Long.valueOf(5L), asyncExecutor.taskId);
        Assert.assertEquals(Long.valueOf(501L), asyncExecutor.logId);
    }

    @Test
    public void scheduledExecutionShouldRecordSystemTaskIdentity() throws Exception {
        CapturingLogMapper logMapper = new CapturingLogMapper();
        CapturingRunner runner = new CapturingRunner();
        SyncTaskExecutionServiceImpl service = buildService(logMapper, new CapturingAsyncExecutor(), runner);

        service.executeScheduled(5L);

        Assert.assertEquals("SCHEDULED", logMapper.insertedLog.getTriggerType());
        Assert.assertEquals(Long.valueOf(0L), logMapper.insertedLog.getTriggerUserId());
        Assert.assertEquals(Long.valueOf(5L), runner.taskId);
        Assert.assertEquals(Long.valueOf(501L), runner.logId);
    }

    private SyncTaskExecutionServiceImpl buildService(CapturingLogMapper logMapper,
                                                      CapturingAsyncExecutor asyncExecutor,
                                                      CapturingRunner runner) throws Exception {
        SyncTaskExecutionServiceImpl service = new SyncTaskExecutionServiceImpl();
        setField(service, "syncTaskMapper", createSyncTaskMapper(buildTask(5L)));
        setField(service, "syncTaskLogMapper", logMapper.proxy());
        setField(service, "syncTaskHandlers", Collections.singletonList(buildHandler()));
        setField(service, "syncTaskAsyncExecutor", asyncExecutor);
        setField(service, "syncTaskExecutionRunner", runner);
        setField(service, "syncTaskRunningRegistry", new SyncTaskRunningRegistry());
        return service;
    }

    private SyncTask buildTask(Long id) {
        SyncTask task = new SyncTask();
        task.setId(id);
        task.setTaskCode("MACHINE_BARCODE_SYNC");
        task.setTaskName("条码档案同步");
        task.setHandlerCode("machineBarcodeSync");
        return task;
    }

    private SyncTaskHandler buildHandler() {
        return new SyncTaskHandler() {
            @Override
            public String getCode() {
                return "machineBarcodeSync";
            }

            @Override
            public String getName() {
                return "条码档案同步";
            }

            @Override
            public SyncTaskExecutionResult execute(SyncTask task, SyncTaskExecutionContext context) {
                return SyncTaskExecutionResult.builder().message("OK").build();
            }
        };
    }

    private SyncTaskMapper createSyncTaskMapper(SyncTask task) {
        InvocationHandler handler = (proxy, method, args) -> {
            if ("selectById".equals(method.getName())) {
                return task;
            }
            return null;
        };
        return (SyncTaskMapper) Proxy.newProxyInstance(
                SyncTaskMapper.class.getClassLoader(),
                new Class[]{SyncTaskMapper.class},
                handler
        );
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static class CapturingAsyncExecutor extends SyncTaskAsyncExecutor {
        private Long taskId;
        private Long logId;

        @Override
        public void executeAsync(Long taskId, Long logId) {
            this.taskId = taskId;
            this.logId = logId;
        }
    }

    private static class CapturingRunner extends SyncTaskExecutionRunner {
        private Long taskId;
        private Long logId;

        @Override
        public void executeWithLog(Long taskId, Long logId) {
            this.taskId = taskId;
            this.logId = logId;
        }
    }

    private static class CapturingLogMapper {
        private SyncTaskLog insertedLog;

        private SyncTaskLogMapper proxy() {
            InvocationHandler handler = (proxy, method, args) -> {
                if ("insert".equals(method.getName())) {
                    insertedLog = (SyncTaskLog) args[0];
                    insertedLog.setId(501L);
                    return 1;
                }
                if ("selectById".equals(method.getName())) {
                    return insertedLog;
                }
                if ("updateById".equals(method.getName())) {
                    return 1;
                }
                return null;
            };
            return (SyncTaskLogMapper) Proxy.newProxyInstance(
                    SyncTaskLogMapper.class.getClassLoader(),
                    new Class[]{SyncTaskLogMapper.class},
                    handler
            );
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
