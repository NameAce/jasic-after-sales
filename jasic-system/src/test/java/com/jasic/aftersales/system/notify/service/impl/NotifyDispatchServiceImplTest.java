package com.jasic.aftersales.system.notify.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyDispatch;
import com.jasic.aftersales.system.notify.domain.enums.NotifyDispatchResultCodeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyDispatchStatusEnum;
import com.jasic.aftersales.system.notify.mapper.SysNotifyDispatchMapper;
import com.jasic.aftersales.system.notify.support.NotifyChannelSendResult;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/*** NotifyDispatchServiceImpl 单测。

@author Zoro*/
public class NotifyDispatchServiceImplTest {

    /**验证BuildSendableQueryWithRetryLimit，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldBuildSendableQueryWithRetryLimit() throws Exception {
        NotifyDispatchServiceImpl service = new NotifyDispatchServiceImpl();
        DispatchMapperState mapperState = new DispatchMapperState();
        mapperState.selectListResult = Collections.emptyList();
        setField(service, "sysNotifyDispatchMapper", createMapperProxy(mapperState));
        initTableInfo();

        service.listSendableDispatches(LocalDateTime.of(2026, 5, 14, 10, 0, 0), 20);

        Assert.assertNotNull(mapperState.lastSelectWrapper);
        String sqlSegment = mapperState.lastSelectWrapper.getSqlSegment();
        Assert.assertTrue(sqlSegment.contains("dispatch_status"));
        Assert.assertTrue(sqlSegment.contains("retry_count"));
        Assert.assertTrue(sqlSegment.contains("next_retry_time"));
        Assert.assertTrue(sqlSegment.contains("ORDER BY id ASC"));
    }

    /**验证MarkDispatchFailedWhenSendResultBelowMax，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldMarkDispatchFailedWhenSendResultBelowMax() throws Exception {
        NotifyDispatchServiceImpl service = new NotifyDispatchServiceImpl();
        DispatchMapperState mapperState = new DispatchMapperState();
        mapperState.updateResult = 1;
        setField(service, "sysNotifyDispatchMapper", createMapperProxy(mapperState));
        initTableInfo();

        SysNotifyDispatch dispatch = new SysNotifyDispatch();
        dispatch.setId(1L);
        dispatch.setRetryCount(1);
        dispatch.setDispatchStatus(NotifyDispatchStatusEnum.PROCESSING.getCode());

        NotifyChannelSendResult sendResult = new NotifyChannelSendResult();
        sendResult.setDispatchStatus(NotifyDispatchStatusEnum.FAILED.getCode());
        sendResult.setResultCode(NotifyDispatchResultCodeEnum.FAILED_CHANNEL_REQUEST.getCode());
        sendResult.setResultMessage("微信接口超时");

        invokeApplySendResult(service, dispatch, sendResult);

        Assert.assertNotNull(mapperState.lastUpdateWrapper);
        Assert.assertTrue(mapperState.lastUpdateWrapper.getParamNameValuePairs().containsValue("FAILED"));
        Assert.assertTrue(mapperState.lastUpdateWrapper.getParamNameValuePairs().containsValue(2));
        Assert.assertTrue(mapperState.lastUpdateWrapper.getParamNameValuePairs()
                .containsValue(NotifyDispatchResultCodeEnum.FAILED_CHANNEL_REQUEST.getCode()));
    }

    /**验证MarkDispatchDeadWhenSendResultReachesMax，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldMarkDispatchDeadWhenSendResultReachesMax() throws Exception {
        NotifyDispatchServiceImpl service = new NotifyDispatchServiceImpl();
        DispatchMapperState mapperState = new DispatchMapperState();
        mapperState.updateResult = 1;
        setField(service, "sysNotifyDispatchMapper", createMapperProxy(mapperState));
        initTableInfo();

        SysNotifyDispatch dispatch = new SysNotifyDispatch();
        dispatch.setId(2L);
        dispatch.setRetryCount(2);
        dispatch.setDispatchStatus(NotifyDispatchStatusEnum.PROCESSING.getCode());

        NotifyChannelSendResult sendResult = new NotifyChannelSendResult();
        sendResult.setDispatchStatus(NotifyDispatchStatusEnum.FAILED.getCode());
        sendResult.setResultCode(NotifyDispatchResultCodeEnum.FAILED_CHANNEL_REQUEST.getCode());
        sendResult.setResultMessage("微信接口返回失败");

        invokeApplySendResult(service, dispatch, sendResult);

        Assert.assertNotNull(mapperState.lastUpdateWrapper);
        Assert.assertTrue(mapperState.lastUpdateWrapper.getParamNameValuePairs().containsValue("DEAD"));
        Assert.assertTrue(mapperState.lastUpdateWrapper.getParamNameValuePairs().containsValue(3));
        Assert.assertTrue(mapperState.lastUpdateWrapper.getParamNameValuePairs()
                .containsValue(NotifyDispatchResultCodeEnum.DEAD_RETRY_EXCEEDED.getCode()));
    }

    /**验证RecoverTimeoutProcessingDispatchesToFailed，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldRecoverTimeoutProcessingDispatchesToFailed() throws Exception {
        NotifyDispatchServiceImpl service = new NotifyDispatchServiceImpl();
        DispatchMapperState mapperState = new DispatchMapperState();
        mapperState.updateResult = 3;
        setField(service, "sysNotifyDispatchMapper", createMapperProxy(mapperState));
        initTableInfo();

        LocalDateTime timeoutBefore = LocalDateTime.of(2026, 5, 14, 9, 50, 0);
        int result = service.recoverTimeoutProcessingDispatches(timeoutBefore);

        Assert.assertEquals(3, result);
        Assert.assertNotNull(mapperState.lastUpdateWrapper);
        Assert.assertTrue(mapperState.lastUpdateWrapper.getSqlSegment().contains("processing_time"));
        Assert.assertTrue(mapperState.lastUpdateWrapper.getParamNameValuePairs().containsValue("FAILED"));
        Assert.assertTrue(mapperState.lastUpdateWrapper.getParamNameValuePairs().containsValue(timeoutBefore));
    }

    /**验证ResetDeadDispatchForManualRetry，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldResetDeadDispatchForManualRetry() throws Exception {
        NotifyDispatchServiceImpl service = new NotifyDispatchServiceImpl();
        DispatchMapperState mapperState = new DispatchMapperState();
        mapperState.updateResult = 1;
        setField(service, "sysNotifyDispatchMapper", createMapperProxy(mapperState));
        initTableInfo();

        service.resetForRetry(99L);

        Assert.assertNotNull(mapperState.lastUpdateWrapper);
        Assert.assertTrue(mapperState.lastUpdateWrapper.getSqlSet().contains("retry_count"));
        Assert.assertTrue(mapperState.lastUpdateWrapper.getSqlSet().contains("result_code"));
        Assert.assertTrue(mapperState.lastUpdateWrapper.getSqlSet().contains("result_message"));
        Assert.assertTrue(mapperState.lastUpdateWrapper.getParamNameValuePairs().containsValue("PENDING"));
        Assert.assertTrue(mapperState.lastUpdateWrapper.getParamNameValuePairs().containsValue(0));
    }

    /**
     * 调用私有发送结果处理方法。
     *
     * @param service 服务实例
     * @param dispatch 分发任务
     * @param sendResult 发送结果
     * @throws Exception 反射异常
     */
    private void invokeApplySendResult(NotifyDispatchServiceImpl service, SysNotifyDispatch dispatch,
                                       NotifyChannelSendResult sendResult) throws Exception {
        Method method = NotifyDispatchServiceImpl.class.getDeclaredMethod(
                "applySendResult", SysNotifyDispatch.class, NotifyChannelSendResult.class);
        method.setAccessible(true);
        method.invoke(service, dispatch, sendResult);
    }

