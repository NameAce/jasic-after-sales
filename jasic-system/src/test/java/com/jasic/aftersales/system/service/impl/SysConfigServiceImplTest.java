package com.jasic.aftersales.system.service.impl;

import com.jasic.aftersales.common.constant.CacheConstants;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.domain.dto.SysConfigDTO;
import com.jasic.aftersales.system.domain.dto.SysConfigGroupSaveDTO;
import com.jasic.aftersales.system.domain.entity.SysConfig;
import com.jasic.aftersales.system.mapper.SysConfigMapper;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 系统参数分组保存服务测试。
 *
 * <p>这里重点覆盖两个容易回归的风险点：
 * 一是分组保存时缓存必须在事务提交后再刷新，不能把回滚前的数据提前暴露给运行时读取链路；
 * 二是分组保存只能维护本组已有配置项的值，不能借这个批量接口篡改配置定义或跨组修改其它记录。</p>
 *
 * @author Codex
 * @date 2026/05/21
 */
public class SysConfigServiceImplTest {

    @Test
    public void shouldRefreshCacheOnlyAfterCommitWhenSavingGroup() throws Exception {
        SysConfigServiceImpl service = new SysConfigServiceImpl();
        FakeSysConfigMapper mapper = new FakeSysConfigMapper();
        FakeRedisTemplate redisTemplate = new FakeRedisTemplate();
        SysConfig storedConfig = buildConfig(1L, "组织管理员初始密码", "org.company.adminInitPassword", "old-value", 1, "org", "旧备注");
        mapper.put(storedConfig);
        redisTemplate.putConfigValue(storedConfig.getConfigKey(), storedConfig.getConfigValue());
        setField(service, "sysConfigMapper", mapper.createProxy());
        setField(service, "redisTemplate", redisTemplate);

        SysConfigGroupSaveDTO dto = new SysConfigGroupSaveDTO();
        dto.setGroupKey("org");
        dto.setConfigs(singletonList(buildConfigDTO(1L, "组织管理员初始密码", "org.company.adminInitPassword", "new-value", 1, "org", "新备注")));

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.saveGroup(dto);

            // 数据库更新发生在事务中，但缓存必须继续保持旧值，直到 afterCommit 触发为止。
            Assert.assertEquals("new-value", mapper.getStored(1L).getConfigValue());
            Assert.assertEquals("old-value", redisTemplate.getConfigValue(storedConfig.getConfigKey()));

            for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCommit();
            }

            Assert.assertEquals("new-value", redisTemplate.getConfigValue(storedConfig.getConfigKey()));
            Assert.assertEquals("新备注", mapper.getStored(1L).getRemark());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    public void shouldRejectWhenGroupSaveAttemptsToModifyAnotherGroupsRecord() throws Exception {
        SysConfigServiceImpl service = new SysConfigServiceImpl();
        FakeSysConfigMapper mapper = new FakeSysConfigMapper();
        FakeRedisTemplate redisTemplate = new FakeRedisTemplate();
        SysConfig storedConfig = buildConfig(7L, "默认归属总部ID", "default.hq.company.id", "100", 0, "work_order", "工单默认总部");
        mapper.put(storedConfig);
        setField(service, "sysConfigMapper", mapper.createProxy());
        setField(service, "redisTemplate", redisTemplate);

        SysConfigGroupSaveDTO dto = new SysConfigGroupSaveDTO();
        dto.setGroupKey("org");
        // 这里故意伪造成 org 分组内的配置 key，验证后端不能只信前端回传的 key，而必须校验真实记录归属。
        dto.setConfigs(singletonList(buildConfigDTO(7L, "组织管理员初始密码", "org.company.adminInitPassword", "hacked", 1, "org", "伪造请求")));

        try {
            service.saveGroup(dto);
            Assert.fail("Expected saveGroup to reject cross-group record updates");
        } catch (ServiceException ex) {
            Assert.assertEquals("100", mapper.getStored(7L).getConfigValue());
            Assert.assertEquals("work_order", mapper.getStored(7L).getGroupKey());
        }
    }

