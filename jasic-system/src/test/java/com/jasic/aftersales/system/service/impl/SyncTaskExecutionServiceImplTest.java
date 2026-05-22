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

/**SyncTaskExecutionServiceImplTest 测试类，用于验证对应业务规则、边界条件和回归场景。

@author Zoro*/
public class SyncTaskExecutionServiceImplTest {

    /**setUp 处理逻辑，服务于当前类的业务编排和数据转换。*/
    @Before
    public void setUp() {
        SaManager.setSaTokenContext(new SaTokenContextForThreadLocal());
        SaTokenContextForThreadLocalStorage.setBox(new MockSaRequest(), new MockSaResponse(), new MockSaStorage());
        StpUtil.login(101L);
    }

    /**tearDown 处理逻辑，服务于当前类的业务编排和数据转换。*/
    @After
    public void tearDown() {
        try {
            StpUtil.logout();
        } finally {
            SaTokenContextForThreadLocalStorage.clearBox();
        }
    }

    /**验证manualExecutionShouldRecordCurrentTriggerUser，保证相关业务规则在回归场景下保持稳定。*/
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

    /**验证scheduledExecutionShouldRecordSystemTaskIdentity，保证相关业务规则在回归场景下保持稳定。*/
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

    /**buildService 业务数据，统一收口字段清洗、默认值处理和返回对象组装规则。
@param logMapper 业务映射数据，用于提升后续组装或匹配效率。
@param asyncExecutor asyncExecutor 字段参数。
@param runner runner 字段参数。
@return 处理后的业务结果。*/
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

    /**buildTask 业务数据，统一收口字段清洗、默认值处理和返回对象组装规则。
@param id 主键ID。
@return 处理后的业务结果。*/
    private SyncTask buildTask(Long id) {
        SyncTask task = new SyncTask();
        task.setId(id);
        task.setTaskCode("MACHINE_BARCODE_SYNC");
        task.setTaskName("条码档案同步");
        task.setHandlerCode("machineBarcodeSync");
        return task;
    }

    /**buildHandler 业务数据，统一收口字段清洗、默认值处理和返回对象组装规则。
@return 处理后的业务结果。*/
    private SyncTaskHandler buildHandler() {
        return new SyncTaskHandler() {
            /**getCode 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@return 查询或解析得到的业务对象。*/
            @Override
            public String getCode() {
                return "machineBarcodeSync";
            }

            /**getName 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@return 查询或解析得到的业务对象。*/
            @Override
            public String getName() {
                return "条码档案同步";
            }

            /**execute 处理逻辑，服务于当前类的业务编排和数据转换。
@param task task 字段参数。
@param context context 字段参数。
@return 处理后的业务结果。*/
            @Override
            public SyncTaskExecutionResult execute(SyncTask task, SyncTaskExecutionContext context) {
                return SyncTaskExecutionResult.builder().message("OK").build();
            }
        };
    }

    /**createSyncTaskMapper 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@param task task 字段参数。
@return 新增或保存后的业务标识或处理结果。*/
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

    /**setField 处理逻辑，服务于当前类的业务编排和数据转换。
@param target target 字段参数。
@param fieldName 名称文本，用于展示、匹配或保存业务对象名称。
@param value value 字段参数。*/
    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /**CapturingAsyncExecutor 测试类，用于验证对应业务规则、边界条件和回归场景。

@author Zoro*/
    private static class CapturingAsyncExecutor extends SyncTaskAsyncExecutor {
        /**taskId 字段，用于当前类内部业务处理。*/
        private Long taskId;
        /**logId 字段，用于当前类内部业务处理。*/
        private Long logId;

        /**executeAsync 处理逻辑，服务于当前类的业务编排和数据转换。
@param taskId taskId 字段。
@param logId logId 字段。*/
        @Override
        public void executeAsync(Long taskId, Long logId) {
            this.taskId = taskId;
            this.logId = logId;
        }
    }

    /**CapturingRunner 测试类，用于验证对应业务规则、边界条件和回归场景。

@author Zoro*/
    private static class CapturingRunner extends SyncTaskExecutionRunner {
        /**taskId 字段，用于当前类内部业务处理。*/
        private Long taskId;
        /**logId 字段，用于当前类内部业务处理。*/
        private Long logId;

        /**executeWithLog 处理逻辑，服务于当前类的业务编排和数据转换。
@param taskId taskId 字段。
@param logId logId 字段。*/
        @Override
        public void executeWithLog(Long taskId, Long logId) {
            this.taskId = taskId;
            this.logId = logId;
        }
    }

    /**CapturingLogMapper 测试类，用于验证对应业务规则、边界条件和回归场景。

@author Zoro*/
    private static class CapturingLogMapper {
        /**insertedLog 字段，用于当前类内部业务处理。*/
        private SyncTaskLog insertedLog;

        /**proxy 处理逻辑，服务于当前类的业务编排和数据转换。
@return 处理后的业务结果。*/
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
