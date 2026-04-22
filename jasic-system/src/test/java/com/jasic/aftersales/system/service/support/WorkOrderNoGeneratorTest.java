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

public class WorkOrderNoGeneratorTest {

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

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = findField(target.getClass(), fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

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

    private static final class FakeStringRedisTemplate extends StringRedisTemplate {

        private final Map<String, Long> counters = new HashMap<>();
        private final Map<String, Duration> expires = new HashMap<>();
        private final ValueOperations<String, String> valueOperations;

        private FakeStringRedisTemplate() {
            this.valueOperations = createValueOperationsProxy();
        }

        @Override
        public ValueOperations<String, String> opsForValue() {
            return valueOperations;
        }

        @Override
        public Boolean expire(String key, Duration timeout) {
            expires.put(key, timeout);
            return Boolean.TRUE;
        }

        private Duration getRecordedExpire(String key) {
            return expires.get(key);
        }

        private void setCounter(String key, long value) {
            counters.put(key, value);
        }

        @SuppressWarnings("unchecked")
        private ValueOperations<String, String> createValueOperationsProxy() {
            InvocationHandler handler = new InvocationHandler() {
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
