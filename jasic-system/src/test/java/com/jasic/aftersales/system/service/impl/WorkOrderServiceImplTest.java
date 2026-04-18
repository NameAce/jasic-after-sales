package com.jasic.aftersales.system.service.impl;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaTokenContextForThreadLocal;
import cn.dev33.satoken.context.SaTokenContextForThreadLocalStorage;
import cn.dev33.satoken.context.model.SaRequest;
import cn.dev33.satoken.context.model.SaResponse;
import cn.dev33.satoken.context.model.SaStorage;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jasic.aftersales.common.constant.WorkOrderStatusConstants;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.enums.BrandTypeEnum;
import com.jasic.aftersales.common.enums.WorkOrderUserParticipationActionEnum;
import com.jasic.aftersales.common.enums.WorkOrderRelationTypeEnum;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.domain.dto.WorkOrderAssignDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderCloseDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderFaultPartItemDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderProxyCreateDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderRepairDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderReviewDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderTechAcceptDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderTransferDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderUpdateProductModelDTO;
import com.jasic.aftersales.system.domain.entity.FirstSecondRelation;
import com.jasic.aftersales.system.domain.entity.HqFirstContract;
import com.jasic.aftersales.system.domain.entity.SysCompany;
import com.jasic.aftersales.system.domain.entity.SysCompanyType;
import com.jasic.aftersales.system.domain.entity.SysUser;
import com.jasic.aftersales.system.domain.entity.SysUserCompany;
import com.jasic.aftersales.system.domain.entity.WorkOrder;
import com.jasic.aftersales.system.domain.entity.WorkOrderCustomer;
import com.jasic.aftersales.system.domain.entity.WorkOrderFault;
import com.jasic.aftersales.system.domain.entity.WorkOrderFlow;
import com.jasic.aftersales.system.domain.entity.WorkOrderQuote;
import com.jasic.aftersales.system.domain.entity.WorkOrderRepair;
import com.jasic.aftersales.system.domain.query.WorkOrderQuery;
import com.jasic.aftersales.system.domain.vo.WorkOrderCreateBarcodeInfoVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderListVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderRepairFaultOptionVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderUserOptionVO;
import com.jasic.aftersales.system.mapper.FirstSecondRelationMapper;
import com.jasic.aftersales.system.mapper.HqFirstContractMapper;
import com.jasic.aftersales.system.mapper.SysCompanyMapper;
import com.jasic.aftersales.system.mapper.SysCompanyTypeMapper;
import com.jasic.aftersales.system.mapper.SysMenuMapper;
import com.jasic.aftersales.system.mapper.SysUserMapper;
import com.jasic.aftersales.system.mapper.SysUserCompanyMapper;
import com.jasic.aftersales.system.mapper.WorkOrderFlowMapper;
import com.jasic.aftersales.system.mapper.WorkOrderCustomerMapper;
import com.jasic.aftersales.system.mapper.WorkOrderFaultPartMapper;
import com.jasic.aftersales.system.mapper.WorkOrderMapper;
import com.jasic.aftersales.system.mapper.WorkOrderQuoteMapper;
import com.jasic.aftersales.system.mapper.WorkOrderRepairMapper;
import com.jasic.aftersales.system.service.IFaultRepairConfigService;
import com.jasic.aftersales.system.service.ISysConfigService;
import com.jasic.aftersales.system.service.WorkOrderNotifyEventService;
import com.jasic.aftersales.system.service.WorkOrderParticipantService;
import com.jasic.aftersales.system.service.WorkOrderPermissionService;
import com.jasic.aftersales.system.service.WorkOrderUserParticipantService;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 宸ュ崟涓氬姟鏈嶅姟娴嬭瘯銆? *
 * @author Codex
 * @date 2026/04/01
 */
public class WorkOrderServiceImplTest {

    @Test
    public void shouldReturnFaultDescInWorkOrderListPage() throws Exception {
        WorkOrderListVO record = new WorkOrderListVO();
        record.setId(99L);
        record.setMainStatus(WorkOrderStatusConstants.MainStatus.PENDING_ASSIGN);
        record.setCurrentAcceptCompanyId(2002L);
        record.setFaultDesc("寮€鏈烘棤鍙嶅簲");
        record.setBrandType(BrandTypeEnum.JASIC);
        WorkOrderQuote olderValidQuote = new WorkOrderQuote();
        olderValidQuote.setWorkOrderId(99L);
        olderValidQuote.setIsCurrentValid(1);
        olderValidQuote.setQuoteAmount(new BigDecimal("100.00"));
        olderValidQuote.setCreateTime(LocalDateTime.of(2026, 4, 1, 9, 0, 0));
        WorkOrderQuote latestValidQuote = new WorkOrderQuote();
        latestValidQuote.setWorkOrderId(99L);
        latestValidQuote.setIsCurrentValid(1);
        latestValidQuote.setQuoteAmount(new BigDecimal("188.50"));
        latestValidQuote.setCreateTime(LocalDateTime.of(2026, 4, 2, 9, 0, 0));
        WorkOrderQuote invalidQuote = new WorkOrderQuote();
        invalidQuote.setWorkOrderId(99L);
        invalidQuote.setIsCurrentValid(0);
        invalidQuote.setQuoteAmount(new BigDecimal("299.00"));
        invalidQuote.setCreateTime(LocalDateTime.of(2026, 4, 3, 9, 0, 0));

        WorkOrderServiceImpl service = new WorkOrderServiceImpl();
        setField(service, "workOrderMapper", createPagedWorkOrderMapperProxy(Collections.singletonList(record)));
        setField(service, "workOrderQuoteMapper", createQuoteMapperProxy(Arrays.asList(olderValidQuote, invalidQuote, latestValidQuote)));
        setField(service, "workOrderPermissionService", new WorkOrderPermissionService() {
            @Override
            public void fillQueryScope(WorkOrderQuery query) {
                query.setCurrentUserId(101L);
                query.setCompanyId(2002L);
                query.setSubjectType("BRANCH");
                query.setDataScope("ALL");
                query.setRelatedCompanyIds(Collections.emptyList());
            }
        });

        WorkOrderQuery query = new WorkOrderQuery();
        query.setPageNum(1);
        query.setPageSize(10);

        final PageResult<WorkOrderListVO>[] holder = new PageResult[1];
        runWithLoginContext(101L, new ThrowingRunnable() {
            @Override
            public void run() {
                com.jasic.aftersales.framework.security.SecurityContext.setCurrentCompanyId(2002L);
                com.jasic.aftersales.framework.security.SecurityContext.setCurrentSubjectType("BRANCH");
                holder[0] = service.listPage(query);
            }
        });

        Assert.assertEquals(1L, holder[0].getTotal().longValue());
        Assert.assertEquals(1, holder[0].getRecords().size());
        Assert.assertEquals("寮€鏈烘棤鍙嶅簲", holder[0].getRecords().get(0).getFaultDesc());
        Assert.assertEquals(BrandTypeEnum.JASIC, holder[0].getRecords().get(0).getBrandType());
        Assert.assertEquals("佳士品牌", holder[0].getRecords().get(0).getBrandTypeLabel());
        Assert.assertEquals(0, new BigDecimal("188.50").compareTo(holder[0].getRecords().get(0).getQuoteAmount()));
    }

    @Test
    public void shouldRejectTransferTargetQueryWhenTransferNotAllowed() throws Exception {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(1L);
        workOrder.setMainStatus(WorkOrderStatusConstants.MainStatus.IN_PROGRESS);
        workOrder.setCurrentAcceptCompanyId(2L);

        WorkOrderServiceImpl service = new WorkOrderServiceImpl();
        setField(service, "workOrderMapper", createWorkOrderMapperProxy(workOrder));
        setField(service, "workOrderPermissionService", new WorkOrderPermissionService() {
            @Override
            public boolean canView(WorkOrder target) {
                return true;
            }

            @Override
            public boolean canTransfer(WorkOrder target) {
                return false;
            }
        });

        try {
            service.listTransferTargetOptions(workOrder.getId());
            Assert.fail("预期应拒绝查询转单目标");
        } catch (ServiceException ex) {
            Assert.assertEquals("当前工单不允许转单", ex.getMessage());
        }
    }

    @Test
    public void shouldRejectEmptyRepairSubmission() throws Exception {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(2L);
        workOrder.setMainStatus(WorkOrderStatusConstants.MainStatus.IN_PROGRESS);
        workOrder.setCurrentAcceptCompanyId(3L);

        WorkOrderServiceImpl service = new WorkOrderServiceImpl();
        setField(service, "workOrderMapper", createWorkOrderMapperProxy(workOrder));
        setField(service, "workOrderPermissionService", new WorkOrderPermissionService() {
            @Override
            public boolean canSaveRepair(WorkOrder target) {
                return true;
            }
        });

        WorkOrderRepairDTO dto = new WorkOrderRepairDTO();
        dto.setWorkOrderId(workOrder.getId());

        try {
            service.saveRepair(dto);
            Assert.fail("棰勬湡搴旀嫆缁濈┖缁翠慨鐧昏");
        } catch (ServiceException ex) {
            Assert.assertEquals("请至少填写一项维修内容", ex.getMessage());
        }
    }