    /**
     * 创建分发 Mapper 代理。
     *
     * @param state 测试状态
     * @return Mapper 代理
     */
    @SuppressWarnings("unchecked")
    private SysNotifyDispatchMapper createMapperProxy(DispatchMapperState state) {
        InvocationHandler handler = new InvocationHandler() {
            /**invoke 处理逻辑，服务于当前类的业务编排和数据转换。
@param proxy proxy 字段参数。
@param method method 字段参数。
@param args args 字段参数。
@return 处理后的业务结果。*/
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                String name = method.getName();
                if ("selectList".equals(name)) {
                    state.lastSelectWrapper = (LambdaQueryWrapper<SysNotifyDispatch>) args[0];
                    return state.selectListResult;
                }
                if ("update".equals(name)) {
                    state.lastUpdateWrapper = (LambdaUpdateWrapper<SysNotifyDispatch>) args[1];
                    return state.updateResult;
                }
                if ("selectById".equals(name) || "selectOne".equals(name)) {
                    return null;
                }
                if ("insert".equals(name)) {
                    return 1;
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
     * 初始化 MyBatis-Plus 实体元数据。
     */
    private void initTableInfo() {
        if (TableInfoHelper.getTableInfo(SysNotifyDispatch.class) != null) {
            return;
        }
        Configuration configuration = new Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "test");
        assistant.setCurrentNamespace(SysNotifyDispatchMapper.class.getName());
        TableInfoHelper.initTableInfo(assistant, SysNotifyDispatch.class);
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
     * 分发 Mapper 调用状态。
     */
    private static class DispatchMapperState {
        /**selectListResult 字段，用于当前类内部业务处理。*/
        private List<SysNotifyDispatch> selectListResult = Collections.emptyList();
        /**updateResult 字段，用于当前类内部业务处理。*/
        private int updateResult;
        /**lastSelectWrapper 字段，用于当前类内部业务处理。*/
        private LambdaQueryWrapper<SysNotifyDispatch> lastSelectWrapper;
        /**lastUpdateWrapper 字段，用于当前类内部业务处理。*/
        private LambdaUpdateWrapper<SysNotifyDispatch> lastUpdateWrapper;
    }
}
