package com.jasic.aftersales.system.service.support;

import com.jasic.aftersales.common.exception.ServiceException;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**WorkOrderNoGeneratorTest 测试类，用于验证对应业务规则、边界条件和回归场景。

@author Zoro*/
public class WorkOrderNoGeneratorTest {

    /**验证GenerateOrderNoWithDailySequence，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldGenerateOrderNoWithDailySequence() throws Exception {
        WorkOrderNoGenerator generator = new WorkOrderNoGenerator();
        FakeStringRedisTemplate redisTemplate = new FakeStringRedisTemplate();
        setField(generator, "stringRedisTemplate", redisTemplate);

        String orderNo = generator.nextOrderNo();
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        Assert.assertEquals("JSWX" + datePart + "00001", orderNo);
        Assert.assertTrue(redisTemplate.getRecordedExpire("workorder:order-no:" + datePart).getSeconds() > 0);
    }

    /**验证RejectWhenDailySequenceExceedsLimit，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldRejectWhenDailySequenceExceedsLimit() throws Exception {
        WorkOrderNoGenerator generator = new WorkOrderNoGenerator();
        FakeStringRedisTemplate redisTemplate = new FakeStringRedisTemplate();
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        redisTemplate.setCounter("workorder:order-no:" + datePart, 99999L);
        setField(generator, "stringRedisTemplate", redisTemplate);

        try {
            generator.nextOrderNo();
            Assert.fail("Expected generator to reject sequences beyond 99999");
        } catch (ServiceException ex) {
            Assert.assertEquals("当日工单号流水已达上限，请联系管理员处理", ex.getMessage());
        }
    }

    /**setField 处理逻辑，服务于当前类的业务编排和数据转换。
@param target target 字段参数。
@param fieldName 名称文本，用于展示、匹配或保存业务对象名称。
@param value value 字段参数。*/
    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = findField(target.getClass(), fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /**findField 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@param type type 字段参数。
@param fieldName 名称文本，用于展示、匹配或保存业务对象名称。
@return 查询或解析得到的业务对象。*/
    private Field findField(Class<?> type, String fieldName) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }

    /**FakeStringRedisTemplate 测试类，用于验证对应业务规则、边界条件和回归场景。

@author Zoro*/
    private static final class FakeStringRedisTemplate extends StringRedisTemplate {

        /**counters 字段，用于当前类内部业务处理。*/
        private final Map<String, Long> counters = new HashMap<>();
        /**expires 字段，用于当前类内部业务处理。*/
        private final Map<String, Duration> expires = new HashMap<>();
        /**valueOperations 字段，用于当前类内部业务处理。*/
        private final ValueOperations<String, String> valueOperations;

        /**构造 FakeStringRedisTemplate 实例，初始化当前对象在业务流程中需要持有的基础数据。*/
        private FakeStringRedisTemplate() {
            this.valueOperations = createValueOperationsProxy();
        }

        /**opsForValue 处理逻辑，服务于当前类的业务编排和数据转换。
@return 处理后的业务结果。*/
        @Override
        public ValueOperations<String, String> opsForValue() {
            return valueOperations;
        }

        /**expire 处理逻辑，服务于当前类的业务编排和数据转换。
@param key key 字段参数。
@param timeout timeout 字段参数。
@return true 表示满足业务条件，false 表示不满足。*/
        @Override
        public Boolean expire(String key, Duration timeout) {
            expires.put(key, timeout);
            return Boolean.TRUE;
        }

        /**getRecordedExpire 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@param key key 字段参数。
@return 查询或解析得到的业务对象。*/
        private Duration getRecordedExpire(String key) {
            return expires.get(key);
        }

        /**setCounter 处理逻辑，服务于当前类的业务编排和数据转换。
@param key key 字段参数。
@param value value 字段参数。*/
        private void setCounter(String key, long value) {
            counters.put(key, value);
        }

        /**createValueOperationsProxy 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@return 新增或保存后的业务标识或处理结果。*/
        @SuppressWarnings("unchecked")
        private ValueOperations<String, String> createValueOperationsProxy() {
            InvocationHandler handler = new InvocationHandler() {
                /**invoke 处理逻辑，服务于当前类的业务编排和数据转换。
@param proxy proxy 字段参数。
@param method method 字段参数。
@param args args 字段参数。
@return 处理后的业务结果。*/
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) {
                    String methodName = method.getName();
                    if ("increment".equals(methodName) && args != null && args.length >= 1) {
                        String key = (String) args[0];
                        long nextValue = counters.getOrDefault(key, 0L) + 1L;
                        counters.put(key, nextValue);
                        return nextValue;
                    }
                    if ("toString".equals(methodName)) {
                        return "FakeValueOperations";
                    }
                    if ("hashCode".equals(methodName)) {
                        return System.identityHashCode(proxy);
                    }
                    if ("equals".equals(methodName)) {
                        return proxy == args[0];
                    }
                    throw new UnsupportedOperationException(methodName);
                }
            };
            return (ValueOperations<String, String>) Proxy.newProxyInstance(
                    ValueOperations.class.getClassLoader(),
                    new Class<?>[]{ValueOperations.class},
                    handler
            );
        }
    }
}