    @Test
    public void shouldRejectUnsupportedFaultJudgeValue() throws Exception {
        WorkOrderServiceImpl service = new WorkOrderServiceImpl();
        Method method = WorkOrderServiceImpl.class
                .getDeclaredMethod("normalizeFaultJudge", String.class, String.class);
        method.setAccessible(true);

        try {
            method.invoke(service, "待确认", "故障判定不能为空");
            Assert.fail("棰勬湡搴旀嫆缁濋潪鏋氫妇鏁呴殰鍒ゆ柇");
        } catch (InvocationTargetException ex) {
            Assert.assertTrue(ex.getCause() instanceof ServiceException);
            Assert.assertEquals("故障判定只能为有故障或无故障", ex.getCause().getMessage());
        }
    }

    @Test
    public void shouldListAdminUserWhenAdminHasAcceptPermission() throws Exception {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(13L);
        workOrder.setMainStatus(WorkOrderStatusConstants.MainStatus.PENDING_ASSIGN);
        workOrder.setCurrentAcceptCompanyId(2002L);

        SysUser adminUser = buildUser(101L, "鑰佹澘璐﹀彿", "13800138000", 1);
        SysUser normalUser = buildUser(102L, "普通账号", "13800138001", 1);

        WorkOrderServiceImpl service = new WorkOrderServiceImpl();
        setField(service, "workOrderMapper", createWorkOrderMapperProxy(workOrder));
        setField(service, "workOrderPermissionService", new WorkOrderPermissionService() {
            @Override
            public boolean canAssign(WorkOrder target) {
                return true;
            }
        });
        setField(service, "sysUserCompanyMapper", createUserCompanyMapperProxy(2002L, Arrays.asList(101L, 102L)));
        setField(service, "sysUserMapper", createSysUserMapperProxy(Arrays.asList(adminUser, normalUser)));
        setField(service, "sysMenuMapper", createSysMenuMapperProxy(Collections.singletonMap(
                101L, Collections.singleton("workorder:accept")
        )));

        List<WorkOrderUserOptionVO> options = service.listAssignUserOptions(workOrder.getId());

        Assert.assertEquals(1, options.size());
        Assert.assertEquals(Long.valueOf(101L), options.get(0).getId());
        Assert.assertEquals("鑰佹澘璐﹀彿", options.get(0).getRealName());
    }

