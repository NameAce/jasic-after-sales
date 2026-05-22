package com.jasic.aftersales.system.service;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.context.SaTokenContextForThreadLocal;
import cn.dev33.satoken.context.SaTokenContextForThreadLocalStorage;
import cn.dev33.satoken.context.model.SaRequest;
import cn.dev33.satoken.context.model.SaResponse;
import cn.dev33.satoken.context.model.SaStorage;
import cn.dev33.satoken.stp.StpUtil;
import com.jasic.aftersales.common.constant.WorkOrderCreateEntryConstants;
import com.jasic.aftersales.common.constant.WorkOrderStatusConstants;
import com.jasic.aftersales.common.enums.ServiceModeEnum;
import com.jasic.aftersales.common.enums.WorkOrderActionEnum;
import com.jasic.aftersales.common.enums.WorkOrderRelationTagEnum;
import com.jasic.aftersales.framework.security.SecurityContext;
import com.jasic.aftersales.system.domain.entity.FirstSecondRelation;
import com.jasic.aftersales.system.domain.entity.HqFirstContract;
import com.jasic.aftersales.system.domain.entity.WorkOrder;
import com.jasic.aftersales.system.domain.entity.WorkOrderParticipant;
import com.jasic.aftersales.system.domain.query.WorkOrderQuery;
import com.jasic.aftersales.system.domain.query.WorkOrderScopedQuery;
import com.jasic.aftersales.system.mapper.FirstSecondRelationMapper;
import com.jasic.aftersales.system.mapper.HqFirstContractMapper;
import com.jasic.aftersales.system.mapper.WorkOrderParticipantMapper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 工单权限服务测试。
 *
 * @author Codex
 * @date 2026/04/01
 */
public class WorkOrderPermissionServiceTest {

    private WorkOrderPermissionService service;
    private Set<String> permissionCodes;
    private Set<String> historyParticipationKeys;
    private Map<Long, Long> temporaryCreatorUserIds;

    @Before
    public void setUp() throws Exception {
        SaManager.setSaTokenContext(new SaTokenContextForThreadLocal());
        SaTokenContextForThreadLocalStorage.setBox(new MockSaRequest(), new MockSaResponse(), new MockSaStorage());
        StpUtil.login(101L);
        permissionCodes = new LinkedHashSet<>();
        historyParticipationKeys = new LinkedHashSet<>();
        temporaryCreatorUserIds = new LinkedHashMap<>();
        service = new WorkOrderPermissionService() {
            @Override
            protected boolean hasPermissionCode(String permissionCode) {
                return permissionCode == null || permissionCode.trim().isEmpty() || permissionCodes.contains(permissionCode);
            }

            @Override
            protected boolean hasHistoryUserParticipation(Long workOrderId, Long companyId, Long userId) {
                return historyParticipationKeys.contains(buildHistoryParticipationKey(workOrderId, companyId, userId));
            }

            @Override
            protected Long resolveTemporaryCreatorUserId(Long workOrderId) {
                return temporaryCreatorUserIds.get(workOrderId);
            }
        };
        setEmptyMapperDependencies();
    }

    @After
    public void tearDown() {
        try {
            StpUtil.logout();
        } finally {
            SaTokenContextForThreadLocalStorage.clearBox();
        }
    }

    @Test
    public void shouldAllowRegionManagerViewCurrentHqWorkOrderWithinRegion() throws Exception {
        setCurrentHqRegionContext();
        setField(service, "workOrderParticipantMapper", createParticipantMapperProxy(null, 0L));
        setField(service, "hqFirstContractMapper", createContractMapperProxy(
                Collections.singletonList(buildContract(900L, 1001L, 10L))
        ));
        setField(service, "firstSecondRelationMapper", createRelationMapperProxy(
                Collections.singletonList(buildRelation(1001L, 1002L))
        ));

        WorkOrder workOrder = buildWorkOrder(1L, 900L, 900L, 1001L, null);

        Assert.assertTrue(service.canView(workOrder));
    }

