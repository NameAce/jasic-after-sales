package com.jasic.aftersales.system.service.impl;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaTokenContextForThreadLocal;
import cn.dev33.satoken.context.SaTokenContextForThreadLocalStorage;
import cn.dev33.satoken.context.model.SaRequest;
import cn.dev33.satoken.context.model.SaResponse;
import cn.dev33.satoken.context.model.SaStorage;
import cn.dev33.satoken.stp.StpUtil;
import com.jasic.aftersales.common.constant.WorkOrderStatusConstants;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.domain.dto.WorkOrderCloseDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderFaultItemDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderRepairDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderReviewDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderTransferDTO;
import com.jasic.aftersales.system.domain.entity.FirstSecondRelation;
import com.jasic.aftersales.system.domain.entity.HqFirstContract;
import com.jasic.aftersales.system.domain.entity.SysCompany;
import com.jasic.aftersales.system.domain.entity.SysCompanyType;
import com.jasic.aftersales.system.domain.entity.WorkOrder;
import com.jasic.aftersales.system.domain.entity.WorkOrderFlow;
import com.jasic.aftersales.system.domain.entity.WorkOrderQuote;
import com.jasic.aftersales.system.domain.entity.WorkOrderReview;
import com.jasic.aftersales.system.domain.vo.WorkOrderRepairFaultOptionVO;
import com.jasic.aftersales.system.mapper.FirstSecondRelationMapper;
import com.jasic.aftersales.system.mapper.HqFirstContractMapper;
import com.jasic.aftersales.system.mapper.SysCompanyMapper;
import com.jasic.aftersales.system.mapper.SysCompanyTypeMapper;
import com.jasic.aftersales.system.mapper.WorkOrderFlowMapper;
import com.jasic.aftersales.system.mapper.WorkOrderMapper;
import com.jasic.aftersales.system.mapper.WorkOrderQuoteMapper;
import com.jasic.aftersales.system.mapper.WorkOrderRepairMapper;
import com.jasic.aftersales.system.mapper.WorkOrderReviewMapper;
import com.jasic.aftersales.system.service.IFaultRepairConfigService;
import com.jasic.aftersales.system.service.WorkOrderNotifyEventService;
import com.jasic.aftersales.system.service.WorkOrderParticipantService;
import com.jasic.aftersales.system.service.WorkOrderPermissionService;
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
import java.util.List;
import java.util.Map;

/**
 * 工单业务服务测试。
 *
 * @author Codex
 * @date 2026/04/01
 */
public class WorkOrderServiceImplTest {

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
        dto.setFaults(Collections.emptyList());

