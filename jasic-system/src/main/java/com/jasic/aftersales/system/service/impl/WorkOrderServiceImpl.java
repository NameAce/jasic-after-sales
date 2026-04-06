package com.jasic.aftersales.system.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jasic.aftersales.common.constant.RoleConstants;
import com.jasic.aftersales.common.constant.WorkOrderCreateEntryConstants;
import com.jasic.aftersales.common.constant.WorkOrderStatusConstants;
import com.jasic.aftersales.common.constant.WorkOrderStatusFlow;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.framework.security.SecurityContext;
import com.jasic.aftersales.system.domain.dto.WorkOrderAssignDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderCloseDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderFaultItemDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderProxyCreateDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderQuoteDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderRepairDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderSendExpressDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderTechAcceptDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderReviewDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderTransferDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderUpstreamCreateDTO;
import com.jasic.aftersales.system.domain.entity.SysCompany;
import com.jasic.aftersales.system.domain.entity.SysCompanyType;
import com.jasic.aftersales.system.domain.entity.SysRole;
import com.jasic.aftersales.system.domain.entity.SysUser;
import com.jasic.aftersales.system.domain.entity.SysUserCompany;
import com.jasic.aftersales.system.domain.entity.SysUserRole;
import com.jasic.aftersales.system.domain.entity.FirstSecondRelation;
import com.jasic.aftersales.system.domain.entity.HqFirstContract;
import com.jasic.aftersales.system.domain.entity.MachineBarcode;
import com.jasic.aftersales.system.domain.entity.WorkOrder;
import com.jasic.aftersales.system.domain.entity.WorkOrderCustomer;
import com.jasic.aftersales.system.domain.entity.WorkOrderEvaluation;
import com.jasic.aftersales.system.domain.entity.WorkOrderFault;
import com.jasic.aftersales.system.domain.entity.WorkOrderFlow;
import com.jasic.aftersales.system.domain.entity.WorkOrderNotifyEvent;
import com.jasic.aftersales.system.domain.entity.WorkOrderQuote;
import com.jasic.aftersales.system.domain.entity.WorkOrderRepair;
import com.jasic.aftersales.system.domain.entity.WorkOrderReview;
import com.jasic.aftersales.system.domain.query.WorkOrderQuery;
import com.jasic.aftersales.system.domain.vo.WorkOrderCreateBarcodeInfoVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderDetailVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderEvaluationVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderFaultVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderFlowVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderListVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderNotifyEventVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderQuoteVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderRepairFaultOptionVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderRepairVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderReviewVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderStatusCountVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderUserOptionVO;
import com.jasic.aftersales.system.domain.vo.SysCompanySimpleVO;
import com.jasic.aftersales.system.mapper.FirstSecondRelationMapper;
import com.jasic.aftersales.system.mapper.HqFirstContractMapper;
import com.jasic.aftersales.system.mapper.MachineBarcodeMapper;
import com.jasic.aftersales.system.mapper.SysCompanyMapper;
import com.jasic.aftersales.system.mapper.SysCompanyTypeMapper;
import com.jasic.aftersales.system.mapper.SysRoleMapper;
import com.jasic.aftersales.system.mapper.SysUserMapper;
import com.jasic.aftersales.system.mapper.SysUserCompanyMapper;
import com.jasic.aftersales.system.mapper.SysUserRoleMapper;
import com.jasic.aftersales.system.mapper.WorkOrderEvaluationMapper;
import com.jasic.aftersales.system.mapper.WorkOrderFaultMapper;
import com.jasic.aftersales.system.mapper.WorkOrderFlowMapper;
import com.jasic.aftersales.system.mapper.WorkOrderMapper;
import com.jasic.aftersales.system.mapper.WorkOrderCustomerMapper;
import com.jasic.aftersales.system.mapper.WorkOrderNotifyEventMapper;
import com.jasic.aftersales.system.mapper.WorkOrderQuoteMapper;
import com.jasic.aftersales.system.mapper.WorkOrderRepairMapper;
import com.jasic.aftersales.system.mapper.WorkOrderReviewMapper;
import com.jasic.aftersales.system.service.IFaultRepairConfigService;
import com.jasic.aftersales.system.service.IWorkOrderService;
import com.jasic.aftersales.system.service.WorkOrderNotifyEventService;
import com.jasic.aftersales.system.service.WorkOrderParticipantService;
import com.jasic.aftersales.system.service.WorkOrderPermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 工单业务 Service 实现。
 *
 * @author Codex
 * @date 2026/03/26
 */
@Service
public class WorkOrderServiceImpl implements IWorkOrderService {

    private static final DateTimeFormatter ORDER_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String REVIEW_RESULT_PASS = "通过";
    private static final String REVIEW_RESULT_CONTINUE = "继续维修";
    private static final String RETURN_METHOD_MAIL = "回寄";
    private static final String RETURN_METHOD_PICKUP = "自提";
    private static final String FAULT_JUDGE_HAS_FAULT = "有故障";
    private static final String FAULT_JUDGE_NO_FAULT = "无故障";
    private static final String OTHER_REPAIR_OPTION = "其它维修说明";
    private static final String OTHER_FAULT_LABEL = "其它故障";
    private static final String FAULT_DESC_SEPARATOR = "；";

    @Resource
    private WorkOrderMapper workOrderMapper;

    @Resource
    private WorkOrderCustomerMapper workOrderCustomerMapper;

    @Resource
    private WorkOrderFlowMapper workOrderFlowMapper;

    @Resource
    private WorkOrderQuoteMapper workOrderQuoteMapper;

    @Resource
    private WorkOrderRepairMapper workOrderRepairMapper;

    @Resource
    private WorkOrderFaultMapper workOrderFaultMapper;

    @Resource
    private WorkOrderReviewMapper workOrderReviewMapper;

    @Resource
    private WorkOrderEvaluationMapper workOrderEvaluationMapper;

    @Resource
    private WorkOrderNotifyEventMapper workOrderNotifyEventMapper;

    @Resource
    private WorkOrderPermissionService workOrderPermissionService;

    @Resource
    private WorkOrderParticipantService workOrderParticipantService;

    @Resource
    private WorkOrderNotifyEventService workOrderNotifyEventService;

    @Resource
    private IFaultRepairConfigService faultRepairConfigService;

    @Resource
    private SysCompanyMapper sysCompanyMapper;

    @Resource
    private SysCompanyTypeMapper sysCompanyTypeMapper;

    @Resource
    private SysUserCompanyMapper sysUserCompanyMapper;

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private SysRoleMapper sysRoleMapper;

    @Resource
    private SysUserRoleMapper sysUserRoleMapper;

    @Resource
    private FirstSecondRelationMapper firstSecondRelationMapper;

    @Resource
    private HqFirstContractMapper hqFirstContractMapper;

    @Resource
    private MachineBarcodeMapper machineBarcodeMapper;