    @Test
    public void shouldAllowAllScopeUserViewCurrentHqWorkOrder() throws Exception {
        setCurrentHqAllContext();
        setField(service, "workOrderParticipantMapper", createParticipantMapperProxy(null, 0L));
        setField(service, "hqFirstContractMapper", createContractMapperProxy(Collections.emptyList()));
        setField(service, "firstSecondRelationMapper", createRelationMapperProxy(Collections.emptyList()));

        WorkOrder workOrder = buildWorkOrder(11L, 900L, 900L, 1001L, null);

        Assert.assertTrue(service.canView(workOrder));
    }

    @Test
    public void shouldAllowAllScopeUserViewReadonlyNetworkWorkOrder() throws Exception {
        setCurrentHqAllContext();
        setField(service, "workOrderParticipantMapper", createParticipantMapperProxy(buildParticipant(12L, 900L, "HQ_OBSERVER"), 0L));
        setField(service, "hqFirstContractMapper", createContractMapperProxy(Collections.emptyList()));
        setField(service, "firstSecondRelationMapper", createRelationMapperProxy(Collections.emptyList()));
        grantHistoryParticipation(12L, 900L, 101L);

        WorkOrder workOrder = buildWorkOrder(12L, 900L, 1001L, 1001L, null);

        Assert.assertTrue(service.canView(workOrder));
    }

    @Test
    public void shouldRejectAllScopeUserViewWorkOrderFromAnotherHq() throws Exception {
        setCurrentHqAllContext();
        setField(service, "workOrderParticipantMapper", createParticipantMapperProxy(buildParticipant(13L, 900L, "HQ_OBSERVER"), 0L));
        setField(service, "hqFirstContractMapper", createContractMapperProxy(Collections.emptyList()));
        setField(service, "firstSecondRelationMapper", createRelationMapperProxy(Collections.emptyList()));

        WorkOrder workOrder = buildWorkOrder(13L, 901L, 1001L, 1001L, null);

        Assert.assertFalse(service.canView(workOrder));
    }

    @Test
    public void shouldRejectRegionManagerViewCurrentHqWorkOrderOutsideRegion() throws Exception {
        setCurrentHqRegionContext();
        setField(service, "workOrderParticipantMapper", createParticipantMapperProxy(null, 0L));
        setField(service, "hqFirstContractMapper", createContractMapperProxy(
                Collections.singletonList(buildContract(900L, 1001L, 10L))
        ));
        setField(service, "firstSecondRelationMapper", createRelationMapperProxy(
                Collections.singletonList(buildRelation(1001L, 1002L))
        ));

        WorkOrder workOrder = buildWorkOrder(2L, 900L, 900L, 2001L, null);

        Assert.assertFalse(service.canView(workOrder));
    }

    @Test
    public void shouldRejectRegionManagerReadonlyOrderOutsideRegionEvenIfParticipantExists() throws Exception {
        setCurrentHqRegionContext();
        setField(service, "workOrderParticipantMapper", createParticipantMapperProxy(buildParticipant(3L, 900L, "HQ_OBSERVER"), 0L));
        setField(service, "hqFirstContractMapper", createContractMapperProxy(
                Collections.singletonList(buildContract(900L, 1001L, 10L))
        ));
        setField(service, "firstSecondRelationMapper", createRelationMapperProxy(
                Collections.singletonList(buildRelation(1001L, 1002L))
        ));

        WorkOrder workOrder = buildWorkOrder(3L, 900L, 2001L, 2001L, null);

        Assert.assertFalse(service.canView(workOrder));
    }

    @Test
    public void shouldRejectRegionManagerViewWorkOrderFromAnotherHq() throws Exception {
        setCurrentHqRegionContext();
        WorkOrder workOrder = buildWorkOrder(4L, 901L, 1001L, 1001L, null);

        Assert.assertFalse(service.canView(workOrder));
    }