        try {
            service.saveRepair(dto);
            Assert.fail("预期应拒绝空维修登记");
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
            method.invoke(service, "待确认", "故障判断不能为空");
            Assert.fail("预期应拒绝非枚举故障判断");
        } catch (InvocationTargetException ex) {
            Assert.assertTrue(ex.getCause() instanceof ServiceException);
            Assert.assertEquals("故障判定只能为有故障或无故障", ex.getCause().getMessage());
        }
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
                buildCompany(2002L, "二级网点", "SECOND"),
                buildCompany(1001L, "一级网点", "FIRST")
        )));
        setField(service, "sysCompanyTypeMapper", createCompanyTypeMapperProxy(
                buildCompanyType("FIRST", "SERVICE")
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
                buildCompany(1001L, "一级网点", "FIRST")
        )));
        setField(service, "hqFirstContractMapper", createContractMapperProxy(1L));

        WorkOrderTransferDTO dto = new WorkOrderTransferDTO();
        dto.setWorkOrderId(workOrder.getId());
        dto.setTargetCompanyId(901L);

        try {
            service.transfer(dto);
            Assert.fail("预期应拒绝跨规则转单");
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

        WorkOrderQuote currentQuote = new WorkOrderQuote();
        currentQuote.setWorkOrderId(workOrder.getId());
        currentQuote.setFaultJudge("有故障");
        currentQuote.setQuoteAmount(new BigDecimal("100.00"));
        currentQuote.setQuoteDesc("首次报价");
        currentQuote.setIsCurrentValid(1);

        List<WorkOrderQuote> quotes = new ArrayList<>();
        quotes.add(currentQuote);
        List<WorkOrderQuote> insertedQuotes = new ArrayList<>();
        int[] updateCount = new int[1];

        WorkOrderServiceImpl service = new WorkOrderServiceImpl();
        setField(service, "workOrderMapper", createWorkOrderMapperProxy(workOrder));
        setField(service, "workOrderQuoteMapper", createMutableQuoteMapperProxy(quotes, insertedQuotes, updateCount));
        setField(service, "workOrderRepairMapper", createNoopProxy(WorkOrderRepairMapper.class, "insert"));
        setField(service, "workOrderFlowMapper", createNoopProxy(WorkOrderFlowMapper.class, "insert"));
        setField(service, "workOrderPermissionService", new WorkOrderPermissionService() {
            @Override
            public boolean canSaveRepair(WorkOrder target) {
                return true;
            }
        });

        WorkOrderRepairDTO dto = new WorkOrderRepairDTO();
        dto.setWorkOrderId(workOrder.getId());
        dto.setRepairSummary("更换主板");
        dto.setQuoteAmount(new BigDecimal("120.00"));
        dto.setQuoteDesc("复检前调价");

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
        setField(service, "workOrderPermissionService", new WorkOrderPermissionService() {
            @Override
            public boolean canSaveRepair(WorkOrder target) {
                return true;
            }
        });

        WorkOrderRepairDTO dto = new WorkOrderRepairDTO();
        dto.setWorkOrderId(workOrder.getId());
        dto.setRepairSummary("更换风扇");
        dto.setQuoteAmount(new BigDecimal("88.00"));

        try {
            service.saveRepair(dto);
            Assert.fail("预期应拒绝无有效报价时的维修改价");
        } catch (ServiceException ex) {
            Assert.assertEquals("请先提交报价，再在维修登记中调整报价", ex.getMessage());
        }
    }

    @Test
    public void shouldMoveWorkOrderBackToInProgressWhenReviewRequestsContinueRepair() throws Exception {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(8L);
        workOrder.setMainStatus(WorkOrderStatusConstants.MainStatus.COMPLETED);
        workOrder.setCurrentAcceptCompanyId(3L);
        workOrder.setCompletedTime(LocalDateTime.now());
        List<WorkOrderReview> insertedReviews = new ArrayList<>();
        int[] updateCount = new int[1];

        WorkOrderServiceImpl service = new WorkOrderServiceImpl();
        setField(service, "workOrderMapper", createMutableWorkOrderMapperProxy(workOrder, updateCount));
        setField(service, "workOrderReviewMapper", createReviewMapperProxy(insertedReviews));
        setField(service, "workOrderFlowMapper", createNoopProxy(WorkOrderFlowMapper.class, "insert"));
        setField(service, "workOrderPermissionService", new WorkOrderPermissionService() {
            @Override
            public boolean canReview(WorkOrder target) {
                return true;
            }
        });

        WorkOrderReviewDTO dto = new WorkOrderReviewDTO();
        dto.setWorkOrderId(workOrder.getId());
        dto.setReviewResult("继续维修");
        dto.setReviewDesc("复检后仍需继续处理");

        runWithLoginContext(101L, new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                service.saveReview(dto);
            }
        });

        Assert.assertEquals(1, updateCount[0]);
        Assert.assertEquals(WorkOrderStatusConstants.MainStatus.IN_PROGRESS, workOrder.getMainStatus());
        Assert.assertNull(workOrder.getCompletedTime());
        Assert.assertEquals(1, insertedReviews.size());
        Assert.assertEquals(Integer.valueOf(1), insertedReviews.get(0).getIsContinueRepair());
        Assert.assertEquals(Long.valueOf(3L), insertedReviews.get(0).getCompanyId());
        Assert.assertEquals(Long.valueOf(101L), insertedReviews.get(0).getReviewUserId());
    }

    @Test
    public void shouldKeepCompletedWhenReviewPasses() throws Exception {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(9L);
        workOrder.setMainStatus(WorkOrderStatusConstants.MainStatus.COMPLETED);
        workOrder.setCurrentAcceptCompanyId(3L);
        workOrder.setCompletedTime(LocalDateTime.now());
        List<WorkOrderReview> insertedReviews = new ArrayList<>();
        int[] updateCount = new int[1];

        WorkOrderServiceImpl service = new WorkOrderServiceImpl();
        setField(service, "workOrderMapper", createMutableWorkOrderMapperProxy(workOrder, updateCount));
        setField(service, "workOrderReviewMapper", createReviewMapperProxy(insertedReviews));
        setField(service, "workOrderFlowMapper", createNoopProxy(WorkOrderFlowMapper.class, "insert"));
        setField(service, "workOrderPermissionService", new WorkOrderPermissionService() {
            @Override
            public boolean canReview(WorkOrder target) {
                return true;
            }
        });

        WorkOrderReviewDTO dto = new WorkOrderReviewDTO();
        dto.setWorkOrderId(workOrder.getId());
        dto.setReviewResult("通过");
        dto.setReviewDesc("复检通过");

        runWithLoginContext(101L, new ThrowingRunnable() {
            @Override
            public void run() throws Exception {
                service.saveReview(dto);
            }
        });

        Assert.assertEquals(0, updateCount[0]);
        Assert.assertEquals(WorkOrderStatusConstants.MainStatus.COMPLETED, workOrder.getMainStatus());
        Assert.assertEquals(1, insertedReviews.size());
        Assert.assertEquals(Integer.valueOf(0), insertedReviews.get(0).getIsContinueRepair());
    }

    @Test
    public void shouldRequireReturnExpressNoWhenClosingByMail() throws Exception {
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
            Assert.fail("预期应拒绝缺少回寄单号的关闭请求");
        } catch (ServiceException ex) {
            Assert.assertEquals("回寄时必须填写回寄快递单号", ex.getMessage());
        }
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
    public void shouldDeriveContinueRepairFromReviewResult() throws Exception {
        WorkOrderServiceImpl service = new WorkOrderServiceImpl();
        Method normalizeReviewResult = WorkOrderServiceImpl.class
                .getDeclaredMethod("normalizeReviewResult", String.class);
        normalizeReviewResult.setAccessible(true);
        Method resolveContinueRepair = WorkOrderServiceImpl.class
                .getDeclaredMethod("resolveContinueRepair", String.class);
        resolveContinueRepair.setAccessible(true);

        String reviewResult = (String) normalizeReviewResult.invoke(service, "继续维修");
        Integer continueRepair = (Integer) resolveContinueRepair.invoke(service, reviewResult);

        Assert.assertEquals("继续维修", reviewResult);
        Assert.assertEquals(Integer.valueOf(1), continueRepair);
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

    @Test
    public void shouldRequirePartDescWhenFaultItemHasRepairContent() throws Exception {
        WorkOrderServiceImpl service = new WorkOrderServiceImpl();
        setField(service, "faultRepairConfigService", createFaultRepairConfigServiceProxy(Collections.emptyList()));

        WorkOrderFaultItemDTO faultItem = new WorkOrderFaultItemDTO();
        faultItem.setFaultDesc("电源故障");
        faultItem.setRepairDesc("更换电源板");

        try {
            invokeNormalizeRepairFaults(service, buildRepairWorkOrder(), Collections.singletonList(faultItem));
            Assert.fail("预期应拒绝缺少配件信息的故障点");
        } catch (ServiceException ex) {
            Assert.assertEquals("配件信息不能为空", ex.getMessage());
        }
    }

    @Test
    public void shouldRequireOtherDescWhenOtherRepairOptionSelected() throws Exception {
        WorkOrderServiceImpl service = new WorkOrderServiceImpl();
        setField(service, "faultRepairConfigService", createFaultRepairConfigServiceProxy(Collections.singletonList(
                buildRepairFaultOption("电源故障", Collections.singletonList("更换电源板"))
        )));

        WorkOrderFaultItemDTO faultItem = new WorkOrderFaultItemDTO();
        faultItem.setFaultDesc("电源故障");
        faultItem.setPartDesc("电源板");
        faultItem.setRepairItems(Arrays.asList("更换电源板", "其它维修说明"));

        try {
            invokeNormalizeRepairFaults(service, buildRepairWorkOrder(), Collections.singletonList(faultItem));
            Assert.fail("预期应拒绝缺少其他维修说明的维修登记");
        } catch (ServiceException ex) {
            Assert.assertEquals("选择其它维修说明时，其他维修说明不能为空", ex.getMessage());
        }
    }

    @Test
    public void shouldSnapshotRepairItemsWhenRepairConfigMatched() throws Exception {
        WorkOrderServiceImpl service = new WorkOrderServiceImpl();
        setField(service, "faultRepairConfigService", createFaultRepairConfigServiceProxy(Collections.singletonList(
                buildRepairFaultOption("电源故障", Arrays.asList("更换电源板", "清洁接线"))
        )));

        WorkOrderFaultItemDTO faultItem = new WorkOrderFaultItemDTO();
        faultItem.setFaultDesc("电源故障");
        faultItem.setPartDesc("电源板");
        faultItem.setRepairItems(Collections.singletonList("更换电源板"));

        List<WorkOrderFaultItemDTO> result = invokeNormalizeRepairFaults(
                service,
                buildRepairWorkOrder(),
                Collections.singletonList(faultItem)
        );

        Assert.assertEquals(1, result.size());
        Assert.assertEquals("电源故障", result.get(0).getFaultDesc());
        Assert.assertEquals("更换电源板", result.get(0).getRepairDesc());
        Assert.assertEquals(Collections.singletonList("更换电源板"), result.get(0).getRepairItems());
    }

    @Test
    public void shouldRejectManualRepairDescWhenFaultConfigMatched() throws Exception {
        WorkOrderServiceImpl service = new WorkOrderServiceImpl();
        setField(service, "faultRepairConfigService", createFaultRepairConfigServiceProxy(Collections.singletonList(
                buildRepairFaultOption("电源故障", Arrays.asList("更换电源板", "清洁接线"))
        )));

        WorkOrderFaultItemDTO faultItem = new WorkOrderFaultItemDTO();
        faultItem.setFaultDesc("电源故障");
        faultItem.setPartDesc("电源板");
        faultItem.setRepairDesc("手工输入维修说明");

        try {
            invokeNormalizeRepairFaults(service, buildRepairWorkOrder(), Collections.singletonList(faultItem));
            Assert.fail("预期应拒绝绕过配置的手工维修说明");
        } catch (ServiceException ex) {
            Assert.assertEquals("请选择配置内的维修说明", ex.getMessage());
        }
    }

    @Test
    public void shouldRejectFaultDescOutsideConfiguredRange() throws Exception {
        WorkOrderServiceImpl service = new WorkOrderServiceImpl();
        setField(service, "faultRepairConfigService", createFaultRepairConfigServiceProxy(Collections.singletonList(
                buildRepairFaultOption("电源故障", Collections.singletonList("更换电源板"))
        )));

        WorkOrderFaultItemDTO faultItem = new WorkOrderFaultItemDTO();
        faultItem.setFaultDesc("非配置故障");
        faultItem.setPartDesc("电源板");
        faultItem.setRepairDesc("手工输入维修说明");

        try {
            invokeNormalizeRepairFaults(service, buildRepairWorkOrder(), Collections.singletonList(faultItem));
            Assert.fail("预期应拒绝配置范围外的故障描述");
        } catch (ServiceException ex) {
            Assert.assertEquals("故障描述不在当前配置范围内", ex.getMessage());
        }
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
                    List<?> ids = (List<?>) args[0];
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

    private WorkOrderReviewMapper createReviewMapperProxy(List<WorkOrderReview> insertedReviews) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("insert".equals(method.getName())) {
                    insertedReviews.add((WorkOrderReview) args[0]);
                    return 1;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (WorkOrderReviewMapper) Proxy.newProxyInstance(
                WorkOrderReviewMapper.class.getClassLoader(),
                new Class<?>[]{WorkOrderReviewMapper.class},
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
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("listRepairFaultOptions".equals(method.getName())) {
                    return options;
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

    @SuppressWarnings("unchecked")
    private List<WorkOrderFaultItemDTO> invokeNormalizeRepairFaults(WorkOrderServiceImpl service,
                                                                    WorkOrder workOrder,
                                                                    List<WorkOrderFaultItemDTO> faults) throws Exception {
        Method method = WorkOrderServiceImpl.class
                .getDeclaredMethod("normalizeRepairFaults", WorkOrder.class, List.class);
        method.setAccessible(true);
        try {
            return (List<WorkOrderFaultItemDTO>) method.invoke(service, workOrder, faults);
        } catch (InvocationTargetException ex) {
            if (ex.getCause() instanceof ServiceException) {
                throw (ServiceException) ex.getCause();
            }
            throw ex;
        }
    }

    private WorkOrderRepairFaultOptionVO buildRepairFaultOption(String faultDesc, List<String> repairOptions) {
        WorkOrderRepairFaultOptionVO option = new WorkOrderRepairFaultOptionVO();
        option.setFaultDesc(faultDesc);
        option.setRepairOptions(repairOptions);
        return option;
    }

    private SysCompany buildCompany(Long companyId, String companyName, String typeCode) {
        SysCompany company = new SysCompany();
        company.setId(companyId);
        company.setCompanyName(companyName);
        company.setTypeCode(typeCode);
        company.setStatus(1);
        return company;
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
