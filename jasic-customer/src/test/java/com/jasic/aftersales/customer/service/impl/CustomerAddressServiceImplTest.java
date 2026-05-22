package com.jasic.aftersales.customer.service.impl;

import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.customer.domain.dto.CustomerAddressCreateDTO;
import com.jasic.aftersales.customer.domain.entity.CUser;
import com.jasic.aftersales.customer.domain.entity.CustomerAddress;
import com.jasic.aftersales.customer.mapper.CustomerAddressMapper;
import com.jasic.aftersales.customer.service.ICUserService;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * C端客户地址服务测试
 *
 * @author Zoro
 * @date 2026/04/08
 */
public class CustomerAddressServiceImplTest {

    /**验证AutoSetFirstAddressAsDefault，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldAutoSetFirstAddressAsDefault() throws Exception {
        CustomerAddressServiceImpl service = new CustomerAddressServiceImpl();
        List<CustomerAddress> store = new ArrayList<>();
        setField(service, "customerAddressMapper", createAddressMapperProxy(store));
        setField(service, "cUserService", createUserServiceProxy(200L));

        CustomerAddressCreateDTO dto = buildCreateDTO();
        dto.setIsDefault(0);

        Long addressId = service.create(dto);

        Assert.assertNotNull(addressId);
        Assert.assertEquals(1, store.size());
        Assert.assertEquals(Long.valueOf(200L), store.get(0).getCustomerId());
        Assert.assertEquals(Integer.valueOf(1), store.get(0).getIsDefault());
    }

    /**验证RejectCreateWhenAddressCountExceedsLimit，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldRejectCreateWhenAddressCountExceedsLimit() throws Exception {
        CustomerAddressServiceImpl service = new CustomerAddressServiceImpl();
        List<CustomerAddress> store = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            store.add(buildAddress((long) i, 200L, i == 1 ? 1 : 0, LocalDateTime.now().minusMinutes(i)));
        }
        setField(service, "customerAddressMapper", createAddressMapperProxy(store));
        setField(service, "cUserService", createUserServiceProxy(200L));

        try {
            service.create(buildCreateDTO());
            Assert.fail("预期地址数量超限时应被拒绝");
        } catch (ServiceException ex) {
            Assert.assertEquals("最多只能保存20条地址，请先删除一条后再新增", ex.getMessage());
        }
    }

    /**验证PromoteLatestRemainingAddressAfterDeletingDefault，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldPromoteLatestRemainingAddressAfterDeletingDefault() throws Exception {
        CustomerAddressServiceImpl service = new CustomerAddressServiceImpl();
        List<CustomerAddress> store = new ArrayList<>();
        store.add(buildAddress(1L, 200L, 1, LocalDateTime.of(2026, 4, 8, 9, 0)));
        store.add(buildAddress(2L, 200L, 0, LocalDateTime.of(2026, 4, 8, 10, 0)));
        store.add(buildAddress(3L, 200L, 0, LocalDateTime.of(2026, 4, 8, 11, 0)));
        setField(service, "customerAddressMapper", createAddressMapperProxy(store));
        setField(service, "cUserService", createUserServiceProxy(200L));

        service.delete(1L);

        Assert.assertEquals(2, store.size());
        Assert.assertEquals(Integer.valueOf(1), findAddress(store, 3L).getIsDefault());
        Assert.assertEquals(Integer.valueOf(0), findAddress(store, 2L).getIsDefault());
    }

    /**验证ClearOtherDefaultsWhenSettingDefault，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldClearOtherDefaultsWhenSettingDefault() throws Exception {
        CustomerAddressServiceImpl service = new CustomerAddressServiceImpl();
        List<CustomerAddress> store = new ArrayList<>();
        store.add(buildAddress(1L, 200L, 1, LocalDateTime.of(2026, 4, 8, 9, 0)));
        store.add(buildAddress(2L, 200L, 0, LocalDateTime.of(2026, 4, 8, 10, 0)));
        setField(service, "customerAddressMapper", createAddressMapperProxy(store));
        setField(service, "cUserService", createUserServiceProxy(200L));

        service.setDefault(2L);

        Assert.assertEquals(Integer.valueOf(0), findAddress(store, 1L).getIsDefault());
        Assert.assertEquals(Integer.valueOf(1), findAddress(store, 2L).getIsDefault());
    }

    /**buildCreateDTO 业务数据，统一收口字段清洗、默认值处理和返回对象组装规则。
@return 处理后的业务结果。*/
    private CustomerAddressCreateDTO buildCreateDTO() {
        CustomerAddressCreateDTO dto = new CustomerAddressCreateDTO();
        dto.setContactName("张三");
        dto.setContactMobile("13800138000");
        dto.setProvince("广东省");
        dto.setCity("深圳市");
        dto.setCounty(null);
        dto.setDetailAddress("南山区科技园1号");
        return dto;
    }

    /**buildAddress 业务数据，统一收口字段清洗、默认值处理和返回对象组装规则。
@param id 主键ID。
@param customerId 客户ID。
@param isDefault isDefault 字段参数。
@param updateTime 更新时间参数。
@return 处理后的业务结果。*/
    private CustomerAddress buildAddress(Long id, Long customerId, Integer isDefault, LocalDateTime updateTime) {
        CustomerAddress address = new CustomerAddress();
        address.setId(id);
        address.setCustomerId(customerId);
        address.setContactName("联系人" + id);
        address.setContactMobile("1380013800" + (id % 10));
        address.setProvince("广东省");
        address.setCity("深圳市");
        address.setCounty("南山区");
        address.setDetailAddress("科技园" + id + "号");
        address.setIsDefault(isDefault);
        address.setUpdateTime(updateTime);
        return address;
    }

