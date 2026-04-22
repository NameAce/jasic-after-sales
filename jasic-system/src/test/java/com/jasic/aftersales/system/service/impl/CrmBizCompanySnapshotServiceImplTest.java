package com.jasic.aftersales.system.service.impl;

import com.jasic.aftersales.system.domain.entity.CrmBizCompanySnapshot;
import com.jasic.aftersales.system.domain.vo.CrmBizCompanyImportPreviewVO;
import com.jasic.aftersales.system.mapper.CrmBizCompanySnapshotMapper;
import com.jasic.aftersales.system.mapper.SysCompanyMapper;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * CRM 公司快照服务测试
 */
public class CrmBizCompanySnapshotServiceImplTest {

    @Test
    public void shouldBuildImportPreviewWithCompanyCodeAdminAndUserContact() throws Exception {
        CrmBizCompanySnapshotServiceImpl service = new CrmBizCompanySnapshotServiceImpl();
        CrmBizCompanySnapshot snapshot = new CrmBizCompanySnapshot();
        snapshot.setCustId(1001L);
        snapshot.setCustName("demo-company");
        snapshot.setCompanyShortName("demo");
        snapshot.setSapCompanyCode("SZ001");
        snapshot.setCustRage(0);
        snapshot.setJuristicCustId("demo-contact");
        snapshot.setGroupContactPhone("13800138000");
        snapshot.setCellphone("13800138000");
        snapshot.setCompanyAddress("demo-address");
        snapshot.setProvinceName("GD");
        snapshot.setCityName("SZ");
        snapshot.setDistrictName("NS");
        snapshot.setCustState(1);

        setField(service, "crmBizCompanySnapshotMapper", createSnapshotMapper(snapshot));
        setField(service, "sysCompanyMapper", createCompanyMapper());

        CrmBizCompanyImportPreviewVO preview = service.getImportPreview(1001L);

        Assert.assertNotNull(preview);
        Assert.assertEquals(Long.valueOf(1001L), preview.getCustId());
        Assert.assertEquals("SZ001", preview.getCompanyCode());
        Assert.assertEquals("SZ001", preview.getAdminUsername());
        Assert.assertEquals("demo-contact", preview.getContactName());
        Assert.assertEquals("13800138000", preview.getContactPhone());
        Assert.assertEquals("SITE_FIRST", preview.getTypeCode());
        Assert.assertEquals(Integer.valueOf(1), preview.getStatus());
        Assert.assertEquals(Boolean.TRUE, preview.getCanImport());
    }

    private CrmBizCompanySnapshotMapper createSnapshotMapper(CrmBizCompanySnapshot snapshot) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectOne".equals(method.getName())) {
                    return snapshot;
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

    private SysCompanyMapper createCompanyMapper() {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectOne".equals(method.getName())) {
                    return null;
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

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = CrmBizCompanySnapshotServiceImpl.class.getDeclaredField(fieldName);
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
