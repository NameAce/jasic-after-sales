package com.jasic.aftersales.system.service.impl;

import com.jasic.aftersales.system.domain.entity.CrmBizCompanySnapshot;
import com.jasic.aftersales.system.domain.vo.CrmBizCompanyImportPreviewVO;
import com.jasic.aftersales.system.mapper.CrmBizCompanySnapshotMapper;
import com.jasic.aftersales.system.mapper.SysCompanyMapper;
import com.jasic.aftersales.system.service.ISysAreaService;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/*** CRM 公司快照服务测试

@author Zoro*/
public class CrmBizCompanySnapshotServiceImplTest {

    /**验证BuildImportPreviewWithCompanyCodeAdminAndUserContact，保证相关业务规则在回归场景下保持稳定。*/
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
        setField(service, "sysAreaService", createAreaService());

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

    /**createSnapshotMapper 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@param snapshot snapshot 字段参数。
@return 新增或保存后的业务标识或处理结果。*/
    private CrmBizCompanySnapshotMapper createSnapshotMapper(CrmBizCompanySnapshot snapshot) {
        InvocationHandler handler = new InvocationHandler() {
            /**invoke 处理逻辑，服务于当前类的业务编排和数据转换。
@param proxy proxy 字段参数。
@param method method 字段参数。
@param args args 字段参数。
@return 处理后的业务结果。*/
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

    /**createAreaService 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@return 新增或保存后的业务标识或处理结果。*/
    private ISysAreaService createAreaService() {
        InvocationHandler handler = new InvocationHandler() {
            /**invoke 处理逻辑，服务于当前类的业务编排和数据转换。
@param proxy proxy 字段参数。
@param method method 字段参数。
@param args args 字段参数。
@return 处理后的业务结果。*/
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("matchRegion".equals(method.getName())) {
                    return new ISysAreaService.AreaMatchResult(null, null, null);
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (ISysAreaService) Proxy.newProxyInstance(
                ISysAreaService.class.getClassLoader(),
                new Class<?>[]{ISysAreaService.class},
                handler
        );
    }

    /**createCompanyMapper 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@return 新增或保存后的业务标识或处理结果。*/
    private SysCompanyMapper createCompanyMapper() {
        InvocationHandler handler = new InvocationHandler() {
            /**invoke 处理逻辑，服务于当前类的业务编排和数据转换。
@param proxy proxy 字段参数。
@param method method 字段参数。
@param args args 字段参数。
@return 处理后的业务结果。*/
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

    /**setField 处理逻辑，服务于当前类的业务编排和数据转换。
@param target target 字段参数。
@param fieldName 名称文本，用于展示、匹配或保存业务对象名称。
@param value value 字段参数。*/
    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = CrmBizCompanySnapshotServiceImpl.class.getDeclaredField(fieldName);
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
