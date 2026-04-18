package com.jasic.aftersales.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.jasic.aftersales.system.domain.entity.CrmBizCompanySnapshot;
import com.jasic.aftersales.system.domain.query.CrmBizCompanySnapshotQuery;
import com.jasic.aftersales.system.mapper.CrmBizCompanySnapshotMapper;
import org.junit.Assert;
import org.junit.Test;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * CRM 公司快照服务测试
 */
public class CrmBizCompanySnapshotServiceImplTest {

    @Test
    public void shouldOrderExternalCompanyListBySapCompanyCodeAsc() throws Exception {
        CrmBizCompanySnapshotServiceImpl service = new CrmBizCompanySnapshotServiceImpl();
        SnapshotMapperState state = new SnapshotMapperState();
        setField(service, "crmBizCompanySnapshotMapper", createSnapshotMapperProxy(state));
        initTableInfo();

        CrmBizCompanySnapshotQuery query = new CrmBizCompanySnapshotQuery();
        query.setPageNum(1);
        query.setPageSize(10);

        service.listPage(query);

        Assert.assertNotNull(state.wrapper);
        String sqlSegment = state.wrapper.getSqlSegment();
        Assert.assertTrue(sqlSegment.contains("ORDER BY sap_company_code ASC,cust_id ASC"));
    }

    private void initTableInfo() {
        if (TableInfoHelper.getTableInfo(CrmBizCompanySnapshot.class) != null) {
            return;
        }
        Configuration configuration = new Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "test");
        assistant.setCurrentNamespace(CrmBizCompanySnapshotMapper.class.getName());
        TableInfoHelper.initTableInfo(assistant, CrmBizCompanySnapshot.class);
    }

    private CrmBizCompanySnapshotMapper createSnapshotMapperProxy(SnapshotMapperState state) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectPage".equals(method.getName())) {
                    state.wrapper = (LambdaQueryWrapper<CrmBizCompanySnapshot>) args[1];
                    return args[0];
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (CrmBizCompanySnapshotMapper) Proxy.newProxyInstance(
                CrmBizCompanySnapshotMapper.class.getClassLoader(),
                new Class<?>[]{CrmBizCompanySnapshotMapper.class},
                handler
        );
    }

    private Object defaultValue(Class<?> type) {
        if (type == null || !type.isPrimitive()) {
            return null;
        }
        if (boolean.class.equals(type)) {
            return false;
        }
        if (byte.class.equals(type)) {
            return (byte) 0;
        }
        if (short.class.equals(type)) {
            return (short) 0;
        }
        if (int.class.equals(type)) {
            return 0;
        }
        if (long.class.equals(type)) {
            return 0L;
        }
        if (float.class.equals(type)) {
            return 0F;
        }
        if (double.class.equals(type)) {
            return 0D;
        }
        if (char.class.equals(type)) {
            return '\0';
        }
        return null;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static class SnapshotMapperState {
        private LambdaQueryWrapper<CrmBizCompanySnapshot> wrapper;
    }
}