    @Test
    public void shouldRejectSelfScopeUserViewUnassignedHqWorkOrder() {
        SecurityContext.setCurrentCompanyId(900L);
        SecurityContext.setCurrentSubjectType("HQ");
        SecurityContext.setCurrentTypeCode("HQ_A");
        SecurityContext.setEffectiveDataScope("SELF");
        SecurityContext.setCurrentRegionIds(Collections.emptyList());

        WorkOrder workOrder = buildWorkOrder(5L, 900L, 900L, 1001L, 202L);

        Assert.assertFalse(service.canView(workOrder));
    }

    @Test
    public void shouldAllowSelfScopeUserViewHistoricalWorkOrderWhenParticipationExists() throws Exception {
        SecurityContext.setCurrentCompanyId(1001L);
        SecurityContext.setCurrentSubjectType("SERVICE");
        SecurityContext.setCurrentTypeCode("FIRST");
        SecurityContext.setEffectiveDataScope("SELF");
        SecurityContext.setCurrentRegionIds(Collections.emptyList());
        setField(service, "workOrderParticipantMapper", createParticipantMapperProxy(buildParticipant(21L, 1001L, "HISTORY"), 0L));
        setField(service, "hqFirstContractMapper", createContractMapperProxy(Collections.emptyList()));
        setField(service, "firstSecondRelationMapper", createRelationMapperProxy(Collections.emptyList()));
        grantHistoryParticipation(21L, 1001L, 101L);

        WorkOrder workOrder = buildWorkOrder(21L, 900L, 2001L, 1001L, null);

        Assert.assertTrue(service.canView(workOrder));
    }

    @Test
    public void shouldRejectSelfScopeHistoricalWorkOrderWhenParticipationMissing() throws Exception {
        SecurityContext.setCurrentCompanyId(1001L);
        SecurityContext.setCurrentSubjectType("SERVICE");
        SecurityContext.setCurrentTypeCode("FIRST");
        SecurityContext.setEffectiveDataScope("SELF");
        SecurityContext.setCurrentRegionIds(Collections.emptyList());
        setField(service, "workOrderParticipantMapper", createParticipantMapperProxy(buildParticipant(22L, 1001L, "HISTORY"), 0L));
        setField(service, "hqFirstContractMapper", createContractMapperProxy(Collections.emptyList()));
        setField(service, "firstSecondRelationMapper", createRelationMapperProxy(Collections.emptyList()));

        WorkOrder workOrder = buildWorkOrder(22L, 900L, 2001L, 1001L, null);

        Assert.assertFalse(service.canView(workOrder));
    }

    @Test
    public void shouldAllowAllScopeHistoricalWorkOrderWithoutUserParticipation() throws Exception {
        SecurityContext.setCurrentCompanyId(1001L);
        SecurityContext.setCurrentSubjectType("SERVICE");
        SecurityContext.setCurrentTypeCode("FIRST");
        SecurityContext.setEffectiveDataScope("ALL");
        SecurityContext.setCurrentRegionIds(Collections.emptyList());
        setField(service, "workOrderParticipantMapper", createParticipantMapperProxy(buildParticipant(23L, 1001L, "HISTORY"), 0L));
        setField(service, "hqFirstContractMapper", createContractMapperProxy(Collections.emptyList()));
        setField(service, "firstSecondRelationMapper", createRelationMapperProxy(Collections.emptyList()));

        WorkOrder workOrder = buildWorkOrder(23L, 900L, 2001L, 1001L, null);

        Assert.assertTrue(service.canView(workOrder));
    }

