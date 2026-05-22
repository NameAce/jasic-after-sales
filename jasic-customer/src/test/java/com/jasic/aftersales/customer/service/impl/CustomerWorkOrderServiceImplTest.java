package com.jasic.aftersales.customer.service.impl;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaTokenContextForThreadLocal;
import cn.dev33.satoken.context.SaTokenContextForThreadLocalStorage;
import cn.dev33.satoken.context.model.SaRequest;
import cn.dev33.satoken.context.model.SaResponse;
import cn.dev33.satoken.context.model.SaStorage;
import com.jasic.aftersales.common.constant.WorkOrderConfigConstants;
import com.jasic.aftersales.common.constant.WorkOrderStatusConstants;
import com.jasic.aftersales.common.enums.BrandTypeEnum;
import com.jasic.aftersales.common.enums.SysFileBizTypeEnum;
import com.jasic.aftersales.common.enums.SysFileUploadUserTypeEnum;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.customer.domain.dto.CustomerWorkOrderCreateDTO;
import com.jasic.aftersales.customer.domain.dto.CustomerWorkOrderEvaluateDTO;
import com.jasic.aftersales.customer.domain.dto.CustomerWorkOrderSenderVoucherDTO;
import com.jasic.aftersales.customer.domain.entity.CUser;
import com.jasic.aftersales.customer.domain.vo.CustomerBarcodeInfoVO;
import com.jasic.aftersales.customer.domain.vo.CustomerNearbyServiceCompanyVO;
import com.jasic.aftersales.customer.domain.vo.CustomerServiceCompanyOptionVO;
import com.jasic.aftersales.customer.domain.vo.CustomerWorkOrderDetailVO;
import com.jasic.aftersales.customer.domain.vo.CustomerWorkOrderLatestSummaryVO;
import com.jasic.aftersales.customer.domain.vo.CustomerWorkOrderListVO;
import com.jasic.aftersales.framework.security.StpCustomerUtil;
import com.jasic.aftersales.customer.mapper.CUserMapper;
import com.jasic.aftersales.system.domain.entity.FirstSecondRelation;
import com.jasic.aftersales.system.domain.entity.HqFirstContract;
import com.jasic.aftersales.system.domain.entity.MachineBarcode;
import com.jasic.aftersales.system.domain.entity.SysCompany;
import com.jasic.aftersales.system.domain.entity.WorkOrder;
import com.jasic.aftersales.system.domain.entity.WorkOrderEvaluation;
import com.jasic.aftersales.system.domain.entity.WorkOrderFlow;
import com.jasic.aftersales.system.domain.entity.WorkOrderQuote;
import com.jasic.aftersales.system.domain.entity.WorkOrderRepair;
import com.jasic.aftersales.system.domain.vo.WorkOrderCompanyRepairHistoryStatVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderRepairFaultOptionVO;
import com.jasic.aftersales.system.domain.vo.SysFileItemVO;
import com.jasic.aftersales.system.mapper.FirstSecondRelationMapper;
import com.jasic.aftersales.system.mapper.HqFirstContractMapper;
import com.jasic.aftersales.system.mapper.MachineBarcodeMapper;
import com.jasic.aftersales.system.mapper.SysCompanyMapper;
import com.jasic.aftersales.system.mapper.WorkOrderEvaluationMapper;
import com.jasic.aftersales.system.mapper.WorkOrderFlowMapper;
import com.jasic.aftersales.system.mapper.WorkOrderMapper;
import com.jasic.aftersales.system.mapper.WorkOrderQuoteMapper;
import com.jasic.aftersales.system.mapper.WorkOrderRepairMapper;
import com.jasic.aftersales.system.service.IFaultRepairConfigService;
import com.jasic.aftersales.system.service.ISysConfigService;
import com.jasic.aftersales.system.service.SysFileService;
import com.jasic.aftersales.system.service.WorkOrderParticipantService;
import com.jasic.aftersales.system.service.support.WorkOrderNoGenerator;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * C端工单服务测试
 *
 * @author Zoro
 * @date 2026/04/01
 */
public class CustomerWorkOrderServiceImplTest {

    /**验证ResolveSingleHqForCreate，保证相关业务规则在回归场景下保持稳定。*/
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

    /**验证RejectWhenCreateHqHasMultipleCandidates，保证相关业务规则在回归场景下保持稳定。*/
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

    /**验证PreferBarcodeArchiveWhenResolvingCreateHq，保证相关业务规则在回归场景下保持稳定。*/
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