    @Test
    public void shouldAllowAssignToAdminWhenAdminHasAcceptPermission() throws Exception {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(14L);
        workOrder.setMainStatus(WorkOrderStatusConstants.MainStatus.PENDING_ASSIGN);
        workOrder.setCurrentAcceptCompanyId(2002L);

        WorkOrderServiceImpl service = new WorkOrderServiceImpl();
        setField(service, "workOrderMapper", createMutableWorkOrderMapperProxy(workOrder, new int[1]));
        setField(service, "workOrderPermissionService", new WorkOrderPermissionService() {
            @Override
            public boolean canAssign(WorkOrder target) {
                return true;
            }
        });
        setField(service, "sysUserCompanyMapper", createUserCompanyMapperProxy(2002L, Collections.singletonList(101L)));
        setField(service, "sysUserMapper", createSysUserMapperProxy(Collections.singletonList(
                buildUser(101L, "鑰佹澘璐﹀彿", "13800138000", 1)
        )));
        setField(service, "sysMenuMapper", createSysMenuMapperProxy(Collections.singletonMap(
                101L, Collections.singleton("workorder:accept")
        )));
        setField(service, "workOrderFlowMapper", createNoopProxy(WorkOrderFlowMapper.class, "insert"));

        WorkOrderAssignDTO dto = new WorkOrderAssignDTO();
        dto.setWorkOrderId(workOrder.getId());
        dto.setAssignedUserId(101L);

        runWithLoginContext(101L, new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                service.assign(dto);
            }
        });

        Assert.assertEquals(Long.valueOf(101L), workOrder.getAssignedUserId());
        Assert.assertEquals(WorkOrderStatusConstants.MainStatus.PENDING_TECH_ACCEPT, workOrder.getMainStatus());
    }

    @Test
    public void shouldTechAcceptFaultWorkOrderAndCreateQuote() throws Exception {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(14L);
        workOrder.setMainStatus(WorkOrderStatusConstants.MainStatus.PENDING_TECH_ACCEPT);
        workOrder.setCurrentAcceptCompanyId(3L);

        List<WorkOrderQuote> quotes = new ArrayList<>();
        List<WorkOrderQuote> insertedQuotes = new ArrayList<>();
        List<WorkOrderFlow> insertedFlows = new ArrayList<>();
        UserParticipantRecorder participantRecorder = new UserParticipantRecorder();

        WorkOrderServiceImpl service = new WorkOrderServiceImpl();
        setField(service, "workOrderMapper", createMutableWorkOrderMapperProxy(workOrder, new int[1]));
        setField(service, "workOrderQuoteMapper", createMutableQuoteMapperProxy(quotes, insertedQuotes, new int[1]));
        setField(service, "workOrderFlowMapper", createFlowMapperProxy(insertedFlows));
        setField(service, "workOrderPermissionService", new WorkOrderPermissionService() {
            @Override
            public boolean canTechAccept(WorkOrder target) {
                return true;
            }
        });
        setField(service, "workOrderUserParticipantService", participantRecorder);

        WorkOrderTechAcceptDTO dto = new WorkOrderTechAcceptDTO();
        dto.setWorkOrderId(workOrder.getId());
        dto.setFaultJudge("有故障");

        runWithLoginContext(101L, new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                service.techAccept(dto);
            }
        });

        Assert.assertEquals(WorkOrderStatusConstants.MainStatus.IN_PROGRESS, workOrder.getMainStatus());
        Assert.assertEquals(1, insertedQuotes.size());
        Assert.assertEquals("有故障", insertedQuotes.get(0).getFaultJudge());
        Assert.assertNull(insertedQuotes.get(0).getQuoteAmount());
        Assert.assertNull(insertedQuotes.get(0).getQuoteDesc());
        Assert.assertEquals(Integer.valueOf(1), insertedQuotes.get(0).getIsCurrentValid());
        Assert.assertEquals(2, insertedFlows.size());
        Assert.assertEquals("TECH_ACCEPT", insertedFlows.get(0).getActionType());
        Assert.assertEquals("QUOTE", insertedFlows.get(1).getActionType());
        Assert.assertEquals(WorkOrderStatusConstants.MainStatus.IN_PROGRESS, insertedFlows.get(1).getAfterStatus());
        Assert.assertEquals(Arrays.asList("3-101-TECH_ACCEPT", "3-101-QUOTE"), participantRecorder.records);
    }

    @Test
    public void shouldTechAcceptNoFaultWorkOrderAndCloseImmediately() throws Exception {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(15L);
        workOrder.setMainStatus(WorkOrderStatusConstants.MainStatus.PENDING_TECH_ACCEPT);
        workOrder.setCurrentAcceptCompanyId(3L);
        workOrder.setServiceMode("STORE");

        List<WorkOrderQuote> quotes = new ArrayList<>();
        List<WorkOrderQuote> insertedQuotes = new ArrayList<>();
        List<WorkOrderFlow> insertedFlows = new ArrayList<>();

        WorkOrderServiceImpl service = new WorkOrderServiceImpl();
        setField(service, "workOrderMapper", createMutableWorkOrderMapperProxy(workOrder, new int[1]));
        setField(service, "workOrderQuoteMapper", createMutableQuoteMapperProxy(quotes, insertedQuotes, new int[1]));
        setField(service, "workOrderFlowMapper", createFlowMapperProxy(insertedFlows));
        setField(service, "workOrderPermissionService", new WorkOrderPermissionService() {
            @Override
            public boolean canTechAccept(WorkOrder target) {
                return true;
            }
        });
        setField(service, "sysFileService", createNoopProxy(com.jasic.aftersales.system.service.SysFileService.class, "replaceBizFiles"));

        WorkOrderTechAcceptDTO dto = new WorkOrderTechAcceptDTO();
        dto.setWorkOrderId(workOrder.getId());
        dto.setFaultJudge("无故障");
        dto.setReturnMethod("自提");
        dto.setCloseReason("检测无故障，客户自提");

        runWithLoginContext(101L, new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                service.techAccept(dto);
            }
        });

        Assert.assertEquals(WorkOrderStatusConstants.MainStatus.CLOSED, workOrder.getMainStatus());
        Assert.assertEquals(WorkOrderStatusConstants.EvaluateStatus.NOT_OPEN, workOrder.getEvaluateStatus());
        Assert.assertEquals("自提", workOrder.getReturnMethod());
        Assert.assertEquals("检测无故障，客户自提", workOrder.getCloseReason());
        Assert.assertNotNull(workOrder.getCompletedTime());
        Assert.assertNotNull(workOrder.getClosedTime());
        Assert.assertEquals(workOrder.getCompletedTime(), workOrder.getClosedTime());
        Assert.assertEquals(1, insertedQuotes.size());
        Assert.assertEquals("无故障", insertedQuotes.get(0).getFaultJudge());
        Assert.assertNull(insertedQuotes.get(0).getQuoteAmount());
        Assert.assertNull(insertedQuotes.get(0).getQuoteDesc());
        Assert.assertEquals(4, insertedFlows.size());
        Assert.assertEquals("TECH_ACCEPT", insertedFlows.get(0).getActionType());
        Assert.assertEquals("QUOTE", insertedFlows.get(1).getActionType());
        Assert.assertEquals("RETURN_METHOD", insertedFlows.get(2).getActionType());
        Assert.assertEquals("CLOSE", insertedFlows.get(3).getActionType());
        Assert.assertEquals(WorkOrderStatusConstants.MainStatus.CLOSED, insertedFlows.get(3).getAfterStatus());
    }

    @Test
    public void shouldTechAcceptNoFaultMailWorkOrderWithoutReturnExpressNo() throws Exception {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(17L);
        workOrder.setMainStatus(WorkOrderStatusConstants.MainStatus.PENDING_TECH_ACCEPT);
        workOrder.setCurrentAcceptCompanyId(3L);
        workOrder.setServiceMode("MAIL");

        List<WorkOrderQuote> quotes = new ArrayList<>();
        List<WorkOrderQuote> insertedQuotes = new ArrayList<>();
        List<WorkOrderFlow> insertedFlows = new ArrayList<>();

        WorkOrderServiceImpl service = new WorkOrderServiceImpl();
        setField(service, "workOrderMapper", createMutableWorkOrderMapperProxy(workOrder, new int[1]));
        setField(service, "workOrderQuoteMapper", createMutableQuoteMapperProxy(quotes, insertedQuotes, new int[1]));
        setField(service, "workOrderFlowMapper", createFlowMapperProxy(insertedFlows));
        setField(service, "workOrderPermissionService", new WorkOrderPermissionService() {
            @Override
            public boolean canTechAccept(WorkOrder target) {
                return true;
            }
        });
        setField(service, "sysFileService", createNoopProxy(com.jasic.aftersales.system.service.SysFileService.class, "replaceBizFiles"));

        WorkOrderTechAcceptDTO dto = new WorkOrderTechAcceptDTO();
        dto.setWorkOrderId(workOrder.getId());
        dto.setFaultJudge("无故障");
        dto.setReturnMethod("回寄");
        dto.setReturnExpressNo("   ");
        dto.setReturnVoucherFileIds(Collections.singletonList(128L));
        dto.setCloseReason("检测无故障，安排回寄");

        runWithLoginContext(101L, new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                service.techAccept(dto);
            }
        });

        Assert.assertEquals(WorkOrderStatusConstants.MainStatus.CLOSED, workOrder.getMainStatus());
        Assert.assertEquals(WorkOrderStatusConstants.EvaluateStatus.NOT_OPEN, workOrder.getEvaluateStatus());
        Assert.assertEquals("回寄", workOrder.getReturnMethod());
        Assert.assertNull(workOrder.getReturnExpressNo());
        Assert.assertEquals("检测无故障，安排回寄", workOrder.getCloseReason());
        Assert.assertNotNull(workOrder.getCompletedTime());
        Assert.assertNotNull(workOrder.getClosedTime());
        Assert.assertEquals(1, insertedQuotes.size());
        Assert.assertEquals("RETURN_METHOD", insertedFlows.get(2).getActionType());
        Assert.assertEquals("CLOSE", insertedFlows.get(3).getActionType());
    }

    @Test
    public void shouldLoadBarcodelessProxyCreateInfoFromDefaultHq() throws Exception {
        WorkOrderServiceImpl service = new WorkOrderServiceImpl();
        setField(service, "sysConfigService", createSysConfigServiceProxy("900"));
        setField(service, "sysCompanyMapper", createCompanyMapperProxy(Collections.singletonList(
                buildCompany(900L, "榛樿鎬婚儴", "HQ_A")
        )));
        setField(service, "faultRepairConfigService", createFaultRepairConfigServiceProxy(Collections.singletonList(
                buildRepairFaultOption("榛樿鏁呴殰", Collections.singletonList("鏇存崲閰嶄欢"))
        )));

        final WorkOrderCreateBarcodeInfoVO[] holder = new WorkOrderCreateBarcodeInfoVO[1];
        runWithLoginContext(101L, new ThrowingRunnable() {
            @Override
            public void run() {
                com.jasic.aftersales.framework.security.SecurityContext.setCurrentCompanyId(2002L);
                holder[0] = service.getProxyCreateBarcodeInfo(null);
            }
        });

        Assert.assertNotNull(holder[0]);
        Assert.assertNull(holder[0].getBarcode());
        Assert.assertEquals(Long.valueOf(900L), holder[0].getHqCompanyId());
        Assert.assertEquals("榛樿鎬婚儴", holder[0].getHqCompanyName());
        Assert.assertEquals(Collections.emptyList(), holder[0].getFaultOptions());
    }

    @Test
    public void shouldAllowEmptyFaultItemsForBarcodelessCreate() throws Exception {
        WorkOrderServiceImpl service = new WorkOrderServiceImpl();

        Object selection = invokePrivateMethod(service, "resolveCreateFaultSelection",
                new Class<?>[]{List.class, String.class, Long.class, String.class, String.class},
                Collections.emptyList(), "", 900L, null, null);

        Assert.assertNull(invokeGetter(selection, "getFaultDesc"));
        Assert.assertNull(invokeGetter(selection, "getFaultRemark"));
    }

    @Test
    public void shouldAllowOtherFaultForBarcodelessCreateWhenRemarkProvided() throws Exception {
        WorkOrderServiceImpl service = new WorkOrderServiceImpl();

        Object selection = invokePrivateMethod(service, "resolveCreateFaultSelection",
                new Class<?>[]{List.class, String.class, Long.class, String.class, String.class},
                Collections.singletonList("其它故障"), "无码机器手工描述故障", 900L, null, null);

        Assert.assertEquals("其它故障", invokeGetter(selection, "getFaultDesc"));
        Assert.assertEquals("无码机器手工描述故障", invokeGetter(selection, "getFaultRemark"));
    }
    @Test
    public void shouldRequireRepairFaultItemsWhenRepairConfigExists() throws Exception {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(25L);
        workOrder.setFaultRepairConfigId(88L);

        WorkOrderServiceImpl service = new WorkOrderServiceImpl();
        setField(service, "faultRepairConfigService", createFaultRepairConfigServiceProxy(Collections.singletonList(
                buildRepairFaultOption("主板故障", Collections.singletonList("更换主板"))
        )));

        try {
            invokePrivateMethod(service, "resolveRepairFaultSelectionForSaveRepair",
                    new Class<?>[]{WorkOrder.class, List.class, String.class},
                    workOrder, Collections.emptyList(), null);
            Assert.fail("预期应拒绝缺少维修侧故障的维修登记");
        } catch (InvocationTargetException ex) {
            Assert.assertTrue(ex.getCause() instanceof ServiceException);
            Assert.assertEquals("请选择故障描述", ex.getCause().getMessage());
        }
    }


    @Test
    public void shouldResolveReviewFaultSelectionFromFirstRepairRecord() throws Exception {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(26L);
        workOrder.setFaultRepairConfigId(89L);

        WorkOrderRepair repair = new WorkOrderRepair();
        repair.setId(2601L);
        repair.setWorkOrderId(workOrder.getId());
        repair.setRegisterStage("REPAIR");

        WorkOrderFault fault = new WorkOrderFault();
        fault.setId(2602L);
        fault.setWorkOrderId(workOrder.getId());
        fault.setRepairId(repair.getId());
        fault.setFaultDesc("主板故障，风扇故障");
        fault.setFaultRemark("首次维修确认的其它故障说明");

        WorkOrderServiceImpl service = new WorkOrderServiceImpl();
        setField(service, "faultRepairConfigService", createFaultRepairConfigServiceProxy(Collections.singletonList(
                buildRepairFaultOption("主板故障", Collections.singletonList("更换主板"))
        )));
        setField(service, "workOrderRepairMapper", createWorkOrderRepairMapperProxy(Collections.singletonList(repair)));
        setField(service, "workOrderFaultMapper", createWorkOrderFaultMapperProxy(Collections.singletonList(fault)));

        Object selection = invokePrivateMethod(service, "resolveRepairFaultSelectionForReview",
                new Class<?>[]{WorkOrder.class}, workOrder);

        Assert.assertEquals("主板故障，风扇故障", invokeGetter(selection, "getFaultDesc"));
        Assert.assertEquals("首次维修确认的其它故障说明", invokeGetter(selection, "getFaultRemark"));
        Assert.assertEquals(Collections.singletonList("主板故障，风扇故障"), invokeGetter(selection, "getFaultItems"));
    }

    @Test
    public void shouldFallbackProxyCustomerNameToMobileWhenBlank() throws Exception {
        WorkOrderServiceImpl service = new WorkOrderServiceImpl();
        WorkOrderProxyCreateDTO dto = new WorkOrderProxyCreateDTO();
        dto.setCustomerName("   ");
        dto.setCustomerMobile("13800138000");

        Object identity = invokePrivateMethod(service, "resolveProxyCreateCustomerIdentity",
                new Class<?>[]{WorkOrderProxyCreateDTO.class}, dto);
        Assert.assertEquals("13800138000", invokeGetter(identity, "getCustomerName"));
        Assert.assertEquals("13800138000", invokeGetter(identity, "getCustomerMobile"));
    }

    @Test
    public void shouldLeaveCustomerIdNullWhenProxyCustomerNotMatched() throws Exception {
        WorkOrderServiceImpl service = new WorkOrderServiceImpl();
        setField(service, "workOrderCustomerMapper", createWorkOrderCustomerMapperProxy(Collections.emptyList()));

        Object customerId = invokePrivateMethod(service, "resolveCreateCustomerId",
                new Class<?>[]{String.class, String.class}, "临时报修人", "13800138000");

        Assert.assertNull(customerId);
    }

    @Test
    public void shouldResolveReportSubjectByCreateEntryType() throws Exception {
        WorkOrderServiceImpl service = new WorkOrderServiceImpl();

        Object proxySubjectType = invokePrivateMethod(service, "resolveReportSubjectType",
                new Class<?>[]{String.class}, "PROXY_SELF");
        Object upstreamSubjectType = invokePrivateMethod(service, "resolveReportSubjectType",
                new Class<?>[]{String.class}, "UPSTREAM_FIRST");
        Object proxyReportCompanyId = invokePrivateMethod(service, "resolveReportCompanyId",
                new Class<?>[]{Long.class, String.class}, 2002L, "PROXY_SELF");
        Object upstreamReportCompanyId = invokePrivateMethod(service, "resolveReportCompanyId",
                new Class<?>[]{Long.class, String.class}, 2002L, "UPSTREAM_HQ");

        Assert.assertEquals("CUSTOMER", proxySubjectType);
        Assert.assertEquals("COMPANY", upstreamSubjectType);
        Assert.assertNull(proxyReportCompanyId);
        Assert.assertEquals(Long.valueOf(2002L), upstreamReportCompanyId);
    }

    @Test
    public void shouldRejectUpstreamCreateWhenLoginPhoneMissing() throws Exception {
        WorkOrderServiceImpl service = new WorkOrderServiceImpl();
        SysUser currentUser = new SysUser();
        currentUser.setId(101L);
        currentUser.setRealName("褰撳墠鍛樺伐");
        currentUser.setPhone("   ");
        setField(service, "sysUserMapper", createSysUserMapperProxy(currentUser));

        runWithLoginContext(101L, new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                try {
                    invokePrivateMethod(service, "resolveUpstreamCreateCustomerIdentity", new Class<?>[0]);
                    Assert.fail("预期应拒绝缺少手机号的上游报修入口");
                } catch (InvocationTargetException ex) {
                    Assert.assertTrue(ex.getCause() instanceof ServiceException);
                    Assert.assertEquals("当前登录账号未维护手机号，无法提交上级报修", ex.getCause().getMessage());
                }
            }
        });
    }

    @Test
    public void shouldTransferSecondLevelWorkOrderAndRecordHistory() throws Exception {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(3L);
        workOrder.setMainStatus(WorkOrderStatusConstants.MainStatus.IN_PROGRESS);
        workOrder.setCurrentAcceptCompanyId(2002L);
        workOrder.setCurrentAcceptSubjectType("SERVICE");
        workOrder.setAssignedUserId(301L);
        workOrder.setHasTransfer(0);
        workOrder.setTransferCount(0);
        workOrder.setHqCompanyId(900L);

        int[] updateCount = new int[1];
        List<WorkOrderFlow> insertedFlows = new ArrayList<>();
        TransferParticipantRecorder participantRecorder = new TransferParticipantRecorder();

        WorkOrderServiceImpl service = new WorkOrderServiceImpl();
        setField(service, "workOrderMapper", createMutableWorkOrderMapperProxy(workOrder, updateCount));
        setField(service, "workOrderPermissionService", new WorkOrderPermissionService() {
            @Override
            public boolean canTransfer(WorkOrder target) {
                return true;
            }
        });
        setField(service, "sysCompanyMapper", createCompanyMapperProxy(Arrays.asList(
                buildCompany(2002L, "浜岀骇缃戠偣", "SITE_SECOND"),
                buildCompany(1001L, "一级网点", "SITE_FIRST")
        )));
        setField(service, "sysCompanyTypeMapper", createCompanyTypeMapperProxy(
                buildCompanyType("SITE_FIRST", "SERVICE")
        ));
        setField(service, "firstSecondRelationMapper", createRelationMapperProxy(
                Collections.singletonList(buildRelation(1001L, 2002L))
        ));
        setField(service, "workOrderFlowMapper", createFlowMapperProxy(insertedFlows));
        setField(service, "workOrderParticipantService", participantRecorder);

        WorkOrderTransferDTO dto = new WorkOrderTransferDTO();
        dto.setWorkOrderId(workOrder.getId());
        dto.setTargetCompanyId(1001L);
        dto.setRemark("维修不了，转一级处理");

        runWithLoginContext(101L, new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                service.transfer(dto);
            }
        });

        Assert.assertEquals(1, updateCount[0]);
        Assert.assertEquals(Long.valueOf(1001L), workOrder.getCurrentAcceptCompanyId());
        Assert.assertEquals("SERVICE", workOrder.getCurrentAcceptSubjectType());
        Assert.assertNull(workOrder.getAssignedUserId());
        Assert.assertEquals(WorkOrderStatusConstants.MainStatus.PENDING_ASSIGN, workOrder.getMainStatus());
        Assert.assertEquals(Integer.valueOf(1), workOrder.getHasTransfer());
        Assert.assertEquals(Integer.valueOf(1), workOrder.getTransferCount());

        Assert.assertEquals(1, insertedFlows.size());
        WorkOrderFlow flow = insertedFlows.get(0);
        Assert.assertEquals("TRANSFER", flow.getActionType());
        Assert.assertEquals(WorkOrderStatusConstants.MainStatus.IN_PROGRESS, flow.getBeforeStatus());
        Assert.assertEquals(WorkOrderStatusConstants.MainStatus.PENDING_ASSIGN, flow.getAfterStatus());
        Assert.assertEquals(Long.valueOf(2002L), flow.getFromCompanyId());
        Assert.assertEquals(Long.valueOf(1001L), flow.getToCompanyId());
        Assert.assertEquals(Long.valueOf(2002L), flow.getOperatorCompanyId());
        Assert.assertEquals(Long.valueOf(101L), flow.getOperatorUserId());
        Assert.assertEquals("维修不了，转一级处理", flow.getRemark());

        Assert.assertEquals(Long.valueOf(3L), participantRecorder.workOrderId);
        Assert.assertEquals(Long.valueOf(2002L), participantRecorder.fromCompanyId);
        Assert.assertEquals("SERVICE", participantRecorder.fromSubjectType);
        Assert.assertEquals(Long.valueOf(1001L), participantRecorder.toCompanyId);
        Assert.assertEquals("SERVICE", participantRecorder.toSubjectType);
    }

    @Test
    public void shouldRejectTransferOutsideAllowedScopeForFirstLevelWorkOrder() throws Exception {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(4L);
        workOrder.setMainStatus(WorkOrderStatusConstants.MainStatus.COMPLETED);
        workOrder.setCurrentAcceptCompanyId(1001L);
        workOrder.setCurrentAcceptSubjectType("SERVICE");
        workOrder.setHqCompanyId(900L);

        WorkOrderServiceImpl service = new WorkOrderServiceImpl();
        setField(service, "workOrderMapper", createWorkOrderMapperProxy(workOrder));
        setField(service, "workOrderPermissionService", new WorkOrderPermissionService() {
            @Override
            public boolean canTransfer(WorkOrder target) {
                return true;
            }
        });
        setField(service, "sysCompanyMapper", createCompanyMapperProxy(Collections.singletonList(
                buildCompany(1001L, "一级网点", "SITE_FIRST")
        )));
        setField(service, "hqFirstContractMapper", createContractMapperProxy(1L));

        WorkOrderTransferDTO dto = new WorkOrderTransferDTO();
        dto.setWorkOrderId(workOrder.getId());
        dto.setTargetCompanyId(901L);

        try {
            service.transfer(dto);
            Assert.fail("棰勬湡搴旀嫆缁濊法瑙勫垯杞崟");
        } catch (ServiceException ex) {
            Assert.assertEquals("当前工单不允许转到该目标公司", ex.getMessage());
        }
    }

    @Test
    public void shouldCreateQuoteRevisionWhenRepairUpdatesQuote() throws Exception {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(6L);
        workOrder.setMainStatus(WorkOrderStatusConstants.MainStatus.IN_PROGRESS);
        workOrder.setCurrentAcceptCompanyId(3L);
        workOrder.setFaultDesc("涓绘澘鏁呴殰");

        WorkOrderQuote currentQuote = new WorkOrderQuote();
        currentQuote.setWorkOrderId(workOrder.getId());
        currentQuote.setFaultJudge("有故障");
        currentQuote.setQuoteAmount(new BigDecimal("100.00"));
        currentQuote.setQuoteDesc("棣栨鎶ヤ环");
        currentQuote.setIsCurrentValid(1);

        List<WorkOrderQuote> quotes = new ArrayList<>();
        quotes.add(currentQuote);
        List<WorkOrderQuote> insertedQuotes = new ArrayList<>();
        int[] updateCount = new int[1];
        UserParticipantRecorder participantRecorder = new UserParticipantRecorder();

        WorkOrderServiceImpl service = new WorkOrderServiceImpl();
        setField(service, "workOrderMapper", createWorkOrderMapperProxy(workOrder));
        setField(service, "workOrderQuoteMapper", createMutableQuoteMapperProxy(quotes, insertedQuotes, updateCount));
        setField(service, "workOrderRepairMapper", createNoopProxy(WorkOrderRepairMapper.class, "insert"));
        setField(service, "workOrderFaultMapper", createNoopProxy(com.jasic.aftersales.system.mapper.WorkOrderFaultMapper.class, "insert"));
        setField(service, "workOrderFaultPartMapper", createNoopProxy(WorkOrderFaultPartMapper.class, "insert"));
        setField(service, "workOrderFlowMapper", createNoopProxy(WorkOrderFlowMapper.class, "insert"));
        setField(service, "faultRepairConfigService", createFaultRepairConfigServiceProxy(Collections.emptyList()));
        setField(service, "sysFileService", createNoopProxy(com.jasic.aftersales.system.service.SysFileService.class, "replaceBizFiles"));
        setField(service, "workOrderPermissionService", new WorkOrderPermissionService() {
            @Override
            public boolean canSaveRepair(WorkOrder target) {
                return true;
            }
        });
        setField(service, "workOrderNotifyEventService", new WorkOrderNotifyEventService() {
            @Override
            public void recordRepairFinished(WorkOrder target, String detail) {
            }
        });
        setField(service, "workOrderUserParticipantService", participantRecorder);

        WorkOrderRepairDTO dto = new WorkOrderRepairDTO();
        dto.setWorkOrderId(workOrder.getId());
        dto.setQuoteAmount(new BigDecimal("120.00"));
        dto.setQuoteDesc("复检前调价");
        fillRepairSubmission(dto, "鏇存崲涓绘澘", "涓绘澘", 1);

        runWithLoginContext(101L, new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                service.saveRepair(dto);
            }
        });

        Assert.assertEquals(1, updateCount[0]);
        Assert.assertEquals(Integer.valueOf(0), currentQuote.getIsCurrentValid());
        Assert.assertEquals(1, insertedQuotes.size());
        Assert.assertEquals("有故障", insertedQuotes.get(0).getFaultJudge());
        Assert.assertEquals(0, insertedQuotes.get(0).getQuoteAmount().compareTo(new BigDecimal("120.00")));
        Assert.assertEquals("复检前调价", insertedQuotes.get(0).getQuoteDesc());
        Assert.assertEquals(Integer.valueOf(1), insertedQuotes.get(0).getIsCurrentValid());
        Assert.assertEquals(Arrays.asList("3-101-QUOTE", "3-101-REPAIR"), participantRecorder.records);
    }

    @Test
    public void shouldCompleteWorkOrderWhenRepairSubmitted() throws Exception {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(6L);
        workOrder.setMainStatus(WorkOrderStatusConstants.MainStatus.IN_PROGRESS);
        workOrder.setCurrentAcceptCompanyId(3L);
        workOrder.setFaultDesc("涓绘澘鏁呴殰");
        List<WorkOrderFlow> insertedFlows = new ArrayList<>();
        int[] updateCount = new int[1];
        int[] notifyCount = new int[1];
        UserParticipantRecorder participantRecorder = new UserParticipantRecorder();

        WorkOrderServiceImpl service = new WorkOrderServiceImpl();
        setField(service, "workOrderMapper", createMutableWorkOrderMapperProxy(workOrder, updateCount));
        setField(service, "workOrderQuoteMapper", createQuoteMapperProxy(Collections.emptyList()));
        setField(service, "workOrderRepairMapper", createNoopProxy(WorkOrderRepairMapper.class, "insert"));
        setField(service, "workOrderFaultMapper", createNoopProxy(com.jasic.aftersales.system.mapper.WorkOrderFaultMapper.class, "insert"));
        setField(service, "workOrderFaultPartMapper", createNoopProxy(WorkOrderFaultPartMapper.class, "insert"));
        setField(service, "workOrderFlowMapper", createFlowMapperProxy(insertedFlows));
        setField(service, "faultRepairConfigService", createFaultRepairConfigServiceProxy(Collections.emptyList()));
        setField(service, "sysFileService", createNoopProxy(com.jasic.aftersales.system.service.SysFileService.class, "replaceBizFiles"));
        setField(service, "workOrderPermissionService", new WorkOrderPermissionService() {
            @Override
            public boolean canSaveRepair(WorkOrder target) {
                return true;
            }
        });
        setField(service, "workOrderNotifyEventService", new WorkOrderNotifyEventService() {
            @Override
            public void recordRepairFinished(WorkOrder target, String detail) {
                notifyCount[0]++;
            }
        });
        setField(service, "workOrderUserParticipantService", participantRecorder);

        WorkOrderRepairDTO dto = new WorkOrderRepairDTO();
        dto.setWorkOrderId(workOrder.getId());
        fillRepairSubmission(dto, "鏇存崲涓绘澘", "涓绘澘", 1);

        runWithLoginContext(101L, new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                service.saveRepair(dto);
            }
        });

        Assert.assertEquals(1, updateCount[0]);
        Assert.assertEquals(WorkOrderStatusConstants.MainStatus.COMPLETED, workOrder.getMainStatus());
        Assert.assertNotNull(workOrder.getCompletedTime());
        Assert.assertEquals(1, insertedFlows.size());
        Assert.assertEquals("REPAIR_FINISH", insertedFlows.get(0).getActionType());
        Assert.assertEquals(WorkOrderStatusConstants.MainStatus.COMPLETED, insertedFlows.get(0).getAfterStatus());
        Assert.assertEquals(1, notifyCount[0]);
        Assert.assertEquals(Collections.singletonList("3-101-REPAIR"), participantRecorder.records);
    }

    @Test
    public void shouldUpdateRepairProductModelFromEnabledOptions() throws Exception {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(18L);
        workOrder.setBrandType(BrandTypeEnum.JASIC);
        workOrder.setMainStatus(WorkOrderStatusConstants.MainStatus.IN_PROGRESS);
        workOrder.setCurrentAcceptCompanyId(3L);
        workOrder.setHqCompanyId(900L);
        workOrder.setProductModel("   ");

        int[] updateCount = new int[1];
        WorkOrderServiceImpl service = new WorkOrderServiceImpl();
        setField(service, "workOrderMapper", createMutableWorkOrderMapperProxy(workOrder, updateCount));
        setField(service, "workOrderPermissionService", new WorkOrderPermissionService() {
            @Override
            public boolean canSaveRepair(WorkOrder target) {
                return true;
            }
        });
        setField(service, "faultRepairConfigService", createFaultRepairConfigServiceProxy(
                Collections.emptyList(),
                Arrays.asList("M-200", "M-300"),
                88L
        ));

        WorkOrderUpdateProductModelDTO dto = new WorkOrderUpdateProductModelDTO();
        dto.setWorkOrderId(workOrder.getId());
        dto.setProductModel("  M-200  ");

        service.updateRepairProductModel(dto);

        Assert.assertEquals(1, updateCount[0]);
        Assert.assertEquals("M-200", workOrder.getProductModel());
        Assert.assertEquals(Long.valueOf(88L), workOrder.getFaultRepairConfigId());
    }

    @Test
    public void shouldRejectRepairProductModelUpdateWhenAlreadyExists() throws Exception {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(19L);
        workOrder.setBrandType(BrandTypeEnum.JASIC);
        workOrder.setMainStatus(WorkOrderStatusConstants.MainStatus.IN_PROGRESS);
        workOrder.setCurrentAcceptCompanyId(3L);
        workOrder.setHqCompanyId(900L);
        workOrder.setProductModel("M-100");

        WorkOrderServiceImpl service = new WorkOrderServiceImpl();
        setField(service, "workOrderMapper", createWorkOrderMapperProxy(workOrder));
        setField(service, "workOrderPermissionService", new WorkOrderPermissionService() {
            @Override
            public boolean canSaveRepair(WorkOrder target) {
                return true;
            }
        });

        WorkOrderUpdateProductModelDTO dto = new WorkOrderUpdateProductModelDTO();
        dto.setWorkOrderId(workOrder.getId());
        dto.setProductModel("M-200");

        try {
            service.updateRepairProductModel(dto);
            Assert.fail("预期应拒绝重复补录机器型号");
        } catch (ServiceException ex) {
            Assert.assertEquals("当前工单已存在机器型号，不能重复补录", ex.getMessage());
        }
    }

    @Test
    public void shouldRequireProductModelBeforeRepairSubmissionForJasicWorkOrder() throws Exception {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(20L);
        workOrder.setBrandType(BrandTypeEnum.JASIC);
        workOrder.setMainStatus(WorkOrderStatusConstants.MainStatus.IN_PROGRESS);
        workOrder.setCurrentAcceptCompanyId(3L);
        workOrder.setFaultDesc("涓绘澘鏁呴殰");

        WorkOrderServiceImpl service = new WorkOrderServiceImpl();
        setField(service, "workOrderMapper", createWorkOrderMapperProxy(workOrder));
        setField(service, "workOrderPermissionService", new WorkOrderPermissionService() {
            @Override
            public boolean canSaveRepair(WorkOrder target) {
                return true;
            }
        });

        WorkOrderRepairDTO dto = new WorkOrderRepairDTO();
        dto.setWorkOrderId(workOrder.getId());
        fillRepairSubmission(dto, "鏇存崲涓绘澘", "涓绘澘", 1);

        try {
            service.saveRepair(dto);
            Assert.fail("预期应拒绝未补录机型的维修登记");
        } catch (ServiceException ex) {
            Assert.assertEquals("佳士品牌工单缺少机器型号，请先补录机器型号后再进行维修登记", ex.getMessage());
        }
    }

    @Test
    public void shouldRequireProductModelBeforeReviewSubmissionForJasicWorkOrder() throws Exception {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(21L);
        workOrder.setBrandType(BrandTypeEnum.JASIC);
        workOrder.setMainStatus(WorkOrderStatusConstants.MainStatus.COMPLETED);
        workOrder.setCurrentAcceptCompanyId(3L);
        workOrder.setFaultDesc("涓绘澘鏁呴殰");

        WorkOrderServiceImpl service = new WorkOrderServiceImpl();
        setField(service, "workOrderMapper", createWorkOrderMapperProxy(workOrder));
        setField(service, "workOrderPermissionService", new WorkOrderPermissionService() {
            @Override
            public boolean canReview(WorkOrder target) {
                return true;
            }
        });

        WorkOrderReviewDTO dto = new WorkOrderReviewDTO();
        dto.setWorkOrderId(workOrder.getId());
        dto.setRepairDesc("澶嶆纭");

        try {
            service.saveReview(dto);
            Assert.fail("棰勬湡搴旀嫆缁濇湭琛ュ綍鏈哄瀷鐨勫妫€鐧昏");
        } catch (ServiceException ex) {
            Assert.assertEquals("佳士品牌工单缺少机器型号，请先补录机器型号后再进行复检登记", ex.getMessage());
        }
    }

    @Test
    public void shouldRequireFaultRepairConfigBindingBeforeRepairSubmissionForJasicWorkOrder() throws Exception {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(21L);
        workOrder.setBrandType(BrandTypeEnum.JASIC);
        workOrder.setProductModel("M-200");
        workOrder.setMainStatus(WorkOrderStatusConstants.MainStatus.IN_PROGRESS);
        workOrder.setCurrentAcceptCompanyId(3L);

        WorkOrderServiceImpl service = new WorkOrderServiceImpl();
        setField(service, "workOrderMapper", createWorkOrderMapperProxy(workOrder));
        setField(service, "workOrderPermissionService", new WorkOrderPermissionService() {
            @Override
            public boolean canSaveRepair(WorkOrder target) {
                return true;
            }
        });

        WorkOrderRepairDTO dto = new WorkOrderRepairDTO();
        dto.setWorkOrderId(workOrder.getId());
        fillRepairSubmission(dto, "鏇存崲涓绘澘", "涓绘澘", 1);

        try {
            service.saveRepair(dto);
            Assert.fail("预期应拒绝未绑定配置的维修登记");
        } catch (ServiceException ex) {
            Assert.assertEquals("当前总部未配置故障与维修配置，请先维护", ex.getMessage());
        }
    }

    @Test
    public void shouldRejectRepairQuoteRevisionWithoutCurrentQuote() throws Exception {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(7L);
        workOrder.setMainStatus(WorkOrderStatusConstants.MainStatus.IN_PROGRESS);
        workOrder.setCurrentAcceptCompanyId(3L);

        WorkOrderServiceImpl service = new WorkOrderServiceImpl();
        setField(service, "workOrderMapper", createWorkOrderMapperProxy(workOrder));
        setField(service, "workOrderQuoteMapper", createQuoteMapperProxy(Collections.emptyList()));
        setField(service, "faultRepairConfigService", createFaultRepairConfigServiceProxy(Collections.emptyList()));
        setField(service, "workOrderPermissionService", new WorkOrderPermissionService() {
            @Override
            public boolean canSaveRepair(WorkOrder target) {
                return true;
            }
        });

        WorkOrderRepairDTO dto = new WorkOrderRepairDTO();
        dto.setWorkOrderId(workOrder.getId());
        dto.setQuoteAmount(new BigDecimal("88.00"));
        fillRepairSubmission(dto, "鏇存崲椋庢墖", "椋庢墖", 1);

        try {
            service.saveRepair(dto);
            Assert.fail("棰勬湡搴旀嫆缁濇棤鏈夋晥鎶ヤ环鏃剁殑缁翠慨鏀逛环");
        } catch (ServiceException ex) {
            Assert.assertEquals("请先提交报价，再在维修登记中调整报价", ex.getMessage());
        }
    }

    @Test
    public void shouldRequireReturnVoucherWhenClosingByMail() throws Exception {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(10L);
        workOrder.setMainStatus(WorkOrderStatusConstants.MainStatus.COMPLETED);
        workOrder.setCurrentAcceptCompanyId(3L);

        WorkOrderServiceImpl service = new WorkOrderServiceImpl();
        setField(service, "workOrderMapper", createWorkOrderMapperProxy(workOrder));
        setField(service, "workOrderPermissionService", new WorkOrderPermissionService() {
            @Override
            public boolean canClose(WorkOrder target) {
                return true;
            }
        });

        WorkOrderCloseDTO dto = new WorkOrderCloseDTO();
        dto.setWorkOrderId(workOrder.getId());
        dto.setReturnMethod("回寄");
        dto.setCloseReason("客户要求回寄");
        dto.setReturnExpressNo("   ");

        try {
            service.close(dto);
            Assert.fail("棰勬湡搴旀嫆缁濈己灏戝洖瀵勫嚟璇佺殑鍏抽棴璇锋眰");
        } catch (ServiceException ex) {
            Assert.assertEquals("回寄时必须上传回寄凭证", ex.getMessage());
        }
    }

    @Test
    public void shouldAllowBlankReturnExpressNoWhenClosingByMailWithVoucher() throws Exception {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(16L);
        workOrder.setMainStatus(WorkOrderStatusConstants.MainStatus.COMPLETED);
        workOrder.setCurrentAcceptCompanyId(3L);
        WorkOrderQuote currentQuote = new WorkOrderQuote();
        currentQuote.setWorkOrderId(workOrder.getId());
        currentQuote.setFaultJudge("有故障");
        currentQuote.setIsCurrentValid(1);
        int[] inviteCount = new int[1];

        WorkOrderServiceImpl service = new WorkOrderServiceImpl();
        setField(service, "workOrderMapper", createMutableWorkOrderMapperProxy(workOrder, new int[1]));
        setField(service, "workOrderQuoteMapper", createQuoteMapperProxy(Collections.singletonList(currentQuote)));
        setField(service, "workOrderFlowMapper", createNoopProxy(WorkOrderFlowMapper.class, "insert"));
        setField(service, "workOrderPermissionService", new WorkOrderPermissionService() {
            @Override
            public boolean canClose(WorkOrder target) {
                return true;
            }
        });
        setField(service, "workOrderNotifyEventService", new WorkOrderNotifyEventService() {
            @Override
            public void recordEvaluationInvite(WorkOrder target) {
                inviteCount[0]++;
            }
        });
        setField(service, "sysFileService", createNoopProxy(com.jasic.aftersales.system.service.SysFileService.class, "replaceBizFiles"));

        WorkOrderCloseDTO dto = new WorkOrderCloseDTO();
        dto.setWorkOrderId(workOrder.getId());
        dto.setReturnMethod("回寄");
        dto.setReturnExpressNo("   ");
        dto.setReturnVoucherFileIds(Collections.singletonList(128L));
        dto.setCloseReason("客户要求回寄");

        runWithLoginContext(101L, new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                service.close(dto);
            }
        });

        Assert.assertEquals(WorkOrderStatusConstants.MainStatus.CLOSED, workOrder.getMainStatus());
        Assert.assertEquals(WorkOrderStatusConstants.EvaluateStatus.PENDING_EVALUATE, workOrder.getEvaluateStatus());
        Assert.assertEquals("回寄", workOrder.getReturnMethod());
        Assert.assertNull(workOrder.getReturnExpressNo());
        Assert.assertEquals("客户要求回寄", workOrder.getCloseReason());
        Assert.assertEquals(1, inviteCount[0]);
    }

    @Test
    public void shouldCloseNoFaultWorkOrderAsNotOpenWithoutInvite() throws Exception {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(11L);
        workOrder.setMainStatus(WorkOrderStatusConstants.MainStatus.COMPLETED);
        workOrder.setCurrentAcceptCompanyId(3L);
        WorkOrderQuote currentQuote = new WorkOrderQuote();
        currentQuote.setWorkOrderId(workOrder.getId());
        currentQuote.setFaultJudge("无故障");
        currentQuote.setIsCurrentValid(1);
        int[] updateCount = new int[1];
        int[] inviteCount = new int[1];

        WorkOrderServiceImpl service = new WorkOrderServiceImpl();
        setField(service, "workOrderMapper", createMutableWorkOrderMapperProxy(workOrder, updateCount));
        setField(service, "workOrderQuoteMapper", createQuoteMapperProxy(Collections.singletonList(currentQuote)));
        setField(service, "workOrderFlowMapper", createNoopProxy(WorkOrderFlowMapper.class, "insert"));
        setField(service, "workOrderPermissionService", new WorkOrderPermissionService() {
            @Override
            public boolean canClose(WorkOrder target) {
                return true;
            }
        });
        setField(service, "workOrderNotifyEventService", new WorkOrderNotifyEventService() {
            @Override
            public void recordEvaluationInvite(WorkOrder target) {
                inviteCount[0]++;
            }
        });
        setField(service, "sysFileService", createNoopProxy(com.jasic.aftersales.system.service.SysFileService.class, "replaceBizFiles"));

        WorkOrderCloseDTO dto = new WorkOrderCloseDTO();
        dto.setWorkOrderId(workOrder.getId());
        dto.setReturnMethod("自提");
        dto.setCloseReason("无故障，客户自提");

        runWithLoginContext(101L, new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                service.close(dto);
            }
        });

        Assert.assertEquals(1, updateCount[0]);
        Assert.assertEquals(WorkOrderStatusConstants.MainStatus.CLOSED, workOrder.getMainStatus());
        Assert.assertEquals(WorkOrderStatusConstants.EvaluateStatus.NOT_OPEN, workOrder.getEvaluateStatus());
        Assert.assertEquals("自提", workOrder.getReturnMethod());
        Assert.assertEquals("无故障，客户自提", workOrder.getCloseReason());
        Assert.assertEquals(0, inviteCount[0]);
    }

    @Test
    public void shouldCloseFaultWorkOrderAsPendingEvaluateAndRecordInvite() throws Exception {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(12L);
        workOrder.setMainStatus(WorkOrderStatusConstants.MainStatus.COMPLETED);
        workOrder.setCurrentAcceptCompanyId(3L);
        WorkOrderQuote currentQuote = new WorkOrderQuote();
        currentQuote.setWorkOrderId(workOrder.getId());
        currentQuote.setFaultJudge("有故障");
        currentQuote.setIsCurrentValid(1);
        int[] inviteCount = new int[1];

        WorkOrderServiceImpl service = new WorkOrderServiceImpl();
        setField(service, "workOrderMapper", createMutableWorkOrderMapperProxy(workOrder, new int[1]));
        setField(service, "workOrderQuoteMapper", createQuoteMapperProxy(Collections.singletonList(currentQuote)));
        setField(service, "workOrderFlowMapper", createNoopProxy(WorkOrderFlowMapper.class, "insert"));
        setField(service, "workOrderPermissionService", new WorkOrderPermissionService() {
            @Override
            public boolean canClose(WorkOrder target) {
                return true;
            }
        });
        setField(service, "workOrderNotifyEventService", new WorkOrderNotifyEventService() {
            @Override
            public void recordEvaluationInvite(WorkOrder target) {
                inviteCount[0]++;
            }
        });
        setField(service, "sysFileService", createNoopProxy(com.jasic.aftersales.system.service.SysFileService.class, "replaceBizFiles"));

        WorkOrderCloseDTO dto = new WorkOrderCloseDTO();
        dto.setWorkOrderId(workOrder.getId());
        dto.setReturnMethod("自提");
        dto.setCloseReason("维修完成");

        runWithLoginContext(101L, new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                service.close(dto);
            }
        });

        Assert.assertEquals(WorkOrderStatusConstants.MainStatus.CLOSED, workOrder.getMainStatus());
        Assert.assertEquals(WorkOrderStatusConstants.EvaluateStatus.PENDING_EVALUATE, workOrder.getEvaluateStatus());
        Assert.assertEquals(1, inviteCount[0]);
    }

    @Test
    public void shouldSkipEvaluationInviteWhenCurrentQuoteIsNoFault() throws Exception {
        WorkOrderQuote currentQuote = new WorkOrderQuote();
        currentQuote.setWorkOrderId(4L);
        currentQuote.setFaultJudge("无故障");
        currentQuote.setIsCurrentValid(1);

        WorkOrderServiceImpl service = new WorkOrderServiceImpl();
        setField(service, "workOrderQuoteMapper", createQuoteMapperProxy(Collections.singletonList(currentQuote)));
        Method isNoFaultWorkOrder = WorkOrderServiceImpl.class
                .getDeclaredMethod("isNoFaultWorkOrder", Long.class);
        isNoFaultWorkOrder.setAccessible(true);

        Boolean noFault = (Boolean) isNoFaultWorkOrder.invoke(service, 4L);

        Assert.assertTrue(noFault);
    }

    private WorkOrderMapper createWorkOrderMapperProxy(WorkOrder workOrder) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectById".equals(method.getName())) {
                    return workOrder;
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

    private WorkOrderMapper createPagedWorkOrderMapperProxy(List<WorkOrderListVO> records) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectWorkOrderPage".equals(method.getName())) {
                    Page<WorkOrderListVO> page = new Page<>(1, records.size());
                    page.setRecords(new ArrayList<>(records));
                    page.setTotal(records.size());
                    return page;
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

    private WorkOrderMapper createMutableWorkOrderMapperProxy(WorkOrder workOrder, int[] updateCount) {
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

    private WorkOrderRepairMapper createWorkOrderRepairMapperProxy(List<WorkOrderRepair> repairs) {
        InvocationHandler handler = new InvocationHandler() {
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

    private com.jasic.aftersales.system.mapper.WorkOrderFaultMapper createWorkOrderFaultMapperProxy(List<WorkOrderFault> faults) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectList".equals(method.getName())) {
                    return faults;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (com.jasic.aftersales.system.mapper.WorkOrderFaultMapper) Proxy.newProxyInstance(
                com.jasic.aftersales.system.mapper.WorkOrderFaultMapper.class.getClassLoader(),
                new Class<?>[]{com.jasic.aftersales.system.mapper.WorkOrderFaultMapper.class},
                handler
        );
    }

    private WorkOrderCustomerMapper createWorkOrderCustomerMapperProxy(List<WorkOrderCustomer> customers) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectList".equals(method.getName())) {
                    return customers;
                }
                if ("updateById".equals(method.getName())) {
                    return 1;
                }
                if ("insert".equals(method.getName())) {
                    Assert.fail("鏈疆涓嶅簲鑷姩鍒涘缓瀹㈡埛");
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (WorkOrderCustomerMapper) Proxy.newProxyInstance(
                WorkOrderCustomerMapper.class.getClassLoader(),
                new Class<?>[]{WorkOrderCustomerMapper.class},
                handler
        );
    }

    private SysCompanyMapper createCompanyMapperProxy(List<SysCompany> companies) {
        Map<Long, SysCompany> companyMap = new LinkedHashMap<>();
        for (SysCompany company : companies) {
            companyMap.put(company.getId(), company);
        }
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectById".equals(method.getName())) {
                    return companyMap.get(args[0]);
                }
                if ("selectBatchIds".equals(method.getName())) {
                    Iterable<?> ids = (Iterable<?>) args[0];
                    List<SysCompany> result = new ArrayList<>();
                    for (Object id : ids) {
                        SysCompany company = companyMap.get(id);
                        if (company != null) {
                            result.add(company);
                        }
                    }
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

    private SysCompanyTypeMapper createCompanyTypeMapperProxy(SysCompanyType companyType) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectOne".equals(method.getName())) {
                    return companyType;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (SysCompanyTypeMapper) Proxy.newProxyInstance(
                SysCompanyTypeMapper.class.getClassLoader(),
                new Class<?>[]{SysCompanyTypeMapper.class},
                handler
        );
    }

    private FirstSecondRelationMapper createRelationMapperProxy(List<FirstSecondRelation> relations) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectList".equals(method.getName())) {
                    return relations;
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

    private HqFirstContractMapper createContractMapperProxy(Long count) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectCount".equals(method.getName())) {
                    return count;
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

    private WorkOrderFlowMapper createFlowMapperProxy(List<WorkOrderFlow> insertedFlows) {
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

    private WorkOrderQuoteMapper createQuoteMapperProxy(List<WorkOrderQuote> quotes) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectList".equals(method.getName())) {
                    return quotes;
                }
                if ("update".equals(method.getName())) {
                    return 1;
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

    private WorkOrderQuoteMapper createMutableQuoteMapperProxy(List<WorkOrderQuote> quotes,
                                                               List<WorkOrderQuote> insertedQuotes,
                                                               int[] updateCount) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectList".equals(method.getName())) {
                    return new ArrayList<>(quotes);
                }
                if ("update".equals(method.getName())) {
                    updateCount[0]++;
                    for (WorkOrderQuote quote : quotes) {
                        quote.setIsCurrentValid(0);
                    }
                    return quotes.size();
                }
                if ("insert".equals(method.getName())) {
                    WorkOrderQuote quote = (WorkOrderQuote) args[0];
                    insertedQuotes.add(quote);
                    quotes.add(0, quote);
                    return 1;
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

    @SuppressWarnings("unchecked")
    private <T> T createNoopProxy(Class<T> type, String successMethodName) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if (successMethodName.equals(method.getName())) {
                    return 1;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    private IFaultRepairConfigService createFaultRepairConfigServiceProxy(List<WorkOrderRepairFaultOptionVO> options) {
        return createFaultRepairConfigServiceProxy(options, Collections.emptyList(), null);
    }

    private IFaultRepairConfigService createFaultRepairConfigServiceProxy(List<WorkOrderRepairFaultOptionVO> options,
                                                                          List<String> productModels) {
        return createFaultRepairConfigServiceProxy(options, productModels, null);
    }

    private IFaultRepairConfigService createFaultRepairConfigServiceProxy(List<WorkOrderRepairFaultOptionVO> options,
                                                                          List<String> productModels,
                                                                          Long matchedConfigId) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("listRepairFaultOptions".equals(method.getName())) {
                    return options;
                }
                if ("listRepairFaultOptionsByConfigId".equals(method.getName())) {
                    return options;
                }
                if ("listEnabledProductModels".equals(method.getName())) {
                    return productModels;
                }
                if ("findEnabledConfigId".equals(method.getName())) {
                    return matchedConfigId;
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

    private ISysConfigService createSysConfigServiceProxy(String defaultHqCompanyId) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("getValueByKey".equals(method.getName())
                        && args != null
                        && args.length > 0
                        && ("defaultHqCompanyId".equals(args[0]) || "default.hq.company.id".equals(args[0]))) {
                    return defaultHqCompanyId;
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

    private SysUserMapper createSysUserMapperProxy(SysUser currentUser) {
        return createSysUserMapperProxy(Collections.singletonList(currentUser));
    }

    private SysUserMapper createSysUserMapperProxy(List<SysUser> users) {
        Map<Long, SysUser> userMap = new LinkedHashMap<>();
        for (SysUser user : users) {
            userMap.put(user.getId(), user);
        }
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectById".equals(method.getName())) {
                    return userMap.get(args[0]);
                }
                if ("selectBatchIds".equals(method.getName())) {
                    Iterable<?> ids = (Iterable<?>) args[0];
                    List<SysUser> result = new ArrayList<>();
                    for (Object id : ids) {
                        SysUser user = userMap.get(id);
                        if (user != null) {
                            result.add(user);
                        }
                    }
                    return result;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (SysUserMapper) Proxy.newProxyInstance(
                SysUserMapper.class.getClassLoader(),
                new Class<?>[]{SysUserMapper.class},
                handler
        );
    }

    private SysUserCompanyMapper createUserCompanyMapperProxy(Long companyId, List<Long> userIds) {
        List<SysUserCompany> userCompanies = new ArrayList<>();
        for (Long userId : userIds) {
            SysUserCompany userCompany = new SysUserCompany();
            userCompany.setCompanyId(companyId);
            userCompany.setUserId(userId);
            userCompanies.add(userCompany);
        }
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectList".equals(method.getName())) {
                    return userCompanies;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (SysUserCompanyMapper) Proxy.newProxyInstance(
                SysUserCompanyMapper.class.getClassLoader(),
                new Class<?>[]{SysUserCompanyMapper.class},
                handler
        );
    }

    private SysMenuMapper createSysMenuMapperProxy(Map<Long, Set<String>> userPermsMap) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectPermsByUserIdAndCompanyId".equals(method.getName())) {
                    Long userId = (Long) args[0];
                    Set<String> perms = userPermsMap.get(userId);
                    return perms == null ? Collections.emptySet() : new LinkedHashSet<>(perms);
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (SysMenuMapper) Proxy.newProxyInstance(
                SysMenuMapper.class.getClassLoader(),
                new Class<?>[]{SysMenuMapper.class},
                handler
        );
    }

    private Object invokePrivateMethod(Object target, String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private Object invokeGetter(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method.invoke(target);
    }

    private WorkOrderRepairFaultOptionVO buildRepairFaultOption(String faultDesc, List<String> repairOptions) {
        WorkOrderRepairFaultOptionVO option = new WorkOrderRepairFaultOptionVO();
        option.setFaultDesc(faultDesc);
        option.setRepairOptions(repairOptions);
        return option;
    }

    private void fillRepairSubmission(WorkOrderRepairDTO dto, String repairDesc, String partName, Integer partQty) {
        dto.setRepairDesc(repairDesc);
        WorkOrderFaultPartItemDTO partItem = new WorkOrderFaultPartItemDTO();
        partItem.setPartName(partName);
        partItem.setPartQty(partQty);
        dto.setPartList(Collections.singletonList(partItem));
    }

    private SysCompany buildCompany(Long companyId, String companyName, String typeCode) {
        SysCompany company = new SysCompany();
        company.setId(companyId);
        company.setCompanyName(companyName);
        company.setTypeCode(typeCode);
        company.setStatus(1);
        return company;
    }

    private SysUser buildUser(Long userId, String realName, String phone, Integer status) {
        SysUser user = new SysUser();
        user.setId(userId);
        user.setRealName(realName);
        user.setPhone(phone);
        user.setStatus(status);
        return user;
    }

    private SysCompanyType buildCompanyType(String typeCode, String subjectType) {
        SysCompanyType companyType = new SysCompanyType();
        companyType.setTypeCode(typeCode);
        companyType.setSubjectType(subjectType);
        return companyType;
    }

    private FirstSecondRelation buildRelation(Long firstCompanyId, Long secondCompanyId) {
        FirstSecondRelation relation = new FirstSecondRelation();
        relation.setFirstCompanyId(firstCompanyId);
        relation.setSecondCompanyId(secondCompanyId);
        relation.setStatus(1);
        return relation;
    }

    private WorkOrder buildRepairWorkOrder() {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(5L);
        workOrder.setHqCompanyId(9L);
        workOrder.setProductCode("P-100");
        workOrder.setProductModel("M-200");
        return workOrder;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = WorkOrderServiceImpl.class.getDeclaredField(fieldName);
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

    private void runWithLoginContext(Long userId, ThrowingRunnable runnable) throws Exception {
        SaManager.setSaTokenContext(new SaTokenContextForThreadLocal());
        SaTokenContextForThreadLocalStorage.setBox(new MockSaRequest(), new MockSaResponse(), new MockSaStorage());
        StpUtil.login(userId);
        try {
            runnable.run();
        } finally {
            try {
                StpUtil.logout();
            } finally {
                SaTokenContextForThreadLocalStorage.clearBox();
            }
        }
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static class TransferParticipantRecorder extends WorkOrderParticipantService {

        private Long workOrderId;
        private Long fromCompanyId;
        private String fromSubjectType;
        private Long toCompanyId;
        private String toSubjectType;

        @Override
        public void transferParticipant(Long workOrderId, Long fromCompanyId, String fromSubjectType,
                                        Long toCompanyId, String toSubjectType) {
            this.workOrderId = workOrderId;
            this.fromCompanyId = fromCompanyId;
            this.fromSubjectType = fromSubjectType;
            this.toCompanyId = toCompanyId;
            this.toSubjectType = toSubjectType;
        }
    }

    private static class UserParticipantRecorder extends WorkOrderUserParticipantService {

        private final List<String> records = new ArrayList<>();

        @Override
        public void recordAction(Long workOrderId, Long companyId, Long userId,
                                 WorkOrderUserParticipationActionEnum action, LocalDateTime actionTime) {
            records.add(String.valueOf(companyId) + "-" + String.valueOf(userId) + "-" + action.getCode());
        }
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




