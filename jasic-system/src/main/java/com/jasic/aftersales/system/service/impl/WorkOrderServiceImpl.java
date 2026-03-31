package com.jasic.aftersales.system.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jasic.aftersales.common.constant.RoleConstants;
import com.jasic.aftersales.common.constant.WorkOrderStatusConstants;
import com.jasic.aftersales.common.constant.WorkOrderStatusFlow;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.framework.security.SecurityContext;
import com.jasic.aftersales.system.domain.dto.WorkOrderAssignDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderCloseDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderCreateDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderFaultItemDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderQuoteDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderRepairDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderSendExpressDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderTechAcceptDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderReviewDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderTransferDTO;
import com.jasic.aftersales.system.domain.entity.SysCompany;
import com.jasic.aftersales.system.domain.entity.SysCompanyType;
import com.jasic.aftersales.system.domain.entity.SysRole;
import com.jasic.aftersales.system.domain.entity.SysUser;
import com.jasic.aftersales.system.domain.entity.SysUserCompany;
import com.jasic.aftersales.system.domain.entity.SysUserRole;
import com.jasic.aftersales.system.domain.entity.FirstSecondRelation;
import com.jasic.aftersales.system.domain.entity.HqFirstContract;
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
import com.jasic.aftersales.system.domain.vo.WorkOrderDetailVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderEvaluationVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderFaultVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderFlowVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderListVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderNotifyEventVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderQuoteVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderRepairVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderReviewVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderStatusCountVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderUserOptionVO;
import com.jasic.aftersales.system.domain.vo.SysCompanySimpleVO;
import com.jasic.aftersales.system.mapper.FirstSecondRelationMapper;
import com.jasic.aftersales.system.mapper.HqFirstContractMapper;
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
import com.jasic.aftersales.system.service.IWorkOrderService;
import com.jasic.aftersales.system.service.WorkOrderNotifyEventService;
import com.jasic.aftersales.system.service.WorkOrderParticipantService;
import com.jasic.aftersales.system.service.WorkOrderPermissionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
            fillListStatus(record);
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
        fillListStatus(detail);
        detail.setEvaluateStatusLabel(resolveEvaluateStatusLabel(detail.getEvaluateStatus()));
        return detail;
    }

    /**
     * 创建工单。
     *
     * @param dto 工单创建参数
     * @return 工单 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long save(WorkOrderCreateDTO dto) {
        Long currentCompanyId = requireCurrentCompanyId();
        validateCreateHqCompany(currentCompanyId, dto.getHqCompanyId());
        validateSendInfo(dto);
        WorkOrder entity = new WorkOrder();
        entity.setOrderNo(generateOrderNo());
        entity.setCustomerId(resolveCustomerId(dto));
        entity.setCustomerName(dto.getCustomerName());
        entity.setCustomerMobile(dto.getCustomerMobile());
        entity.setBarcode(dto.getBarcode());
        entity.setProductCode(dto.getProductCode());
        entity.setProductModel(dto.getProductModel());
        entity.setBrandCode(dto.getBrandCode());
        entity.setServiceMode(dto.getServiceMode());
        entity.setWarrantyStatus(dto.getWarrantyStatus());
        entity.setFaultDesc(dto.getFaultDesc());
        entity.setSenderName(resolveSendField(dto.getServiceMode(), dto.getSenderName()));
        entity.setSenderMobile(resolveSendField(dto.getServiceMode(), dto.getSenderMobile()));
        entity.setSenderAddress(resolveSendField(dto.getServiceMode(), dto.getSenderAddress()));
        entity.setSendExpressNo(resolveSendField(dto.getServiceMode(), dto.getSendExpressNo()));
        entity.setMainStatus(WorkOrderStatusFlow.afterCreate());
        entity.setEvaluateStatus(WorkOrderStatusFlow.afterCreateEvaluateStatus());
        entity.setCurrentAcceptSubjectType(SecurityContext.getCurrentSubjectType());
        entity.setCurrentAcceptCompanyId(currentCompanyId);
        entity.setCreateCompanyId(currentCompanyId);
        entity.setHqCompanyId(dto.getHqCompanyId());
        entity.setHasTransfer(0);
        entity.setTransferCount(0);
        workOrderMapper.insert(entity);

        saveFlow(entity.getId(), "CREATE", null, entity.getMainStatus(), null, currentCompanyId, currentCompanyId, null);
        workOrderParticipantService.initParticipants(entity);
        return entity.getId();
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
        LambdaUpdateWrapper<WorkOrderQuote> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WorkOrderQuote::getWorkOrderId, workOrder.getId())
                .set(WorkOrderQuote::getIsCurrentValid, 0);
        workOrderQuoteMapper.update(null, updateWrapper);

        WorkOrderQuote quote = new WorkOrderQuote();
        quote.setWorkOrderId(workOrder.getId());
        quote.setCompanyId(workOrder.getCurrentAcceptCompanyId());
        quote.setQuotedBy(SecurityContext.getCurrentUserId());
        quote.setFaultJudge(dto.getFaultJudge());
        quote.setQuoteAmount(dto.getQuoteAmount());
        quote.setQuoteDesc(dto.getQuoteDesc());
        quote.setIsCurrentValid(1);
        workOrderQuoteMapper.insert(quote);

        saveFlow(workOrder.getId(), "QUOTE", workOrder.getMainStatus(), workOrder.getMainStatus(),
                workOrder.getCurrentAcceptCompanyId(), workOrder.getCurrentAcceptCompanyId(),
                workOrder.getCurrentAcceptCompanyId(), dto.getQuoteDesc());
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
        WorkOrderRepair repair = new WorkOrderRepair();
        repair.setWorkOrderId(workOrder.getId());
        repair.setCompanyId(workOrder.getCurrentAcceptCompanyId());
        repair.setRepairUserId(SecurityContext.getCurrentUserId());
        repair.setRepairSummary(dto.getRepairSummary());
        repair.setRepairDesc(dto.getRepairDesc());
        repair.setOtherDesc(dto.getOtherDesc());
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
                    workOrder.getCurrentAcceptCompanyId(), dto.getRepairSummary());
            workOrderNotifyEventService.recordRepairFinished(workOrder, dto.getRepairSummary());
        } else {
            saveFlow(workOrder.getId(), "REPAIR_SAVE", workOrder.getMainStatus(), workOrder.getMainStatus(),
                    workOrder.getCurrentAcceptCompanyId(), workOrder.getCurrentAcceptCompanyId(),
                    workOrder.getCurrentAcceptCompanyId(), dto.getRepairSummary());
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
        WorkOrderReview review = new WorkOrderReview();
        review.setWorkOrderId(workOrder.getId());
        review.setCompanyId(workOrder.getCurrentAcceptCompanyId());
        review.setReviewUserId(SecurityContext.getCurrentUserId());
        review.setReviewResult(dto.getReviewResult());
        review.setReviewDesc(dto.getReviewDesc());
        review.setIsContinueRepair(dto.getIsContinueRepair() != null && dto.getIsContinueRepair() == 1 ? 1 : 0);
        workOrderReviewMapper.insert(review);

        String beforeStatus = workOrder.getMainStatus();
        if (review.getIsContinueRepair() == 1) {
            workOrder.setMainStatus(WorkOrderStatusFlow.afterReview(true));
            workOrder.setCompletedTime(null);
            workOrderMapper.updateById(workOrder);
        }
        saveFlow(workOrder.getId(), "REVIEW", beforeStatus, workOrder.getMainStatus(),
                workOrder.getCurrentAcceptCompanyId(), workOrder.getCurrentAcceptCompanyId(),
                workOrder.getCurrentAcceptCompanyId(), dto.getReviewDesc());
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
        workOrder.setSendExpressNo(dto.getSendExpressNo().trim());
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
        validateCloseReturnInfo(dto);
        saveFlow(workOrder.getId(), "RETURN_METHOD", workOrder.getMainStatus(), workOrder.getMainStatus(),
                workOrder.getCurrentAcceptCompanyId(), workOrder.getCurrentAcceptCompanyId(),
                workOrder.getCurrentAcceptCompanyId(), dto.getReturnMethod());
        String beforeStatus = workOrder.getMainStatus();
        workOrder.setReturnMethod(dto.getReturnMethod());
        workOrder.setReturnExpressNo(resolveReturnExpressNo(dto));
        workOrder.setCloseReason(dto.getCloseReason());
        workOrder.setMainStatus(WorkOrderStatusFlow.afterClose());
        workOrder.setEvaluateStatus(WorkOrderStatusFlow.afterCloseEvaluateStatus());
        workOrder.setClosedTime(LocalDateTime.now());
        workOrderMapper.updateById(workOrder);
        saveFlow(workOrder.getId(), "CLOSE", beforeStatus, workOrder.getMainStatus(),
                workOrder.getCurrentAcceptCompanyId(), workOrder.getCurrentAcceptCompanyId(),
                workOrder.getCurrentAcceptCompanyId(), dto.getCloseReason());
        workOrderNotifyEventService.recordEvaluationInvite(workOrder);
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
        if (!workOrderPermissionService.canView(workOrder)) {
            throw new ServiceException("\u65e0\u6743\u67e5\u770b\u8be5\u5de5\u5355");
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

    private void fillListStatus(WorkOrderListVO target) {
        if (target == null) {
            return;
        }
        target.setMainStatusLabel(resolveMainStatusLabel(target.getMainStatus()));
        target.setDisplayStatus(resolveDisplayStatus(target.getMainStatus()));
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
        vo.setScore(evaluation.getScore());
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

    private void validateSendInfo(WorkOrderCreateDTO dto) {
        if (dto == null || !"\u5bc4\u4fee".equals(dto.getServiceMode())) {
            return;
        }
        if (isBlank(dto.getSenderName())) {
            throw new ServiceException("\u5bc4\u4fee\u5de5\u5355\u5fc5\u987b\u586b\u5199\u5bc4\u4ef6\u4eba\u59d3\u540d");
        }
        if (isBlank(dto.getSenderMobile())) {
            throw new ServiceException("\u5bc4\u4fee\u5de5\u5355\u5fc5\u987b\u586b\u5199\u5bc4\u4ef6\u4eba\u624b\u673a\u53f7");
        }
        if (isBlank(dto.getSenderAddress())) {
            throw new ServiceException("\u5bc4\u4fee\u5de5\u5355\u5fc5\u987b\u586b\u5199\u5bc4\u4ef6\u5730\u5740");
        }
    }

    private void validateCloseReturnInfo(WorkOrderCloseDTO dto) {
        if (dto == null || !"\u56de\u5bc4".equals(dto.getReturnMethod())) {
            return;
        }
        if (isBlank(dto.getReturnExpressNo())) {
            throw new ServiceException("\u56de\u5bc4\u65f6\u5fc5\u987b\u586b\u5199\u56de\u5bc4\u5feb\u9012\u5355\u53f7");
        }
    }

    private Long resolveCustomerId(WorkOrderCreateDTO dto) {
        if (dto.getCustomerId() != null) {
            WorkOrderCustomer customer = workOrderCustomerMapper.selectById(dto.getCustomerId());
            if (customer == null) {
                throw new ServiceException("\u5ba2\u6237\u4e0d\u5b58\u5728");
            }
            if (customer.getStatus() != null && customer.getStatus() == 0) {
                throw new ServiceException("\u5ba2\u6237\u5df2\u505c\u7528");
            }
            return customer.getId();
        }
        LambdaQueryWrapper<WorkOrderCustomer> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkOrderCustomer::getPhone, dto.getCustomerMobile())
                .orderByAsc(WorkOrderCustomer::getId);
        List<WorkOrderCustomer> customers = workOrderCustomerMapper.selectList(wrapper);
        WorkOrderCustomer customer = customers.isEmpty() ? null : customers.get(0);
        if (customer != null) {
            if (customer.getStatus() != null && customer.getStatus() == 0) {
                throw new ServiceException("\u5ba2\u6237\u5df2\u505c\u7528");
            }
            if ((customer.getNickname() == null || customer.getNickname().trim().isEmpty())
                    && dto.getCustomerName() != null && !dto.getCustomerName().trim().isEmpty()) {
                customer.setNickname(dto.getCustomerName());
                workOrderCustomerMapper.updateById(customer);
            }
            return customer.getId();
        }
        WorkOrderCustomer newCustomer = new WorkOrderCustomer();
        newCustomer.setOpenid(generateSystemCustomerOpenid());
        newCustomer.setPhone(dto.getCustomerMobile());
        newCustomer.setNickname(dto.getCustomerName());
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
        if (dto == null || !"\u56de\u5bc4".equals(dto.getReturnMethod()) || isBlank(dto.getReturnExpressNo())) {
            return null;
        }
        return dto.getReturnExpressNo().trim();
    }

    private void validateAssignedRepairer(Long userId, Long companyId) {
        if (!listCompanyRepairerUserIds(companyId).contains(userId)) {
            throw new ServiceException("\u6d3e\u5355\u5bf9\u8c61\u5fc5\u987b\u662f\u5f53\u524d\u53d7\u7406\u516c\u53f8\u7684\u7cfb\u7edf\u7ef4\u4fee\u5458");
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
            WorkOrderFault fault = new WorkOrderFault();
            fault.setWorkOrderId(workOrderId);
            fault.setRepairId(repairId);
            fault.setCompanyId(companyId);
            fault.setFaultDesc(item.getFaultDesc());
            fault.setRepairDesc(item.getRepairDesc());
            fault.setPartDesc(item.getPartDesc());
            fault.setImageUrls(item.getImageUrls());
            fault.setSortNum(sort++);
            fault.setCreatedBy(SecurityContext.getCurrentUserId());
            workOrderFaultMapper.insert(fault);
        }
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