    /**
     * 闁告帒妫濋妴澶愬蓟閵夘煈鍤勭€规悶鍎卞畷鐔煎礆濡ゅ嫨鈧?
     *
     * @param query 闁哄被鍎撮妤呭矗閸屾稒娈?
     * @return 闁告帒妫濋妴澶岀磼閹惧浜?
     */
    @Override
    public PageResult<WorkOrderListVO> listPage(WorkOrderQuery query) {
        normalizeQuery(query);
        Page<WorkOrderListVO> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<WorkOrderListVO> result = workOrderMapper.selectWorkOrderPage(page, query);
        List<WorkOrderListVO> records = result.getRecords();
        for (WorkOrderListVO record : records) {
            fillListSnapshot(record);
        }
        return PageResult.of(records, result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    @Override
    public List<WorkOrderStatusCountVO> countByStatus(WorkOrderQuery query) {
        normalizeQuery(query);
        WorkOrderQuery countQuery = copyQueryForCount(query);
        List<WorkOrderStatusCountVO> counts = workOrderMapper.selectStatusCount(countQuery);
        Map<String, Long> countMap = new HashMap<>();
        if (counts != null) {
            for (WorkOrderStatusCountVO item : counts) {
                if (item != null && item.getMainStatus() != null) {
                    countMap.put(item.getMainStatus(), item.getCountNum() == null ? 0L : item.getCountNum());
                }
            }
        }
        List<WorkOrderStatusCountVO> result = new ArrayList<>();
        result.add(buildStatusCountVo("ALL", "\u5168\u90e8", countMap.values().stream().mapToLong(Long::longValue).sum()));
        result.add(buildStatusCountVo(
                WorkOrderStatusConstants.MainStatus.PENDING_ASSIGN,
                resolveMainStatusLabel(WorkOrderStatusConstants.MainStatus.PENDING_ASSIGN),
                countMap.getOrDefault(WorkOrderStatusConstants.MainStatus.PENDING_ASSIGN, 0L)
        ));
        result.add(buildStatusCountVo(
                WorkOrderStatusConstants.MainStatus.PENDING_TECH_ACCEPT,
                resolveMainStatusLabel(WorkOrderStatusConstants.MainStatus.PENDING_TECH_ACCEPT),
                countMap.getOrDefault(WorkOrderStatusConstants.MainStatus.PENDING_TECH_ACCEPT, 0L)
        ));
        result.add(buildStatusCountVo(
                WorkOrderStatusConstants.MainStatus.IN_PROGRESS,
                resolveMainStatusLabel(WorkOrderStatusConstants.MainStatus.IN_PROGRESS),
                countMap.getOrDefault(WorkOrderStatusConstants.MainStatus.IN_PROGRESS, 0L)
        ));
        result.add(buildStatusCountVo(
                WorkOrderStatusConstants.MainStatus.COMPLETED,
                resolveMainStatusLabel(WorkOrderStatusConstants.MainStatus.COMPLETED),
                countMap.getOrDefault(WorkOrderStatusConstants.MainStatus.COMPLETED, 0L)
        ));
        result.add(buildStatusCountVo(
                WorkOrderStatusConstants.MainStatus.CLOSED,
                resolveMainStatusLabel(WorkOrderStatusConstants.MainStatus.CLOSED),
                countMap.getOrDefault(WorkOrderStatusConstants.MainStatus.CLOSED, 0L)
        ));
        return result;
    }

    /**
     * 查询工单详情。
     *
     * @param workOrderId 工单 ID
     * @return 工单详情
     */
    @Override
    public WorkOrderDetailVO getById(Long workOrderId) {
        WorkOrder entity = workOrderMapper.selectById(workOrderId);
        if (entity == null) {
            throw new ServiceException("\u5de5\u5355\u4e0d\u5b58\u5728");
        }
        if (!workOrderPermissionService.canView(entity)) {
            throw new ServiceException("\u65e0\u6743\u67e5\u770b\u8be5\u5de5\u5355");
        }
        WorkOrderDetailVO detail = workOrderMapper.selectDetailById(workOrderId);
        if (detail == null) {
            throw new ServiceException("\u5de5\u5355\u8be6\u60c5\u4e0d\u5b58\u5728");
        }
        detail.setParticipants(workOrderMapper.selectParticipantList(workOrderId));
        detail.setQuotes(listQuoteVos(workOrderId));
        detail.setRepairs(listRepairVos(workOrderId));
        detail.setReviews(listReviewVos(workOrderId));
        detail.setFlows(listFlowVos(workOrderId));
        detail.setEvaluation(getEvaluationVo(workOrderId));
        detail.setNotifyEvents(listNotifyEventVos(workOrderId));
        detail.setAvailableActions(workOrderPermissionService.listAvailableActions(entity));
        fillListSnapshot(detail, entity);
        detail.setEvaluateStatusLabel(resolveEvaluateStatusLabel(detail.getEvaluateStatus()));
        return detail;
    }

    @Override
    public WorkOrderCreateBarcodeInfoVO getProxyCreateBarcodeInfo(String barcode) {
        Long currentCompanyId = requireCurrentCompanyId();
        MachineBarcode barcodeArchive = requireActiveMachineBarcode(barcode);
        Long hqCompanyId = resolveSingleHqCompanyId(currentCompanyId, barcodeArchive);
        return buildCreateBarcodeInfo(barcodeArchive, hqCompanyId, Collections.emptyList(), null);
    }

    @Override
    public WorkOrderCreateBarcodeInfoVO getUpstreamFirstCreateBarcodeInfo(String barcode) {
        Long currentCompanyId = requireCurrentCompanyId();
        validateCurrentCompanyType("SECOND", "当前公司不支持报修一级");
        MachineBarcode barcodeArchive = requireActiveMachineBarcode(barcode);
        Long hqCompanyId = resolveSingleHqCompanyId(currentCompanyId, barcodeArchive);
        List<SysCompanySimpleVO> targetOptions = listUpstreamFirstOptions(currentCompanyId, hqCompanyId);
        Long defaultTargetCompanyId = resolveDefaultTargetCompanyId(targetOptions);
        return buildCreateBarcodeInfo(barcodeArchive, hqCompanyId, targetOptions, defaultTargetCompanyId);
    }

    @Override
    public WorkOrderCreateBarcodeInfoVO getUpstreamHqCreateBarcodeInfo(String barcode, Long targetCompanyId) {
        Long currentCompanyId = requireCurrentCompanyId();
        validateCurrentCompanyType("FIRST", "当前公司不支持报修佳士");
        MachineBarcode barcodeArchive = requireActiveMachineBarcode(barcode);
        List<SysCompanySimpleVO> targetOptions = listUpstreamHqOptions(currentCompanyId, barcodeArchive);
        Long defaultTargetCompanyId = resolveDefaultTargetCompanyId(targetOptions);
        Long resolvedHqCompanyId = targetCompanyId != null
                ? resolveSelectedTargetCompanyId(targetCompanyId, targetOptions, "请选择报修佳士")
                : resolveResolvedHqCompanyId(barcodeArchive, targetOptions);
        if (resolvedHqCompanyId == null) {
            resolvedHqCompanyId = defaultTargetCompanyId;
        }
        return buildCreateBarcodeInfo(barcodeArchive, resolvedHqCompanyId, targetOptions, defaultTargetCompanyId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createProxy(WorkOrderProxyCreateDTO dto) {
        Long currentCompanyId = requireCurrentCompanyId();
        MachineBarcode barcodeArchive = requireActiveMachineBarcode(dto.getBarcode());
        Long hqCompanyId = resolveSingleHqCompanyId(currentCompanyId, barcodeArchive);
        String currentSubjectType = normalizeNullableText(SecurityContext.getCurrentSubjectType());
        if (currentSubjectType == null) {
            currentSubjectType = resolveCompanySubjectType(currentCompanyId);
        }
        return saveCreateWorkOrder(dto, barcodeArchive, hqCompanyId, currentCompanyId,
                currentSubjectType, WorkOrderCreateEntryConstants.PROXY_SELF, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUpstreamFirst(WorkOrderUpstreamCreateDTO dto) {
        Long currentCompanyId = requireCurrentCompanyId();
        validateCurrentCompanyType("SECOND", "当前公司不支持报修一级");
        MachineBarcode barcodeArchive = requireActiveMachineBarcode(dto.getBarcode());
        Long hqCompanyId = resolveSingleHqCompanyId(currentCompanyId, barcodeArchive);
        List<SysCompanySimpleVO> targetOptions = listUpstreamFirstOptions(currentCompanyId, hqCompanyId);
        Long targetCompanyId = resolveSelectedTargetCompanyId(dto.getTargetCompanyId(), targetOptions, "请选择报修一级");
        return saveCreateWorkOrder(dto, barcodeArchive, hqCompanyId, targetCompanyId,
                "SERVICE", WorkOrderCreateEntryConstants.UPSTREAM_FIRST, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUpstreamHq(WorkOrderUpstreamCreateDTO dto) {
        Long currentCompanyId = requireCurrentCompanyId();
        validateCurrentCompanyType("FIRST", "当前公司不支持报修佳士");
        MachineBarcode barcodeArchive = requireActiveMachineBarcode(dto.getBarcode());
        List<SysCompanySimpleVO> targetOptions = listUpstreamHqOptions(currentCompanyId, barcodeArchive);
        Long targetCompanyId = resolveSelectedTargetCompanyId(dto.getTargetCompanyId(), targetOptions, "请选择报修佳士");
        return saveCreateWorkOrder(dto, barcodeArchive, targetCompanyId, targetCompanyId,
                "HQ", WorkOrderCreateEntryConstants.UPSTREAM_HQ, false);
    }

    /**
     * 婵炲弶鍎冲畷?
     *
     * @param dto 婵炲弶鍎冲畷鐔煎矗閸屾稒娈?
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assign(WorkOrderAssignDTO dto) {
        WorkOrder workOrder = requireWorkOrder(dto.getWorkOrderId());
        if (!workOrderPermissionService.canAssign(workOrder)) {
            throw new ServiceException("\u5f53\u524d\u5de5\u5355\u4e0d\u5141\u8bb8\u6d3e\u5355");
        }
        validateAssignedRepairer(dto.getAssignedUserId(), workOrder.getCurrentAcceptCompanyId());
        String beforeStatus = workOrder.getMainStatus();
        workOrder.setAssignedUserId(dto.getAssignedUserId());
        workOrder.setMainStatus(WorkOrderStatusFlow.afterAssign());
        workOrderMapper.updateById(workOrder);
        saveFlow(workOrder.getId(), "ASSIGN", beforeStatus, workOrder.getMainStatus(),
                workOrder.getCurrentAcceptCompanyId(), workOrder.getCurrentAcceptCompanyId(),
                workOrder.getCurrentAcceptCompanyId(), null);
    }

    /**
     * 缂備胶绻濋幈銊╁川濡澶嶉柛?     *
     * @param dto 闁规亽鍎卞畷鐔煎矗閸屾稒娈?
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void techAccept(WorkOrderTechAcceptDTO dto) {
        WorkOrder workOrder = requireWorkOrder(dto.getWorkOrderId());
        if (!workOrderPermissionService.canTechAccept(workOrder)) {
            throw new ServiceException("\u5f53\u524d\u5de5\u5355\u4e0d\u5141\u8bb8\u63a5\u5355");
        }
        String beforeStatus = workOrder.getMainStatus();
        workOrder.setMainStatus(WorkOrderStatusFlow.afterTechAccept());
        workOrderMapper.updateById(workOrder);
        saveFlow(workOrder.getId(), "TECH_ACCEPT", beforeStatus, workOrder.getMainStatus(),
                workOrder.getCurrentAcceptCompanyId(), workOrder.getCurrentAcceptCompanyId(),
                workOrder.getCurrentAcceptCompanyId(), null);
    }

    /**
     * 閺夌儐鍓欏畷?
     *
     * @param dto 閺夌儐鍓欏畷鐔煎矗閸屾稒娈?
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transfer(WorkOrderTransferDTO dto) {
        WorkOrder workOrder = requireWorkOrder(dto.getWorkOrderId());
        if (!workOrderPermissionService.canTransfer(workOrder)) {
            throw new ServiceException("\u5f53\u524d\u5de5\u5355\u4e0d\u5141\u8bb8\u8f6c\u5355");
        }
        validateTransferTarget(workOrder, dto.getTargetCompanyId());
        String targetSubjectType = resolveCompanySubjectType(dto.getTargetCompanyId());
        String fromSubjectType = workOrder.getCurrentAcceptSubjectType();
        Long fromCompanyId = workOrder.getCurrentAcceptCompanyId();
        String beforeStatus = workOrder.getMainStatus();

        workOrder.setCurrentAcceptCompanyId(dto.getTargetCompanyId());
        workOrder.setCurrentAcceptSubjectType(targetSubjectType);
        workOrder.setAssignedUserId(null);
        workOrder.setMainStatus(WorkOrderStatusFlow.afterTransfer());
        workOrder.setHasTransfer(1);
        workOrder.setTransferCount(workOrder.getTransferCount() == null ? 1 : workOrder.getTransferCount() + 1);
        workOrderMapper.updateById(workOrder);

        saveFlow(workOrder.getId(), "TRANSFER", beforeStatus, workOrder.getMainStatus(),
                fromCompanyId, dto.getTargetCompanyId(), fromCompanyId, dto.getRemark());
        workOrderParticipantService.transferParticipant(workOrder.getId(), fromCompanyId, fromSubjectType,
                dto.getTargetCompanyId(), targetSubjectType);
    }

    /**
     * 保存报价信息。
     *
     * @param dto 报价参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveQuote(WorkOrderQuoteDTO dto) {
        WorkOrder workOrder = requireWorkOrder(dto.getWorkOrderId());
        if (!workOrderPermissionService.canQuote(workOrder)) {
            throw new ServiceException("\u5f53\u524d\u5de5\u5355\u4e0d\u5141\u8bb8\u62a5\u4ef7");
        }
        String faultJudge = normalizeFaultJudge(dto.getFaultJudge(), "\u6545\u969c\u5224\u5b9a\u4e0d\u80fd\u4e3a\u7a7a");
        WorkOrderQuote quote = replaceCurrentQuote(workOrder, faultJudge, dto.getQuoteAmount(), dto.getQuoteDesc());

        saveFlow(workOrder.getId(), "QUOTE", workOrder.getMainStatus(), workOrder.getMainStatus(),
                workOrder.getCurrentAcceptCompanyId(), workOrder.getCurrentAcceptCompanyId(),
                workOrder.getCurrentAcceptCompanyId(), quote.getQuoteDesc());
    }

    /**
     * 保存维修记录。
     *
     * @param dto 维修参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRepair(WorkOrderRepairDTO dto) {
        WorkOrder workOrder = requireWorkOrder(dto.getWorkOrderId());
        if (!workOrderPermissionService.canSaveRepair(workOrder)) {
            throw new ServiceException("\u5f53\u524d\u5de5\u5355\u4e0d\u5141\u8bb8\u767b\u8bb0\u7ef4\u4fee");
        }
        validateRepairContent(workOrder, dto);
        saveRepairQuoteIfNeeded(workOrder, dto);
        WorkOrderRepair repair = new WorkOrderRepair();
        repair.setWorkOrderId(workOrder.getId());
        repair.setCompanyId(workOrder.getCurrentAcceptCompanyId());
        repair.setRepairUserId(SecurityContext.getCurrentUserId());
        repair.setRepairSummary(normalizeNullableText(dto.getRepairSummary()));
        repair.setRepairDesc(normalizeNullableText(dto.getRepairDesc()));
        repair.setOtherDesc(normalizeNullableText(dto.getOtherDesc()));
        repair.setIsFinished(dto.getIsFinished() != null && dto.getIsFinished() == 1 ? 1 : 0);
        if (repair.getIsFinished() == 1) {
            repair.setFinishedTime(LocalDateTime.now());
        }
        workOrderRepairMapper.insert(repair);
        saveFaults(workOrder.getId(), repair.getId(), workOrder.getCurrentAcceptCompanyId(), dto.getFaults());

        if (repair.getIsFinished() == 1) {
            String beforeStatus = workOrder.getMainStatus();
            workOrder.setMainStatus(WorkOrderStatusFlow.afterRepairFinish());
            workOrder.setCompletedTime(LocalDateTime.now());
            workOrderMapper.updateById(workOrder);
            saveFlow(workOrder.getId(), "REPAIR_FINISH", beforeStatus, workOrder.getMainStatus(),
                    workOrder.getCurrentAcceptCompanyId(), workOrder.getCurrentAcceptCompanyId(),
                    workOrder.getCurrentAcceptCompanyId(), repair.getRepairSummary());
            workOrderNotifyEventService.recordRepairFinished(workOrder, repair.getRepairSummary());
        } else {
            saveFlow(workOrder.getId(), "REPAIR_SAVE", workOrder.getMainStatus(), workOrder.getMainStatus(),
                    workOrder.getCurrentAcceptCompanyId(), workOrder.getCurrentAcceptCompanyId(),
                    workOrder.getCurrentAcceptCompanyId(), repair.getRepairSummary());
        }
    }

    /**
     * 保存复检结果。
     *
     * @param dto 复检参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveReview(WorkOrderReviewDTO dto) {
        WorkOrder workOrder = requireWorkOrder(dto.getWorkOrderId());
        if (!workOrderPermissionService.canReview(workOrder)) {
            throw new ServiceException("\u5f53\u524d\u5de5\u5355\u4e0d\u5141\u8bb8\u590d\u68c0");
        }
        String reviewResult = normalizeReviewResult(dto.getReviewResult());
        int continueRepair = resolveContinueRepair(reviewResult);
        WorkOrderReview review = new WorkOrderReview();
        review.setWorkOrderId(workOrder.getId());
        review.setCompanyId(workOrder.getCurrentAcceptCompanyId());
        review.setReviewUserId(SecurityContext.getCurrentUserId());
        review.setReviewResult(reviewResult);
        review.setReviewDesc(normalizeNullableText(dto.getReviewDesc()));
        review.setIsContinueRepair(continueRepair);
        workOrderReviewMapper.insert(review);

        String beforeStatus = workOrder.getMainStatus();
        if (continueRepair == 1) {
            workOrder.setMainStatus(WorkOrderStatusFlow.afterReview(true));
            workOrder.setCompletedTime(null);
            workOrderMapper.updateById(workOrder);
        }
        saveFlow(workOrder.getId(), "REVIEW", beforeStatus, workOrder.getMainStatus(),
                workOrder.getCurrentAcceptCompanyId(), workOrder.getCurrentAcceptCompanyId(),
                workOrder.getCurrentAcceptCompanyId(), review.getReviewDesc());
    }

    /**
     * 更新寄修工单的寄件快递单号。
     *
     * @param dto 寄件信息参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSendExpress(WorkOrderSendExpressDTO dto) {
        WorkOrder workOrder = requireWorkOrder(dto.getWorkOrderId());
        if (!workOrderPermissionService.canUpdateSendExpress(workOrder)) {
            throw new ServiceException("\u5f53\u524d\u5de5\u5355\u4e0d\u5141\u8bb8\u4e0a\u4f20\u5bc4\u4ef6\u5feb\u9012\u5355\u53f7");
        }
        workOrder.setSendExpressNo(normalizeRequiredText(dto.getSendExpressNo(), "\u5bc4\u4ef6\u5feb\u9012\u5355\u53f7\u4e0d\u80fd\u4e3a\u7a7a"));
        workOrderMapper.updateById(workOrder);
        saveFlow(workOrder.getId(), "UPLOAD_SEND_EXPRESS", workOrder.getMainStatus(), workOrder.getMainStatus(),
                workOrder.getCurrentAcceptCompanyId(), workOrder.getCurrentAcceptCompanyId(),
                workOrder.getCurrentAcceptCompanyId(), workOrder.getSendExpressNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void close(WorkOrderCloseDTO dto) {
        WorkOrder workOrder = requireWorkOrder(dto.getWorkOrderId());
        if (!workOrderPermissionService.canClose(workOrder)) {
            throw new ServiceException("\u5f53\u524d\u5de5\u5355\u4e0d\u5141\u8bb8\u5173\u95ed");
        }
        String returnMethod = normalizeReturnMethod(dto.getReturnMethod());
        String closeReason = normalizeRequiredText(dto.getCloseReason(), "\u5173\u95ed\u539f\u56e0\u4e0d\u80fd\u4e3a\u7a7a");
        validateCloseReturnInfo(dto);
        saveFlow(workOrder.getId(), "RETURN_METHOD", workOrder.getMainStatus(), workOrder.getMainStatus(),
                workOrder.getCurrentAcceptCompanyId(), workOrder.getCurrentAcceptCompanyId(),
                workOrder.getCurrentAcceptCompanyId(), returnMethod);
        String beforeStatus = workOrder.getMainStatus();
        boolean canEvaluate = !isNoFaultWorkOrder(workOrder.getId());
        workOrder.setReturnMethod(returnMethod);
        workOrder.setReturnExpressNo(resolveReturnExpressNo(dto));
        workOrder.setCloseReason(closeReason);
        workOrder.setMainStatus(WorkOrderStatusFlow.afterClose());
        workOrder.setEvaluateStatus(WorkOrderStatusFlow.afterCloseEvaluateStatus(canEvaluate));
        workOrder.setClosedTime(LocalDateTime.now());
        workOrderMapper.updateById(workOrder);
        saveFlow(workOrder.getId(), "CLOSE", beforeStatus, workOrder.getMainStatus(),
                workOrder.getCurrentAcceptCompanyId(), workOrder.getCurrentAcceptCompanyId(),
                workOrder.getCurrentAcceptCompanyId(), workOrder.getCloseReason());
        if (canEvaluate) {
            workOrderNotifyEventService.recordEvaluationInvite(workOrder);
        }
    }

    /**
     * 查询当前用户可选的建单总部列表。
     *
     * @return 总部选项列表
     */
    @Override
    public List<SysCompanySimpleVO> listCreateHqOptions() {
        Long currentCompanyId = requireCurrentCompanyId();
        List<Long> hqCompanyIds = resolveCreateHqCompanyIds(currentCompanyId);
        if (hqCompanyIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<SysCompany> companies = sysCompanyMapper.selectBatchIds(hqCompanyIds);
        Map<String, String> typeNameMap = buildTypeNameMap(companies);
        return companies.stream()
                .filter(company -> company != null)
                .sorted(java.util.Comparator.comparing(SysCompany::getId))
                .map(company -> buildCompanySimpleVo(company, typeNameMap.get(company.getTypeCode())))
                .collect(Collectors.toList());
    }

    /**
     * 查询当前工单可指派的人员列表。
     *
     * @param workOrderId 工单 ID
     * @return 指派人员选项列表
     */
    @Override
    public List<WorkOrderUserOptionVO> listAssignUserOptions(Long workOrderId) {
        WorkOrder workOrder = requireWorkOrder(workOrderId);
        if (!workOrderPermissionService.canAssign(workOrder)) {
            throw new ServiceException("\u5f53\u524d\u5de5\u5355\u4e0d\u5141\u8bb8\u6d3e\u5355");
        }
        Set<Long> userIds = listCompanyRepairerUserIds(workOrder.getCurrentAcceptCompanyId());
        if (userIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<SysUser> users = sysUserMapper.selectBatchIds(userIds);
        return users.stream()
                .filter(user -> user != null && user.getStatus() != null && user.getStatus() == 1)
                .sorted(java.util.Comparator.comparing(SysUser::getRealName, java.util.Comparator.nullsLast(String::compareTo))
                        .thenComparing(SysUser::getId))
                .map(this::buildUserOption)
                .collect(Collectors.toList());
    }

    /**
     * 查询当前工单可转派的目标公司列表。
     *
     * @param workOrderId 工单 ID
     * @return 目标公司选项列表
     */
    @Override
    public List<SysCompanySimpleVO> listTransferTargetOptions(Long workOrderId) {
        WorkOrder workOrder = requireWorkOrder(workOrderId);
        if (!workOrderPermissionService.canTransfer(workOrder)) {
            throw new ServiceException("\u5f53\u524d\u5de5\u5355\u4e0d\u5141\u8bb8\u8f6c\u5355");
        }
        List<Long> targetCompanyIds = resolveTransferTargetCompanyIds(workOrder);
        if (targetCompanyIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<SysCompany> companies = sysCompanyMapper.selectBatchIds(targetCompanyIds);
        Map<String, String> typeNameMap = buildTypeNameMap(companies);
        return companies.stream()
                .filter(company -> company != null)
                .sorted(java.util.Comparator.comparing(SysCompany::getId))
                .map(company -> buildCompanySimpleVo(company, typeNameMap.get(company.getTypeCode())))
                .collect(Collectors.toList());
    }

    @Override
    public List<WorkOrderRepairFaultOptionVO> listRepairFaultOptions(Long workOrderId) {
        WorkOrder workOrder = requireWorkOrder(workOrderId);
        if (!workOrderPermissionService.canSaveRepair(workOrder)) {
            throw new ServiceException("\u5f53\u524d\u5de5\u5355\u4e0d\u5141\u8bb8\u767b\u8bb0\u7ef4\u4fee");
        }
        return faultRepairConfigService.listRepairFaultOptions(
                workOrder.getHqCompanyId(),
                workOrder.getProductCode(),
                workOrder.getProductModel()
        );
    }

    private void normalizeQuery(WorkOrderQuery query) {
        workOrderPermissionService.fillQueryScope(query);
        if (!SecurityContext.isPlatformUser() && query.getCompanyId() == null) {
            throw new ServiceException("\u5f53\u524d\u516c\u53f8\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (query.getViewScope() == null || query.getViewScope().trim().isEmpty()) {
            query.setViewScope("CURRENT");
        }
    }

    private WorkOrderQuery copyQueryForCount(WorkOrderQuery query) {
        WorkOrderQuery target = new WorkOrderQuery();
        target.setViewScope(query.getViewScope());
        target.setCompanyId(query.getCompanyId());
        target.setCurrentUserId(query.getCurrentUserId());
        target.setSubjectType(query.getSubjectType());
        target.setDataScope(query.getDataScope());
        target.setRelatedCompanyIds(query.getRelatedCompanyIds());
        target.setOrderNo(query.getOrderNo());
        target.setCustomerName(query.getCustomerName());
        target.setCustomerMobile(query.getCustomerMobile());
        target.setBarcode(query.getBarcode());
        target.setHasTransfer(query.getHasTransfer());
        return target;
    }

    private WorkOrderStatusCountVO buildStatusCountVo(String mainStatus, String displayStatus, Long countNum) {
        WorkOrderStatusCountVO vo = new WorkOrderStatusCountVO();
        vo.setMainStatus(mainStatus);
        vo.setDisplayStatus(displayStatus);
        vo.setCountNum(countNum == null ? 0L : countNum);
        return vo;
    }

    private String resolveDisplayStatus(String mainStatus) {
        return WorkOrderStatusConstants.resolveDisplayStatus(mainStatus);
    }

    private String resolveMainStatusLabel(String mainStatus) {
        return WorkOrderStatusConstants.resolveMainStatusLabel(mainStatus);
    }

    private String resolveEvaluateStatusLabel(String evaluateStatus) {
        return WorkOrderStatusConstants.resolveEvaluateStatusLabel(evaluateStatus);
    }

    private void fillListSnapshot(WorkOrderListVO target) {
        fillListSnapshot(target, buildWorkOrderSnapshot(target));
    }

    private void fillListSnapshot(WorkOrderListVO target, WorkOrder workOrder) {
        if (target == null) {
            return;
        }
        target.setMainStatusLabel(resolveMainStatusLabel(target.getMainStatus()));
        target.setDisplayStatus(resolveDisplayStatus(target.getMainStatus()));
        if (workOrder == null) {
            return;
        }
        String relationType = workOrderPermissionService.resolveRelationType(workOrder);
        target.setRelationType(relationType);
        target.setIsReadonly(resolveReadonlyFlag(relationType, target.getIsReadonly()));
    }

    private List<WorkOrderQuoteVO> listQuoteVos(Long workOrderId) {
        LambdaQueryWrapper<WorkOrderQuote> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkOrderQuote::getWorkOrderId, workOrderId)
                .orderByDesc(WorkOrderQuote::getCreateTime);
        List<WorkOrderQuote> quotes = workOrderQuoteMapper.selectList(wrapper);
        if (quotes.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, String> companyNameMap = buildCompanyNameMap(quotes.stream().map(WorkOrderQuote::getCompanyId).collect(Collectors.toSet()));
        Map<Long, String> userNameMap = buildUserNameMap(quotes.stream().map(WorkOrderQuote::getQuotedBy).collect(Collectors.toSet()));
        List<WorkOrderQuoteVO> result = new ArrayList<>();
        for (WorkOrderQuote quote : quotes) {
            WorkOrderQuoteVO vo = new WorkOrderQuoteVO();
            vo.setId(quote.getId());
            vo.setCompanyId(quote.getCompanyId());
            vo.setCompanyName(companyNameMap.get(quote.getCompanyId()));
            vo.setQuotedBy(quote.getQuotedBy());
            vo.setQuotedByName(userNameMap.get(quote.getQuotedBy()));
            vo.setFaultJudge(quote.getFaultJudge());
            vo.setQuoteAmount(quote.getQuoteAmount());
            vo.setQuoteDesc(quote.getQuoteDesc());
            vo.setIsCurrentValid(quote.getIsCurrentValid());
            vo.setCreateTime(quote.getCreateTime());
            result.add(vo);
        }
        return result;
    }

    private List<WorkOrderRepairVO> listRepairVos(Long workOrderId) {
        LambdaQueryWrapper<WorkOrderRepair> repairWrapper = new LambdaQueryWrapper<>();
        repairWrapper.eq(WorkOrderRepair::getWorkOrderId, workOrderId)
                .orderByDesc(WorkOrderRepair::getCreateTime);
        List<WorkOrderRepair> repairs = workOrderRepairMapper.selectList(repairWrapper);
        if (repairs.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> companyIds = repairs.stream().map(WorkOrderRepair::getCompanyId).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> userIds = repairs.stream().map(WorkOrderRepair::getRepairUserId).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> repairIds = repairs.stream().map(WorkOrderRepair::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, String> companyNameMap = buildCompanyNameMap(companyIds);
        Map<Long, String> userNameMap = buildUserNameMap(userIds);
        Map<Long, List<WorkOrderFaultVO>> faultMap = buildFaultMap(workOrderId, repairIds);
        List<WorkOrderRepairVO> result = new ArrayList<>();
        for (WorkOrderRepair repair : repairs) {
            WorkOrderRepairVO vo = new WorkOrderRepairVO();
            vo.setId(repair.getId());
            vo.setCompanyId(repair.getCompanyId());
            vo.setCompanyName(companyNameMap.get(repair.getCompanyId()));
            vo.setRepairUserId(repair.getRepairUserId());
            vo.setRepairUserName(userNameMap.get(repair.getRepairUserId()));
            vo.setRepairSummary(repair.getRepairSummary());
            vo.setRepairDesc(repair.getRepairDesc());
            vo.setOtherDesc(repair.getOtherDesc());
            vo.setIsFinished(repair.getIsFinished());
            vo.setFinishedTime(repair.getFinishedTime());
            vo.setCreateTime(repair.getCreateTime());
            vo.setFaults(faultMap.getOrDefault(repair.getId(), Collections.emptyList()));
            result.add(vo);
        }
        return result;
    }

    private Map<Long, List<WorkOrderFaultVO>> buildFaultMap(Long workOrderId, Set<Long> repairIds) {
        if (repairIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<WorkOrderFault> faultWrapper = new LambdaQueryWrapper<>();
        faultWrapper.eq(WorkOrderFault::getWorkOrderId, workOrderId)
                .in(WorkOrderFault::getRepairId, repairIds)
                .orderByDesc(WorkOrderFault::getCreateTime)
                .orderByAsc(WorkOrderFault::getSortNum);
        List<WorkOrderFault> faults = workOrderFaultMapper.selectList(faultWrapper);
        if (faults.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> userNameMap = buildUserNameMap(faults.stream().map(WorkOrderFault::getCreatedBy).collect(Collectors.toSet()));
        Map<Long, List<WorkOrderFaultVO>> result = new HashMap<>();
        for (WorkOrderFault fault : faults) {
            WorkOrderFaultVO vo = new WorkOrderFaultVO();
            vo.setId(fault.getId());
            vo.setCompanyId(fault.getCompanyId());
            vo.setFaultDesc(fault.getFaultDesc());
            vo.setRepairDesc(fault.getRepairDesc());
            vo.setOtherDesc(fault.getOtherDesc());
            vo.setPartDesc(fault.getPartDesc());
            vo.setImageUrls(fault.getImageUrls());
            vo.setSortNum(fault.getSortNum());
            vo.setCreatedBy(fault.getCreatedBy());
            vo.setCreatedByName(userNameMap.get(fault.getCreatedBy()));
            vo.setCreateTime(fault.getCreateTime());
            result.computeIfAbsent(fault.getRepairId(), key -> new ArrayList<>()).add(vo);
        }
        return result;
    }

    private List<WorkOrderReviewVO> listReviewVos(Long workOrderId) {
        LambdaQueryWrapper<WorkOrderReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkOrderReview::getWorkOrderId, workOrderId)
                .orderByDesc(WorkOrderReview::getCreateTime);
        List<WorkOrderReview> reviews = workOrderReviewMapper.selectList(wrapper);
        if (reviews.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, String> companyNameMap = buildCompanyNameMap(reviews.stream().map(WorkOrderReview::getCompanyId).collect(Collectors.toSet()));
        Map<Long, String> userNameMap = buildUserNameMap(reviews.stream().map(WorkOrderReview::getReviewUserId).collect(Collectors.toSet()));
        List<WorkOrderReviewVO> result = new ArrayList<>();
        for (WorkOrderReview review : reviews) {
            WorkOrderReviewVO vo = new WorkOrderReviewVO();
            vo.setId(review.getId());
            vo.setCompanyId(review.getCompanyId());
            vo.setCompanyName(companyNameMap.get(review.getCompanyId()));
            vo.setReviewUserId(review.getReviewUserId());
            vo.setReviewUserName(userNameMap.get(review.getReviewUserId()));
            vo.setReviewResult(review.getReviewResult());
            vo.setReviewDesc(review.getReviewDesc());
            vo.setIsContinueRepair(review.getIsContinueRepair());
            vo.setCreateTime(review.getCreateTime());
            result.add(vo);
        }
        return result;
    }

    private List<WorkOrderFlowVO> listFlowVos(Long workOrderId) {
        LambdaQueryWrapper<WorkOrderFlow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkOrderFlow::getWorkOrderId, workOrderId)
                .orderByAsc(WorkOrderFlow::getCreateTime)
                .orderByAsc(WorkOrderFlow::getId);
        List<WorkOrderFlow> flows = workOrderFlowMapper.selectList(wrapper);
        if (flows.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> companyIds = new LinkedHashSet<>();
        Set<Long> userIds = new LinkedHashSet<>();
        for (WorkOrderFlow flow : flows) {
            if (flow.getFromCompanyId() != null) {
                companyIds.add(flow.getFromCompanyId());
            }
            if (flow.getToCompanyId() != null) {
                companyIds.add(flow.getToCompanyId());
            }
            if (flow.getOperatorCompanyId() != null) {
                companyIds.add(flow.getOperatorCompanyId());
            }
            if (flow.getOperatorUserId() != null) {
                userIds.add(flow.getOperatorUserId());
            }
        }
        Map<Long, String> companyNameMap = buildCompanyNameMap(companyIds);
        Map<Long, String> userNameMap = buildUserNameMap(userIds);
        List<WorkOrderFlowVO> result = new ArrayList<>();
        for (WorkOrderFlow flow : flows) {
            WorkOrderFlowVO vo = new WorkOrderFlowVO();
            vo.setId(flow.getId());
            vo.setActionType(flow.getActionType());
            vo.setActionName(resolveActionName(flow.getActionType()));
            vo.setBeforeStatus(flow.getBeforeStatus());
            vo.setBeforeStatusName(resolveDisplayStatus(flow.getBeforeStatus()));
            vo.setAfterStatus(flow.getAfterStatus());
            vo.setAfterStatusName(resolveDisplayStatus(flow.getAfterStatus()));
            vo.setFromCompanyId(flow.getFromCompanyId());
            vo.setFromCompanyName(companyNameMap.get(flow.getFromCompanyId()));
            vo.setToCompanyId(flow.getToCompanyId());
            vo.setToCompanyName(companyNameMap.get(flow.getToCompanyId()));
            vo.setOperatorCompanyId(flow.getOperatorCompanyId());
            vo.setOperatorCompanyName(companyNameMap.get(flow.getOperatorCompanyId()));
            vo.setOperatorUserId(flow.getOperatorUserId());
            vo.setOperatorUserName(userNameMap.get(flow.getOperatorUserId()));
            vo.setRemark(flow.getRemark());
            vo.setCreateTime(flow.getCreateTime());
            result.add(vo);
        }
        return result;
    }

    private WorkOrderEvaluationVO getEvaluationVo(Long workOrderId) {
        LambdaQueryWrapper<WorkOrderEvaluation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkOrderEvaluation::getWorkOrderId, workOrderId);
        WorkOrderEvaluation evaluation = workOrderEvaluationMapper.selectOne(wrapper);
        if (evaluation == null) {
            return null;
        }
        WorkOrderEvaluationVO vo = new WorkOrderEvaluationVO();
        vo.setId(evaluation.getId());
        vo.setCustomerId(evaluation.getCustomerId());
        vo.setCompanyId(evaluation.getCompanyId());
        vo.setTimelinessScore(evaluation.getTimelinessScore());
        vo.setQualityScore(evaluation.getQualityScore());
        vo.setSatisfactionScore(evaluation.getSatisfactionScore());
        vo.setTags(evaluation.getTags());
        vo.setContent(evaluation.getContent());
        vo.setCreateTime(evaluation.getCreateTime());
        return vo;
    }

    private List<WorkOrderNotifyEventVO> listNotifyEventVos(Long workOrderId) {
        LambdaQueryWrapper<WorkOrderNotifyEvent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkOrderNotifyEvent::getWorkOrderId, workOrderId)
                .orderByDesc(WorkOrderNotifyEvent::getCreateTime);
        List<WorkOrderNotifyEvent> events = workOrderNotifyEventMapper.selectList(wrapper);
        if (events.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, String> companyNameMap = buildCompanyNameMap(
                events.stream().map(WorkOrderNotifyEvent::getCompanyId).collect(Collectors.toSet())
        );
        List<WorkOrderNotifyEventVO> result = new ArrayList<>();
        for (WorkOrderNotifyEvent event : events) {
            WorkOrderNotifyEventVO vo = new WorkOrderNotifyEventVO();
            vo.setId(event.getId());
            vo.setCompanyId(event.getCompanyId());
            vo.setCompanyName(companyNameMap.get(event.getCompanyId()));
            vo.setEventType(event.getEventType());
            vo.setTriggerNode(event.getTriggerNode());
            vo.setReceiverType(event.getReceiverType());
            vo.setReceiverId(event.getReceiverId());
            vo.setTitleSnapshot(event.getTitleSnapshot());
            vo.setContentSnapshot(event.getContentSnapshot());
            vo.setSendStatus(event.getSendStatus());
            vo.setSendTime(event.getSendTime());
            vo.setFailReason(event.getFailReason());
            vo.setCreateTime(event.getCreateTime());
            result.add(vo);
        }
        return result;
    }

    private String resolveActionName(String actionType) {
        if ("CREATE".equals(actionType)) {
            return "\u5efa\u5355";
        }
        if ("ASSIGN".equals(actionType)) {
            return "\u6d3e\u5355";
        }
        if ("TECH_ACCEPT".equals(actionType)) {
            return "\u7ef4\u4fee\u5458\u63a5\u5355";
        }
        if ("TRANSFER".equals(actionType)) {
            return "\u8f6c\u5355";
        }
        if ("QUOTE".equals(actionType)) {
            return "\u62a5\u4ef7";
        }
        if ("REPAIR_SAVE".equals(actionType)) {
            return "\u4fdd\u5b58\u7ef4\u4fee";
        }
        if ("REPAIR_FINISH".equals(actionType)) {
            return "\u7ef4\u4fee\u5b8c\u6210";
        }
        if ("REVIEW".equals(actionType)) {
            return "\u590d\u68c0";
        }
        if ("UPLOAD_SEND_EXPRESS".equals(actionType)) {
            return "\u4e0a\u4f20\u5bc4\u4ef6\u5355\u53f7";
        }
        if ("RETURN_METHOD".equals(actionType)) {
            return "\u9009\u62e9\u8fd4\u56de\u65b9\u5f0f";
        }
        if ("CLOSE".equals(actionType)) {
            return "\u5173\u95ed\u5de5\u5355";
        }
        return actionType;
    }

    private WorkOrder requireWorkOrder(Long workOrderId) {
        WorkOrder workOrder = workOrderMapper.selectById(workOrderId);
        if (workOrder == null) {
            throw new ServiceException("\u5de5\u5355\u4e0d\u5b58\u5728");
        }
        return workOrder;
    }

    private Long requireCurrentCompanyId() {
        Long currentCompanyId = SecurityContext.getCurrentCompanyId();
        if (currentCompanyId == null) {
            throw new ServiceException("\u5f53\u524d\u516c\u53f8\u4e0d\u80fd\u4e3a\u7a7a");
        }
        return currentCompanyId;
    }

    private void validateCurrentCompanyType(String expectedTypeCode, String message) {
        String currentTypeCode = SecurityContext.getCurrentTypeCode();
        if (!expectedTypeCode.equals(currentTypeCode)) {
            throw new ServiceException(message);
        }
    }

    private Long saveCreateWorkOrder(WorkOrderProxyCreateDTO dto, MachineBarcode barcodeArchive, Long hqCompanyId,
                                     Long targetCompanyId, String targetSubjectType, String createEntryType,
                                     boolean autoCreateCustomer) {
        return saveCreateWorkOrder(
                dto.getCustomerName(),
                dto.getCustomerMobile(),
                dto.getBarcode(),
                dto.getServiceMode(),
                dto.getFaultItems(),
                dto.getFaultRemark(),
                dto.getSenderName(),
                dto.getSenderMobile(),
                dto.getSenderAddress(),
                dto.getSendExpressNo(),
                barcodeArchive,
                hqCompanyId,
                targetCompanyId,
                targetSubjectType,
                createEntryType,
                autoCreateCustomer
        );
    }

    private Long saveCreateWorkOrder(WorkOrderUpstreamCreateDTO dto, MachineBarcode barcodeArchive, Long hqCompanyId,
                                     Long targetCompanyId, String targetSubjectType, String createEntryType,
                                     boolean autoCreateCustomer) {
        return saveCreateWorkOrder(
                dto.getCustomerName(),
                dto.getCustomerMobile(),
                dto.getBarcode(),
                dto.getServiceMode(),
                dto.getFaultItems(),
                dto.getFaultRemark(),
                dto.getSenderName(),
                dto.getSenderMobile(),
                dto.getSenderAddress(),
                dto.getSendExpressNo(),
                barcodeArchive,
                hqCompanyId,
                targetCompanyId,
                targetSubjectType,
                createEntryType,
                autoCreateCustomer
        );
    }

    private Long saveCreateWorkOrder(String customerName, String customerMobile, String barcode, String serviceMode,
                                     List<String> faultItems, String faultRemark, String senderName, String senderMobile,
                                     String senderAddress, String sendExpressNo, MachineBarcode barcodeArchive,
                                     Long hqCompanyId, Long targetCompanyId, String targetSubjectType,
                                     String createEntryType, boolean autoCreateCustomer) {
        Long currentCompanyId = requireCurrentCompanyId();
        String normalizedCustomerName = normalizeRequiredText(customerName, "\u5ba2\u6237\u59d3\u540d\u4e0d\u80fd\u4e3a\u7a7a");
        String normalizedCustomerMobile = normalizeRequiredText(customerMobile, "\u5ba2\u6237\u624b\u673a\u53f7\u4e0d\u80fd\u4e3a\u7a7a");
        String normalizedBarcode = normalizeRequiredText(barcode, "\u673a\u5668\u6761\u7801\u4e0d\u80fd\u4e3a\u7a7a");
        String normalizedServiceMode = normalizeRequiredText(serviceMode, "\u670d\u52a1\u65b9\u5f0f\u4e0d\u80fd\u4e3a\u7a7a");
        validateCreateSendInfo(normalizedServiceMode, senderName, senderMobile, senderAddress);
        CustomerFaultSelection faultSelection = resolveCreateFaultSelection(
                faultItems, faultRemark, hqCompanyId, barcodeArchive.getProductCode(), barcodeArchive.getProductModel()
        );

        WorkOrder entity = new WorkOrder();
        entity.setOrderNo(generateOrderNo());
        entity.setCustomerId(resolveCreateCustomerId(normalizedCustomerName, normalizedCustomerMobile, autoCreateCustomer));
        entity.setCustomerName(normalizedCustomerName);
        entity.setCustomerMobile(normalizedCustomerMobile);
        entity.setBarcode(normalizedBarcode);
        entity.setProductCode(normalizeNullableText(barcodeArchive.getProductCode()));
        entity.setProductName(normalizeNullableText(barcodeArchive.getProductName()));
        entity.setProductModel(normalizeNullableText(barcodeArchive.getProductModel()));
        entity.setMachineNo(normalizeNullableText(barcodeArchive.getMachineNo()));
        entity.setBrandCode(normalizeNullableText(barcodeArchive.getBrandCode()));
        entity.setServiceMode(normalizedServiceMode);
        entity.setWarrantyStatus(normalizeNullableText(barcodeArchive.getWarrantyStatus()));
        entity.setFaultDesc(faultSelection.getFaultDesc());
        entity.setFaultRemark(faultSelection.getFaultRemark());
        entity.setSenderName(resolveSendField(normalizedServiceMode, senderName));
        entity.setSenderMobile(resolveSendField(normalizedServiceMode, senderMobile));
        entity.setSenderAddress(resolveSendField(normalizedServiceMode, senderAddress));
        entity.setSendExpressNo(resolveSendField(normalizedServiceMode, sendExpressNo));
        entity.setMainStatus(WorkOrderStatusFlow.afterCreate());
        entity.setEvaluateStatus(WorkOrderStatusFlow.afterCreateEvaluateStatus());
        entity.setCurrentAcceptSubjectType(targetSubjectType);
        entity.setCurrentAcceptCompanyId(targetCompanyId);
        entity.setCreateCompanyId(currentCompanyId);
        entity.setCreateEntryType(createEntryType);
        entity.setHqCompanyId(hqCompanyId);
        entity.setHasTransfer(0);
        entity.setTransferCount(0);
        workOrderMapper.insert(entity);

        saveFlow(entity.getId(), "CREATE", null, entity.getMainStatus(), null, targetCompanyId, currentCompanyId, null);
        workOrderParticipantService.initParticipants(entity, resolveCreateCompanySubjectType(currentCompanyId));
        return entity.getId();
    }

    private void validateCreateSendInfo(String serviceMode, String senderName, String senderMobile, String senderAddress) {
        if (!"\u5bc4\u4fee".equals(serviceMode)) {
            return;
        }
        if (isBlank(senderName)) {
            throw new ServiceException("\u5bc4\u4fee\u5de5\u5355\u5fc5\u987b\u586b\u5199\u5bc4\u4ef6\u4eba\u59d3\u540d");
        }
        if (isBlank(senderMobile)) {
            throw new ServiceException("\u5bc4\u4fee\u5de5\u5355\u5fc5\u987b\u586b\u5199\u5bc4\u4ef6\u4eba\u624b\u673a\u53f7");
        }
        if (isBlank(senderAddress)) {
            throw new ServiceException("\u5bc4\u4fee\u5de5\u5355\u5fc5\u987b\u586b\u5199\u5bc4\u4ef6\u5730\u5740");
        }
    }

    private void validateCloseReturnInfo(WorkOrderCloseDTO dto) {
        if (dto == null || !RETURN_METHOD_MAIL.equals(normalizeNullableText(dto.getReturnMethod()))) {
            return;
        }
        if (isBlank(dto.getReturnExpressNo())) {
            throw new ServiceException("\u56de\u5bc4\u65f6\u5fc5\u987b\u586b\u5199\u56de\u5bc4\u5feb\u9012\u5355\u53f7");
        }
    }

    private void saveRepairQuoteIfNeeded(WorkOrder workOrder, WorkOrderRepairDTO dto) {
        WorkOrderQuote currentQuote = getCurrentValidQuote(workOrder.getId());
        BigDecimal nextQuoteAmount = dto == null ? null : dto.getQuoteAmount();
        String nextQuoteDesc = normalizeNullableText(dto == null ? null : dto.getQuoteDesc());
        if (currentQuote == null) {
            if (nextQuoteAmount != null || nextQuoteDesc != null) {
                throw new ServiceException("\u8bf7\u5148\u63d0\u4ea4\u62a5\u4ef7\uff0c\u518d\u5728\u7ef4\u4fee\u767b\u8bb0\u4e2d\u8c03\u6574\u62a5\u4ef7");
            }
            return;
        }
        if (!isQuoteChanged(currentQuote, nextQuoteAmount, nextQuoteDesc)) {
            return;
        }
        String faultJudge = normalizeFaultJudge(
                currentQuote.getFaultJudge(),
                "\u5f53\u524d\u6709\u6548\u62a5\u4ef7\u7684\u6545\u969c\u5224\u5b9a\u4e0d\u80fd\u4e3a\u7a7a"
        );
        WorkOrderQuote quote = replaceCurrentQuote(workOrder, faultJudge, nextQuoteAmount, nextQuoteDesc);
        saveFlow(workOrder.getId(), "QUOTE", workOrder.getMainStatus(), workOrder.getMainStatus(),
                workOrder.getCurrentAcceptCompanyId(), workOrder.getCurrentAcceptCompanyId(),
                workOrder.getCurrentAcceptCompanyId(), quote.getQuoteDesc());
    }

    private void validateRepairContent(WorkOrder workOrder, WorkOrderRepairDTO dto) {
        if (dto == null) {
            throw new ServiceException("\u7ef4\u4fee\u767b\u8bb0\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (!hasRepairContent(dto)) {
            throw new ServiceException("\u8bf7\u81f3\u5c11\u586b\u5199\u4e00\u9879\u7ef4\u4fee\u5185\u5bb9");
        }
        dto.setFaults(normalizeRepairFaults(workOrder, dto.getFaults()));
    }

    private boolean hasRepairContent(WorkOrderRepairDTO dto) {
        if (!isBlank(dto.getRepairSummary()) || !isBlank(dto.getRepairDesc()) || !isBlank(dto.getOtherDesc())) {
            return true;
        }
        if (dto.getFaults() == null || dto.getFaults().isEmpty()) {
            return false;
        }
        for (WorkOrderFaultItemDTO fault : dto.getFaults()) {
            if (fault == null) {
                continue;
            }
            if (!isBlank(fault.getFaultDesc())
                    || (fault.getRepairItems() != null && !fault.getRepairItems().isEmpty())
                    || !isBlank(fault.getRepairDesc())
                    || !isBlank(fault.getOtherDesc())
                    || !isBlank(fault.getPartDesc())
                    || !isBlank(fault.getImageUrls())) {
                return true;
            }
        }
        return false;
    }

    private WorkOrderQuote getCurrentValidQuote(Long workOrderId) {
        LambdaQueryWrapper<WorkOrderQuote> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkOrderQuote::getWorkOrderId, workOrderId)
                .eq(WorkOrderQuote::getIsCurrentValid, 1)
                .orderByDesc(WorkOrderQuote::getCreateTime);
        List<WorkOrderQuote> quotes = workOrderQuoteMapper.selectList(wrapper);
        if (quotes == null || quotes.isEmpty()) {
            return null;
        }
        return quotes.get(0);
    }

    private boolean isQuoteChanged(WorkOrderQuote currentQuote, BigDecimal nextQuoteAmount, String nextQuoteDesc) {
        if (!isSameQuoteAmount(currentQuote.getQuoteAmount(), nextQuoteAmount)) {
            return true;
        }
        return !isSameText(currentQuote.getQuoteDesc(), nextQuoteDesc);
    }

    private boolean isSameQuoteAmount(BigDecimal left, BigDecimal right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.compareTo(right) == 0;
    }

    private boolean isSameText(String left, String right) {
        String normalizedLeft = normalizeNullableText(left);
        String normalizedRight = normalizeNullableText(right);
        if (normalizedLeft == null) {
            return normalizedRight == null;
        }
        return normalizedLeft.equals(normalizedRight);
    }

    private WorkOrderQuote replaceCurrentQuote(WorkOrder workOrder, String faultJudge, BigDecimal quoteAmount, String quoteDesc) {
        UpdateWrapper<WorkOrderQuote> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("work_order_id", workOrder.getId())
                .set("is_current_valid", 0);
        workOrderQuoteMapper.update(null, updateWrapper);

        WorkOrderQuote quote = new WorkOrderQuote();
        quote.setWorkOrderId(workOrder.getId());
        quote.setCompanyId(workOrder.getCurrentAcceptCompanyId());
        quote.setQuotedBy(SecurityContext.getCurrentUserId());
        quote.setFaultJudge(faultJudge);
        quote.setQuoteAmount(quoteAmount);
        quote.setQuoteDesc(normalizeNullableText(quoteDesc));
        quote.setIsCurrentValid(1);
        workOrderQuoteMapper.insert(quote);
        return quote;
    }

    private Long resolveCreateCustomerId(String customerName, String customerMobile, boolean autoCreateCustomer) {
        LambdaQueryWrapper<WorkOrderCustomer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkOrderCustomer::getPhone, customerMobile)
                .orderByAsc(WorkOrderCustomer::getId);
        List<WorkOrderCustomer> customers = workOrderCustomerMapper.selectList(wrapper);
        WorkOrderCustomer customer = customers.stream()
                .filter(item -> item != null && (item.getStatus() == null || item.getStatus() == 1))
                .findFirst()
                .orElse(null);
        if (customer != null) {
            if ((customer.getNickname() == null || customer.getNickname().trim().isEmpty())
                    && customerName != null && !customerName.trim().isEmpty()) {
                customer.setNickname(customerName);
                workOrderCustomerMapper.updateById(customer);
            }
            return customer.getId();
        }
        if (!autoCreateCustomer) {
            return null;
        }
        WorkOrderCustomer newCustomer = new WorkOrderCustomer();
        newCustomer.setOpenid(generateSystemCustomerOpenid());
        newCustomer.setPhone(customerMobile);
        newCustomer.setNickname(customerName);
        newCustomer.setStatus(1);
        workOrderCustomerMapper.insert(newCustomer);
        return newCustomer.getId();
    }

    private String resolveSendField(String serviceMode, String value) {
        if (!"\u5bc4\u4fee".equals(serviceMode) || isBlank(value)) {
            return null;
        }
        return value.trim();
    }

    private String resolveReturnExpressNo(WorkOrderCloseDTO dto) {
        if (dto == null || !RETURN_METHOD_MAIL.equals(normalizeNullableText(dto.getReturnMethod()))
                || isBlank(dto.getReturnExpressNo())) {
            return null;
        }
        return dto.getReturnExpressNo().trim();
    }

    private void validateAssignedRepairer(Long userId, Long companyId) {
        if (userId == null || !listCompanyRepairerUserIds(companyId).contains(userId)) {
            throw new ServiceException("\u6d3e\u5355\u5bf9\u8c61\u5fc5\u987b\u662f\u5f53\u524d\u53d7\u7406\u516c\u53f8\u7684\u7cfb\u7edf\u7ef4\u4fee\u5458");
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new ServiceException("\u6d3e\u5355\u5bf9\u8c61\u5fc5\u987b\u662f\u542f\u7528\u72b6\u6001\u7684\u7cfb\u7edf\u7ef4\u4fee\u5458");
        }
    }

    private void validateTransferTarget(WorkOrder workOrder, Long targetCompanyId) {
        if (targetCompanyId == null) {
            throw new ServiceException("\u76ee\u6807\u516c\u53f8\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (targetCompanyId.equals(workOrder.getCurrentAcceptCompanyId())) {
            throw new ServiceException("\u76ee\u6807\u516c\u53f8\u4e0d\u80fd\u548c\u5f53\u524d\u53d7\u7406\u516c\u53f8\u76f8\u540c");
        }
        List<Long> targetCompanyIds = resolveTransferTargetCompanyIds(workOrder);
        if (!targetCompanyIds.contains(targetCompanyId)) {
            throw new ServiceException("\u5f53\u524d\u5de5\u5355\u4e0d\u5141\u8bb8\u8f6c\u5230\u8be5\u76ee\u6807\u516c\u53f8");
        }
    }

    private void validateCreateHqCompany(Long currentCompanyId, Long hqCompanyId) {
        List<Long> hqCompanyIds = resolveCreateHqCompanyIds(currentCompanyId);
        if (hqCompanyIds.isEmpty()) {
            throw new ServiceException("\u5f53\u524d\u516c\u53f8\u6ca1\u6709\u53ef\u9009\u5f52\u5c5e\u603b\u90e8");
        }
        if (!hqCompanyIds.contains(hqCompanyId)) {
            throw new ServiceException("\u5f53\u524d\u516c\u53f8\u4e0d\u5141\u8bb8\u5f52\u5c5e\u5230\u8be5\u603b\u90e8");
        }
    }

    private String resolveCreateCompanySubjectType(Long currentCompanyId) {
        String subjectType = normalizeNullableText(SecurityContext.getCurrentSubjectType());
        if (subjectType != null) {
            return subjectType;
        }
        return resolveCompanySubjectType(currentCompanyId);
    }

    private WorkOrderCreateBarcodeInfoVO buildCreateBarcodeInfo(MachineBarcode barcodeArchive, Long hqCompanyId,
                                                                List<SysCompanySimpleVO> targetOptions,
                                                                Long defaultTargetCompanyId) {
        WorkOrderCreateBarcodeInfoVO vo = new WorkOrderCreateBarcodeInfoVO();
        vo.setBarcode(normalizeNullableText(barcodeArchive.getBarcode()));
        vo.setProductCode(normalizeNullableText(barcodeArchive.getProductCode()));
        vo.setProductName(normalizeNullableText(barcodeArchive.getProductName()));
        vo.setProductModel(normalizeNullableText(barcodeArchive.getProductModel()));
        vo.setMachineNo(normalizeNullableText(barcodeArchive.getMachineNo()));
        vo.setBrandCode(normalizeNullableText(barcodeArchive.getBrandCode()));
        vo.setWarrantyStatus(normalizeNullableText(barcodeArchive.getWarrantyStatus()));
        if (hqCompanyId != null) {
            SysCompany hqCompany = requireActiveHqCompany(hqCompanyId);
            vo.setHqCompanyId(hqCompany.getId());
            vo.setHqCompanyName(hqCompany.getCompanyName());
            vo.setFaultOptions(buildCreateFaultOptions(hqCompany.getId(), barcodeArchive.getProductCode(), barcodeArchive.getProductModel()));
            vo.setOtherFaultLabel(OTHER_FAULT_LABEL);
        }
        vo.setTargetCompanyOptions(targetOptions == null ? Collections.emptyList() : targetOptions);
        vo.setDefaultTargetCompanyId(defaultTargetCompanyId);
        return vo;
    }

    private MachineBarcode requireActiveMachineBarcode(String barcode) {
        MachineBarcode barcodeArchive = findActiveMachineBarcode(barcode);
        if (barcodeArchive == null) {
            throw new ServiceException("\u5f53\u524d\u6761\u7801\u672a\u7ef4\u62a4\u6863\u6848\u4fe1\u606f");
        }
        return barcodeArchive;
    }

    private MachineBarcode findActiveMachineBarcode(String barcode) {
        String normalizedBarcode = normalizeRequiredText(barcode, "\u673a\u5668\u6761\u7801\u4e0d\u80fd\u4e3a\u7a7a");
        LambdaQueryWrapper<MachineBarcode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MachineBarcode::getBarcode, normalizedBarcode)
                .eq(MachineBarcode::getStatus, 1)
                .last("LIMIT 1");
        return machineBarcodeMapper.selectOne(wrapper);
    }

    private SysCompany requireActiveHqCompany(Long hqCompanyId) {
        SysCompany company = sysCompanyMapper.selectById(hqCompanyId);
        if (company == null || (company.getStatus() != null && company.getStatus() == 0)) {
            throw new ServiceException("\u5f52\u5c5e\u603b\u90e8\u4e0d\u5b58\u5728");
        }
        if ("FIRST".equals(company.getTypeCode()) || "SECOND".equals(company.getTypeCode())) {
            throw new ServiceException("\u5f52\u5c5e\u603b\u90e8\u7c7b\u578b\u4e0d\u6b63\u786e");
        }
        return company;
    }

    private Long resolveSingleHqCompanyId(Long currentCompanyId, MachineBarcode barcodeArchive) {
        Long archiveHqCompanyId = resolveBarcodeArchiveHqCompanyId(barcodeArchive);
        if (archiveHqCompanyId != null) {
            validateCreateHqCompany(currentCompanyId, archiveHqCompanyId);
            return archiveHqCompanyId;
        }
        List<Long> hqCompanyIds = resolveCreateHqCompanyIds(currentCompanyId);
        if (hqCompanyIds.isEmpty()) {
            throw new ServiceException("\u5f53\u524d\u6761\u7801\u5f52\u5c5e\u603b\u90e8\u6682\u65e0\u6cd5\u81ea\u52a8\u8bc6\u522b\uff0c\u8bf7\u8054\u7cfb\u7ba1\u7406\u5458\u5b8c\u5584\u6761\u7801\u914d\u7f6e");
        }
        if (hqCompanyIds.size() > 1) {
            throw new ServiceException("\u5f53\u524d\u6761\u7801\u5f52\u5c5e\u603b\u90e8\u5b58\u5728\u591a\u4e2a\u5019\u9009\u9879\uff0c\u8bf7\u8054\u7cfb\u7ba1\u7406\u5458\u5b8c\u5584\u6761\u7801\u914d\u7f6e");
        }
        return hqCompanyIds.get(0);
    }

    private Long resolveBarcodeArchiveHqCompanyId(MachineBarcode barcodeArchive) {
        if (barcodeArchive == null || barcodeArchive.getHqCompanyId() == null) {
            return null;
        }
        return requireActiveHqCompany(barcodeArchive.getHqCompanyId()).getId();
    }

    private Long resolveResolvedHqCompanyId(MachineBarcode barcodeArchive, List<SysCompanySimpleVO> targetOptions) {
        Long archiveHqCompanyId = resolveBarcodeArchiveHqCompanyId(barcodeArchive);
        if (archiveHqCompanyId != null) {
            return archiveHqCompanyId;
        }
        if (targetOptions != null && targetOptions.size() == 1) {
            return targetOptions.get(0).getId();
        }
        return null;
    }

    private Long resolveDefaultTargetCompanyId(List<SysCompanySimpleVO> targetOptions) {
        if (targetOptions == null || targetOptions.size() != 1) {
            return null;
        }
        return targetOptions.get(0).getId();
    }

    private Long resolveSelectedTargetCompanyId(Long selectedTargetCompanyId, List<SysCompanySimpleVO> targetOptions,
                                               String emptyMessage) {
        if (targetOptions == null || targetOptions.isEmpty()) {
            throw new ServiceException("\u5f53\u524d\u6ca1\u6709\u53ef\u9009\u7684\u4e0a\u6e38\u53d7\u7406\u516c\u53f8");
        }
        Set<Long> allowedIds = targetOptions.stream()
                .map(SysCompanySimpleVO::getId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (selectedTargetCompanyId == null) {
            if (allowedIds.size() == 1) {
                return allowedIds.iterator().next();
            }
            throw new ServiceException(emptyMessage);
        }
        if (!allowedIds.contains(selectedTargetCompanyId)) {
            throw new ServiceException("\u9009\u62e9\u7684\u4e0a\u6e38\u53d7\u7406\u516c\u53f8\u4e0d\u5728\u5141\u8bb8\u8303\u56f4\u5185");
        }
        return selectedTargetCompanyId;
    }

    private List<Long> resolveCreateHqCompanyIds(Long currentCompanyId) {
        if (currentCompanyId == null) {
            return Collections.emptyList();
        }
        if ("HQ".equals(SecurityContext.getCurrentSubjectType())) {
            return Collections.singletonList(currentCompanyId);
        }
        String currentTypeCode = requireCompanyTypeCode(currentCompanyId);
        if ("FIRST".equals(currentTypeCode)) {
            LambdaQueryWrapper<HqFirstContract> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(HqFirstContract::getFirstCompanyId, currentCompanyId)
                    .eq(HqFirstContract::getStatus, 1)
                    .orderByAsc(HqFirstContract::getId);
            return resolveDistinctHqIds(hqFirstContractMapper.selectList(wrapper));
        }
        if ("SECOND".equals(currentTypeCode)) {
            LambdaQueryWrapper<FirstSecondRelation> relationWrapper = new LambdaQueryWrapper<>();
            relationWrapper.eq(FirstSecondRelation::getSecondCompanyId, currentCompanyId)
                    .eq(FirstSecondRelation::getStatus, 1)
                    .orderByAsc(FirstSecondRelation::getId);
            List<Long> firstCompanyIds = firstSecondRelationMapper.selectList(relationWrapper).stream()
                    .map(FirstSecondRelation::getFirstCompanyId)
                    .filter(id -> id != null)
                    .distinct()
                    .collect(Collectors.toList());
            if (firstCompanyIds.isEmpty()) {
                return Collections.emptyList();
            }
            LambdaQueryWrapper<HqFirstContract> contractWrapper = new LambdaQueryWrapper<>();
            contractWrapper.in(HqFirstContract::getFirstCompanyId, firstCompanyIds)
                    .eq(HqFirstContract::getStatus, 1)
                    .orderByAsc(HqFirstContract::getId);
            return resolveDistinctHqIds(hqFirstContractMapper.selectList(contractWrapper));
        }
        return Collections.emptyList();
    }

    private List<Long> resolveDistinctHqIds(List<HqFirstContract> contracts) {
        if (contracts == null || contracts.isEmpty()) {
            return Collections.emptyList();
        }
        return contracts.stream()
                .map(HqFirstContract::getHqCompanyId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<SysCompanySimpleVO> listUpstreamFirstOptions(Long currentCompanyId, Long hqCompanyId) {
        LambdaQueryWrapper<FirstSecondRelation> relationWrapper = new LambdaQueryWrapper<>();
        relationWrapper.eq(FirstSecondRelation::getSecondCompanyId, currentCompanyId)
                .eq(FirstSecondRelation::getStatus, 1)
                .orderByAsc(FirstSecondRelation::getId);
        List<Long> firstCompanyIds = firstSecondRelationMapper.selectList(relationWrapper).stream()
                .map(FirstSecondRelation::getFirstCompanyId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (firstCompanyIds.isEmpty()) {
            throw new ServiceException("\u5f53\u524d\u516c\u53f8\u672a\u914d\u7f6e\u53ef\u62a5\u4fee\u7684\u4e00\u7ea7\u516c\u53f8");
        }
        LambdaQueryWrapper<HqFirstContract> contractWrapper = new LambdaQueryWrapper<>();
        contractWrapper.in(HqFirstContract::getFirstCompanyId, firstCompanyIds)
                .eq(HqFirstContract::getStatus, 1);
        if (hqCompanyId != null) {
            contractWrapper.eq(HqFirstContract::getHqCompanyId, hqCompanyId);
        }
        List<Long> allowedFirstCompanyIds = hqFirstContractMapper.selectList(contractWrapper).stream()
                .map(HqFirstContract::getFirstCompanyId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (allowedFirstCompanyIds.isEmpty()) {
            throw new ServiceException("\u5f53\u524d\u6761\u7801\u672a\u5339\u914d\u5230\u53ef\u62a5\u4fee\u7684\u4e00\u7ea7\u516c\u53f8");
        }
        return buildCompanySimpleVoList(allowedFirstCompanyIds);
    }

    private List<SysCompanySimpleVO> listUpstreamHqOptions(Long currentCompanyId, MachineBarcode barcodeArchive) {
        Long archiveHqCompanyId = resolveBarcodeArchiveHqCompanyId(barcodeArchive);
        if (archiveHqCompanyId != null) {
            validateCreateHqCompany(currentCompanyId, archiveHqCompanyId);
            return buildCompanySimpleVoList(Collections.singletonList(archiveHqCompanyId));
        }
        List<Long> hqCompanyIds = resolveCreateHqCompanyIds(currentCompanyId);
        if (hqCompanyIds.isEmpty()) {
            throw new ServiceException("\u5f53\u524d\u516c\u53f8\u672a\u914d\u7f6e\u53ef\u62a5\u4fee\u7684\u4f73\u58eb\u603b\u90e8");
        }
        return buildCompanySimpleVoList(hqCompanyIds);
    }

    private List<SysCompanySimpleVO> buildCompanySimpleVoList(List<Long> companyIds) {
        if (companyIds == null || companyIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<SysCompany> companies = sysCompanyMapper.selectBatchIds(companyIds);
        List<SysCompany> activeCompanies = companies.stream()
                .filter(company -> company != null && (company.getStatus() == null || company.getStatus() == 1))
                .sorted(java.util.Comparator.comparing(SysCompany::getId))
                .collect(Collectors.toList());
        if (activeCompanies.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, String> typeNameMap = buildTypeNameMap(activeCompanies);
        return activeCompanies.stream()
                .map(company -> buildCompanySimpleVo(company, typeNameMap.get(company.getTypeCode())))
                .collect(Collectors.toList());
    }

    private CustomerFaultSelection resolveCreateFaultSelection(List<String> faultItems, String faultRemark,
                                                              Long hqCompanyId, String productCode, String productModel) {
        List<String> configuredFaultOptions = listConfiguredFaultOptions(hqCompanyId, productCode, productModel);
        if (configuredFaultOptions.isEmpty()) {
            throw new ServiceException("\u5f53\u524d\u4ea7\u54c1\u672a\u914d\u7f6e\u6545\u969c\u9879\uff0c\u4e0d\u80fd\u5efa\u5355\uff0c\u8bf7\u8054\u7cfb\u7ba1\u7406\u5458\u5b8c\u5584\u914d\u7f6e");
        }
        List<String> normalizedFaultItems = normalizeFaultItems(faultItems);
        if (normalizedFaultItems.isEmpty()) {
            throw new ServiceException("\u8bf7\u9009\u62e9\u6545\u969c\u63cf\u8ff0");
        }
        LinkedHashSet<String> allowedFaultOptions = new LinkedHashSet<>(configuredFaultOptions);
        allowedFaultOptions.add(OTHER_FAULT_LABEL);
        for (String faultItem : normalizedFaultItems) {
            if (!allowedFaultOptions.contains(faultItem)) {
                throw new ServiceException("\u6545\u969c\u63cf\u8ff0\u4e0d\u5728\u53ef\u9009\u8303\u56f4\u5185");
            }
        }
        String normalizedFaultRemark = normalizeNullableText(faultRemark);
        if (normalizedFaultItems.contains(OTHER_FAULT_LABEL) && normalizedFaultRemark == null) {
            throw new ServiceException("\u9009\u62e9\u5176\u5b83\u6545\u969c\u65f6\u5fc5\u987b\u586b\u5199\u6545\u969c\u8bf4\u660e");
        }
        return new CustomerFaultSelection(String.join(FAULT_DESC_SEPARATOR, normalizedFaultItems), normalizedFaultRemark);
    }

    private List<String> buildCreateFaultOptions(Long hqCompanyId, String productCode, String productModel) {
        LinkedHashSet<String> result = new LinkedHashSet<>(listConfiguredFaultOptions(hqCompanyId, productCode, productModel));
        if (result.isEmpty()) {
            throw new ServiceException("\u5f53\u524d\u4ea7\u54c1\u672a\u914d\u7f6e\u6545\u969c\u9879\uff0c\u4e0d\u80fd\u5efa\u5355\uff0c\u8bf7\u8054\u7cfb\u7ba1\u7406\u5458\u5b8c\u5584\u914d\u7f6e");
        }
        result.add(OTHER_FAULT_LABEL);
        return new ArrayList<>(result);
    }

    private List<String> listConfiguredFaultOptions(Long hqCompanyId, String productCode, String productModel) {
        List<WorkOrderRepairFaultOptionVO> options = faultRepairConfigService == null
                ? Collections.emptyList()
                : faultRepairConfigService.listRepairFaultOptions(hqCompanyId, productCode, productModel);
        if (options == null || options.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (WorkOrderRepairFaultOptionVO option : options) {
            String faultDesc = normalizeNullableText(option == null ? null : option.getFaultDesc());
            if (faultDesc != null) {
                result.add(faultDesc);
            }
        }
        return new ArrayList<>(result);
    }

    private List<String> normalizeFaultItems(List<String> faultItems) {
        if (faultItems == null || faultItems.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String faultItem : faultItems) {
            String normalized = normalizeNullableText(faultItem);
            if (normalized != null) {
                result.add(normalized);
            }
        }
        return new ArrayList<>(result);
    }

    private List<Long> resolveTransferTargetCompanyIds(WorkOrder workOrder) {
        Long currentCompanyId = workOrder.getCurrentAcceptCompanyId();
        String currentTypeCode = requireCompanyTypeCode(currentCompanyId);
        if ("SECOND".equals(currentTypeCode)) {
            LambdaQueryWrapper<FirstSecondRelation> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(FirstSecondRelation::getSecondCompanyId, currentCompanyId)
                    .eq(FirstSecondRelation::getStatus, 1)
                    .orderByAsc(FirstSecondRelation::getId);
            List<FirstSecondRelation> relations = firstSecondRelationMapper.selectList(wrapper);
            return relations.stream()
                    .map(FirstSecondRelation::getFirstCompanyId)
                    .filter(id -> id != null)
                    .distinct()
                    .collect(Collectors.toList());
        }
        if ("FIRST".equals(currentTypeCode)) {
            if (workOrder.getHqCompanyId() == null) {
                return Collections.emptyList();
            }
            LambdaQueryWrapper<HqFirstContract> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(HqFirstContract::getFirstCompanyId, currentCompanyId)
                    .eq(HqFirstContract::getHqCompanyId, workOrder.getHqCompanyId())
                    .eq(HqFirstContract::getStatus, 1);
            if (hqFirstContractMapper.selectCount(wrapper) == 0) {
                return Collections.emptyList();
            }
            return Collections.singletonList(workOrder.getHqCompanyId());
        }
        return Collections.emptyList();
    }

    private String resolveCompanySubjectType(Long companyId) {
        SysCompany company = sysCompanyMapper.selectById(companyId);
        if (company == null) {
            throw new ServiceException("\u76ee\u6807\u516c\u53f8\u4e0d\u5b58\u5728");
        }
        LambdaQueryWrapper<SysCompanyType> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysCompanyType::getTypeCode, company.getTypeCode());
        SysCompanyType companyType = sysCompanyTypeMapper.selectOne(wrapper);
        if (companyType == null) {
            throw new ServiceException("\u76ee\u6807\u516c\u53f8\u7c7b\u578b\u4e0d\u5b58\u5728");
        }
        return companyType.getSubjectType();
    }

    private String requireCompanyTypeCode(Long companyId) {
        SysCompany company = sysCompanyMapper.selectById(companyId);
        if (company == null) {
            throw new ServiceException("\u5f53\u524d\u53d7\u7406\u516c\u53f8\u4e0d\u5b58\u5728");
        }
        return company.getTypeCode();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String normalizeRequiredText(String value, String message) {
        String normalized = normalizeNullableText(value);
        if (normalized == null) {
            throw new ServiceException(message);
        }
        return normalized;
    }

    private String normalizeFaultJudge(String faultJudge, String blankMessage) {
        String normalized = normalizeRequiredText(faultJudge, blankMessage);
        if (FAULT_JUDGE_HAS_FAULT.equals(normalized) || FAULT_JUDGE_NO_FAULT.equals(normalized)) {
            return normalized;
        }
        throw new ServiceException("\u6545\u969c\u5224\u5b9a\u53ea\u80fd\u4e3a\u6709\u6545\u969c\u6216\u65e0\u6545\u969c");
    }

    private String normalizeNullableText(String value) {
        if (isBlank(value)) {
            return null;
        }
        return value.trim();
    }

    private String normalizeReviewResult(String reviewResult) {
        String normalized = normalizeRequiredText(reviewResult, "\u590d\u68c0\u7ed3\u679c\u4e0d\u80fd\u4e3a\u7a7a");
        if (REVIEW_RESULT_PASS.equals(normalized) || REVIEW_RESULT_CONTINUE.equals(normalized)) {
            return normalized;
        }
        throw new ServiceException("\u590d\u68c0\u7ed3\u679c\u4e0d\u5408\u6cd5");
    }

    private int resolveContinueRepair(String reviewResult) {
        return REVIEW_RESULT_CONTINUE.equals(reviewResult) ? 1 : 0;
    }

    private String normalizeReturnMethod(String returnMethod) {
        String normalized = normalizeRequiredText(returnMethod, "\u673a\u5668\u8fd4\u56de\u65b9\u5f0f\u4e0d\u80fd\u4e3a\u7a7a");
        if (RETURN_METHOD_PICKUP.equals(normalized) || RETURN_METHOD_MAIL.equals(normalized)) {
            return normalized;
        }
        throw new ServiceException("\u673a\u5668\u8fd4\u56de\u65b9\u5f0f\u4e0d\u5408\u6cd5");
    }

    private WorkOrder buildWorkOrderSnapshot(WorkOrderListVO target) {
        if (target == null || target.getId() == null) {
            return null;
        }
        WorkOrder snapshot = new WorkOrder();
        snapshot.setId(target.getId());
        snapshot.setMainStatus(target.getMainStatus());
        snapshot.setCurrentAcceptCompanyId(target.getCurrentAcceptCompanyId());
        snapshot.setAssignedUserId(target.getAssignedUserId());
        return snapshot;
    }

    private Integer resolveReadonlyFlag(String relationType, Integer currentFlag) {
        if ("HISTORY_PARTICIPANT_READONLY".equals(relationType) || "HQ_OBSERVER".equals(relationType)) {
            return 1;
        }
        if (relationType != null && !"NONE".equals(relationType)) {
            return 0;
        }
        return currentFlag;
    }

    private boolean isNoFaultWorkOrder(Long workOrderId) {
        if (workOrderId == null) {
            return false;
        }
        LambdaQueryWrapper<WorkOrderQuote> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkOrderQuote::getWorkOrderId, workOrderId)
                .eq(WorkOrderQuote::getIsCurrentValid, 1)
                .orderByDesc(WorkOrderQuote::getCreateTime);
        List<WorkOrderQuote> quotes = workOrderQuoteMapper.selectList(wrapper);
        if (quotes == null || quotes.isEmpty()) {
            return false;
        }
        String faultJudge = normalizeNullableText(quotes.get(0).getFaultJudge());
        return FAULT_JUDGE_NO_FAULT.equals(faultJudge);
    }

    private String generateSystemCustomerOpenid() {
        return "SYS_WO_" + IdUtil.fastSimpleUUID();
    }

    private void saveFlow(Long workOrderId, String actionType, String beforeStatus, String afterStatus,
                          Long fromCompanyId, Long toCompanyId, Long operatorCompanyId, String remark) {
        WorkOrderFlow flow = new WorkOrderFlow();
        flow.setWorkOrderId(workOrderId);
        flow.setActionType(actionType);
        flow.setBeforeStatus(beforeStatus);
        flow.setAfterStatus(afterStatus);
        flow.setFromCompanyId(fromCompanyId);
        flow.setToCompanyId(toCompanyId);
        flow.setOperatorCompanyId(operatorCompanyId);
        flow.setOperatorUserId(SecurityContext.getCurrentUserId());
        flow.setRemark(remark);
        workOrderFlowMapper.insert(flow);
    }

    private void saveFaults(Long workOrderId, Long repairId, Long companyId, List<WorkOrderFaultItemDTO> faults) {
        if (faults == null || faults.isEmpty()) {
            return;
        }
        int sort = 1;
        for (WorkOrderFaultItemDTO item : faults) {
            if (item == null) {
                throw new ServiceException("\u6545\u969c\u70b9\u4fe1\u606f\u4e0d\u80fd\u4e3a\u7a7a");
            }
            if (isBlank(item.getFaultDesc())) {
                throw new ServiceException("\u6545\u969c\u63cf\u8ff0\u4e0d\u80fd\u4e3a\u7a7a");
            }
            WorkOrderFault fault = new WorkOrderFault();
            fault.setWorkOrderId(workOrderId);
            fault.setRepairId(repairId);
            fault.setCompanyId(companyId);
            fault.setFaultDesc(item.getFaultDesc().trim());
            fault.setRepairDesc(normalizeNullableText(item.getRepairDesc()));
            fault.setOtherDesc(normalizeNullableText(item.getOtherDesc()));
            fault.setPartDesc(normalizeNullableText(item.getPartDesc()));
            fault.setImageUrls(normalizeNullableText(item.getImageUrls()));
            fault.setSortNum(sort++);
            fault.setCreatedBy(SecurityContext.getCurrentUserId());
            workOrderFaultMapper.insert(fault);
        }
    }

    private List<WorkOrderFaultItemDTO> normalizeRepairFaults(WorkOrder workOrder, List<WorkOrderFaultItemDTO> faults) {
        if (faults == null || faults.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, Set<String>> optionMap = buildRepairOptionMap(workOrder);
        boolean hasConfiguredFaults = !optionMap.isEmpty();
        List<WorkOrderFaultItemDTO> result = new ArrayList<>();
        for (WorkOrderFaultItemDTO item : faults) {
            if (item == null) {
                throw new ServiceException("\u6545\u969c\u70b9\u4fe1\u606f\u4e0d\u80fd\u4e3a\u7a7a");
            }
            if (!hasFaultContent(item)) {
                continue;
            }
            WorkOrderFaultItemDTO normalized = new WorkOrderFaultItemDTO();
            String faultDesc = normalizeRequiredText(item.getFaultDesc(), "\u6545\u969c\u63cf\u8ff0\u4e0d\u80fd\u4e3a\u7a7a");
            normalized.setFaultDesc(faultDesc);
            normalized.setPartDesc(normalizeRequiredText(item.getPartDesc(), "\u914d\u4ef6\u4fe1\u606f\u4e0d\u80fd\u4e3a\u7a7a"));
            normalized.setImageUrls(normalizeNullableText(item.getImageUrls()));
            List<String> repairItems = normalizeRepairItems(item.getRepairItems());
            Set<String> allowedOptions = optionMap.get(faultDesc);
            String repairDesc = normalizeNullableText(item.getRepairDesc());
            String otherDesc = normalizeNullableText(item.getOtherDesc());
            if (hasConfiguredFaults && allowedOptions == null) {
                throw new ServiceException("\u6545\u969c\u63cf\u8ff0\u4e0d\u5728\u5f53\u524d\u914d\u7f6e\u8303\u56f4\u5185");
            }
            if (allowedOptions != null && !allowedOptions.isEmpty()) {
                if (repairItems.isEmpty()) {
                    throw new ServiceException("\u8bf7\u9009\u62e9\u914d\u7f6e\u5185\u7684\u7ef4\u4fee\u8bf4\u660e");
                }
                validateRepairItems(repairItems, allowedOptions);
                if (repairItems.contains(OTHER_REPAIR_OPTION) && otherDesc == null) {
                    throw new ServiceException("\u9009\u62e9\u5176\u5b83\u7ef4\u4fee\u8bf4\u660e\u65f6\uff0c\u5176\u4ed6\u7ef4\u4fee\u8bf4\u660e\u4e0d\u80fd\u4e3a\u7a7a");
                }
                repairDesc = String.join("\uff1b", repairItems);
                normalized.setRepairItems(repairItems);
            } else if (!repairItems.isEmpty()) {
                validateRepairItems(repairItems, allowedOptions);
                if (repairItems.contains(OTHER_REPAIR_OPTION) && otherDesc == null) {
                    throw new ServiceException("\u9009\u62e9\u5176\u5b83\u7ef4\u4fee\u8bf4\u660e\u65f6\uff0c\u5176\u4ed6\u7ef4\u4fee\u8bf4\u660e\u4e0d\u80fd\u4e3a\u7a7a");
                }
                repairDesc = String.join("\uff1b", repairItems);
                normalized.setRepairItems(repairItems);
            } else if (repairDesc == null) {
                throw new ServiceException("\u7ef4\u4fee\u8bf4\u660e\u4e0d\u80fd\u4e3a\u7a7a");
            }
            normalized.setRepairDesc(repairDesc);
            normalized.setOtherDesc(otherDesc);
            result.add(normalized);
        }
        return result;
    }

    private Map<String, Set<String>> buildRepairOptionMap(WorkOrder workOrder) {
        List<WorkOrderRepairFaultOptionVO> options = faultRepairConfigService.listRepairFaultOptions(
                workOrder.getHqCompanyId(),
                workOrder.getProductCode(),
                workOrder.getProductModel()
        );
        if (options == null || options.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Set<String>> result = new HashMap<>();
        for (WorkOrderRepairFaultOptionVO option : options) {
            if (option == null || isBlank(option.getFaultDesc())) {
                continue;
            }
            Set<String> repairOptions = option.getRepairOptions() == null
                    ? Collections.emptySet()
                    : option.getRepairOptions().stream()
                    .filter(StrUtil::isNotBlank)
                    .map(String::trim)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            result.put(option.getFaultDesc().trim(), repairOptions);
        }
        return result;
    }

    private List<String> normalizeRepairItems(List<String> repairItems) {
        if (repairItems == null || repairItems.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (String repairItem : repairItems) {
            String normalized = normalizeNullableText(repairItem);
            if (normalized != null) {
                result.add(normalized);
            }
        }
        return result;
    }

    private void validateRepairItems(List<String> repairItems, Set<String> allowedOptions) {
        if (repairItems.isEmpty()) {
            throw new ServiceException("\u7ef4\u4fee\u8bf4\u660e\u4e0d\u80fd\u4e3a\u7a7a");
        }
        Set<String> duplicateCheck = new HashSet<>();
        for (String repairItem : repairItems) {
            if (!duplicateCheck.add(repairItem)) {
                throw new ServiceException("\u7ef4\u4fee\u8bf4\u660e\u4e0d\u80fd\u91cd\u590d");
            }
            if (!allowedOptions.isEmpty()
                    && !allowedOptions.contains(repairItem)
                    && !OTHER_REPAIR_OPTION.equals(repairItem)) {
                throw new ServiceException("\u7ef4\u4fee\u8bf4\u660e\u4e0d\u5728\u5f53\u524d\u6545\u969c\u914d\u7f6e\u8303\u56f4\u5185");
            }
        }
    }

    private boolean hasFaultContent(WorkOrderFaultItemDTO fault) {
        if (fault == null) {
            return false;
        }
        return !isBlank(fault.getFaultDesc())
                || (fault.getRepairItems() != null && !fault.getRepairItems().isEmpty())
                || !isBlank(fault.getRepairDesc())
                || !isBlank(fault.getOtherDesc())
                || !isBlank(fault.getPartDesc())
                || !isBlank(fault.getImageUrls());
    }

    private Map<Long, String> buildCompanyNameMap(Set<Long> companyIds) {
        if (companyIds == null || companyIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SysCompany> companies = sysCompanyMapper.selectBatchIds(companyIds);
        return companies.stream().collect(Collectors.toMap(SysCompany::getId, SysCompany::getCompanyName, (a, b) -> a));
    }

    private Map<String, String> buildTypeNameMap(List<SysCompany> companies) {
        if (companies == null || companies.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<String> typeCodes = companies.stream()
                .map(SysCompany::getTypeCode)
                .filter(code -> code != null && !code.trim().isEmpty())
                .collect(Collectors.toSet());
        if (typeCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<SysCompanyType> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysCompanyType::getTypeCode, typeCodes);
        List<SysCompanyType> companyTypes = sysCompanyTypeMapper.selectList(wrapper);
        return companyTypes.stream().collect(Collectors.toMap(SysCompanyType::getTypeCode, SysCompanyType::getTypeName, (a, b) -> a));
    }

    private Map<Long, String> buildUserNameMap(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SysUser> users = sysUserMapper.selectBatchIds(userIds);
        return users.stream().collect(Collectors.toMap(SysUser::getId, SysUser::getRealName, (a, b) -> a));
    }

    private Set<Long> listCompanyUserIds(Long companyId) {
        if (companyId == null) {
            return Collections.emptySet();
        }
        LambdaQueryWrapper<SysUserCompany> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUserCompany::getCompanyId, companyId);
        List<SysUserCompany> userCompanies = sysUserCompanyMapper.selectList(wrapper);
        return userCompanies.stream()
                .map(SysUserCompany::getUserId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 查询当前公司下拥有系统维修员角色的用户ID集合。
     *
     * @param companyId 公司ID
     * @return 用户ID集合
     */
    private Set<Long> listCompanyRepairerUserIds(Long companyId) {
        Set<Long> companyUserIds = listCompanyUserIds(companyId);
        if (companyUserIds.isEmpty()) {
            return Collections.emptySet();
        }
        LambdaQueryWrapper<SysRole> roleWrapper = new LambdaQueryWrapper<>();
        roleWrapper.eq(SysRole::getCompanyId, companyId)
                .eq(SysRole::getStatus, 1)
                .eq(SysRole::getIsSystem, 1)
                .eq(SysRole::getRoleKey, RoleConstants.REPAIRER_ROLE_KEY);
        List<SysRole> roles = sysRoleMapper.selectList(roleWrapper);
        if (roles.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> roleIds = roles.stream()
                .map(SysRole::getId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (roleIds.isEmpty()) {
            return Collections.emptySet();
        }
        LambdaQueryWrapper<SysUserRole> userRoleWrapper = new LambdaQueryWrapper<>();
        userRoleWrapper.in(SysUserRole::getRoleId, roleIds);
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(userRoleWrapper);
        if (userRoles.isEmpty()) {
            return Collections.emptySet();
        }
        return userRoles.stream()
                .map(SysUserRole::getUserId)
                .filter(companyUserIds::contains)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static class CustomerFaultSelection {

        private final String faultDesc;

        private final String faultRemark;

        private CustomerFaultSelection(String faultDesc, String faultRemark) {
            this.faultDesc = faultDesc;
            this.faultRemark = faultRemark;
        }

        private String getFaultDesc() {
            return faultDesc;
        }

        private String getFaultRemark() {
            return faultRemark;
        }
    }

    private WorkOrderUserOptionVO buildUserOption(SysUser user) {
        WorkOrderUserOptionVO vo = new WorkOrderUserOptionVO();
        vo.setId(user.getId());
        vo.setRealName(user.getRealName());
        vo.setPhone(user.getPhone());
        return vo;
    }

    private SysCompanySimpleVO buildCompanySimpleVo(SysCompany company, String typeName) {
        SysCompanySimpleVO vo = new SysCompanySimpleVO();
        vo.setId(company.getId());
        vo.setCompanyName(company.getCompanyName());
        vo.setCompanyCode(company.getCompanyCode());
        vo.setTypeCode(company.getTypeCode());
        vo.setTypeName(typeName);
        return vo;
    }

    private String generateOrderNo() {
        String datePart = LocalDateTime.now().format(ORDER_DATE_FORMATTER);
        String suffix = IdUtil.getSnowflakeNextIdStr();
        suffix = suffix.substring(Math.max(0, suffix.length() - 5));
        return "WO" + datePart + "-" + suffix;
    }
}