    @Test
    public void shouldRejectWhenGroupSaveAttemptsToChangeConfigKey() throws Exception {
        SysConfigServiceImpl service = new SysConfigServiceImpl();
        FakeSysConfigMapper mapper = new FakeSysConfigMapper();
        FakeRedisTemplate redisTemplate = new FakeRedisTemplate();
        SysConfig storedConfig = buildConfig(1L, "组织管理员初始密码", "org.company.adminInitPassword", "old-value", 1, "org", "旧备注");
        mapper.put(storedConfig);
        setField(service, "sysConfigMapper", mapper.createProxy());
        setField(service, "redisTemplate", redisTemplate);

        SysConfigGroupSaveDTO dto = new SysConfigGroupSaveDTO();
        dto.setGroupKey("org");
        dto.setConfigs(singletonList(buildConfigDTO(1L, "组织管理员初始密码", "org.company.adminInitPassword.changed", "new-value", 1, "org", "新备注")));

        try {
            service.saveGroup(dto);
            Assert.fail("Expected saveGroup to reject config definition changes");
        } catch (ServiceException ex) {
            Assert.assertEquals("org.company.adminInitPassword", mapper.getStored(1L).getConfigKey());
            Assert.assertEquals("old-value", mapper.getStored(1L).getConfigValue());
        }
    }

    /**
     * 构造测试配置实体，保持测试数据初始化入口统一，避免每个测试手工拼装遗漏分组或内置标识。
     */
    private SysConfig buildConfig(Long id, String configName, String configKey, String configValue, Integer configType,
                                  String groupKey, String remark) {
        SysConfig config = new SysConfig();
        config.setId(id);
        config.setConfigName(configName);
        config.setConfigKey(configKey);
        config.setConfigValue(configValue);
        config.setConfigType(configType);
        config.setGroupKey(groupKey);
        config.setRemark(remark);
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        return config;
    }

    /**
     * 构造分组保存入参，确保每个测试都显式声明配置定义字段，便于覆盖“禁止改定义”的保护逻辑。
     */
    private SysConfigDTO buildConfigDTO(Long id, String configName, String configKey, String configValue, Integer configType,
                                        String groupKey, String remark) {
        SysConfigDTO dto = new SysConfigDTO();
        dto.setId(id);
        dto.setConfigName(configName);
        dto.setConfigKey(configKey);
        dto.setConfigValue(configValue);
        dto.setConfigType(configType);
        dto.setGroupKey(groupKey);
        dto.setRemark(remark);
        return dto;
    }

    /**
     * 统一封装单元素列表构造，避免各测试重复创建中间局部变量。
     */
    private <T> List<T> singletonList(T value) {
        List<T> list = new ArrayList<>(1);
        list.add(value);
        return list;
    }