    @Test
    public void shouldFillQueryScopeWithAllScopeAndEmptyRelatedCompanyIds() throws Exception {
        setCurrentHqAllContext();
        setField(service, "workOrderParticipantMapper", createParticipantMapperProxy(null, 0L));
        setField(service, "hqFirstContractMapper", createContractMapperProxy(Collections.emptyList()));
        setField(service, "firstSecondRelationMapper", createRelationMapperProxy(Collections.emptyList()));

        WorkOrderQuery query = new WorkOrderQuery();
        WorkOrderScopedQuery scopedQuery = service.buildScopedQuery(query);

        Assert.assertEquals(Long.valueOf(900L), scopedQuery.getAccessContext().getCurrentCompanyId());
        Assert.assertEquals(Long.valueOf(101L), scopedQuery.getAccessContext().getCurrentUserId());
        Assert.assertEquals("HQ", scopedQuery.getAccessContext().getSubjectType());
        Assert.assertEquals("ALL", scopedQuery.getAccessContext().getDataScope());
        Assert.assertEquals(Collections.emptyList(), scopedQuery.getAccessContext().getRelatedCompanyIds());
    }

    @Test
    public void shouldFillQueryScopeWithRegionCompanyIds() throws Exception {
        setCurrentHqRegionContext();
        setField(service, "workOrderParticipantMapper", createParticipantMapperProxy(null, 0L));
        setField(service, "hqFirstContractMapper", createContractMapperProxy(
                Collections.singletonList(buildContract(900L, 1001L, 10L))
        ));
        setField(service, "firstSecondRelationMapper", createRelationMapperProxy(
                Collections.singletonList(buildRelation(1001L, 1002L))
        ));

        WorkOrderQuery query = new WorkOrderQuery();
        WorkOrderScopedQuery scopedQuery = service.buildScopedQuery(query);

        Assert.assertEquals(Long.valueOf(900L), scopedQuery.getAccessContext().getCurrentCompanyId());
        Assert.assertEquals(Long.valueOf(101L), scopedQuery.getAccessContext().getCurrentUserId());
        Assert.assertEquals("HQ", scopedQuery.getAccessContext().getSubjectType());
        Assert.assertEquals("REGION", scopedQuery.getAccessContext().getDataScope());
        Assert.assertEquals(Arrays.asList(1001L, 1002L), scopedQuery.getAccessContext().getRelatedCompanyIds());
    }

    @Test
    public void shouldResolveRelationTagsForAssignedDispatcherInCurrentAcceptCompany() throws Exception {
        setCurrentServiceContext(1001L);
        setEmptyMapperDependencies();

        WorkOrder workOrder = buildWorkOrder(16L, 900L, 1001L, 1001L, 101L);

        EnumSet<WorkOrderRelationTagEnum> relationTags = service.resolveRelationTags(workOrder);

        Assert.assertTrue(relationTags.contains(WorkOrderRelationTagEnum.CURRENT_ACCEPT_COMPANY));
        Assert.assertTrue(relationTags.contains(WorkOrderRelationTagEnum.ASSIGNEE));
        Assert.assertTrue(relationTags.contains(WorkOrderRelationTagEnum.CREATOR_COMPANY));
    }

    @Test
    public void shouldRejectTransferForAssignedTechWithoutTransferPermission() throws Exception {
        setCurrentServiceContext(1001L);
        setEmptyMapperDependencies();
        grantPermissions("workorder:repair");

        WorkOrder workOrder = buildWorkOrder(18L, 900L, 1001L, 1001L, 101L);
        workOrder.setMainStatus(WorkOrderStatusConstants.MainStatus.IN_PROGRESS);

        Assert.assertFalse(service.canTransfer(workOrder));
        Assert.assertFalse(service.listAvailableActions(workOrder).contains(WorkOrderActionEnum.TRANSFER.getCode()));
    }

