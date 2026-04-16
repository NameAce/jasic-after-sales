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
import com.jasic.aftersales.system.service.IFaultRepairConfigService;
import com.jasic.aftersales.system.service.ISysConfigService;
import com.jasic.aftersales.system.service.SysFileService;
import com.jasic.aftersales.system.service.WorkOrderParticipantService;
import com.jasic.aftersales.system.service.WorkOrderNotifyEventService;
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
import java.util.Set;

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
        setField(service, "workOrderParticipantService", new WorkOrderParticipantService() {
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
        Assert.assertEquals(1, insertedFlows.size());
        Assert.assertEquals("CREATE", insertedFlows.get(0).getActionType());
    }

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
    public void shouldBuildListVoWithBrandTypeAndServiceModeLabels() throws Exception {
        CustomerWorkOrderServiceImpl service = new CustomerWorkOrderServiceImpl();
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(51L);
        workOrder.setBrandType(BrandTypeEnum.JASIC);
        workOrder.setServiceMode("MAIL");
        workOrder.setMainStatus(WorkOrderStatusConstants.MainStatus.PENDING_ASSIGN);

        Method method = CustomerWorkOrderServiceImpl.class.getDeclaredMethod(
                "buildListVo",
                WorkOrder.class,
                Map.class,
                Map.class,
                Set.class,
                Map.class
        );
        method.setAccessible(true);

        CustomerWorkOrderListVO vo = (CustomerWorkOrderListVO) method.invoke(
                service,
                workOrder,
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.emptySet(),
                Collections.singletonMap(51L, new BigDecimal("256.80"))
        );

        Assert.assertEquals(BrandTypeEnum.JASIC, vo.getBrandType());
        Assert.assertEquals("佳士品牌", vo.getBrandTypeLabel());
        Assert.assertEquals("MAIL", vo.getServiceMode());
        Assert.assertEquals("寄修", vo.getServiceModeLabel());
        Assert.assertTrue(vo.getCanUploadSendExpress());
        Assert.assertEquals(0, new BigDecimal("256.80").compareTo(vo.getQuoteAmount()));
    }

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
            @Override
            public void run() {
                result[0] = service.getLatestSummary();
            }
        });

        Assert.assertNotNull(result[0]);
        Assert.assertEquals(Long.valueOf(72L), result[0].getId());
        Assert.assertEquals("已关闭", result[0].getDisplayStatus());
    }

    @Test
    public void shouldReturnNullWhenNoWorkOrderExistsForLatestSummary() throws Exception {
        CustomerWorkOrderServiceImpl service = new CustomerWorkOrderServiceImpl();
        setField(service, "workOrderMapper", createWorkOrderMapperSelectListProxy(Arrays.asList(
                Collections.<WorkOrder>emptyList(),
                Collections.<WorkOrder>emptyList()
        )));

        final CustomerWorkOrderLatestSummaryVO[] result = new CustomerWorkOrderLatestSummaryVO[1];
        runWithCustomerLoginContext(200L, new ThrowingRunnable() {
            @Override
            public void run() {
                result[0] = service.getLatestSummary();
            }
        });

        Assert.assertNull(result[0]);
    }

    @Test
    public void shouldHideUploadSenderVoucherWhenCurrentVoucherAlreadyExists() throws Exception {
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
                Set.class,
                Map.class
        );
        method.setAccessible(true);

        CustomerWorkOrderListVO vo = (CustomerWorkOrderListVO) method.invoke(
                service,
                workOrder,
                Collections.emptyMap(),
                Collections.emptyMap(),
                Collections.singleton(52L),
                Collections.emptyMap()
        );

        Assert.assertFalse(vo.getCanUploadSendExpress());
    }

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
            @Override
            public void run() throws Exception {
                service.updateSenderVoucher(dto);
            }
        });

        Assert.assertEquals(Arrays.asList(101L, 102L), replacedFileIds);
    }

    @Test
    public void shouldRejectUploadSenderVoucherWhenCurrentVoucherAlreadyExists() throws Exception {
        CustomerWorkOrderServiceImpl service = new CustomerWorkOrderServiceImpl();
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(62L);
        workOrder.setCustomerId(200L);
        workOrder.setServiceMode("MAIL");
        workOrder.setMainStatus(WorkOrderStatusConstants.MainStatus.PENDING_ASSIGN);
        SysFileItemVO voucher = new SysFileItemVO();
        voucher.setFileId(301L);

        setField(service, "workOrderMapper", createWorkOrderMapperProxy(workOrder, new int[1]));
        setField(service, "sysFileService",
                createSysFileServiceProxy(Collections.singletonList(voucher), new ArrayList<Long>()));

        CustomerWorkOrderSenderVoucherDTO dto = new CustomerWorkOrderSenderVoucherDTO();
        dto.setWorkOrderId(62L);
        dto.setSenderVoucherFileIds(Collections.singletonList(101L));

        try {
            runWithCustomerLoginContext(200L, new ThrowingRunnable() {
                @Override
                public void run() throws Exception {
                    service.updateSenderVoucher(dto);
                }
            });
            Assert.fail("Expected duplicate sender voucher upload to be rejected");
        } catch (ServiceException ex) {
            Assert.assertEquals("当前工单已上传寄件凭证", ex.getMessage());
        }
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
        int[] notifiedScores = new int[3];
        String[] notifiedContent = new String[1];

        setField(service, "workOrderMapper", createWorkOrderMapperProxy(workOrder, updateCount));
        setField(service, "workOrderQuoteMapper", createWorkOrderQuoteMapperProxy(Collections.singletonList(currentQuote)));
        setField(service, "workOrderEvaluationMapper", createWorkOrderEvaluationMapperProxy(insertedEvaluations, 0L));
        setField(service, "workOrderFlowMapper", createWorkOrderFlowMapperProxy(insertedFlows));
        setField(service, "workOrderNotifyEventService", new WorkOrderNotifyEventService() {
            @Override
            public void recordCustomerEvaluated(WorkOrder target, Integer timelinessScore, Integer qualityScore,
                                                Integer satisfactionScore, String content) {
                notifiedScores[0] = timelinessScore == null ? 0 : timelinessScore;
                notifiedScores[1] = qualityScore == null ? 0 : qualityScore;
                notifiedScores[2] = satisfactionScore == null ? 0 : satisfactionScore;
                notifiedContent[0] = content;
            }
        });

        CustomerWorkOrderEvaluateDTO dto = new CustomerWorkOrderEvaluateDTO();
        dto.setWorkOrderId(workOrder.getId());
        dto.setTimelinessScore(5);
        dto.setQualityScore(4);
        dto.setSatisfactionScore(3);
        dto.setTags("响应快,态度好");
        dto.setContent("维修完成较及时");

        runWithCustomerLoginContext(200L, new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                service.evaluate(dto);
            }
        });

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
        Assert.assertEquals(5, notifiedScores[0]);
        Assert.assertEquals(4, notifiedScores[1]);
        Assert.assertEquals(3, notifiedScores[2]);
        Assert.assertEquals("维修完成较及时", notifiedContent[0]);
    }

    private SysCompany buildFirstCompany() {
        SysCompany company = new SysCompany();
        company.setId(11L);
        company.setTypeCode("SITE_FIRST");
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

    private CUserMapper createCUserMapperProxy(CUser customer) {
        InvocationHandler handler = new InvocationHandler() {
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

    private IFaultRepairConfigService createFaultRepairConfigServiceProxy(String... faultDescs) {
        return createFaultRepairConfigServiceProxy(null, faultDescs);
    }

    private IFaultRepairConfigService createFaultRepairConfigServiceProxy(Long configId, String... faultDescs) {
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
                if ("findEnabledConfigId".equals(method.getName())) {
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

    private ISysConfigService createSysConfigServiceProxy(Map<String, String> configMap) {
        InvocationHandler handler = new InvocationHandler() {
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

    private WorkOrder buildClosedWorkOrder(Long workOrderId, Long customerId, Long companyId) {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(workOrderId);
        workOrder.setCustomerId(customerId);
        workOrder.setCurrentAcceptCompanyId(companyId);
        workOrder.setMainStatus("CLOSED");
        workOrder.setEvaluateStatus("PENDING_EVALUATE");
        return workOrder;
    }

    private WorkOrderMapper createWorkOrderMapperProxy(WorkOrder workOrder, int[] updateCount) {
        InvocationHandler handler = new InvocationHandler() {
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

    private WorkOrderMapper createInsertWorkOrderMapperProxy(WorkOrder[] insertedHolder) {
        InvocationHandler handler = new InvocationHandler() {
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

    private WorkOrderMapper createWorkOrderMapperSelectListProxy(List<List<WorkOrder>> selectListResults) {
        Queue<List<WorkOrder>> results = new LinkedList<>(selectListResults);
        InvocationHandler handler = new InvocationHandler() {
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

    private WorkOrderQuoteMapper createWorkOrderQuoteMapperProxy(List<WorkOrderQuote> quotes) {
        InvocationHandler handler = new InvocationHandler() {
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

    private WorkOrderEvaluationMapper createWorkOrderEvaluationMapperProxy(List<WorkOrderEvaluation> insertedEvaluations,
                                                                           Long existingCount) {
        InvocationHandler handler = new InvocationHandler() {
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

    private WorkOrderFlowMapper createWorkOrderFlowMapperProxy(List<WorkOrderFlow> insertedFlows) {
        InvocationHandler handler = new InvocationHandler() {
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

    private SysFileService createSysFileServiceProxy(List<SysFileItemVO> bizFiles, List<Long> replacedFileIds) {
        InvocationHandler handler = new InvocationHandler() {
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

    private SysFileService createNoopSysFileServiceProxy() {
        InvocationHandler handler = new InvocationHandler() {
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

    private Long getLongFieldValue(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (Long) field.get(target);
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

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static class MockSaRequest implements SaRequest {

        @Override
        public Object getSource() {
            return this;
        }

        @Override
        public String getParam(String name) {
            return null;
        }

        @Override
        public List<String> getParamNames() {
            return Collections.emptyList();
        }

        @Override
        public Map<String, String> getParamMap() {
            return Collections.emptyMap();
        }

        @Override
        public String getHeader(String name) {
            return null;
        }

        @Override
        public String getCookieValue(String name) {
            return null;
        }

        @Override
        public String getRequestPath() {
            return "/";
        }

        @Override
        public String getUrl() {
            return "http://localhost/test";
        }

        @Override
        public String getMethod() {
            return "GET";
        }

        @Override
        public Object forward(String path) {
            return null;
        }
    }

    private static class MockSaResponse implements SaResponse {

        @Override
        public Object getSource() {
            return this;
        }

        @Override
        public SaResponse setStatus(int sc) {
            return this;
        }

        @Override
        public SaResponse setHeader(String name, String value) {
            return this;
        }

        @Override
        public SaResponse addHeader(String name, String value) {
            return this;
        }

        @Override
        public Object redirect(String url) {
            return null;
        }
    }

    private static class MockSaStorage implements SaStorage {

        private final Map<String, Object> values = new LinkedHashMap<>();

        @Override
        public Object getSource() {
            return this;
        }

        @Override
        public Object get(String key) {
            return values.get(key);
        }

        @Override
        public SaStorage set(String key, Object value) {
            values.put(key, value);
            return this;
        }

        @Override
        public SaStorage delete(String key) {
            values.remove(key);
            return this;
        }
    }
}
