package com.jasic.aftersales.customer.service.impl;

import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.customer.domain.vo.CustomerBarcodeInfoVO;
import com.jasic.aftersales.customer.domain.vo.CustomerServiceCompanyOptionVO;
import com.jasic.aftersales.system.domain.entity.FirstSecondRelation;
import com.jasic.aftersales.system.domain.entity.HqFirstContract;
import com.jasic.aftersales.system.domain.entity.MachineBarcode;
import com.jasic.aftersales.system.domain.entity.SysCompany;
import com.jasic.aftersales.system.mapper.FirstSecondRelationMapper;
import com.jasic.aftersales.system.mapper.HqFirstContractMapper;
import com.jasic.aftersales.system.mapper.MachineBarcodeMapper;
import com.jasic.aftersales.system.mapper.SysCompanyMapper;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * C端工单服务测试
 *
 * @author Codex
 * @date 2026/04/01
 */
public class CustomerWorkOrderServiceImplTest {

    @Test
    public void shouldResolveSingleHqForCreate() throws Exception {
        CustomerWorkOrderServiceImpl service = new CustomerWorkOrderServiceImpl();
        setField(service, "sysCompanyMapper", createCompanyMapperProxy(buildFirstCompany(), buildHqCompany()));
        setField(service, "hqFirstContractMapper", createHqFirstContractMapperProxy(buildContracts(21L)));
        setField(service, "firstSecondRelationMapper", createFirstSecondRelationMapperProxy());
        setField(service, "machineBarcodeMapper", createMachineBarcodeMapperProxy());

        Long hqCompanyId = invokeResolveCreateHqCompanyId(service, "JASIC-001", buildFirstCompany());

        Assert.assertEquals(Long.valueOf(21L), hqCompanyId);
    }

    @Test
    public void shouldRejectWhenCreateHqHasMultipleCandidates() throws Exception {
        CustomerWorkOrderServiceImpl service = new CustomerWorkOrderServiceImpl();
        setField(service, "sysCompanyMapper", createCompanyMapperProxy(buildFirstCompany(), buildHqCompany(), buildHqCompanyB()));
        setField(service, "hqFirstContractMapper", createHqFirstContractMapperProxy(buildContracts(21L, 22L)));
        setField(service, "firstSecondRelationMapper", createFirstSecondRelationMapperProxy());
        setField(service, "machineBarcodeMapper", createMachineBarcodeMapperProxy());

        try {
            invokeResolveCreateHqCompanyId(service, "JASIC-001", buildFirstCompany());
            Assert.fail("预期应拒绝多个总部候选");
        } catch (InvocationTargetException ex) {
            Throwable target = ex.getTargetException();
            Assert.assertTrue(target instanceof ServiceException);
            Assert.assertEquals("当前机器条码归属总部存在多个候选项，暂无法自动识别，请联系管理员完善条码归属配置", target.getMessage());
        }
    }

    @Test
    public void shouldPreferBarcodeArchiveWhenResolvingCreateHq() throws Exception {
        CustomerWorkOrderServiceImpl service = new CustomerWorkOrderServiceImpl();
        setField(service, "sysCompanyMapper", createCompanyMapperProxy(buildFirstCompany(), buildHqCompany(), buildHqCompanyB()));
        setField(service, "hqFirstContractMapper", createHqFirstContractMapperProxy(buildContracts(21L, 22L)));
        setField(service, "firstSecondRelationMapper", createFirstSecondRelationMapperProxy());
        setField(service, "machineBarcodeMapper", createMachineBarcodeMapperProxy(buildMachineBarcode(21L)));

        Long hqCompanyId = invokeResolveCreateHqCompanyId(service, "JASIC-001", buildFirstCompany());

        Assert.assertEquals(Long.valueOf(21L), hqCompanyId);
    }

    @Test
    public void shouldReturnBarcodeInfoFromArchive() throws Exception {
        CustomerWorkOrderServiceImpl service = new CustomerWorkOrderServiceImpl();
        setField(service, "sysCompanyMapper", createCompanyMapperProxy(buildHqCompany()));
        setField(service, "machineBarcodeMapper", createMachineBarcodeMapperProxy(buildMachineBarcode(21L)));

        CustomerBarcodeInfoVO barcodeInfo = service.getBarcodeInfo("JASIC-001");

        Assert.assertEquals("JASIC-001", barcodeInfo.getBarcode());
        Assert.assertEquals("P-100", barcodeInfo.getProductCode());
        Assert.assertEquals("MODEL-A", barcodeInfo.getProductModel());
        Assert.assertEquals("JASIC", barcodeInfo.getBrandCode());
        Assert.assertEquals("IN_WARRANTY", barcodeInfo.getWarrantyStatus());
        Assert.assertEquals(Long.valueOf(21L), barcodeInfo.getHqCompanyId());
        Assert.assertEquals(buildHqCompany().getCompanyName(), barcodeInfo.getHqCompanyName());
    }