    @Test
    public void shouldOnlyExposeRepairRegisterActionForInProgressAssignee() throws Exception {
        setCurrentServiceContext(1001L);
        setEmptyMapperDependencies();
        grantPermissions("workorder:repair");

        WorkOrder workOrder = buildWorkOrder(19L, 900L, 1001L, 1001L, 101L);
        workOrder.setMainStatus(WorkOrderStatusConstants.MainStatus.IN_PROGRESS);

        List<String> actions = service.listAvailableActions(workOrder);

        Assert.assertTrue(service.canSaveRepair(workOrder));
        Assert.assertTrue(actions.contains(WorkOrderActionEnum.REPAIR_FINISH.getCode()));
    }

    @Test
    public void shouldAllowCloseForCompletedCurrentAcceptCompanyWhenHasClosePermission() throws Exception {
        setCurrentServiceContext(1001L);
        setEmptyMapperDependencies();
        grantPermissions("workorder:close");

        WorkOrder workOrder = buildWorkOrder(24L, 900L, 1001L, 2001L, 202L);
        workOrder.setMainStatus(WorkOrderStatusConstants.MainStatus.COMPLETED);

        Assert.assertTrue(service.canClose(workOrder));
        Assert.assertTrue(service.listAvailableActions(workOrder).contains(WorkOrderActionEnum.CLOSE.getCode()));
    }

    @Test
    public void shouldNotExposeIndependentCloseForPendingTechAcceptAssignee() throws Exception {
        setCurrentServiceContext(1001L);
        setEmptyMapperDependencies();
        grantPermissions("workorder:accept", "workorder:close");

        WorkOrder workOrder = buildWorkOrder(25L, 900L, 1001L, 2001L, 101L);
        workOrder.setMainStatus(WorkOrderStatusConstants.MainStatus.PENDING_TECH_ACCEPT);

        Assert.assertTrue(service.canTechAccept(workOrder));
        Assert.assertFalse(service.canClose(workOrder));
        Assert.assertFalse(service.listAvailableActions(workOrder).contains(WorkOrderActionEnum.CLOSE.getCode()));
    }

    @Test
    public void shouldReturnHistoryReadonlyReasonForTransferredWorkOrder() throws Exception {
        setCurrentServiceContext(1001L);
        setField(service, "workOrderParticipantMapper", createParticipantMapperProxy(
                buildParticipant(31L, 1001L, "HISTORY"), 0L
        ));
        setField(service, "hqFirstContractMapper", createContractMapperProxy(Collections.emptyList()));
        setField(service, "firstSecondRelationMapper", createRelationMapperProxy(Collections.emptyList()));

        WorkOrder workOrder = buildWorkOrder(31L, 900L, 2001L, 1001L, null);
        workOrder.setMainStatus(WorkOrderStatusConstants.MainStatus.IN_PROGRESS);

        Assert.assertEquals("当前非受理方，仅可查看", service.getReadonlyReason(workOrder));
    }

    @Test
    public void shouldReturnAssigneeReadonlyReasonWhenOtherTechnicianIsHandlingWorkOrder() throws Exception {
        setCurrentServiceContext(1001L);
        setEmptyMapperDependencies();

        WorkOrder workOrder = buildWorkOrder(32L, 900L, 1001L, 2001L, 202L);
        workOrder.setMainStatus(WorkOrderStatusConstants.MainStatus.IN_PROGRESS);

        Assert.assertEquals("当前由其他维修人员处理", service.getReadonlyReason(workOrder));
    }

    @Test
    public void shouldReturnNullReadonlyReasonWhenActionsAreAvailable() throws Exception {
        setCurrentServiceContext(1001L);
        setEmptyMapperDependencies();
        grantPermissions("workorder:review");

        WorkOrder workOrder = buildWorkOrder(33L, 900L, 1001L, 2001L, null);
        workOrder.setMainStatus(WorkOrderStatusConstants.MainStatus.COMPLETED);

        Assert.assertNull(service.getReadonlyReason(workOrder));
    }

