package com.jasic.aftersales.system.notify.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.jasic.aftersales.system.notify.domain.entity.SysNotifyEvent;
import com.jasic.aftersales.system.notify.mapper.SysNotifyEventMapper;
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

/*** NotifyEventServiceImpl 单测。

@author Zoro*/
public class NotifyEventServiceImplTest {

    /**验证BuildConsumableQueryWithRetryLimit，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldBuildConsumableQueryWithRetryLimit() throws Exception {
        NotifyEventServiceImpl service = new NotifyEventServiceImpl();
        EventMapperState mapperState = new EventMapperState();
        mapperState.selectListResult = Collections.emptyList();
        setField(service, "sysNotifyEventMapper", createMapperProxy(mapperState));
        initTableInfo();

        service.listConsumableEvents(LocalDateTime.of(2026, 5, 14, 10, 0, 0), 20);

        Assert.assertNotNull(mapperState.lastSelectWrapper);
        String sqlSegment = mapperState.lastSelectWrapper.getSqlSegment();
        Assert.assertTrue(sqlSegment.contains("status"));
        Assert.assertTrue(sqlSegment.contains("retry_count"));
        Assert.assertTrue(sqlSegment.contains("next_retry_time"));
        Assert.assertTrue(sqlSegment.contains("ORDER BY id ASC"));
    }

    /**验证RecoverTimeoutProcessingEventsToFailed，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldRecoverTimeoutProcessingEventsToFailed() throws Exception {
        NotifyEventServiceImpl service = new NotifyEventServiceImpl();
        EventMapperState mapperState = new EventMapperState();
        mapperState.updateResult = 2;
        setField(service, "sysNotifyEventMapper", createMapperProxy(mapperState));
        initTableInfo();

        LocalDateTime timeoutBefore = LocalDateTime.of(2026, 5, 14, 9, 50, 0);
        int result = service.recoverTimeoutProcessingEvents(timeoutBefore);

        Assert.assertEquals(2, result);
        Assert.assertNotNull(mapperState.lastUpdateWrapper);
        Assert.assertTrue(mapperState.lastUpdateWrapper.getSqlSegment().contains("processing_time"));
        Assert.assertTrue(mapperState.lastUpdateWrapper.getSqlSet().contains("status"));
        Assert.assertTrue(mapperState.lastUpdateWrapper.getSqlSet().contains("error_message"));
        Assert.assertTrue(mapperState.lastUpdateWrapper.getParamNameValuePairs().containsValue(timeoutBefore));
        Assert.assertTrue(mapperState.lastUpdateWrapper.getParamNameValuePairs().containsValue("FAILED"));
    }

    /**验证ResetDeadEventForManualRetry，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldResetDeadEventForManualRetry() throws Exception {
        NotifyEventServiceImpl service = new NotifyEventServiceImpl();
        EventMapperState mapperState = new EventMapperState();
        mapperState.updateResult = 1;
        setField(service, "sysNotifyEventMapper", createMapperProxy(mapperState));
        initTableInfo();

        service.resetForRetry(100L);

        Assert.assertNotNull(mapperState.lastUpdateWrapper);
        Assert.assertTrue(mapperState.lastUpdateWrapper.getSqlSet().contains("retry_count"));
        Assert.assertTrue(mapperState.lastUpdateWrapper.getSqlSet().contains("next_retry_time"));
        Assert.assertTrue(mapperState.lastUpdateWrapper.getSqlSet().contains("processing_time"));
        Assert.assertTrue(mapperState.lastUpdateWrapper.getSqlSet().contains("error_message"));
        Assert.assertTrue(mapperState.lastUpdateWrapper.getParamNameValuePairs().containsValue("NEW"));
        Assert.assertTrue(mapperState.lastUpdateWrapper.getParamNameValuePairs().containsValue(0));
    }

    /**
     * 创建事件 Mapper 代理。
     *
     * @param state 测试状态
     * @return Mapper 代理
     */
    @SuppressWarnings("unchecked")
    private SysNotifyEventMapper createMapperProxy(EventMapperState state) {
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
                    state.lastSelectWrapper = (LambdaQueryWrapper<SysNotifyEvent>) args[0];
                    return state.selectListResult;
                }
                if ("update".equals(name)) {
                    state.lastUpdateWrapper = (LambdaUpdateWrapper<SysNotifyEvent>) args[1];
                    return state.updateResult;
                }
                if ("selectOne".equals(name)) {
                    return null;
                }
                if ("selectById".equals(name)) {
                    return null;
                }
                if ("insert".equals(name)) {
                    return 1;
                }
                if ("updateById".equals(name)) {
                    state.lastUpdateEntity = (SysNotifyEvent) args[0];
                    return 1;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (SysNotifyEventMapper) Proxy.newProxyInstance(
                SysNotifyEventMapper.class.getClassLoader(),
                new Class[]{SysNotifyEventMapper.class},
                handler
        );
    }

    /**
     * 初始化 MyBatis-Plus 实体元数据。
     */
    private void initTableInfo() {
        if (TableInfoHelper.getTableInfo(SysNotifyEvent.class) != null) {
            return;
        }
        Configuration configuration = new Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "test");
        assistant.setCurrentNamespace(SysNotifyEventMapper.class.getName());
        TableInfoHelper.initTableInfo(assistant, SysNotifyEvent.class);
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
     * 事件 Mapper 调用状态。
     */
    private static class EventMapperState {
        /**selectListResult 字段，用于当前类内部业务处理。*/
        private List<SysNotifyEvent> selectListResult = Collections.emptyList();
        /**updateResult 字段，用于当前类内部业务处理。*/
        private int updateResult;
        /**lastSelectWrapper 字段，用于当前类内部业务处理。*/
        private LambdaQueryWrapper<SysNotifyEvent> lastSelectWrapper;
        /**lastUpdateWrapper 字段，用于当前类内部业务处理。*/
        private LambdaUpdateWrapper<SysNotifyEvent> lastUpdateWrapper;
        /**lastUpdateEntity 字段，用于当前类内部业务处理。*/
        private SysNotifyEvent lastUpdateEntity;
    }
}
