package com.jasic.aftersales.system.service.impl;

import com.jasic.aftersales.system.domain.entity.FaultRepairConfig;
import com.jasic.aftersales.system.domain.entity.FaultRepairConfigFault;
import com.jasic.aftersales.system.domain.entity.FaultRepairConfigOption;
import com.jasic.aftersales.system.domain.entity.SysCompany;
import com.jasic.aftersales.system.domain.vo.WorkOrderRepairFaultOptionVO;
import com.jasic.aftersales.system.mapper.FaultRepairConfigFaultMapper;
import com.jasic.aftersales.system.mapper.FaultRepairConfigMapper;
import com.jasic.aftersales.system.mapper.FaultRepairConfigOptionMapper;
import com.jasic.aftersales.system.mapper.SysCompanyMapper;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 故障与维修配置服务测试。
 *
 * @author Codex
 * @date 2026/04/01
 */
public class FaultRepairConfigServiceImplTest {

    @Test
    public void shouldReturnRepairFaultOptionsForExactProductMatch() throws Exception {
        FaultRepairConfigServiceImpl service = new FaultRepairConfigServiceImpl();
        FaultRepairConfig exactConfig = buildConfig(1L, 9L, "P-100", "M-200");
        FaultRepairConfig fallbackConfig = buildConfig(2L, 9L, "P-100", null);

        setField(service, "faultRepairConfigMapper",
                createConfigMapperProxy(Arrays.asList(fallbackConfig, exactConfig)));
        setField(service, "faultRepairConfigFaultMapper",
                createFaultMapperProxy(Arrays.asList(
                        buildFault(11L, 1L, "电源故障"),
                        buildFault(21L, 2L, "通用故障")
                )));
        setField(service, "faultRepairConfigOptionMapper",
                createOptionMapperProxy(Arrays.asList(
                        buildOption(101L, 11L, "更换电源板"),
                        buildOption(102L, 11L, "清洁接线"),
                        buildOption(201L, 21L, "检查线路")
                )));
        setField(service, "sysCompanyMapper",
                createCompanyMapperProxy(Collections.singletonList(buildCompany(9L, "总部A"))));

        List<WorkOrderRepairFaultOptionVO> result = service.listRepairFaultOptions(9L, "P-100", "M-200");

        Assert.assertEquals(1, result.size());
        Assert.assertEquals("电源故障", result.get(0).getFaultDesc());
        Assert.assertEquals(Arrays.asList("更换电源板", "清洁接线"), result.get(0).getRepairOptions());
    }

    @Test
    public void shouldReturnEmptyRepairFaultOptionsWhenNoConfigMatched() throws Exception {
        FaultRepairConfigServiceImpl service = new FaultRepairConfigServiceImpl();
        setField(service, "faultRepairConfigMapper",
                createConfigMapperProxy(Collections.singletonList(buildConfig(1L, 9L, "P-100", "M-200"))));

        List<WorkOrderRepairFaultOptionVO> result = service.listRepairFaultOptions(9L, "P-999", "M-999");

        Assert.assertTrue(result.isEmpty());
    }

    private FaultRepairConfigMapper createConfigMapperProxy(List<FaultRepairConfig> configs) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectList".equals(method.getName())) {
                    return configs;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (FaultRepairConfigMapper) Proxy.newProxyInstance(
                FaultRepairConfigMapper.class.getClassLoader(),
                new Class<?>[]{FaultRepairConfigMapper.class},
                handler
        );
    }

    private FaultRepairConfigFaultMapper createFaultMapperProxy(List<FaultRepairConfigFault> faults) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectList".equals(method.getName())) {
                    return faults;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (FaultRepairConfigFaultMapper) Proxy.newProxyInstance(
                FaultRepairConfigFaultMapper.class.getClassLoader(),
                new Class<?>[]{FaultRepairConfigFaultMapper.class},
                handler
        );
    }

    private FaultRepairConfigOptionMapper createOptionMapperProxy(List<FaultRepairConfigOption> options) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectList".equals(method.getName())) {
                    return options;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (FaultRepairConfigOptionMapper) Proxy.newProxyInstance(
                FaultRepairConfigOptionMapper.class.getClassLoader(),
                new Class<?>[]{FaultRepairConfigOptionMapper.class},
                handler
        );
    }

    private SysCompanyMapper createCompanyMapperProxy(List<SysCompany> companies) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectList".equals(method.getName())) {
                    return companies;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (SysCompanyMapper) Proxy.newProxyInstance(
                SysCompanyMapper.class.getClassLoader(),
                new Class<?>[]{SysCompanyMapper.class},
                handler
        );
    }

    private FaultRepairConfig buildConfig(Long id, Long companyId, String productCode, String productModel) {
        FaultRepairConfig config = new FaultRepairConfig();
        config.setId(id);
        config.setCompanyId(companyId);
        config.setProductCode(productCode);
        config.setProductModel(productModel);
        config.setStatus(1);
        return config;
    }

    private FaultRepairConfigFault buildFault(Long id, Long configId, String faultDesc) {
        FaultRepairConfigFault fault = new FaultRepairConfigFault();
        fault.setId(id);
        fault.setConfigId(configId);
        fault.setFaultDesc(faultDesc);
        fault.setSortNum(1);
        return fault;
    }

    private FaultRepairConfigOption buildOption(Long id, Long faultId, String repairDesc) {
        FaultRepairConfigOption option = new FaultRepairConfigOption();
        option.setId(id);
        option.setFaultId(faultId);
        option.setRepairDesc(repairDesc);
        option.setSortNum(1);
        return option;
    }

    private SysCompany buildCompany(Long id, String companyName) {
        SysCompany company = new SysCompany();
        company.setId(id);
        company.setCompanyName(companyName);
        return company;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = FaultRepairConfigServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

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