    @Test
    public void shouldAllowProxySelfCreatorUploadSendExpressWithoutAssignPermission() throws Exception {
        setCurrentServiceContext(1001L);
        setEmptyMapperDependencies();
        WorkOrder workOrder = buildMailWorkOrder(41L, 900L, 1001L, 1001L,
                WorkOrderStatusConstants.MainStatus.PENDING_ASSIGN, WorkOrderCreateEntryConstants.PROXY_SELF);
        rememberTemporaryCreator(41L, 101L);

        Assert.assertTrue(service.canUpdateSendExpress(workOrder));
        Assert.assertTrue(service.listAvailableActions(workOrder).contains(WorkOrderActionEnum.UPLOAD_SEND_EXPRESS.getCode()));
    }

    @Test
    public void shouldRejectProxySelfSameCompanyOtherUserUploadSendExpress() throws Exception {
        setCurrentServiceContext(1001L);
        setEmptyMapperDependencies();
        WorkOrder workOrder = buildMailWorkOrder(42L, 900L, 1001L, 1001L,
                WorkOrderStatusConstants.MainStatus.PENDING_ASSIGN, WorkOrderCreateEntryConstants.PROXY_SELF);
        rememberTemporaryCreator(42L, 202L);

        Assert.assertFalse(service.canUpdateSendExpress(workOrder));
        Assert.assertFalse(service.listAvailableActions(workOrder).contains(WorkOrderActionEnum.UPLOAD_SEND_EXPRESS.getCode()));
    }

    @Test
    public void shouldAllowUpstreamFirstCreatorUploadSendExpressFromReadonlyList() throws Exception {
        setCurrentServiceContext(2002L);
        setField(service, "workOrderParticipantMapper", createParticipantMapperProxy(
                buildParticipant(43L, 2002L, "CREATE"), 0L
        ));
        setField(service, "hqFirstContractMapper", createContractMapperProxy(Collections.emptyList()));
        setField(service, "firstSecondRelationMapper", createRelationMapperProxy(Collections.emptyList()));
        WorkOrder workOrder = buildMailWorkOrder(43L, 900L, 1001L, 2002L,
                WorkOrderStatusConstants.MainStatus.PENDING_ASSIGN, WorkOrderCreateEntryConstants.UPSTREAM_FIRST);
        rememberTemporaryCreator(43L, 101L);

        Assert.assertTrue(service.canUpdateSendExpress(workOrder));
        Assert.assertTrue(service.listAvailableActions(workOrder).contains(WorkOrderActionEnum.UPLOAD_SEND_EXPRESS.getCode()));
    }

    @Test
    public void shouldAllowUpstreamHqCreatorUploadSendExpressFromReadonlyList() throws Exception {
        setCurrentServiceContext(1001L);
        setField(service, "workOrderParticipantMapper", createParticipantMapperProxy(
                buildParticipant(44L, 1001L, "CREATE"), 0L
        ));
        setField(service, "hqFirstContractMapper", createContractMapperProxy(Collections.emptyList()));
        setField(service, "firstSecondRelationMapper", createRelationMapperProxy(Collections.emptyList()));
        WorkOrder workOrder = buildMailWorkOrder(44L, 900L, 900L, 1001L,
                WorkOrderStatusConstants.MainStatus.PENDING_TECH_ACCEPT, WorkOrderCreateEntryConstants.UPSTREAM_HQ);
        rememberTemporaryCreator(44L, 101L);

        Assert.assertTrue(service.canUpdateSendExpress(workOrder));
        Assert.assertTrue(service.listAvailableActions(workOrder).contains(WorkOrderActionEnum.UPLOAD_SEND_EXPRESS.getCode()));
    }