    /**验证ReturnBarcodeInfoFromArchive，保证相关业务规则在回归场景下保持稳定。*/
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
        Assert.assertNotNull(barcodeInfo.getLastOutDate());
        Assert.assertEquals("IN_WARRANTY", barcodeInfo.getWarrantyStatus());
        Assert.assertEquals(Long.valueOf(21L), barcodeInfo.getHqCompanyId());
        Assert.assertEquals(buildHqCompany().getCompanyName(), barcodeInfo.getHqCompanyName());
        Assert.assertEquals(3, barcodeInfo.getFaultOptions().size());
        Assert.assertEquals(barcodeInfo.getOtherFaultLabel(),
                barcodeInfo.getFaultOptions().get(barcodeInfo.getFaultOptions().size() - 1));
    }

    /**验证FallbackLegacyFaultDescToOtherFaultWhenNoConfig，保证相关业务规则在回归场景下保持稳定。*/
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

    /**验证RejectFaultOutsideConfiguredOptions，保证相关业务规则在回归场景下保持稳定。*/
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

    /**验证PersistFaultRepairConfigIdForJasicBarcodeCreate，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldPersistFaultRepairConfigIdForJasicBarcodeCreate() throws Exception {
        CustomerWorkOrderServiceImpl service = new CustomerWorkOrderServiceImpl();
        CUser customer = new CUser();
        customer.setId(200L);
        customer.setNickname("客户A");
        customer.setPhone("13800138000");
        customer.setStatus(1);
        WorkOrder[] insertedWorkOrder = new WorkOrder[1];
        List<WorkOrderFlow> insertedFlows = new ArrayList<>();

        setField(service, "cUserMapper", createCUserMapperProxy(customer));
        setField(service, "sysCompanyMapper", createCompanyMapperProxy(buildFirstCompany(), buildHqCompany()));
        setField(service, "machineBarcodeMapper", createMachineBarcodeMapperProxy(buildMachineBarcode(21L)));
        setField(service, "faultRepairConfigService", createFaultRepairConfigServiceProxy(91L, "Fault A"));
        setField(service, "workOrderMapper", createInsertWorkOrderMapperProxy(insertedWorkOrder));
        setField(service, "workOrderFlowMapper", createWorkOrderFlowMapperProxy(insertedFlows));
        setField(service, "sysFileService", createNoopSysFileServiceProxy());
        setField(service, "workOrderNoGenerator", new WorkOrderNoGenerator() {
            /**nextOrderNo 处理逻辑，服务于当前类的业务编排和数据转换。
@return 处理后的业务结果。*/
            @Override
            public String nextOrderNo() {
                return "JSWX2026042200001";
            }
        });
        setField(service, "workOrderParticipantService", new WorkOrderParticipantService() {
            /**initParticipants 处理逻辑，服务于当前类的业务编排和数据转换。
@param workOrder workOrder 字段参数。
@param createSubjectType createSubjectType 字段参数。*/
            @Override
            public void initParticipants(WorkOrder workOrder, String createSubjectType) {
            }
        });

        CustomerWorkOrderCreateDTO dto = new CustomerWorkOrderCreateDTO();
        dto.setBrandType(BrandTypeEnum.JASIC);
        dto.setBarcode("JASIC-001");
        dto.setServiceCompanyId(11L);
        dto.setServiceMode("STORE");
        dto.setFaultItems(Collections.singletonList("Fault A"));

        final Long[] createdId = new Long[1];
        runWithCustomerLoginContext(200L, new ThrowingRunnable() {
            /**run 处理逻辑，服务于当前类的业务编排和数据转换。*/
            @Override
            public void run() throws Exception {
                createdId[0] = service.create(dto);
            }
        });

        Assert.assertEquals(Long.valueOf(1001L), createdId[0]);
        Assert.assertNotNull(insertedWorkOrder[0]);
        Assert.assertEquals(Long.valueOf(91L), getLongFieldValue(insertedWorkOrder[0], "faultRepairConfigId"));
        Assert.assertEquals("MODEL-A", insertedWorkOrder[0].getProductModel());
        Assert.assertEquals(Long.valueOf(21L), insertedWorkOrder[0].getHqCompanyId());
        Assert.assertEquals("JSWX2026042200001", insertedWorkOrder[0].getOrderNo());
        Assert.assertEquals(1, insertedFlows.size());
        Assert.assertEquals("CREATE", insertedFlows.get(0).getActionType());
    }

    /**验证ResolveDefaultHqCompanyForNonBarcodeCreate，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldResolveDefaultHqCompanyForNonBarcodeCreate() throws Exception {
        CustomerWorkOrderServiceImpl service = new CustomerWorkOrderServiceImpl();
        setField(service, "sysCompanyMapper", createCompanyMapperProxy(buildFirstCompany(), buildHqCompany()));
        setField(service, "sysConfigService", createSysConfigServiceProxy(Collections.singletonMap(
                WorkOrderConfigConstants.DEFAULT_HQ_COMPANY_ID, "21"
        )));

        Method method = CustomerWorkOrderServiceImpl.class.getDeclaredMethod("resolveDefaultHqCompanyId");
        method.setAccessible(true);

        Long hqCompanyId = (Long) method.invoke(service);

        Assert.assertEquals(Long.valueOf(21L), hqCompanyId);
    }

    /**验证RejectNonBarcodeCreateWhenDefaultHqCompanyMissing，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldRejectNonBarcodeCreateWhenDefaultHqCompanyMissing() throws Exception {
        CustomerWorkOrderServiceImpl service = new CustomerWorkOrderServiceImpl();
        setField(service, "sysConfigService", createSysConfigServiceProxy(Collections.emptyMap()));

        Method method = CustomerWorkOrderServiceImpl.class.getDeclaredMethod("resolveDefaultHqCompanyId");
        method.setAccessible(true);

        try {
            method.invoke(service);
            Assert.fail("Expected missing default HQ config to be rejected");
        } catch (InvocationTargetException ex) {
            Assert.assertTrue(ex.getTargetException() instanceof ServiceException);
            Assert.assertEquals("默认归属总部未配置", ex.getTargetException().getMessage());
        }
    }

    /**验证RejectBarcodeForNonJasicCreate，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldRejectBarcodeForNonJasicCreate() throws Exception {
        CustomerWorkOrderServiceImpl service = new CustomerWorkOrderServiceImpl();
        CustomerWorkOrderCreateDTO dto = new CustomerWorkOrderCreateDTO();
        dto.setBrandType(BrandTypeEnum.NON_JASIC);
        dto.setBarcode("OTHER-001");
        dto.setServiceMode("STORE");

        Method method = CustomerWorkOrderServiceImpl.class.getDeclaredMethod(
                "validateCreateRequest",
                CustomerWorkOrderCreateDTO.class,
                BrandTypeEnum.class,
                boolean.class
        );
        method.setAccessible(true);

        try {
            method.invoke(service, dto, BrandTypeEnum.NON_JASIC, true);
            Assert.fail("Expected non-jasic create with barcode to be rejected");
        } catch (InvocationTargetException ex) {
            Assert.assertTrue(ex.getTargetException() instanceof ServiceException);
            Assert.assertEquals("非佳士报修不支持填写机器条码", ex.getTargetException().getMessage());
        }
    }

    /**验证ForceOtherFaultForNonBarcodeRepair，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldForceOtherFaultForNonBarcodeRepair() throws Exception {
        CustomerWorkOrderServiceImpl service = new CustomerWorkOrderServiceImpl();

        CustomerWorkOrderCreateDTO dto = new CustomerWorkOrderCreateDTO();
        dto.setBrandType(BrandTypeEnum.NON_JASIC);
        dto.setFaultRemark("机器无法启动");

        Method method = CustomerWorkOrderServiceImpl.class.getDeclaredMethod(
                "resolveCustomerFaultSelection",
                CustomerWorkOrderCreateDTO.class,
                BrandTypeEnum.class,
                boolean.class,
                Long.class,
                String.class,
                String.class
        );
        method.setAccessible(true);

        Object selection = method.invoke(service, dto, BrandTypeEnum.NON_JASIC, false, 21L, null, "MODEL-X");

        Assert.assertEquals("其它故障", invokeFaultSelectionGetter(selection, "getFaultDesc"));
        Assert.assertEquals("机器无法启动", invokeFaultSelectionGetter(selection, "getFaultRemark"));
    }

    /**验证SortNearbyServiceCompaniesByDistance，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldSortNearbyServiceCompaniesByDistance() throws Exception {
        CustomerWorkOrderServiceImpl service = new CustomerWorkOrderServiceImpl();
        setFieldQuietly(service, "sysCompanyMapper", createCompanyMapperProxy(
                buildNearbyCompany(31L, "Service A", "FIRST", "113.0000", "23.0000"),
                buildNearbyCompany(32L, "Service B", "SECOND", "113.0500", "23.0200"),
                buildNearbyCompany(33L, "Service C", "FIRST", null, null)
        ));

        final List<CustomerNearbyServiceCompanyVO>[] result = new List[1];
        runWithCustomerLoginContext(200L, new ThrowingRunnable() {
            /**run 处理逻辑，服务于当前类的业务编排和数据转换。*/
            @Override
            public void run() {
                result[0] = service.listNearbyServiceCompanyOptions(
                        new BigDecimal("113.0010"), new BigDecimal("23.0010"), 10);
            }
        });

        List<CustomerNearbyServiceCompanyVO> options = result[0];
        Assert.assertEquals(3, options.size());
        Assert.assertEquals(Long.valueOf(31L), options.get(0).getId());
        Assert.assertEquals(Long.valueOf(32L), options.get(1).getId());
        Assert.assertEquals(Long.valueOf(33L), options.get(2).getId());
        Assert.assertNotNull(options.get(0).getDistanceKm());
        Assert.assertNotNull(options.get(1).getDistanceKm());
        Assert.assertNull(options.get(2).getDistanceKm());
        Assert.assertEquals(Boolean.FALSE, options.get(0).getHasRepairHistory());
    }

    /**验证SortNearbyServiceCompaniesByRepairHistoryBeforeLimit，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldSortNearbyServiceCompaniesByRepairHistoryBeforeLimit() throws Exception {
        CustomerWorkOrderServiceImpl service = new CustomerWorkOrderServiceImpl();
        setFieldQuietly(service, "sysCompanyMapper", createCompanyMapperProxy(
                buildNearbyCompany(31L, "Nearest Service", "FIRST", "113.0000", "23.0000"),
                buildNearbyCompany(32L, "History One", "FIRST", "114.0000", "24.0000"),
                buildNearbyCompany(33L, "History Two", "SECOND", "115.0000", "25.0000")
        ));
        setFieldQuietly(service, "workOrderFlowMapper", createWorkOrderFlowHistoryMapperProxy(Arrays.asList(
                buildRepairHistoryStat(32L, 1L, LocalDateTime.of(2026, 4, 21, 10, 0)),
                buildRepairHistoryStat(33L, 2L, LocalDateTime.of(2026, 4, 20, 10, 0))
        )));

        final List<CustomerNearbyServiceCompanyVO>[] result = new List[1];
        runWithCustomerLoginContext(200L, new ThrowingRunnable() {
            /**run 处理逻辑，服务于当前类的业务编排和数据转换。*/
            @Override
            public void run() {
                result[0] = service.listNearbyServiceCompanyOptions(
                        new BigDecimal("113.0010"), new BigDecimal("23.0010"), 2);
            }
        });

        List<CustomerNearbyServiceCompanyVO> options = result[0];
        Assert.assertEquals(2, options.size());
        Assert.assertEquals(Long.valueOf(33L), options.get(0).getId());
        Assert.assertEquals(Boolean.TRUE, options.get(0).getHasRepairHistory());
        Assert.assertEquals(Long.valueOf(32L), options.get(1).getId());
        Assert.assertEquals(Boolean.TRUE, options.get(1).getHasRepairHistory());
    }

    /**验证RejectInvalidNearbyCoordinate，保证相关业务规则在回归场景下保持稳定。*/
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

    /**验证ReturnServiceCompanyOptionsWithoutLocationFields，保证相关业务规则在回归场景下保持稳定。*/
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

    /**验证BuildListVoWithBrandTypeAndServiceModeLabels，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldBuildListVoWithBrandTypeAndServiceModeLabels() throws Exception {
        CustomerWorkOrderServiceImpl service = new CustomerWorkOrderServiceImpl();
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(51L);
        workOrder.setBrandType(BrandTypeEnum.JASIC);
        workOrder.setServiceMode("MAIL");
        workOrder.setMainStatus(WorkOrderStatusConstants.MainStatus.PENDING_ASSIGN);
        workOrder.setCurrentAcceptCompanyId(31L);

        Method method = CustomerWorkOrderServiceImpl.class.getDeclaredMethod(
                "buildListVo",
                WorkOrder.class,
                Map.class,
                Map.class,
                Map.class
        );
        method.setAccessible(true);

        CustomerWorkOrderListVO vo = (CustomerWorkOrderListVO) method.invoke(
                service,
                workOrder,
                Collections.singletonMap(31L, buildNearbyCompany(31L, "Service A", "FIRST", "113.0000", "23.0000")),
                Collections.emptyMap(),
                Collections.singletonMap(51L, new BigDecimal("256.80"))
        );

        Assert.assertEquals(BrandTypeEnum.JASIC, vo.getBrandType());
        Assert.assertEquals("佳士品牌", vo.getBrandTypeLabel());
        Assert.assertEquals("MAIL", vo.getServiceMode());
        Assert.assertEquals("Service A", vo.getCurrentAcceptCompanyName());
        Assert.assertEquals("0755-00000031", vo.getCurrentAcceptCompanyPhone());
        Assert.assertEquals("寄修", vo.getServiceModeLabel());
        Assert.assertTrue(vo.getCanUploadSendExpress());
        Assert.assertEquals(0, new BigDecimal("256.80").compareTo(vo.getQuoteAmount()));
    }

    /**验证ReturnLatestUnclosedWorkOrderSummaryWhenExists，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldReturnLatestUnclosedWorkOrderSummaryWhenExists() throws Exception {
        CustomerWorkOrderServiceImpl service = new CustomerWorkOrderServiceImpl();
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(71L);
        workOrder.setCustomerId(200L);
        workOrder.setOrderNo("JS-202604080001");
        workOrder.setProductName("ZX7逆变焊机");
        workOrder.setProductModel("MODEL-A");
        workOrder.setFaultDesc("机器无法启动");
        workOrder.setBrandType(BrandTypeEnum.JASIC);
        workOrder.setServiceMode("MAIL");
        workOrder.setMainStatus(WorkOrderStatusConstants.MainStatus.PENDING_TECH_ACCEPT);
        workOrder.setCreateTime(LocalDateTime.of(2026, 4, 8, 10, 30, 0));

        setField(service, "workOrderMapper", createWorkOrderMapperSelectListProxy(
                Collections.singletonList(Collections.singletonList(workOrder))
        ));

        final CustomerWorkOrderLatestSummaryVO[] result = new CustomerWorkOrderLatestSummaryVO[1];
        runWithCustomerLoginContext(200L, new ThrowingRunnable() {
            /**run 处理逻辑，服务于当前类的业务编排和数据转换。*/
            @Override
            public void run() {
                result[0] = service.getLatestSummary();
            }
        });

        Assert.assertNotNull(result[0]);
        Assert.assertEquals(Long.valueOf(71L), result[0].getId());
        Assert.assertEquals("JS-202604080001", result[0].getOrderNo());
        Assert.assertEquals("ZX7逆变焊机", result[0].getProductName());
        Assert.assertEquals("MODEL-A", result[0].getProductModel());
        Assert.assertEquals("机器无法启动", result[0].getFaultDesc());
        Assert.assertEquals("待接单", result[0].getDisplayStatus());
        Assert.assertEquals("佳士品牌", result[0].getBrandTypeLabel());
        Assert.assertEquals("寄修", result[0].getServiceModeLabel());
    }

    /**验证FallbackToLatestCreatedWorkOrderWhenNoUnclosedOrder，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldFallbackToLatestCreatedWorkOrderWhenNoUnclosedOrder() throws Exception {
        CustomerWorkOrderServiceImpl service = new CustomerWorkOrderServiceImpl();
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(72L);
        workOrder.setCustomerId(200L);
        workOrder.setOrderNo("JS-202604080002");
        workOrder.setMainStatus(WorkOrderStatusConstants.MainStatus.CLOSED);
        workOrder.setCreateTime(LocalDateTime.of(2026, 4, 8, 11, 0, 0));

        setField(service, "workOrderMapper", createWorkOrderMapperSelectListProxy(Arrays.asList(
                Collections.<WorkOrder>emptyList(),
                Collections.singletonList(workOrder)
        )));

        final CustomerWorkOrderLatestSummaryVO[] result = new CustomerWorkOrderLatestSummaryVO[1];
        runWithCustomerLoginContext(200L, new ThrowingRunnable() {
            /**run 处理逻辑，服务于当前类的业务编排和数据转换。*/
            @Override
            public void run() {
                result[0] = service.getLatestSummary();
            }
        });

        Assert.assertNotNull(result[0]);
        Assert.assertEquals(Long.valueOf(72L), result[0].getId());
        Assert.assertEquals("已关闭", result[0].getDisplayStatus());
    }

    /**验证ReturnNullWhenNoWorkOrderExistsForLatestSummary，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldReturnNullWhenNoWorkOrderExistsForLatestSummary() throws Exception {
        CustomerWorkOrderServiceImpl service = new CustomerWorkOrderServiceImpl();
        setField(service, "workOrderMapper", createWorkOrderMapperSelectListProxy(Arrays.asList(
                Collections.<WorkOrder>emptyList(),
                Collections.<WorkOrder>emptyList()
        )));

        final CustomerWorkOrderLatestSummaryVO[] result = new CustomerWorkOrderLatestSummaryVO[1];
        runWithCustomerLoginContext(200L, new ThrowingRunnable() {
            /**run 处理逻辑，服务于当前类的业务编排和数据转换。*/
            @Override
            public void run() {
                result[0] = service.getLatestSummary();
            }
        });

        Assert.assertNull(result[0]);
    }

    /**验证ReturnCurrentAcceptCompanyPhoneInWorkOrderDetail，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldReturnCurrentAcceptCompanyPhoneInWorkOrderDetail() throws Exception {
        CustomerWorkOrderServiceImpl service = new CustomerWorkOrderServiceImpl();
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(81L);
        workOrder.setCustomerId(200L);
        workOrder.setCurrentAcceptCompanyId(31L);
        workOrder.setMainStatus(WorkOrderStatusConstants.MainStatus.PENDING_ASSIGN);
        workOrder.setEvaluateStatus("NOT_OPEN");

        setField(service, "workOrderMapper", createWorkOrderMapperProxy(workOrder, new int[1]));
        setField(service, "sysCompanyMapper", createCompanyMapperProxy(
                buildNearbyCompany(31L, "Service A", "FIRST", "113.0000", "23.0000")
        ));
        setField(service, "workOrderQuoteMapper", createWorkOrderQuoteMapperProxy(Collections.<WorkOrderQuote>emptyList()));
        setField(service, "workOrderRepairMapper", createWorkOrderRepairMapperProxy(Collections.<WorkOrderRepair>emptyList()));
        setField(service, "workOrderEvaluationMapper",
                createWorkOrderEvaluationMapperProxy(new ArrayList<WorkOrderEvaluation>(), 0L));
        setField(service, "sysFileService", createNoopSysFileServiceProxy());

        final CustomerWorkOrderDetailVO[] result = new CustomerWorkOrderDetailVO[1];
        runWithCustomerLoginContext(200L, new ThrowingRunnable() {
            /**run 处理逻辑，服务于当前类的业务编排和数据转换。*/
            @Override
            public void run() {
                result[0] = service.getById(81L);
            }
        });

        Assert.assertNotNull(result[0]);
        Assert.assertEquals("Service A", result[0].getCurrentAcceptCompanyName());
        Assert.assertEquals("0755-00000031", result[0].getCurrentAcceptCompanyPhone());
    }

    /**验证ShowUploadSenderVoucherWhenCurrentVoucherAlreadyExists，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldShowUploadSenderVoucherWhenCurrentVoucherAlreadyExists() throws Exception {
        CustomerWorkOrderServiceImpl service = new CustomerWorkOrderServiceImpl();
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(52L);
        workOrder.setServiceMode("MAIL");
        workOrder.setMainStatus(WorkOrderStatusConstants.MainStatus.PENDING_ASSIGN);

        Method method = CustomerWorkOrderServiceImpl.class.getDeclaredMethod(
                "buildListVo",
                WorkOrder.class,
                Map.class,
                Map.class,
                Map.class
        );
        method.setAccessible(true);

        CustomerWorkOrderListVO vo = (CustomerWorkOrderListVO) method.invoke(
                service,
                workOrder,
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptyMap()
        );

        Assert.assertTrue(vo.getCanUploadSendExpress());
    }

    /**验证UploadSenderVoucherForPendingMailOrder，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldUploadSenderVoucherForPendingMailOrder() throws Exception {
        CustomerWorkOrderServiceImpl service = new CustomerWorkOrderServiceImpl();
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(61L);
        workOrder.setCustomerId(200L);
        workOrder.setServiceMode("MAIL");
        workOrder.setMainStatus(WorkOrderStatusConstants.MainStatus.PENDING_ASSIGN);
        List<Long> replacedFileIds = new ArrayList<>();

        setField(service, "workOrderMapper", createWorkOrderMapperProxy(workOrder, new int[1]));
        setField(service, "sysFileService", createSysFileServiceProxy(Collections.<SysFileItemVO>emptyList(), replacedFileIds));

        CustomerWorkOrderSenderVoucherDTO dto = new CustomerWorkOrderSenderVoucherDTO();
        dto.setWorkOrderId(61L);
        dto.setSenderVoucherFileIds(Arrays.asList(101L, 102L));

        runWithCustomerLoginContext(200L, new ThrowingRunnable() {
            /**run 处理逻辑，服务于当前类的业务编排和数据转换。*/
            @Override
            public void run() throws Exception {
                service.updateSenderVoucher(dto);
            }
        });

        Assert.assertEquals(Arrays.asList(101L, 102L), replacedFileIds);
    }

    /**验证ReplaceSenderVoucherWhenCurrentVoucherAlreadyExists，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldReplaceSenderVoucherWhenCurrentVoucherAlreadyExists() throws Exception {
        CustomerWorkOrderServiceImpl service = new CustomerWorkOrderServiceImpl();
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(62L);
        workOrder.setCustomerId(200L);
        workOrder.setServiceMode("MAIL");
        workOrder.setMainStatus(WorkOrderStatusConstants.MainStatus.PENDING_ASSIGN);
        SysFileItemVO voucher = new SysFileItemVO();
        voucher.setFileId(301L);

        setField(service, "workOrderMapper", createWorkOrderMapperProxy(workOrder, new int[1]));
        List<Long> replacedFileIds = new ArrayList<>();
        setField(service, "sysFileService",
                createSysFileServiceProxy(Collections.singletonList(voucher), replacedFileIds));

        CustomerWorkOrderSenderVoucherDTO dto = new CustomerWorkOrderSenderVoucherDTO();
        dto.setWorkOrderId(62L);
        dto.setSenderVoucherFileIds(Collections.singletonList(101L));

        runWithCustomerLoginContext(200L, new ThrowingRunnable() {
            /**run 处理逻辑，服务于当前类的业务编排和数据转换。*/
            @Override
            public void run() throws Exception {
                service.updateSenderVoucher(dto);
            }
        });

        Assert.assertEquals(Collections.singletonList(101L), replacedFileIds);
    }

    /**验证PreferNicknameForCustomerName，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldPreferNicknameForCustomerName() throws Exception {
        CustomerWorkOrderServiceImpl service = new CustomerWorkOrderServiceImpl();
        CUser customer = new CUser();
        customer.setNickname("nick-name");
        customer.setPhone("13800138000");

        String customerName = invokeResolveCustomerName(service, customer);

        Assert.assertEquals("nick-name", customerName);
    }

    /**验证FallbackPhoneWhenNicknameMissing，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldFallbackPhoneWhenNicknameMissing() throws Exception {
        CustomerWorkOrderServiceImpl service = new CustomerWorkOrderServiceImpl();
        CUser customer = new CUser();
        customer.setNickname("   ");
        customer.setPhone("13800138000");

        String customerName = invokeResolveCustomerName(service, customer);

        Assert.assertEquals("13800138000", customerName);
    }

    /**验证RejectEvaluationWhenCurrentQuoteIsNoFault，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldRejectEvaluationWhenCurrentQuoteIsNoFault() throws Exception {
        CustomerWorkOrderServiceImpl service = new CustomerWorkOrderServiceImpl();
        WorkOrder workOrder = buildClosedWorkOrder(41L, 200L, 31L);
        WorkOrderQuote currentQuote = new WorkOrderQuote();
        currentQuote.setWorkOrderId(workOrder.getId());
        currentQuote.setFaultJudge("无故障");
        currentQuote.setIsCurrentValid(1);

        setField(service, "workOrderMapper", createWorkOrderMapperProxy(workOrder, new int[1]));
        setField(service, "workOrderQuoteMapper", createWorkOrderQuoteMapperProxy(Collections.singletonList(currentQuote)));

        CustomerWorkOrderEvaluateDTO dto = new CustomerWorkOrderEvaluateDTO();
        dto.setWorkOrderId(workOrder.getId());
        dto.setTimelinessScore(5);
        dto.setQualityScore(4);
        dto.setSatisfactionScore(5);
        dto.setContent("整体满意");

        try {
            runWithCustomerLoginContext(200L, new ThrowingRunnable() {
                /**run 处理逻辑，服务于当前类的业务编排和数据转换。*/
                @Override
                public void run() throws Exception {
                    service.evaluate(dto);
                }
            });
            Assert.fail("Expected no-fault work order to reject evaluation");
        } catch (ServiceException ex) {
            Assert.assertEquals("当前工单无故障，不能评价", ex.getMessage());
        }
    }

    /**验证PersistThreeDimensionalEvaluationAndNotifyCurrentCompany，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldPersistThreeDimensionalEvaluationAndNotifyCurrentCompany() throws Exception {
        CustomerWorkOrderServiceImpl service = new CustomerWorkOrderServiceImpl();
        WorkOrder workOrder = buildClosedWorkOrder(42L, 200L, 31L);
        WorkOrderQuote currentQuote = new WorkOrderQuote();
        currentQuote.setWorkOrderId(workOrder.getId());
        currentQuote.setFaultJudge("有故障");
        currentQuote.setIsCurrentValid(1);
        List<WorkOrderEvaluation> insertedEvaluations = new ArrayList<>();
        List<WorkOrderFlow> insertedFlows = new ArrayList<>();
        int[] updateCount = new int[1];
        String[] notifiedContent = new String[1];
        setField(service, "workOrderMapper", createWorkOrderMapperProxy(workOrder, updateCount));
        setField(service, "workOrderQuoteMapper", createWorkOrderQuoteMapperProxy(Collections.singletonList(currentQuote)));
        setField(service, "workOrderEvaluationMapper", createWorkOrderEvaluationMapperProxy(insertedEvaluations, 0L));
        setField(service, "workOrderFlowMapper", createWorkOrderFlowMapperProxy(insertedFlows));

        CustomerWorkOrderEvaluateDTO dto = new CustomerWorkOrderEvaluateDTO();
        dto.setWorkOrderId(workOrder.getId());
        dto.setTimelinessScore(5);
        dto.setQualityScore(4);
        dto.setSatisfactionScore(3);
        dto.setTags("响应快,态度好");
        dto.setContent("维修完成较及时");

        runWithCustomerLoginContext(200L, new ThrowingRunnable() {
            /**run 处理逻辑，服务于当前类的业务编排和数据转换。*/
            @Override
            public void run() throws Exception {
                service.evaluate(dto);
            }
        });
        notifiedContent[0] = insertedEvaluations.isEmpty() ? null : insertedEvaluations.get(0).getContent();

        Assert.assertEquals(1, updateCount[0]);
        Assert.assertEquals("EVALUATED", workOrder.getEvaluateStatus());
        Assert.assertEquals(1, insertedEvaluations.size());
        Assert.assertEquals(Integer.valueOf(5), insertedEvaluations.get(0).getTimelinessScore());
        Assert.assertEquals(Integer.valueOf(4), insertedEvaluations.get(0).getQualityScore());
        Assert.assertEquals(Integer.valueOf(3), insertedEvaluations.get(0).getSatisfactionScore());
        Assert.assertEquals("响应快,态度好", insertedEvaluations.get(0).getTags());
        Assert.assertEquals("维修完成较及时", insertedEvaluations.get(0).getContent());
        Assert.assertEquals(1, insertedFlows.size());
        Assert.assertEquals("EVALUATE", insertedFlows.get(0).getActionType());
        Assert.assertEquals(Long.valueOf(200L), insertedFlows.get(0).getOperatorUserId());
        Assert.assertEquals("维修完成较及时", notifiedContent[0]);
    }

    /**buildFirstCompany 业务数据，统一收口字段清洗、默认值处理和返回对象组装规则。
@return 处理后的业务结果。*/
    private SysCompany buildFirstCompany() {
        SysCompany company = new SysCompany();
        company.setId(11L);
        company.setTypeCode("SITE_FIRST");
        company.setStatus(1);
        company.setCompanyName("First Service");
        return company;
    }

    /**buildHqCompany 业务数据，统一收口字段清洗、默认值处理和返回对象组装规则。
@return 处理后的业务结果。*/
    private SysCompany buildHqCompany() {
        SysCompany company = new SysCompany();
        company.setId(21L);
        company.setTypeCode("HQ_A");
        company.setStatus(1);
        company.setCompanyName("HQ A");
        return company;
    }

    /**buildHqCompanyB 业务数据，统一收口字段清洗、默认值处理和返回对象组装规则。
@return 处理后的业务结果。*/
    private SysCompany buildHqCompanyB() {
        SysCompany company = new SysCompany();
        company.setId(22L);
        company.setTypeCode("HQ_B");
        company.setStatus(1);
        company.setCompanyName("HQ B");
        return company;
    }

    /**buildNearbyCompany 业务数据，统一收口字段清洗、默认值处理和返回对象组装规则。
@param id 主键ID。
@param companyName 名称文本，用于展示、匹配或保存业务对象名称。
@param typeCode 业务编码，用于匹配枚举、配置或外部系统数据。
@param longitude longitude 字段参数。
@param latitude latitude 字段参数。
@return 处理后的业务结果。*/
    private SysCompany buildNearbyCompany(Long id, String companyName, String typeCode,
                                          String longitude, String latitude) {
        SysCompany company = new SysCompany();
        company.setId(id);
        company.setTypeCode(typeCode);
        company.setStatus(1);
        company.setCompanyName(companyName);
        company.setContactPhone("0755-000000" + id);
        company.setDetailAddress("Service Address " + id);
        company.setFullAddress("GuangdongShenzhenService Address " + id);
        company.setGeocodeStatus("SUCCESS");
        if (longitude != null) {
            company.setLongitude(new BigDecimal(longitude));
        }
        if (latitude != null) {
            company.setLatitude(new BigDecimal(latitude));
        }
        return company;
    }

    /**buildMachineBarcode 业务数据，统一收口字段清洗、默认值处理和返回对象组装规则。
@param hqCompanyId hqCompanyId 字段。
@return 处理后的业务结果。*/
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
        barcode.setLastOutDate(java.time.LocalDateTime.now().minusDays(1));
        barcode.setStatus(1);
        return barcode;
    }

    /**createCompanyMapperProxy 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@param companies companies 字段参数。
@return 新增或保存后的业务标识或处理结果。*/
    private SysCompanyMapper createCompanyMapperProxy(SysCompany... companies) {
        InvocationHandler handler = new InvocationHandler() {
            /**invoke 处理逻辑，服务于当前类的业务编排和数据转换。
@param proxy proxy 字段参数。
@param method method 字段参数。
@param args args 字段参数。
@return 处理后的业务结果。*/
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
                if ("selectBatchIds".equals(method.getName())) {
                    List<SysCompany> result = new ArrayList<>();
                    Iterable<?> companyIds = (Iterable<?>) args[0];
                    for (Object companyId : companyIds) {
                        for (SysCompany company : companies) {
                            if (company != null && company.getId().equals(companyId)) {
                                result.add(company);
                                break;
                            }
                        }
                    }
                    return result;
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

    /**createWorkOrderFlowHistoryMapperProxy 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@param stats 业务数据列表，用于批量处理或返回组装。
@return 新增或保存后的业务标识或处理结果。*/
    private WorkOrderFlowMapper createWorkOrderFlowHistoryMapperProxy(final List<WorkOrderCompanyRepairHistoryStatVO> stats) {
        InvocationHandler handler = new InvocationHandler() {
            /**invoke 处理逻辑，服务于当前类的业务编排和数据转换。
@param proxy proxy 字段参数。
@param method method 字段参数。
@param args args 字段参数。
@return 处理后的业务结果。*/
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectCustomerCreateCompanyRepairHistory".equals(method.getName())) {
                    return stats;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (WorkOrderFlowMapper) Proxy.newProxyInstance(
                WorkOrderFlowMapper.class.getClassLoader(),
                new Class<?>[]{WorkOrderFlowMapper.class},
                handler
        );
    }

    /**buildRepairHistoryStat 业务数据，统一收口字段清洗、默认值处理和返回对象组装规则。
@param companyId 公司ID。
@param repairCount repairCount 字段参数。
@param lastRepairTime lastRepairTime 字段参数。
@return 处理后的业务结果。*/
    private WorkOrderCompanyRepairHistoryStatVO buildRepairHistoryStat(Long companyId, Long repairCount,
                                                                       LocalDateTime lastRepairTime) {
        WorkOrderCompanyRepairHistoryStatVO stat = new WorkOrderCompanyRepairHistoryStatVO();
        stat.setCompanyId(companyId);
        stat.setRepairCount(repairCount);
        stat.setLastRepairTime(lastRepairTime);
        return stat;
    }

    /**createHqFirstContractMapperProxy 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@param contracts 业务数据列表，用于批量处理或返回组装。
@return 新增或保存后的业务标识或处理结果。*/
    private HqFirstContractMapper createHqFirstContractMapperProxy(List<HqFirstContract> contracts) {
        InvocationHandler handler = new InvocationHandler() {
            /**invoke 处理逻辑，服务于当前类的业务编排和数据转换。
@param proxy proxy 字段参数。
@param method method 字段参数。
@param args args 字段参数。
@return 处理后的业务结果。*/
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

    /**createMachineBarcodeMapperProxy 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@param barcodes 业务编码，用于匹配枚举、配置或外部系统数据。
@return 新增或保存后的业务标识或处理结果。*/
    private MachineBarcodeMapper createMachineBarcodeMapperProxy(MachineBarcode... barcodes) {
        InvocationHandler handler = new InvocationHandler() {
            /**invoke 处理逻辑，服务于当前类的业务编排和数据转换。
@param proxy proxy 字段参数。
@param method method 字段参数。
@param args args 字段参数。
@return 处理后的业务结果。*/
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

    /**createCUserMapperProxy 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@param customer customer 字段参数。
@return 新增或保存后的业务标识或处理结果。*/
    private CUserMapper createCUserMapperProxy(CUser customer) {
        InvocationHandler handler = new InvocationHandler() {
            /**invoke 处理逻辑，服务于当前类的业务编排和数据转换。
@param proxy proxy 字段参数。
@param method method 字段参数。
@param args args 字段参数。
@return 处理后的业务结果。*/
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectById".equals(method.getName())) {
                    return customer;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (CUserMapper) Proxy.newProxyInstance(
                CUserMapper.class.getClassLoader(),
                new Class<?>[]{CUserMapper.class},
                handler
        );
    }

    /**createFaultRepairConfigServiceProxy 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@param faultDescs faultDescs 字段参数。
@return 新增或保存后的业务标识或处理结果。*/
    private IFaultRepairConfigService createFaultRepairConfigServiceProxy(String... faultDescs) {
        return createFaultRepairConfigServiceProxy(null, faultDescs);
    }

    /**createFaultRepairConfigServiceProxy 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@param configId configId 字段。
@param faultDescs faultDescs 字段参数。
@return 新增或保存后的业务标识或处理结果。*/
    private IFaultRepairConfigService createFaultRepairConfigServiceProxy(Long configId, String... faultDescs) {
        InvocationHandler handler = new InvocationHandler() {
            /**invoke 处理逻辑，服务于当前类的业务编排和数据转换。
@param proxy proxy 字段参数。
@param method method 字段参数。
@param args args 字段参数。
@return 处理后的业务结果。*/
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("listRepairFaultOptionsForResolvedHq".equals(method.getName())) {
                    List<WorkOrderRepairFaultOptionVO> result = new ArrayList<>();
                    for (String faultDesc : faultDescs) {
                        WorkOrderRepairFaultOptionVO option = new WorkOrderRepairFaultOptionVO();
                        option.setFaultDesc(faultDesc);
                        option.setRepairOptions(Collections.emptyList());
                        result.add(option);
                    }
                    return result;
                }
                if ("findEnabledConfigIdForResolvedHq".equals(method.getName())) {
                    return configId;
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

    /**createSysConfigServiceProxy 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@param configMap 业务映射数据，用于提升后续组装或匹配效率。
@return 新增或保存后的业务标识或处理结果。*/
    private ISysConfigService createSysConfigServiceProxy(Map<String, String> configMap) {
        InvocationHandler handler = new InvocationHandler() {
            /**invoke 处理逻辑，服务于当前类的业务编排和数据转换。
@param proxy proxy 字段参数。
@param method method 字段参数。
@param args args 字段参数。
@return 处理后的业务结果。*/
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("getValueByKey".equals(method.getName())) {
                    return configMap.get(args[0]);
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (ISysConfigService) Proxy.newProxyInstance(
                ISysConfigService.class.getClassLoader(),
                new Class<?>[]{ISysConfigService.class},
                handler
        );
    }

    /**buildContracts 业务数据，统一收口字段清洗、默认值处理和返回对象组装规则。
@param hqCompanyIds 公司ID，用于定位业务归属或数据权限范围。
@return 查询或组装后的业务数据集合。*/
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

    /**createFirstSecondRelationMapperProxy 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@return 新增或保存后的业务标识或处理结果。*/
    private FirstSecondRelationMapper createFirstSecondRelationMapperProxy() {
        InvocationHandler handler = new InvocationHandler() {
            /**invoke 处理逻辑，服务于当前类的业务编排和数据转换。
@param proxy proxy 字段参数。
@param method method 字段参数。
@param args args 字段参数。
@return 处理后的业务结果。*/
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

    /**buildClosedWorkOrder 业务数据，统一收口字段清洗、默认值处理和返回对象组装规则。
@param workOrderId 工单ID。
@param customerId 客户ID。
@param companyId 公司ID。
@return 处理后的业务结果。*/
    private WorkOrder buildClosedWorkOrder(Long workOrderId, Long customerId, Long companyId) {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(workOrderId);
        workOrder.setCustomerId(customerId);
        workOrder.setCurrentAcceptCompanyId(companyId);
        workOrder.setMainStatus("CLOSED");
        workOrder.setEvaluateStatus("PENDING_EVALUATE");
        return workOrder;
    }

    /**createWorkOrderMapperProxy 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@param workOrder workOrder 字段参数。
@param updateCount updateCount 字段参数。
@return 新增或保存后的业务标识或处理结果。*/
    private WorkOrderMapper createWorkOrderMapperProxy(WorkOrder workOrder, int[] updateCount) {
        InvocationHandler handler = new InvocationHandler() {
            /**invoke 处理逻辑，服务于当前类的业务编排和数据转换。
@param proxy proxy 字段参数。
@param method method 字段参数。
@param args args 字段参数。
@return 处理后的业务结果。*/
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectById".equals(method.getName())) {
                    return workOrder;
                }
                if ("updateById".equals(method.getName())) {
                    updateCount[0]++;
                    return 1;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (WorkOrderMapper) Proxy.newProxyInstance(
                WorkOrderMapper.class.getClassLoader(),
                new Class<?>[]{WorkOrderMapper.class},
                handler
        );
    }

    /**createInsertWorkOrderMapperProxy 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@param insertedHolder insertedHolder 字段参数。
@return 新增或保存后的业务标识或处理结果。*/
    private WorkOrderMapper createInsertWorkOrderMapperProxy(WorkOrder[] insertedHolder) {
        InvocationHandler handler = new InvocationHandler() {
            /**invoke 处理逻辑，服务于当前类的业务编排和数据转换。
@param proxy proxy 字段参数。
@param method method 字段参数。
@param args args 字段参数。
@return 处理后的业务结果。*/
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("insert".equals(method.getName())) {
                    WorkOrder workOrder = (WorkOrder) args[0];
                    workOrder.setId(1001L);
                    insertedHolder[0] = workOrder;
                    return 1;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (WorkOrderMapper) Proxy.newProxyInstance(
                WorkOrderMapper.class.getClassLoader(),
                new Class<?>[]{WorkOrderMapper.class},
                handler
        );
    }

    /**createWorkOrderMapperSelectListProxy 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@param selectListResults 业务数据列表，用于批量处理或返回组装。
@return 新增或保存后的业务标识或处理结果。*/
    private WorkOrderMapper createWorkOrderMapperSelectListProxy(List<List<WorkOrder>> selectListResults) {
        Queue<List<WorkOrder>> results = new LinkedList<>(selectListResults);
        InvocationHandler handler = new InvocationHandler() {
            /**invoke 处理逻辑，服务于当前类的业务编排和数据转换。
@param proxy proxy 字段参数。
@param method method 字段参数。
@param args args 字段参数。
@return 处理后的业务结果。*/
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectList".equals(method.getName())) {
                    return results.isEmpty() ? Collections.<WorkOrder>emptyList() : results.poll();
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (WorkOrderMapper) Proxy.newProxyInstance(
                WorkOrderMapper.class.getClassLoader(),
                new Class<?>[]{WorkOrderMapper.class},
                handler
        );
    }

    /**createWorkOrderQuoteMapperProxy 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@param quotes 业务数据列表，用于批量处理或返回组装。
@return 新增或保存后的业务标识或处理结果。*/
    private WorkOrderQuoteMapper createWorkOrderQuoteMapperProxy(List<WorkOrderQuote> quotes) {
        InvocationHandler handler = new InvocationHandler() {
            /**invoke 处理逻辑，服务于当前类的业务编排和数据转换。
@param proxy proxy 字段参数。
@param method method 字段参数。
@param args args 字段参数。
@return 处理后的业务结果。*/
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectList".equals(method.getName())) {
                    return quotes;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (WorkOrderQuoteMapper) Proxy.newProxyInstance(
                WorkOrderQuoteMapper.class.getClassLoader(),
                new Class<?>[]{WorkOrderQuoteMapper.class},
                handler
        );
    }

    /**createWorkOrderRepairMapperProxy 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@param repairs 业务数据列表，用于批量处理或返回组装。
@return 新增或保存后的业务标识或处理结果。*/
    private WorkOrderRepairMapper createWorkOrderRepairMapperProxy(List<WorkOrderRepair> repairs) {
        InvocationHandler handler = new InvocationHandler() {
            /**invoke 处理逻辑，服务于当前类的业务编排和数据转换。
@param proxy proxy 字段参数。
@param method method 字段参数。
@param args args 字段参数。
@return 处理后的业务结果。*/
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectList".equals(method.getName())) {
                    return repairs;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (WorkOrderRepairMapper) Proxy.newProxyInstance(
                WorkOrderRepairMapper.class.getClassLoader(),
                new Class<?>[]{WorkOrderRepairMapper.class},
                handler
        );
    }

    /**createWorkOrderEvaluationMapperProxy 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@param insertedEvaluations 业务数据列表，用于批量处理或返回组装。
@param existingCount existingCount 字段参数。
@return 新增或保存后的业务标识或处理结果。*/
    private WorkOrderEvaluationMapper createWorkOrderEvaluationMapperProxy(List<WorkOrderEvaluation> insertedEvaluations,
                                                                           Long existingCount) {
        InvocationHandler handler = new InvocationHandler() {
            /**invoke 处理逻辑，服务于当前类的业务编排和数据转换。
@param proxy proxy 字段参数。
@param method method 字段参数。
@param args args 字段参数。
@return 处理后的业务结果。*/
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectCount".equals(method.getName())) {
                    return existingCount;
                }
                if ("insert".equals(method.getName())) {
                    insertedEvaluations.add((WorkOrderEvaluation) args[0]);
                    return 1;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (WorkOrderEvaluationMapper) Proxy.newProxyInstance(
                WorkOrderEvaluationMapper.class.getClassLoader(),
                new Class<?>[]{WorkOrderEvaluationMapper.class},
                handler
        );
    }

    /**createWorkOrderFlowMapperProxy 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@param insertedFlows 业务数据列表，用于批量处理或返回组装。
@return 新增或保存后的业务标识或处理结果。*/
    private WorkOrderFlowMapper createWorkOrderFlowMapperProxy(List<WorkOrderFlow> insertedFlows) {
        InvocationHandler handler = new InvocationHandler() {
            /**invoke 处理逻辑，服务于当前类的业务编排和数据转换。
@param proxy proxy 字段参数。
@param method method 字段参数。
@param args args 字段参数。
@return 处理后的业务结果。*/
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("insert".equals(method.getName())) {
                    insertedFlows.add((WorkOrderFlow) args[0]);
                    return 1;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (WorkOrderFlowMapper) Proxy.newProxyInstance(
                WorkOrderFlowMapper.class.getClassLoader(),
                new Class<?>[]{WorkOrderFlowMapper.class},
                handler
        );
    }

    /**createSysFileServiceProxy 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@param bizFiles 业务数据列表，用于批量处理或返回组装。
@param replacedFileIds 业务数据列表，用于批量处理或返回组装。
@return 新增或保存后的业务标识或处理结果。*/
    private SysFileService createSysFileServiceProxy(List<SysFileItemVO> bizFiles, List<Long> replacedFileIds) {
        InvocationHandler handler = new InvocationHandler() {
            /**invoke 处理逻辑，服务于当前类的业务编排和数据转换。
@param proxy proxy 字段参数。
@param method method 字段参数。
@param args args 字段参数。
@return 处理后的业务结果。*/
            @Override
            @SuppressWarnings("unchecked")
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("listBizFiles".equals(method.getName())) {
                    return bizFiles;
                }
                if ("replaceBizFiles".equals(method.getName())) {
                    replacedFileIds.clear();
                    if (args[2] instanceof List) {
                        replacedFileIds.addAll((List<Long>) args[2]);
                    }
                    Assert.assertEquals(SysFileBizTypeEnum.WORK_ORDER_SENDER_VOUCHER, args[0]);
                    Assert.assertEquals(SysFileUploadUserTypeEnum.CUSTOMER, args[5]);
                    return null;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (SysFileService) Proxy.newProxyInstance(
                SysFileService.class.getClassLoader(),
                new Class<?>[]{SysFileService.class},
                handler
        );
    }

    /**createNoopSysFileServiceProxy 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@return 新增或保存后的业务标识或处理结果。*/
    private SysFileService createNoopSysFileServiceProxy() {
        InvocationHandler handler = new InvocationHandler() {
            /**invoke 处理逻辑，服务于当前类的业务编排和数据转换。
@param proxy proxy 字段参数。
@param method method 字段参数。
@param args args 字段参数。
@return 处理后的业务结果。*/
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                return defaultValue(method.getReturnType());
            }
        };
        return (SysFileService) Proxy.newProxyInstance(
                SysFileService.class.getClassLoader(),
                new Class<?>[]{SysFileService.class},
                handler
        );
    }

    /**setField 处理逻辑，服务于当前类的业务编排和数据转换。
@param target target 字段参数。
@param fieldName 名称文本，用于展示、匹配或保存业务对象名称。
@param value value 字段参数。*/
    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = CustomerWorkOrderServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /**setFieldQuietly 处理逻辑，服务于当前类的业务编排和数据转换。
@param target target 字段参数。
@param fieldName 名称文本，用于展示、匹配或保存业务对象名称。
@param value value 字段参数。*/
    private void setFieldQuietly(Object target, String fieldName, Object value) {
        try {
            setField(target, fieldName, value);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**invokeResolveCreateHqCompanyId 处理逻辑，服务于当前类的业务编排和数据转换。
@param service service 字段参数。
@param barcode 业务编码，用于匹配枚举、配置或外部系统数据。
@param company company 字段参数。
@return 处理后的业务结果。*/
    private Long invokeResolveCreateHqCompanyId(CustomerWorkOrderServiceImpl service, String barcode,
                                                SysCompany company) throws Exception {
        Method method = CustomerWorkOrderServiceImpl.class
                .getDeclaredMethod("resolveCreateHqCompanyId", String.class, SysCompany.class);
        method.setAccessible(true);
        return (Long) method.invoke(service, barcode, company);
    }

    /**invokeResolveCustomerFaultSelection 处理逻辑，服务于当前类的业务编排和数据转换。
@param service service 字段参数。
@param dto 业务请求参数，承载本次操作需要提交的字段。
@param hqCompanyId hqCompanyId 字段。
@param productCode 业务编码，用于匹配枚举、配置或外部系统数据。
@param productModel productModel 字段参数。
@return 处理后的业务结果。*/
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

    /**invokeResolveCustomerName 处理逻辑，服务于当前类的业务编排和数据转换。
@param service service 字段参数。
@param customer customer 字段参数。
@return 处理后的业务结果。*/
    private String invokeResolveCustomerName(CustomerWorkOrderServiceImpl service, CUser customer) throws Exception {
        Method method = CustomerWorkOrderServiceImpl.class.getDeclaredMethod("resolveCustomerName", CUser.class);
        method.setAccessible(true);
        return (String) method.invoke(service, customer);
    }

    /**invokeFaultSelectionGetter 处理逻辑，服务于当前类的业务编排和数据转换。
@param selection selection 字段参数。
@param methodName 名称文本，用于展示、匹配或保存业务对象名称。
@return 处理后的业务结果。*/
    private String invokeFaultSelectionGetter(Object selection, String methodName) throws Exception {
        Method method = selection.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return (String) method.invoke(selection);
    }

    /**getLongFieldValue 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@param target target 字段参数。
@param fieldName 名称文本，用于展示、匹配或保存业务对象名称。
@return 查询或解析得到的业务对象。*/
    private Long getLongFieldValue(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (Long) field.get(target);
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

    /**runWithCustomerLoginContext 处理逻辑，服务于当前类的业务编排和数据转换。
@param customerId 客户ID。
@param runnable runnable 字段参数。*/
    private void runWithCustomerLoginContext(Long customerId, ThrowingRunnable runnable) throws Exception {
        SaManager.setSaTokenContext(new SaTokenContextForThreadLocal());
        SaTokenContextForThreadLocalStorage.setBox(new MockSaRequest(), new MockSaResponse(), new MockSaStorage());
        StpCustomerUtil.login(customerId);
        try {
            runnable.run();
        } finally {
            try {
                StpCustomerUtil.logout();
            } finally {
                SaTokenContextForThreadLocalStorage.clearBox();
            }
        }
    }

    /**ThrowingRunnable 测试类，用于验证对应业务规则、边界条件和回归场景。

@author Zoro*/
    private interface ThrowingRunnable {
        /**run 处理逻辑，服务于当前类的业务编排和数据转换。*/
        void run() throws Exception;
    }

    /**MockSaRequest 测试类，用于验证对应业务规则、边界条件和回归场景。

@author Zoro*/
    private static class MockSaRequest implements SaRequest {

        /**getSource 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@return 查询或解析得到的业务对象。*/
        @Override
        public Object getSource() {
            return this;
        }

        /**getParam 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@param name 名称文本，用于展示、匹配或保存业务对象名称。
@return 查询或解析得到的业务对象。*/
        @Override
        public String getParam(String name) {
            return null;
        }

        /**getParamNames 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@return 查询或组装后的业务数据集合。*/
        @Override
        public List<String> getParamNames() {
            return Collections.emptyList();
        }

        /**getParamMap 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@return 查询或组装后的业务数据集合。*/
        @Override
        public Map<String, String> getParamMap() {
            return Collections.emptyMap();
        }

        /**getHeader 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@param name 名称文本，用于展示、匹配或保存业务对象名称。
@return 查询或解析得到的业务对象。*/
        @Override
        public String getHeader(String name) {
            return null;
        }

        /**getCookieValue 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@param name 名称文本，用于展示、匹配或保存业务对象名称。
@return 查询或解析得到的业务对象。*/
        @Override
        public String getCookieValue(String name) {
            return null;
        }

        /**getRequestPath 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@return 查询或解析得到的业务对象。*/
        @Override
        public String getRequestPath() {
            return "/";
        }

        /**getUrl 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@return 查询或解析得到的业务对象。*/
        @Override
        public String getUrl() {
            return "http://localhost/test";
        }

        /**getMethod 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@return 查询或解析得到的业务对象。*/
        @Override
        public String getMethod() {
            return "GET";
        }

        /**forward 处理逻辑，服务于当前类的业务编排和数据转换。
@param path path 字段参数。
@return 处理后的业务结果。*/
        @Override
        public Object forward(String path) {
            return null;
        }
    }

    /**MockSaResponse 测试类，用于验证对应业务规则、边界条件和回归场景。

@author Zoro*/
    private static class MockSaResponse implements SaResponse {

        /**getSource 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@return 查询或解析得到的业务对象。*/
        @Override
        public Object getSource() {
            return this;
        }

        /**setStatus 处理逻辑，服务于当前类的业务编排和数据转换。
@param sc sc 字段参数。
@return 处理后的业务结果。*/
        @Override
        public SaResponse setStatus(int sc) {
            return this;
        }

        /**setHeader 处理逻辑，服务于当前类的业务编排和数据转换。
@param name 名称文本，用于展示、匹配或保存业务对象名称。
@param value value 字段参数。
@return 处理后的业务结果。*/
        @Override
        public SaResponse setHeader(String name, String value) {
            return this;
        }

        /**addHeader 处理逻辑，服务于当前类的业务编排和数据转换。
@param name 名称文本，用于展示、匹配或保存业务对象名称。
@param value value 字段参数。
@return 处理后的业务结果。*/
        @Override
        public SaResponse addHeader(String name, String value) {
            return this;
        }

        /**redirect 处理逻辑，服务于当前类的业务编排和数据转换。
@param url url 字段参数。
@return 处理后的业务结果。*/
        @Override
        public Object redirect(String url) {
            return null;
        }
    }

    /**MockSaStorage 测试类，用于验证对应业务规则、边界条件和回归场景。

@author Zoro*/
    private static class MockSaStorage implements SaStorage {

        /**values 字段，用于当前类内部业务处理。*/
        private final Map<String, Object> values = new LinkedHashMap<>();

        /**getSource 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@return 查询或解析得到的业务对象。*/
        @Override
        public Object getSource() {
            return this;
        }

        /**get 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@param key key 字段参数。
@return 查询或解析得到的业务对象。*/
        @Override
        public Object get(String key) {
            return values.get(key);
        }

        /**set 处理逻辑，服务于当前类的业务编排和数据转换。
@param key key 字段参数。
@param value value 字段参数。
@return 处理后的业务结果。*/
        @Override
        public SaStorage set(String key, Object value) {
            values.put(key, value);
            return this;
        }

        /**delete 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@param key key 字段参数。
@return 处理后的业务结果。*/
        @Override
        public SaStorage delete(String key) {
            values.remove(key);
            return this;
        }
    }
}