    /**
     * 通过反射注入测试替身，沿用项目当前不依赖 Mock 框架的单元测试风格。
     */
    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = findField(target.getClass(), fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /**
     * 向上遍历父类查找字段，兼容后续服务层如果抽取公共父类时测试仍可复用。
     */
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

    /**
     * 轻量级 Mapper 替身。
     *
     * <p>这里只实现本次测试会走到的 `selectById`、`updateById` 和 `selectList`，把配置记录存在内存里，
     * 用来验证服务层的业务判断和缓存刷新时机，不把测试范围扩大到 MyBatis-Plus 本身。</p>
     */
    private static final class FakeSysConfigMapper implements InvocationHandler {

        private final Map<Long, SysConfig> storage = new HashMap<>();

        private SysConfigMapper createProxy() {
            return (SysConfigMapper) Proxy.newProxyInstance(
                    SysConfigMapper.class.getClassLoader(),
                    new Class<?>[]{SysConfigMapper.class},
                    this
            );
        }

        private void put(SysConfig config) {
            storage.put(config.getId(), copyConfig(config));
        }

        private SysConfig getStored(Long id) {
            return storage.get(id);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String methodName = method.getName();
            if ("selectById".equals(methodName)) {
                return copyConfig(storage.get(args[0]));
            }
            if ("updateById".equals(methodName)) {
                SysConfig config = (SysConfig) args[0];
                storage.put(config.getId(), copyConfig(config));
                return 1;
            }
            if ("selectList".equals(methodName)) {
                List<SysConfig> configs = new ArrayList<>();
                for (SysConfig config : storage.values()) {
                    configs.add(copyConfig(config));
                }
                return configs;
            }
            if ("toString".equals(methodName)) {
                return "FakeSysConfigMapper";
            }
            if ("hashCode".equals(methodName)) {
                return System.identityHashCode(proxy);
            }
            if ("equals".equals(methodName)) {
                return proxy == args[0];
            }
            throw new UnsupportedOperationException(methodName);
        }

        /**
         * 每次返回独立副本，避免服务层拿到实体后直接修改内存仓库里的对象，导致测试无法区分“查询结果”和“持久化结果”。
         */
        private SysConfig copyConfig(SysConfig source) {
            if (source == null) {
                return null;
            }
            SysConfig target = new SysConfig();
            target.setId(source.getId());
            target.setConfigName(source.getConfigName());
            target.setConfigKey(source.getConfigKey());
            target.setConfigValue(source.getConfigValue());
            target.setConfigType(source.getConfigType());
            target.setGroupKey(source.getGroupKey());
            target.setRemark(source.getRemark());
            target.setCreateTime(source.getCreateTime());
            target.setUpdateTime(source.getUpdateTime());
            return target;
        }
    }

    /**
     * 轻量级 RedisTemplate 替身。
     *
     * <p>这里只模拟系统参数服务实际会用到的 `opsForValue/get/set`、`hasKey`、`keys` 和 `delete`，
     * 用来观察分组保存前后缓存值是否按预期变化。</p>
     */
    private static final class FakeRedisTemplate extends RedisTemplate<String, Object> {

        private final Map<String, Object> values = new HashMap<>();
        private final ValueOperations<String, Object> valueOperations;

        private FakeRedisTemplate() {
            this.valueOperations = createValueOperationsProxy();
        }

        @Override
        public ValueOperations<String, Object> opsForValue() {
            return valueOperations;
        }

        @Override
        public Boolean hasKey(String key) {
            return values.containsKey(key);
        }

        @Override
        public Set<String> keys(String pattern) {
            Set<String> matchedKeys = new HashSet<>();
            String prefix = pattern.substring(0, pattern.length() - 1);
            for (String key : values.keySet()) {
                if (key.startsWith(prefix)) {
                    matchedKeys.add(key);
                }
            }
            return matchedKeys;
        }

        @Override
        public Boolean delete(String key) {
            return values.remove(key) != null;
        }

        @Override
        public Long delete(Collection<String> keys) {
            long deletedCount = 0L;
            for (String key : keys) {
                if (values.remove(key) != null) {
                    deletedCount++;
                }
            }
            return deletedCount;
        }

        private void putConfigValue(String configKey, String value) {
            values.put(CacheConstants.CONFIG_KEY + configKey, value);
        }

        private String getConfigValue(String configKey) {
            Object value = values.get(CacheConstants.CONFIG_KEY + configKey);
            return value == null ? null : String.valueOf(value);
        }

        @SuppressWarnings("unchecked")
        private ValueOperations<String, Object> createValueOperationsProxy() {
            InvocationHandler handler = new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) {
                    String methodName = method.getName();
                    if ("set".equals(methodName) && args != null && args.length >= 2) {
                        values.put((String) args[0], args[1]);
                        return null;
                    }
                    if ("get".equals(methodName) && args != null && args.length >= 1) {
                        return values.get(args[0]);
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
            return (ValueOperations<String, Object>) Proxy.newProxyInstance(
                    ValueOperations.class.getClassLoader(),
                    new Class<?>[]{ValueOperations.class},
                    handler
            );
        }
    }
}