    @Test
    public void shouldAllowUploadSendExpressInPendingTechAcceptWindow() throws Exception {
        setCurrentServiceContext(1001L);
        setEmptyMapperDependencies();
        WorkOrder workOrder = buildMailWorkOrder(45L, 900L, 1001L, 1001L,
                WorkOrderStatusConstants.MainStatus.PENDING_TECH_ACCEPT, WorkOrderCreateEntryConstants.PROXY_SELF);
        rememberTemporaryCreator(45L, 101L);

        Assert.assertTrue(service.canUpdateSendExpress(workOrder));
    }

    @Test
    public void shouldRejectUploadSendExpressOutsideWaitAcceptWindow() throws Exception {
        setCurrentServiceContext(1001L);
        setEmptyMapperDependencies();
        WorkOrder workOrder = buildMailWorkOrder(46L, 900L, 1001L, 1001L,
                WorkOrderStatusConstants.MainStatus.IN_PROGRESS, WorkOrderCreateEntryConstants.PROXY_SELF);
        rememberTemporaryCreator(46L, 101L);

        Assert.assertFalse(service.canUpdateSendExpress(workOrder));
    }

    @Test
    public void shouldRejectUploadSendExpressWhenCreateFlowMissingOrOperatorEmpty() throws Exception {
        setCurrentServiceContext(1001L);
        setEmptyMapperDependencies();
        WorkOrder missingCreateFlow = buildMailWorkOrder(47L, 900L, 1001L, 1001L,
                WorkOrderStatusConstants.MainStatus.PENDING_ASSIGN, WorkOrderCreateEntryConstants.PROXY_SELF);
        WorkOrder emptyOperator = buildMailWorkOrder(48L, 900L, 1001L, 1001L,
                WorkOrderStatusConstants.MainStatus.PENDING_ASSIGN, WorkOrderCreateEntryConstants.PROXY_SELF);
        rememberTemporaryCreator(48L, null);

        Assert.assertFalse(service.canUpdateSendExpress(missingCreateFlow));
        Assert.assertFalse(service.canUpdateSendExpress(emptyOperator));
    }

    private void setCurrentHqRegionContext() {
        SecurityContext.setCurrentCompanyId(900L);
        SecurityContext.setCurrentSubjectType("HQ");
        SecurityContext.setCurrentTypeCode("HQ_A");
        SecurityContext.setEffectiveDataScope("REGION");
        SecurityContext.setCurrentRegionIds(Collections.singletonList(10L));
    }

    private void setCurrentHqAllContext() {
        SecurityContext.setCurrentCompanyId(900L);
        SecurityContext.setCurrentSubjectType("HQ");
        SecurityContext.setCurrentTypeCode("HQ_A");
        SecurityContext.setEffectiveDataScope("ALL");
        SecurityContext.setCurrentRegionIds(Collections.emptyList());
    }

    private void setCurrentServiceContext(Long companyId) {
        SecurityContext.setCurrentCompanyId(companyId);
        SecurityContext.setCurrentSubjectType("SERVICE");
        SecurityContext.setCurrentTypeCode("FIRST");
        SecurityContext.setEffectiveDataScope("ALL");
        SecurityContext.setCurrentRegionIds(Collections.emptyList());
    }

    private void grantPermissions(String... permissionCodes) {
        if (permissionCodes == null) {
            return;
        }
        this.permissionCodes.addAll(Arrays.asList(permissionCodes));
    }

    private void grantHistoryParticipation(Long workOrderId, Long companyId, Long userId) {
        historyParticipationKeys.add(buildHistoryParticipationKey(workOrderId, companyId, userId));
    }

    private void rememberTemporaryCreator(Long workOrderId, Long userId) {
        temporaryCreatorUserIds.put(workOrderId, userId);
    }

    private String buildHistoryParticipationKey(Long workOrderId, Long companyId, Long userId) {
        return String.valueOf(workOrderId) + "-" + String.valueOf(companyId) + "-" + String.valueOf(userId);
    }