    /**findAddress 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@param store 业务数据列表，用于批量处理或返回组装。
@param id 主键ID。
@return 查询或解析得到的业务对象。*/
    private CustomerAddress findAddress(List<CustomerAddress> store, Long id) {
        for (CustomerAddress item : store) {
            if (id.equals(item.getId())) {
                return item;
            }
        }
        return null;
    }

    /**createAddressMapperProxy 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@param store 业务数据列表，用于批量处理或返回组装。
@return 新增或保存后的业务标识或处理结果。*/
    private CustomerAddressMapper createAddressMapperProxy(List<CustomerAddress> store) {
        InvocationHandler handler = new InvocationHandler() {
            /**nextId 字段，用于当前类内部业务处理。*/
            private long nextId = store.size() + 1L;

            /**invoke 处理逻辑，服务于当前类的业务编排和数据转换。
@param proxy proxy 字段参数。
@param method method 字段参数。
@param args args 字段参数。
@return 处理后的业务结果。*/
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectList".equals(method.getName())) {
                    return new ArrayList<>(store);
                }
                if ("selectById".equals(method.getName())) {
                    Long id = (Long) args[0];
                    return findAddress(store, id);
                }
                if ("insert".equals(method.getName())) {
                    CustomerAddress inserting = (CustomerAddress) args[0];
                    inserting.setId(nextId++);
                    if (inserting.getCreateTime() == null) {
                        inserting.setCreateTime(LocalDateTime.now());
                    }
                    if (inserting.getUpdateTime() == null) {
                        inserting.setUpdateTime(LocalDateTime.now());
                    }
                    store.add(inserting);
                    return 1;
                }
                if ("updateById".equals(method.getName())) {
                    CustomerAddress updating = (CustomerAddress) args[0];
                    CustomerAddress existing = findAddress(store, updating.getId());
                    if (existing == null) {
                        return 0;
                    }
                    if (updating.getCustomerId() != null) {
                        existing.setCustomerId(updating.getCustomerId());
                    }
                    if (updating.getContactName() != null) {
                        existing.setContactName(updating.getContactName());
                    }
                    if (updating.getContactMobile() != null) {
                        existing.setContactMobile(updating.getContactMobile());
                    }
                    if (updating.getProvince() != null) {
                        existing.setProvince(updating.getProvince());
                    }
                    if (updating.getCity() != null) {
                        existing.setCity(updating.getCity());
                    }
                    existing.setCounty(updating.getCounty());
                    if (updating.getDetailAddress() != null) {
                        existing.setDetailAddress(updating.getDetailAddress());
                    }
                    if (updating.getIsDefault() != null) {
                        existing.setIsDefault(updating.getIsDefault());
                    }
                    existing.setUpdateTime(LocalDateTime.now());
                    return 1;
                }
                if ("deleteById".equals(method.getName())) {
                    Long id = (Long) args[0];
                    CustomerAddress target = findAddress(store, id);
                    if (target != null) {
                        store.remove(target);
                        return 1;
                    }
                    return 0;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (CustomerAddressMapper) Proxy.newProxyInstance(
                CustomerAddressMapper.class.getClassLoader(),
                new Class<?>[]{CustomerAddressMapper.class},
                handler
        );
    }

    /**createUserServiceProxy 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@param customerId 客户ID。
@return 新增或保存后的业务标识或处理结果。*/
    private ICUserService createUserServiceProxy(Long customerId) {
        InvocationHandler handler = new InvocationHandler() {
            /**invoke 处理逻辑，服务于当前类的业务编排和数据转换。
@param proxy proxy 字段参数。
@param method method 字段参数。
@param args args 字段参数。
@return 处理后的业务结果。*/
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("getCurrentUser".equals(method.getName())) {
                    CUser user = new CUser();
                    user.setId(customerId);
                    user.setStatus(1);
                    return user;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (ICUserService) Proxy.newProxyInstance(
                ICUserService.class.getClassLoader(),
                new Class<?>[]{ICUserService.class},
                handler
        );
    }

    /**setField 处理逻辑，服务于当前类的业务编排和数据转换。
@param target target 字段参数。
@param fieldName 名称文本，用于展示、匹配或保存业务对象名称。
@param value value 字段参数。*/
    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = CustomerAddressServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /**defaultValue 处理逻辑，服务于当前类的业务编排和数据转换。
@param returnType returnType 字段参数。
@return 处理后的业务结果。*/
    private Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (boolean.class.equals(returnType)) {
            return false;
        }
        if (char.class.equals(returnType)) {
            return '\0';
        }
        if (byte.class.equals(returnType) || short.class.equals(returnType)
                || int.class.equals(returnType) || long.class.equals(returnType)) {
            return 0;
        }
        if (float.class.equals(returnType)) {
            return 0F;
        }
        if (double.class.equals(returnType)) {
            return 0D;
        }
        return null;
    }
}
