package com.jasic.aftersales.customer.service.impl;

import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.customer.domain.dto.CustomerWorkOrderCreateDTO;
import com.jasic.aftersales.customer.domain.entity.CUser;
import com.jasic.aftersales.customer.domain.vo.CustomerBarcodeInfoVO;
import com.jasic.aftersales.customer.domain.vo.CustomerNearbyServiceCompanyVO;
import com.jasic.aftersales.customer.domain.vo.CustomerServiceCompanyOptionVO;
import com.jasic.aftersales.system.domain.entity.FirstSecondRelation;
import com.jasic.aftersales.system.domain.entity.HqFirstContract;
import com.jasic.aftersales.system.domain.entity.MachineBarcode;
import com.jasic.aftersales.system.domain.entity.SysCompany;
import com.jasic.aftersales.system.domain.vo.WorkOrderRepairFaultOptionVO;
import com.jasic.aftersales.system.mapper.FirstSecondRelationMapper;
import com.jasic.aftersales.system.mapper.HqFirstContractMapper;
import com.jasic.aftersales.system.mapper.MachineBarcodeMapper;
import com.jasic.aftersales.system.mapper.SysCompanyMapper;
import com.jasic.aftersales.system.service.IFaultRepairConfigService;
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
            Assert.fail("Expected multiple HQ candidates to be rejected");
        } catch (InvocationTargetException ex) {
            Assert.assertTrue(ex.getTargetException() instanceof ServiceException);
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
    public void shouldReturnBarcodeInfoFromArchive() {
        CustomerWorkOrderServiceImpl service = new CustomerWorkOrderServiceImpl();
        setFieldQuietly(service, "sysCompanyMapper", createCompanyMapperProxy(buildHqCompany()));
        setFieldQuietly(service, "machineBarcodeMapper", createMachineBarcodeMapperProxy(buildMachineBarcode(21L)));
        setFieldQuietly(service, "faultRepairConfigService", createFaultRepairConfigServiceProxy("Fault A", "Fault B"));

        CustomerBarcodeInfoVO barcodeInfo = service.getBarcodeInfo("JASIC-001");

        Assert.assertEquals("JASIC-001", barcodeInfo.getBarcode());
        Assert.assertEquals("P-100", barcodeInfo.getProductCode());
        Assert.assertEquals("ZX7", barcodeInfo.getProductName());
        Assert.assertEquals("MODEL-A", barcodeInfo.getProductModel());
        Assert.assertEquals("M-001", barcodeInfo.getMachineNo());
        Assert.assertEquals("JASIC", barcodeInfo.getBrandCode());
        Assert.assertEquals("IN_WARRANTY", barcodeInfo.getWarrantyStatus());
        Assert.assertEquals(Long.valueOf(21L), barcodeInfo.getHqCompanyId());
        Assert.assertEquals(buildHqCompany().getCompanyName(), barcodeInfo.getHqCompanyName());
        Assert.assertEquals(3, barcodeInfo.getFaultOptions().size());
        Assert.assertEquals(barcodeInfo.getOtherFaultLabel(),
                barcodeInfo.getFaultOptions().get(barcodeInfo.getFaultOptions().size() - 1));
    }

    @Test
    public void shouldFallbackLegacyFaultDescToOtherFaultWhenNoConfig() throws Exception {
        CustomerWorkOrderServiceImpl service = new CustomerWorkOrderServiceImpl();
        setField(service, "faultRepairConfigService", createFaultRepairConfigServiceProxy());

        CustomerWorkOrderCreateDTO dto = new CustomerWorkOrderCreateDTO();
        dto.setFaultDesc("Legacy fault");

        Object selection = invokeResolveCustomerFaultSelection(service, dto, 21L, "P-100", "MODEL-A");

        Assert.assertNotNull(invokeFaultSelectionGetter(selection, "getFaultDesc"));
        Assert.assertEquals("Legacy fault", invokeFaultSelectionGetter(selection, "getFaultRemark"));
    }

    @Test
    public void shouldRejectFaultOutsideConfiguredOptions() throws Exception {
        CustomerWorkOrderServiceImpl service = new CustomerWorkOrderServiceImpl();
        setField(service, "faultRepairConfigService", createFaultRepairConfigServiceProxy("Allowed"));

        CustomerWorkOrderCreateDTO dto = new CustomerWorkOrderCreateDTO();
        dto.setFaultItems(Collections.singletonList("Blocked"));

        try {
            invokeResolveCustomerFaultSelection(service, dto, 21L, "P-100", "MODEL-A");
            Assert.fail("Expected configured fault options to reject blocked values");
        } catch (InvocationTargetException ex) {
            Assert.assertTrue(ex.getTargetException() instanceof ServiceException);
        }
    }

    @Test
    public void shouldSortNearbyServiceCompaniesByDistance() {
        CustomerWorkOrderServiceImpl service = new CustomerWorkOrderServiceImpl();
        setFieldQuietly(service, "sysCompanyMapper", createCompanyMapperProxy(
                buildNearbyCompany(31L, "Service A", "FIRST", "113.0000", "23.0000"),
                buildNearbyCompany(32L, "Service B", "SECOND", "113.0500", "23.0200"),
                buildNearbyCompany(33L, "Service C", "FIRST", null, null)
        ));

        List<CustomerNearbyServiceCompanyVO> options = service.listNearbyServiceCompanyOptions(
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
    public void shouldRejectInvalidNearbyCoordinate() {
        CustomerWorkOrderServiceImpl service = new CustomerWorkOrderServiceImpl();
        setFieldQuietly(service, "sysCompanyMapper", createCompanyMapperProxy());

        try {
            service.listNearbyServiceCompanyOptions(new BigDecimal("181"), new BigDecimal("23"), 10);
            Assert.fail("Expected invalid coordinate to be rejected");
        } catch (ServiceException ex) {
            Assert.assertNotNull(ex.getMessage());
        }
    }

    @Test
    public void shouldReturnServiceCompanyOptionsWithoutLocationFields() {
        CustomerWorkOrderServiceImpl service = new CustomerWorkOrderServiceImpl();
        setFieldQuietly(service, "sysCompanyMapper", createCompanyMapperProxy(
                buildNearbyCompany(31L, "Service A", "FIRST", "113.0000", "23.0000"),
                buildNearbyCompany(32L, "Service B", "SECOND", "113.0500", "23.0200")
        ));

        List<CustomerServiceCompanyOptionVO> options = service.listServiceCompanyOptions();

        Assert.assertEquals(2, options.size());
        Assert.assertEquals("0755-00000031", options.get(0).getContactPhone());
        Assert.assertTrue(options.get(0).getAddress().contains("Service"));
    }

    @Test
    public void shouldPreferNicknameForCustomerName() throws Exception {
        CustomerWorkOrderServiceImpl service = new CustomerWorkOrderServiceImpl();
        CUser customer = new CUser();
        customer.setNickname("nick-name");
        customer.setPhone("13800138000");

        String customerName = invokeResolveCustomerName(service, customer);

        Assert.assertEquals("nick-name", customerName);
    }

    @Test
    public void shouldFallbackPhoneWhenNicknameMissing() throws Exception {
        CustomerWorkOrderServiceImpl service = new CustomerWorkOrderServiceImpl();
        CUser customer = new CUser();
        customer.setNickname("   ");
        customer.setPhone("13800138000");

        String customerName = invokeResolveCustomerName(service, customer);

        Assert.assertEquals("13800138000", customerName);
    }

    private SysCompany buildFirstCompany() {
        SysCompany company = new SysCompany();
        company.setId(11L);
        company.setTypeCode("FIRST");
        company.setStatus(1);
        company.setCompanyName("First Service");
        return company;
    }

    private SysCompany buildHqCompany() {
        SysCompany company = new SysCompany();
        company.setId(21L);
        company.setTypeCode("HQ_A");
        company.setStatus(1);
        company.setCompanyName("HQ A");
        return company;
    }

    private SysCompany buildHqCompanyB() {
        SysCompany company = new SysCompany();
        company.setId(22L);
        company.setTypeCode("HQ_B");
        company.setStatus(1);
        company.setCompanyName("HQ B");
        return company;
    }

    private SysCompany buildNearbyCompany(Long id, String companyName, String typeCode,
                                          String longitude, String latitude) {
        SysCompany company = new SysCompany();
        company.setId(id);
        company.setTypeCode(typeCode);
        company.setStatus(1);
        company.setCompanyName(companyName);
        company.setContactPhone("0755-000000" + id);
        company.setAddress("Service Address " + id);
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
        barcode.setProductName("ZX7");
        barcode.setProductModel("MODEL-A");
        barcode.setMachineNo("M-001");
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

    private HqFirstContractMapper createHqFirstContractMapperProxy(List<HqFirstContract> contracts) {
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

    private IFaultRepairConfigService createFaultRepairConfigServiceProxy(String... faultDescs) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("listRepairFaultOptions".equals(method.getName())) {
                    List<WorkOrderRepairFaultOptionVO> result = new ArrayList<>();
                    for (String faultDesc : faultDescs) {
                        WorkOrderRepairFaultOptionVO option = new WorkOrderRepairFaultOptionVO();
                        option.setFaultDesc(faultDesc);
                        option.setRepairOptions(Collections.emptyList());
                        result.add(option);
                    }
                    return result;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (IFaultRepairConfigService) Proxy.newProxyInstance(
                IFaultRepairConfigService.class.getClassLoader(),
                new Class<?>[]{IFaultRepairConfigService.class},
                handler
        );
    }

    private List<HqFirstContract> buildContracts(Long... hqCompanyIds) {
        List<HqFirstContract> result = new ArrayList<>();
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

    private void setFieldQuietly(Object target, String fieldName, Object value) {
        try {
            setField(target, fieldName, value);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private Long invokeResolveCreateHqCompanyId(CustomerWorkOrderServiceImpl service, String barcode,
                                                SysCompany company) throws Exception {
        Method method = CustomerWorkOrderServiceImpl.class
                .getDeclaredMethod("resolveCreateHqCompanyId", String.class, SysCompany.class);
        method.setAccessible(true);
        return (Long) method.invoke(service, barcode, company);
    }

    private Object invokeResolveCustomerFaultSelection(CustomerWorkOrderServiceImpl service, CustomerWorkOrderCreateDTO dto,
                                                       Long hqCompanyId, String productCode, String productModel) throws Exception {
        Method method = CustomerWorkOrderServiceImpl.class.getDeclaredMethod(
                "resolveCustomerFaultSelection",
                CustomerWorkOrderCreateDTO.class,
                Long.class,
                String.class,
                String.class
        );
        method.setAccessible(true);
        return method.invoke(service, dto, hqCompanyId, productCode, productModel);
    }

    private String invokeResolveCustomerName(CustomerWorkOrderServiceImpl service, CUser customer) throws Exception {
        Method method = CustomerWorkOrderServiceImpl.class.getDeclaredMethod("resolveCustomerName", CUser.class);
        method.setAccessible(true);
        return (String) method.invoke(service, customer);
    }

    private String invokeFaultSelectionGetter(Object selection, String methodName) throws Exception {
        Method method = selection.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return (String) method.invoke(selection);
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