    private void setEmptyMapperDependencies() throws Exception {
        setField(service, "workOrderParticipantMapper", createParticipantMapperProxy(null, 0L));
        setField(service, "hqFirstContractMapper", createContractMapperProxy(Collections.emptyList()));
        setField(service, "firstSecondRelationMapper", createRelationMapperProxy(Collections.emptyList()));
    }

    private WorkOrder buildWorkOrder(Long id, Long hqCompanyId, Long currentAcceptCompanyId,
                                     Long createCompanyId, Long assignedUserId) {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setId(id);
        workOrder.setHqCompanyId(hqCompanyId);
        workOrder.setCurrentAcceptCompanyId(currentAcceptCompanyId);
        workOrder.setCreateCompanyId(createCompanyId);
        workOrder.setAssignedUserId(assignedUserId);
        return workOrder;
    }

    private WorkOrder buildMailWorkOrder(Long id, Long hqCompanyId, Long currentAcceptCompanyId,
                                         Long createCompanyId, String mainStatus, String createEntryType) {
        WorkOrder workOrder = buildWorkOrder(id, hqCompanyId, currentAcceptCompanyId, createCompanyId, null);
        workOrder.setServiceMode(ServiceModeEnum.MAIL.getCode());
        workOrder.setMainStatus(mainStatus);
        workOrder.setCreateEntryType(createEntryType);
        return workOrder;
    }

    private WorkOrderParticipant buildParticipant(Long workOrderId, Long companyId, String participateType) {
        WorkOrderParticipant participant = new WorkOrderParticipant();
        participant.setWorkOrderId(workOrderId);
        participant.setCompanyId(companyId);
        participant.setParticipateType(participateType);
        participant.setIsCurrentHandler(0);
        return participant;
    }

    private HqFirstContract buildContract(Long hqCompanyId, Long firstCompanyId, Long regionId) {
        HqFirstContract contract = new HqFirstContract();
        contract.setHqCompanyId(hqCompanyId);
        contract.setFirstCompanyId(firstCompanyId);
        contract.setRegionId(regionId);
        contract.setStatus(1);
        return contract;
    }

    private FirstSecondRelation buildRelation(Long firstCompanyId, Long secondCompanyId) {
        FirstSecondRelation relation = new FirstSecondRelation();
        relation.setFirstCompanyId(firstCompanyId);
        relation.setSecondCompanyId(secondCompanyId);
        relation.setStatus(1);
        return relation;
    }

    private WorkOrderParticipantMapper createParticipantMapperProxy(WorkOrderParticipant participant, Long relatedCount) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("selectOne".equals(method.getName())) {
                    return participant;
                }
                if ("selectCount".equals(method.getName())) {
                    return relatedCount == null ? 0L : relatedCount;
                }
                return defaultValue(method.getReturnType());
            }
        };
        return (WorkOrderParticipantMapper) Proxy.newProxyInstance(
                WorkOrderParticipantMapper.class.getClassLoader(),
                new Class<?>[]{WorkOrderParticipantMapper.class},
                handler
        );
    }

    private HqFirstContractMapper createContractMapperProxy(List<HqFirstContract> contracts) {
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

    private void setField(Object target, String fieldName, Object value) throws Exception {
        if ("hqFirstContractMapper".equals(fieldName) || "firstSecondRelationMapper".equals(fieldName)) {
            Field accessContextResolverField = WorkOrderPermissionService.class.getDeclaredField("accessContextResolver");
            accessContextResolverField.setAccessible(true);
            WorkOrderAccessContextResolver resolver = (WorkOrderAccessContextResolver) accessContextResolverField.get(target);
            if (resolver == null) {
                resolver = new WorkOrderAccessContextResolver();
                accessContextResolverField.set(target, resolver);
            }
            Field resolverField = WorkOrderAccessContextResolver.class.getDeclaredField(fieldName);
            resolverField.setAccessible(true);
            resolverField.set(resolver, value);
            return;
        }
        Field field = WorkOrderPermissionService.class.getDeclaredField(fieldName);
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