    @Test
    public void shouldSortNearbyServiceCompaniesByDistance() throws Exception {
        CustomerWorkOrderServiceImpl service = new CustomerWorkOrderServiceImpl();
        setField(service, "sysCompanyMapper", createCompanyMapperProxy(
                buildNearbyCompany(31L, "一级网点A", "FIRST", "113.0000", "23.0000"),
                buildNearbyCompany(32L, "二级网点B", "SECOND", "113.0500", "23.0200"),
                buildNearbyCompany(33L, "一级网点C", "FIRST", null, null)
        ));

        List<CustomerServiceCompanyOptionVO> options = service.listNearbyServiceCompanyOptions(
                new BigDecimal("113.0010"), new BigDecimal("23.0010"), 10);

        Assert.assertEquals(3, options.size());
        Assert.assertEquals(Long.valueOf(31L), options.get(0).getId());
        Assert.assertEquals(Long.valueOf(32L), options.get(1).getId());
        Assert.assertEquals(Long.valueOf(33L), options.get(2).getId());
        Assert.assertNotNull(options.get(0).getDistanceKm());
        Assert.assertNotNull(options.get(1).getDistanceKm());
        Assert.assertNull(options.get(2).getDistanceKm());
    }

    @Test
    public void shouldRejectInvalidNearbyCoordinate() throws Exception {
        CustomerWorkOrderServiceImpl service = new CustomerWorkOrderServiceImpl();
        setField(service, "sysCompanyMapper", createCompanyMapperProxy());

        try {
            service.listNearbyServiceCompanyOptions(new BigDecimal("181"), new BigDecimal("23"), 10);
            Assert.fail("预期应拒绝无效经度");
        } catch (ServiceException ex) {
            Assert.assertEquals("经度超出有效范围", ex.getMessage());
        }
    }

    private SysCompany buildFirstCompany() {
        SysCompany company = new SysCompany();
        company.setId(11L);
        company.setTypeCode("FIRST");
        company.setStatus(1);
        company.setCompanyName("一级网点A");
        return company;
    }

    private SysCompany buildHqCompany() {
        SysCompany company = new SysCompany();
        company.setId(21L);
        company.setTypeCode("HQ_A");
        company.setStatus(1);
        company.setCompanyName("总部A");
        return company;
    }

    private SysCompany buildHqCompanyB() {
        SysCompany company = new SysCompany();
        company.setId(22L);
        company.setTypeCode("HQ_B");
        company.setStatus(1);
        company.setCompanyName("总部B");
        return company;
    }

    private SysCompany buildNearbyCompany(Long id, String companyName, String typeCode,
                                          String longitude, String latitude) {
        SysCompany company = new SysCompany();
        company.setId(id);
        company.setTypeCode(typeCode);
        company.setStatus(1);
        company.setCompanyName(companyName);
        if (longitude != null) {
            company.setLongitude(new BigDecimal(longitude));
        }
        if (latitude != null) {
            company.setLatitude(new BigDecimal(latitude));
        }
        return company;
    }

    private MachineBarcode buildMachineBarcode(Long hqCompanyId) {
        MachineBarcode barcode = new MachineBarcode();
        barcode.setId(1L);
        barcode.setBarcode("JASIC-001");
        barcode.setHqCompanyId(hqCompanyId);
        barcode.setProductCode("P-100");
        barcode.setProductModel("MODEL-A");
        barcode.setBrandCode("JASIC");
        barcode.setWarrantyStatus("IN_WARRANTY");
        barcode.setStatus(1);
        return barcode;
    }

    private SysCompanyMapper createCompanyMapperProxy(SysCompany... companies) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectById".equals(method.getName())) {
                    Long companyId = (Long) args[0];
                    for (SysCompany company : companies) {
                        if (company.getId().equals(companyId)) {
                            return company;
                        }
                    }
                }
                if ("selectList".equals(method.getName())) {
                    List<SysCompany> result = new ArrayList<>();
                    Collections.addAll(result, companies);
                    return result;
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

    private HqFirstContractMapper createHqFirstContractMapperProxy(java.util.List<HqFirstContract> contracts) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectList".equals(method.getName())) {
                    return contracts;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (HqFirstContractMapper) Proxy.newProxyInstance(
                HqFirstContractMapper.class.getClassLoader(),
                new Class<?>[]{HqFirstContractMapper.class},
                handler
        );
    }

    private MachineBarcodeMapper createMachineBarcodeMapperProxy(MachineBarcode... barcodes) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectOne".equals(method.getName())) {
                    return barcodes.length == 0 ? null : barcodes[0];
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (MachineBarcodeMapper) Proxy.newProxyInstance(
                MachineBarcodeMapper.class.getClassLoader(),
                new Class<?>[]{MachineBarcodeMapper.class},
                handler
        );
    }

    private java.util.List<HqFirstContract> buildContracts(Long... hqCompanyIds) {
        java.util.List<HqFirstContract> result = new java.util.ArrayList<>();
        for (Long hqCompanyId : hqCompanyIds) {
            HqFirstContract contract = new HqFirstContract();
            contract.setFirstCompanyId(11L);
            contract.setHqCompanyId(hqCompanyId);
            contract.setStatus(1);
            result.add(contract);
        }
        return result;
    }

    private FirstSecondRelationMapper createFirstSecondRelationMapperProxy() {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectList".equals(method.getName())) {
                    return Collections.<FirstSecondRelation>emptyList();
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (FirstSecondRelationMapper) Proxy.newProxyInstance(
                FirstSecondRelationMapper.class.getClassLoader(),
                new Class<?>[]{FirstSecondRelationMapper.class},
                handler
        );
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = CustomerWorkOrderServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Long invokeResolveCreateHqCompanyId(CustomerWorkOrderServiceImpl service, String barcode,
                                                SysCompany company) throws Exception {
        Method method = CustomerWorkOrderServiceImpl.class
                .getDeclaredMethod("resolveCreateHqCompanyId", String.class, SysCompany.class);
        method.setAccessible(true);
        return (Long) method.invoke(service, barcode, company);
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
