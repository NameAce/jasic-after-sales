package com.jasic.aftersales.system.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jasic.aftersales.common.constant.WorkOrderCreateEntryConstants;
import com.jasic.aftersales.common.constant.WorkOrderReportSubjectConstants;
import com.jasic.aftersales.common.constant.WorkOrderStatusConstants;
import com.jasic.aftersales.common.constant.WorkOrderStatusFlow;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.enums.BrandTypeEnum;
import com.jasic.aftersales.common.enums.ServiceModeEnum;
import com.jasic.aftersales.common.enums.SysFileBizTypeEnum;
import com.jasic.aftersales.common.enums.SysFileUploadUserTypeEnum;
import com.jasic.aftersales.common.enums.WorkOrderActionEnum;
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
import com.jasic.aftersales.system.domain.vo.SysFileItemVO;
import com.jasic.aftersales.system.domain.vo.SysCompanySimpleVO;
import com.jasic.aftersales.system.mapper.FirstSecondRelationMapper;
import com.jasic.aftersales.system.mapper.HqFirstContractMapper;
import com.jasic.aftersales.system.mapper.MachineBarcodeMapper;
import com.jasic.aftersales.system.mapper.SysCompanyMapper;
import com.jasic.aftersales.system.mapper.SysCompanyTypeMapper;
import com.jasic.aftersales.system.mapper.SysMenuMapper;
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
import com.jasic.aftersales.system.service.ISysConfigService;
import com.jasic.aftersales.system.service.IWorkOrderService;
import com.jasic.aftersales.system.service.WorkOrderNotifyEventService;
import com.jasic.aftersales.system.service.WorkOrderParticipantService;
import com.jasic.aftersales.system.service.WorkOrderPermissionService;
import com.jasic.aftersales.system.service.SysFileService;
import com.jasic.aftersales.system.service.support.MachineBarcodeWarrantyResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
    private static final String WORKORDER_ACCEPT_PERMISSION = "workorder:accept";
    private static final List<SysFileBizTypeEnum> WORK_ORDER_FILE_BIZ_TYPES = Arrays.asList(
            SysFileBizTypeEnum.WORK_ORDER_FAULT_IMAGE,
            SysFileBizTypeEnum.WORK_ORDER_FAULT_VIDEO,
            SysFileBizTypeEnum.WORK_ORDER_FAULT_VOICE,
            SysFileBizTypeEnum.WORK_ORDER_SENDER_VOUCHER,
            SysFileBizTypeEnum.WORK_ORDER_RETURN_VOUCHER
    );
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
    private ISysConfigService sysConfigService;

    @Resource
    private SysCompanyMapper sysCompanyMapper;

    @Resource
    private SysCompanyTypeMapper sysCompanyTypeMapper;

    @Resource
    private SysMenuMapper sysMenuMapper;

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

    @Resource
    private SysFileService sysFileService;

    /**
     * 分页查询工单列表，并补齐列表页所需的状态快照字段。
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @Override
    public PageResult<WorkOrderListVO> listPage(WorkOrderQuery query) {
        normalizeQuery(query);
        Page<WorkOrderListVO> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<WorkOrderListVO> result = workOrderMapper.selectWorkOrderPage(page, query);
        List<WorkOrderListVO> records = result.getRecords();
        Map<Long, BigDecimal> currentQuoteAmountMap = buildCurrentValidQuoteAmountMap(
                records.stream().map(WorkOrderListVO::getId).collect(Collectors.toList())
        );
        for (WorkOrderListVO record : records) {
            fillListSnapshot(record, currentQuoteAmountMap);
        }
        return PageResult.of(records, result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 统计当前筛选条件下各主状态的工单数量。
     *
     * @param query 查询参数
     * @return 状态统计结果
     */
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
        fillListSnapshot(detail, entity, buildCurrentValidQuoteAmountMap(Collections.singletonList(workOrderId)));
        detail.setEvaluateStatusLabel(resolveEvaluateStatusLabel(detail.getEvaluateStatus()));
        detail.setBrandTypeLabel(detail.getBrandType() == null ? null : detail.getBrandType().getLabel());
        detail.setServiceModeLabel(ServiceModeEnum.resolveLabel(detail.getServiceMode()));
        fillAttachmentDetail(detail, buildWorkOrderFileMap(workOrderId));
        return detail;
    }

    /**
     * 查询代客建单时条码对应的产品和归属总部信息。
     *
     * @param barcode 机器条码
     * @return 建单条码信息
     */
    @Override
    public WorkOrderCreateBarcodeInfoVO getProxyCreateBarcodeInfo(String barcode) {
        Long currentCompanyId = requireCurrentCompanyId();
        MachineBarcode barcodeArchive = findCreateBarcodeArchive(barcode);
        Long hqCompanyId = barcodeArchive == null
                ? resolveDefaultHqCompanyId()
                : resolveSingleHqCompanyId(currentCompanyId, barcodeArchive);
        return buildCreateBarcodeInfo(barcodeArchive, hqCompanyId, Collections.emptyList(), null);
    }

    @Override
    public List<SysCompanySimpleVO> listUpstreamFirstCreateTargetOptions() {
        Long currentCompanyId = requireCurrentCompanyId();
        validateCurrentCompanyType("SITE_SECOND", "当前公司不支持报修一级");
        return listUpstreamFirstOptions(currentCompanyId, null);
    }

    /**
     * 查询向一级服务网点上报时的条码建单信息。
     *
     * @param barcode 机器条码
     * @return 建单条码信息
     */
    @Override
    public WorkOrderCreateBarcodeInfoVO getUpstreamFirstCreateBarcodeInfo(String barcode) {
        Long currentCompanyId = requireCurrentCompanyId();
        validateCurrentCompanyType("SITE_SECOND", "当前公司不支持报修一级");
        MachineBarcode barcodeArchive = findCreateBarcodeArchive(barcode);
        Long hqCompanyId = barcodeArchive == null
                ? resolveDefaultHqCompanyId()
                : resolveSingleHqCompanyId(currentCompanyId, barcodeArchive);
        List<SysCompanySimpleVO> targetOptions = listUpstreamFirstOptions(currentCompanyId, barcodeArchive == null ? null : hqCompanyId);
        Long defaultTargetCompanyId = resolveDefaultTargetCompanyId(targetOptions);
        return buildCreateBarcodeInfo(barcodeArchive, hqCompanyId, targetOptions, defaultTargetCompanyId);
    }

    /**
     * 查询向佳士总部上报时的条码建单信息。
     *
     * @param barcode 机器条码
     * @param targetCompanyId 指定目标总部ID
     * @return 建单条码信息
     */
    @Override
    public WorkOrderCreateBarcodeInfoVO getUpstreamHqCreateBarcodeInfo(String barcode, Long targetCompanyId) {
        Long currentCompanyId = requireCurrentCompanyId();
        validateCurrentCompanyType("SITE_FIRST", "当前公司不支持报修佳士");
        MachineBarcode barcodeArchive = findCreateBarcodeArchive(barcode);
        List<SysCompanySimpleVO> targetOptions = barcodeArchive == null
                ? Collections.emptyList()
                : listUpstreamHqOptions(currentCompanyId, barcodeArchive);
        Long defaultTargetCompanyId = barcodeArchive == null ? null : resolveDefaultTargetCompanyId(targetOptions);
        Long resolvedHqCompanyId = barcodeArchive == null
                ? resolveDefaultHqCompanyId()
                : targetCompanyId != null
                ? resolveSelectedTargetCompanyId(targetCompanyId, targetOptions, "请选择报修佳士")
                : resolveResolvedHqCompanyId(barcodeArchive, targetOptions);
        if (resolvedHqCompanyId == null) {
            resolvedHqCompanyId = defaultTargetCompanyId;
        }
        return buildCreateBarcodeInfo(barcodeArchive, resolvedHqCompanyId, targetOptions, defaultTargetCompanyId);
    }

    /**
     * 代客创建工单，当前公司同时作为建单方和受理方。
     *
     * @param dto 建单参数
     * @return 工单ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createProxy(WorkOrderProxyCreateDTO dto) {
        Long currentCompanyId = requireCurrentCompanyId();
        MachineBarcode barcodeArchive = findCreateBarcodeArchive(dto.getBarcode());
        Long hqCompanyId = barcodeArchive == null
                ? resolveDefaultHqCompanyId()
                : resolveSingleHqCompanyId(currentCompanyId, barcodeArchive);
        String currentSubjectType = normalizeNullableText(SecurityContext.getCurrentSubjectType());
        if (currentSubjectType == null) {
            currentSubjectType = resolveCompanySubjectType(currentCompanyId);
        }
        CustomerCreateIdentity customerIdentity = resolveProxyCreateCustomerIdentity(dto);
        return saveCreateWorkOrder(
                customerIdentity.getCustomerName(),
                customerIdentity.getCustomerMobile(),
                dto.getBarcode(),
                dto.getServiceMode(),
                dto.getFaultItems(),
                dto.getFaultRemark(),
                dto.getFaultImageFileIds(),
                dto.getFaultVideoFileIds(),
                dto.getFaultVoiceFileIds(),
                dto.getSenderName(),
                dto.getSenderMobile(),
                dto.getSenderAddress(),
                dto.getSendExpressNo(),
                dto.getSenderVoucherFileIds(),
                barcodeArchive,
                hqCompanyId,
                currentCompanyId,
                currentSubjectType,
                WorkOrderCreateEntryConstants.PROXY_SELF
        );
    }

    /**
     * 向一级服务网点转报创建工单。
     *
     * @param dto 建单参数
     * @return 工单ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUpstreamFirst(WorkOrderUpstreamCreateDTO dto) {
        Long currentCompanyId = requireCurrentCompanyId();
        validateCurrentCompanyType("SITE_SECOND", "当前公司不支持报修一级");
        MachineBarcode barcodeArchive = findCreateBarcodeArchive(dto.getBarcode());
        Long hqCompanyId = barcodeArchive == null
                ? resolveDefaultHqCompanyId()
                : resolveSingleHqCompanyId(currentCompanyId, barcodeArchive);
        List<SysCompanySimpleVO> targetOptions = listUpstreamFirstOptions(currentCompanyId, barcodeArchive == null ? null : hqCompanyId);
        Long targetCompanyId = resolveSelectedTargetCompanyId(dto.getTargetCompanyId(), targetOptions, "请选择报修一级");
        CustomerCreateIdentity customerIdentity = resolveUpstreamCreateCustomerIdentity();
        return saveCreateWorkOrder(
                customerIdentity.getCustomerName(),
                customerIdentity.getCustomerMobile(),
                dto.getBarcode(),
                dto.getServiceMode(),
                dto.getFaultItems(),
                dto.getFaultRemark(),
                dto.getFaultImageFileIds(),
                dto.getFaultVideoFileIds(),
                dto.getFaultVoiceFileIds(),
                dto.getSenderName(),
                dto.getSenderMobile(),
                dto.getSenderAddress(),
                dto.getSendExpressNo(),
                dto.getSenderVoucherFileIds(),
                barcodeArchive,
                hqCompanyId,
                targetCompanyId,
                "SERVICE",
                WorkOrderCreateEntryConstants.UPSTREAM_FIRST
        );
    }

    /**
     * 向佳士总部转报创建工单。
     *
     * @param dto 建单参数
     * @return 工单ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUpstreamHq(WorkOrderUpstreamCreateDTO dto) {
        Long currentCompanyId = requireCurrentCompanyId();
        validateCurrentCompanyType("SITE_FIRST", "当前公司不支持报修佳士");
        MachineBarcode barcodeArchive = findCreateBarcodeArchive(dto.getBarcode());
        List<SysCompanySimpleVO> targetOptions = barcodeArchive == null
                ? Collections.emptyList()
                : listUpstreamHqOptions(currentCompanyId, barcodeArchive);
        Long targetCompanyId = barcodeArchive == null
                ? resolveDefaultHqCompanyId()
                : resolveSelectedTargetCompanyId(dto.getTargetCompanyId(), targetOptions, "请选择报修佳士");
        CustomerCreateIdentity customerIdentity = resolveUpstreamCreateCustomerIdentity();
        return saveCreateWorkOrder(
                customerIdentity.getCustomerName(),
                customerIdentity.getCustomerMobile(),
                dto.getBarcode(),
                dto.getServiceMode(),
                dto.getFaultItems(),
                dto.getFaultRemark(),
                dto.getFaultImageFileIds(),
                dto.getFaultVideoFileIds(),
                dto.getFaultVoiceFileIds(),
                dto.getSenderName(),
                dto.getSenderMobile(),
                dto.getSenderAddress(),
                dto.getSendExpressNo(),
                dto.getSenderVoucherFileIds(),
                barcodeArchive,
                targetCompanyId,
                targetCompanyId,
                "HQ",
                WorkOrderCreateEntryConstants.UPSTREAM_HQ
        );
    }

    /**
     * 派单给当前受理公司下具备接单权限的系统用户。
     *
     * @param dto 派单参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assign(WorkOrderAssignDTO dto) {
        WorkOrder workOrder = requireWorkOrder(dto.getWorkOrderId());
        if (!workOrderPermissionService.canAssign(workOrder)) {
            throw new ServiceException("\u5f53\u524d\u5de5\u5355\u4e0d\u5141\u8bb8\u6d3e\u5355");
        }
        validateAssignedUser(dto.getAssignedUserId(), workOrder.getCurrentAcceptCompanyId());
        String beforeStatus = workOrder.getMainStatus();
        workOrder.setAssignedUserId(dto.getAssignedUserId());
        workOrder.setMainStatus(WorkOrderStatusFlow.afterAssign());
        workOrderMapper.updateById(workOrder);
        saveFlow(workOrder.getId(), WorkOrderActionEnum.ASSIGN.getCode(), beforeStatus, workOrder.getMainStatus(),
                workOrder.getCurrentAcceptCompanyId(), workOrder.getCurrentAcceptCompanyId(),
                workOrder.getCurrentAcceptCompanyId(), null);
    }

    /**
     * 维修员接单，把工单推进到维修处理中状态。
     *
     * @param dto 接单参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void techAccept(WorkOrderTechAcceptDTO dto) {
        WorkOrder workOrder = requireWorkOrder(dto.getWorkOrderId());
        if (!workOrderPermissionService.canTechAccept(workOrder)) {
            throw new ServiceException("\u5f53\u524d\u5de5\u5355\u4e0d\u5141\u8bb8\u63a5\u5355");
        }
        String faultJudge = normalizeFaultJudge(dto.getFaultJudge(), "\u6545\u969c\u5224\u5b9a\u4e0d\u80fd\u4e3a\u7a7a");
        String beforeStatus = workOrder.getMainStatus();
        String acceptedStatus = WorkOrderStatusFlow.afterTechAccept();
        WorkOrderQuote quote = replaceCurrentQuote(workOrder, faultJudge, dto.getQuoteAmount(), dto.getQuoteDesc());
        workOrder.setMainStatus(acceptedStatus);
        if (FAULT_JUDGE_NO_FAULT.equals(faultJudge)) {
            String returnMethod = normalizeReturnMethod(dto.getReturnMethod());
            String closeReason = normalizeRequiredText(dto.getCloseReason(), "\u5173\u95ed\u539f\u56e0\u4e0d\u80fd\u4e3a\u7a7a");
            validateCloseReturnInfo(returnMethod, dto.getReturnVoucherFileIds());
            LocalDateTime now = LocalDateTime.now();
            workOrder.setReturnMethod(returnMethod);
            workOrder.setReturnExpressNo(resolveReturnExpressNo(returnMethod, dto.getReturnExpressNo()));
            workOrder.setCloseReason(closeReason);
            workOrder.setMainStatus(WorkOrderStatusFlow.afterClose());
            workOrder.setEvaluateStatus(WorkOrderStatusFlow.afterCloseEvaluateStatus(false));
            workOrder.setCompletedTime(now);
            workOrder.setClosedTime(now);
        }
        workOrderMapper.updateById(workOrder);
        saveFlow(workOrder.getId(), WorkOrderActionEnum.TECH_ACCEPT.getCode(), beforeStatus, acceptedStatus,
                workOrder.getCurrentAcceptCompanyId(), workOrder.getCurrentAcceptCompanyId(),
                workOrder.getCurrentAcceptCompanyId(), null);
        saveFlow(workOrder.getId(), WorkOrderActionEnum.QUOTE.getCode(), acceptedStatus, acceptedStatus,
                workOrder.getCurrentAcceptCompanyId(), workOrder.getCurrentAcceptCompanyId(),
                workOrder.getCurrentAcceptCompanyId(), quote.getQuoteDesc());
        if (FAULT_JUDGE_NO_FAULT.equals(faultJudge)) {
            sysFileService.replaceBizFiles(
                    SysFileBizTypeEnum.WORK_ORDER_RETURN_VOUCHER,
                    workOrder.getId(),
                    dto.getReturnVoucherFileIds(),
                    workOrder.getCurrentAcceptCompanyId(),
                    SecurityContext.getCurrentUserId(),
                    SysFileUploadUserTypeEnum.SYSTEM,
                    null
            );
            saveFlow(workOrder.getId(), WorkOrderActionEnum.RETURN_METHOD.getCode(), acceptedStatus, acceptedStatus,
                    workOrder.getCurrentAcceptCompanyId(), workOrder.getCurrentAcceptCompanyId(),
                    workOrder.getCurrentAcceptCompanyId(), workOrder.getReturnMethod());
            saveFlow(workOrder.getId(), WorkOrderActionEnum.CLOSE.getCode(), acceptedStatus, workOrder.getMainStatus(),
                    workOrder.getCurrentAcceptCompanyId(), workOrder.getCurrentAcceptCompanyId(),
                    workOrder.getCurrentAcceptCompanyId(), workOrder.getCloseReason());
        }
    }

    /**
     * 转单到其它允许接收的公司，并同步参与方关系。
     *
     * @param dto 转单参数
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

        saveFlow(workOrder.getId(), WorkOrderActionEnum.TRANSFER.getCode(), beforeStatus, workOrder.getMainStatus(),
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

        saveFlow(workOrder.getId(), WorkOrderActionEnum.QUOTE.getCode(), workOrder.getMainStatus(), workOrder.getMainStatus(),
                workOrder.getCurrentAcceptCompanyId(), workOrder.getCurrentAcceptCompanyId(),
                workOrder.getCurrentAcceptCompanyId(), quote.getQuoteDesc());
    }

    /**
     * 提交维修登记。
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
        String repairRemark = buildRepairRemark(dto.getFaults());
        WorkOrderRepair repair = new WorkOrderRepair();
        repair.setWorkOrderId(workOrder.getId());
        repair.setCompanyId(workOrder.getCurrentAcceptCompanyId());
        repair.setRepairUserId(SecurityContext.getCurrentUserId());
        LocalDateTime finishedTime = LocalDateTime.now();
        repair.setIsFinished(1);
        repair.setFinishedTime(finishedTime);
        workOrderRepairMapper.insert(repair);
        saveFaults(workOrder.getId(), repair.getId(), workOrder.getCurrentAcceptCompanyId(), dto.getFaults());

        String beforeStatus = workOrder.getMainStatus();
        workOrder.setMainStatus(WorkOrderStatusFlow.afterRepairFinish());
        workOrder.setCompletedTime(finishedTime);
        workOrderMapper.updateById(workOrder);
        saveFlow(workOrder.getId(), WorkOrderActionEnum.REPAIR_FINISH.getCode(), beforeStatus, workOrder.getMainStatus(),
                workOrder.getCurrentAcceptCompanyId(), workOrder.getCurrentAcceptCompanyId(),
                workOrder.getCurrentAcceptCompanyId(), repairRemark);
        workOrderNotifyEventService.recordRepairFinished(workOrder, repairRemark);
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
        saveFlow(workOrder.getId(), WorkOrderActionEnum.REVIEW.getCode(), beforeStatus, workOrder.getMainStatus(),
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
        sysFileService.replaceBizFiles(
                SysFileBizTypeEnum.WORK_ORDER_SENDER_VOUCHER,
                workOrder.getId(),
                dto.getSenderVoucherFileIds(),
                workOrder.getCurrentAcceptCompanyId(),
                SecurityContext.getCurrentUserId(),
                SysFileUploadUserTypeEnum.SYSTEM,
                null
        );
        saveFlow(workOrder.getId(), WorkOrderActionEnum.UPLOAD_SEND_EXPRESS.getCode(), workOrder.getMainStatus(), workOrder.getMainStatus(),
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
        saveFlow(workOrder.getId(), WorkOrderActionEnum.RETURN_METHOD.getCode(), workOrder.getMainStatus(), workOrder.getMainStatus(),
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
        sysFileService.replaceBizFiles(
                SysFileBizTypeEnum.WORK_ORDER_RETURN_VOUCHER,
                workOrder.getId(),
                dto.getReturnVoucherFileIds(),
                workOrder.getCurrentAcceptCompanyId(),
                SecurityContext.getCurrentUserId(),
                SysFileUploadUserTypeEnum.SYSTEM,
                null
        );
        saveFlow(workOrder.getId(), WorkOrderActionEnum.CLOSE.getCode(), beforeStatus, workOrder.getMainStatus(),
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
        Set<Long> userIds = listCompanyAcceptEnabledUserIds(workOrder.getCurrentAcceptCompanyId());
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

    /**
     * 查询当前工单在维修登记时可选择的故障和维修说明配置。
     *
     * @param workOrderId 工单ID
     * @return 故障配置选项
     */
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

    /**
     * 补齐查询权限口径，并给缺省视图范围兜底。
     *
     * @param query 查询参数
     */
    private void normalizeQuery(WorkOrderQuery query) {
        workOrderPermissionService.fillQueryScope(query);
        if (!SecurityContext.isPlatformUser() && query.getCompanyId() == null) {
            throw new ServiceException("\u5f53\u524d\u516c\u53f8\u4e0d\u80fd\u4e3a\u7a7a");
        }
        if (query.getViewScope() == null || query.getViewScope().trim().isEmpty()) {
            query.setViewScope("CURRENT");
        }
    }

    /**
     * 构造用于状态统计的查询副本，避免分页参数和列表展示字段干扰统计结果。
     *
     * @param query 原始查询参数
     * @return 统计查询参数
     */
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
        fillListSnapshot(
                target,
                buildWorkOrderSnapshot(target),
                buildCurrentValidQuoteAmountMap(target == null || target.getId() == null
                        ? Collections.emptyList()
                        : Collections.singletonList(target.getId()))
        );
    }

    private void fillListSnapshot(WorkOrderListVO target, Map<Long, BigDecimal> currentQuoteAmountMap) {
        fillListSnapshot(target, buildWorkOrderSnapshot(target), currentQuoteAmountMap);
    }

    private void fillListSnapshot(WorkOrderListVO target, WorkOrder workOrder, Map<Long, BigDecimal> currentQuoteAmountMap) {
        if (target == null) {
            return;
        }
        target.setMainStatusLabel(resolveMainStatusLabel(target.getMainStatus()));
        target.setDisplayStatus(resolveDisplayStatus(target.getMainStatus()));
        target.setBrandTypeLabel(target.getBrandType() == null ? null : target.getBrandType().getLabel());
        target.setQuoteAmount(currentQuoteAmountMap == null ? null : currentQuoteAmountMap.get(target.getId()));
        if (workOrder == null) {
            return;
        }
    }

    private Map<Long, BigDecimal> buildCurrentValidQuoteAmountMap(List<Long> workOrderIds) {
        if (workOrderIds == null || workOrderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> validIds = workOrderIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());
        if (validIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<WorkOrderQuote> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(WorkOrderQuote::getWorkOrderId, validIds)
                .eq(WorkOrderQuote::getIsCurrentValid, 1)
                .orderByDesc(WorkOrderQuote::getCreateTime)
                .orderByDesc(WorkOrderQuote::getId);
        List<WorkOrderQuote> quotes = workOrderQuoteMapper.selectList(wrapper);
        if (quotes == null || quotes.isEmpty()) {
            return Collections.emptyMap();
        }
        quotes.sort(Comparator
                .comparing(WorkOrderQuote::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(WorkOrderQuote::getId, Comparator.nullsLast(Comparator.reverseOrder())));
        Map<Long, BigDecimal> result = new HashMap<>();
        for (WorkOrderQuote quote : quotes) {
            if (quote == null
                    || quote.getWorkOrderId() == null
                    || !Integer.valueOf(1).equals(quote.getIsCurrentValid())
                    || result.containsKey(quote.getWorkOrderId())) {
                continue;
            }
            result.put(quote.getWorkOrderId(), quote.getQuoteAmount());
        }
        return result;
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
        return WorkOrderActionEnum.resolveLabel(actionType);
    }

    /**
     * 校验工单是否存在，作为后续所有状态流转的统一入口。
     *
     * @param workOrderId 工单ID
     * @return 工单实体
     */
    private WorkOrder requireWorkOrder(Long workOrderId) {
        WorkOrder workOrder = workOrderMapper.selectById(workOrderId);
        if (workOrder == null) {
            throw new ServiceException("\u5de5\u5355\u4e0d\u5b58\u5728");
        }
        return workOrder;
    }

    /**
     * 非平台用户必须具备当前公司上下文，否则无法计算权限和数据范围。
     *
     * @return 当前公司ID
     */
    private Long requireCurrentCompanyId() {
        Long currentCompanyId = SecurityContext.getCurrentCompanyId();
        if (currentCompanyId == null) {
            throw new ServiceException("\u5f53\u524d\u516c\u53f8\u4e0d\u80fd\u4e3a\u7a7a");
        }
        return currentCompanyId;
    }

    /**
     * 校验当前公司类型是否满足业务入口要求。
     *
     * @param expectedTypeCode 期望的公司类型编码
     * @param message 不满足时的提示语
     */
    private void validateCurrentCompanyType(String expectedTypeCode, String message) {
        String currentTypeCode = SecurityContext.getCurrentTypeCode();
        if (!expectedTypeCode.equals(currentTypeCode)) {
            throw new ServiceException(message);
        }
    }

    private Long saveCreateWorkOrder(String customerName, String customerMobile, String barcode, String serviceMode,
                                     List<String> faultItems, String faultRemark, List<Long> faultImageFileIds,
                                     List<Long> faultVideoFileIds, List<Long> faultVoiceFileIds, String senderName,
                                     String senderMobile, String senderAddress, String sendExpressNo,
                                     List<Long> senderVoucherFileIds, MachineBarcode barcodeArchive,
                                     Long hqCompanyId, Long targetCompanyId, String targetSubjectType,
                                     String createEntryType) {
        Long currentCompanyId = requireCurrentCompanyId();
        Long currentUserId = SecurityContext.getCurrentUserId();
        String normalizedCustomerName = normalizeRequiredText(customerName, "\u5ba2\u6237\u59d3\u540d\u4e0d\u80fd\u4e3a\u7a7a");
        String normalizedCustomerMobile = normalizeRequiredText(customerMobile, "\u5ba2\u6237\u624b\u673a\u53f7\u4e0d\u80fd\u4e3a\u7a7a");
        String normalizedBarcode = normalizeNullableText(barcode);
        String normalizedServiceMode = normalizeServiceMode(serviceMode);
        validateCreateSendInfo(normalizedServiceMode, senderName, senderMobile, senderAddress);
        CustomerFaultSelection faultSelection = resolveCreateFaultSelection(
                faultItems, faultRemark, hqCompanyId,
                barcodeArchive == null ? null : barcodeArchive.getProductCode(),
                barcodeArchive == null ? null : barcodeArchive.getProductModel()
        );

        WorkOrder entity = new WorkOrder();
        entity.setOrderNo(generateOrderNo());
        entity.setCustomerId(resolveCreateCustomerId(normalizedCustomerName, normalizedCustomerMobile));
        entity.setCustomerName(normalizedCustomerName);
        entity.setCustomerMobile(normalizedCustomerMobile);
        entity.setReportSubjectType(resolveReportSubjectType(createEntryType));
        entity.setReportCompanyId(resolveReportCompanyId(currentCompanyId, createEntryType));
        entity.setBarcode(normalizedBarcode);
        entity.setProductCode(normalizeNullableText(barcodeArchive == null ? null : barcodeArchive.getProductCode()));
        entity.setProductName(normalizeNullableText(barcodeArchive == null ? null : barcodeArchive.getProductName()));
        entity.setProductModel(normalizeNullableText(barcodeArchive == null ? null : barcodeArchive.getProductModel()));
        entity.setMachineNo(normalizeNullableText(barcodeArchive == null ? null : barcodeArchive.getMachineNo()));
        entity.setBrandType(BrandTypeEnum.JASIC);
        entity.setBrandCode(normalizeNullableText(barcodeArchive == null ? null : barcodeArchive.getBrandCode()));
        entity.setServiceMode(normalizedServiceMode);
        entity.setWarrantyStatus(resolveBarcodeWarrantyStatus(barcodeArchive, null));
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
        replaceWorkOrderCreateFiles(entity.getId(), faultImageFileIds, faultVideoFileIds, faultVoiceFileIds,
                senderVoucherFileIds, currentCompanyId, currentUserId);

        saveFlow(entity.getId(), WorkOrderActionEnum.CREATE.getCode(), null, entity.getMainStatus(), null, targetCompanyId, currentCompanyId, null);
        workOrderParticipantService.initParticipants(entity, resolveCreateCompanySubjectType(currentCompanyId));
        return entity.getId();
    }

    /**
     * 寄修建单必须补齐寄件人信息，到店维修则清空这些字段。
     *
     * @param serviceMode 服务方式
     * @param senderName 寄件人姓名
     * @param senderMobile 寄件人手机号
     * @param senderAddress 寄件地址
     */
    private void validateCreateSendInfo(String serviceMode, String senderName, String senderMobile, String senderAddress) {
        if (!ServiceModeEnum.isMail(serviceMode)) {
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

    /**
     * 关闭工单前校验回寄方式所需的补充信息。
     *
     * @param dto 关单参数
     */
    private void validateCloseReturnInfo(WorkOrderCloseDTO dto) {
        if (dto == null) {
            return;
        }
        validateCloseReturnInfo(dto.getReturnMethod(), dto.getReturnVoucherFileIds());
    }

    private void validateCloseReturnInfo(String returnMethod, List<Long> returnVoucherFileIds) {
        if (!RETURN_METHOD_MAIL.equals(normalizeNullableText(returnMethod))) {
            return;
        }
        if (returnVoucherFileIds == null || returnVoucherFileIds.isEmpty()) {
            throw new ServiceException("\u56de\u5bc4\u65f6\u5fc5\u987b\u4e0a\u4f20\u56de\u5bc4\u51ed\u8bc1");
        }
    }

    /**
     * 维修登记时允许顺带调整报价，但必须先存在一条有效报价记录。
     *
     * @param workOrder 工单实体
     * @param dto 维修参数
     */
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
        saveFlow(workOrder.getId(), WorkOrderActionEnum.QUOTE.getCode(), workOrder.getMainStatus(), workOrder.getMainStatus(),
                workOrder.getCurrentAcceptCompanyId(), workOrder.getCurrentAcceptCompanyId(),
                workOrder.getCurrentAcceptCompanyId(), quote.getQuoteDesc());
    }

    /**
     * 维修登记至少要提交一条故障点明细。
     *
     * @param workOrder 工单实体
     * @param dto 维修参数
     */
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

    private String buildRepairRemark(List<WorkOrderFaultItemDTO> faults) {
        if (faults == null || faults.isEmpty()) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        for (WorkOrderFaultItemDTO fault : faults) {
            if (fault == null || isBlank(fault.getFaultDesc())) {
                continue;
            }
            StringBuilder builder = new StringBuilder(fault.getFaultDesc().trim());
            if (!isBlank(fault.getRepairDesc())) {
                builder.append("：").append(fault.getRepairDesc().trim());
            }
            parts.add(builder.toString());
        }
        if (parts.isEmpty()) {
            return null;
        }
        return String.join("；", parts);
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

    private Long resolveCreateCustomerId(String customerName, String customerMobile) {
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
        return null;
    }

    private String resolveSendField(String serviceMode, String value) {
        if (!ServiceModeEnum.isMail(serviceMode) || isBlank(value)) {
            return null;
        }
        return value.trim();
    }

    private String resolveReturnExpressNo(WorkOrderCloseDTO dto) {
        if (dto == null) {
            return null;
        }
        return resolveReturnExpressNo(dto.getReturnMethod(), dto.getReturnExpressNo());
    }

    private String resolveReturnExpressNo(String returnMethod, String returnExpressNo) {
        if (!RETURN_METHOD_MAIL.equals(normalizeNullableText(returnMethod)) || isBlank(returnExpressNo)) {
            return null;
        }
        return returnExpressNo.trim();
    }

    private String resolveReportSubjectType(String createEntryType) {
        if (WorkOrderCreateEntryConstants.UPSTREAM_FIRST.equals(createEntryType)
                || WorkOrderCreateEntryConstants.UPSTREAM_HQ.equals(createEntryType)) {
            return WorkOrderReportSubjectConstants.COMPANY;
        }
        return WorkOrderReportSubjectConstants.CUSTOMER;
    }

    private Long resolveReportCompanyId(Long currentCompanyId, String createEntryType) {
        if (WorkOrderCreateEntryConstants.UPSTREAM_FIRST.equals(createEntryType)
                || WorkOrderCreateEntryConstants.UPSTREAM_HQ.equals(createEntryType)) {
            return currentCompanyId;
        }
        return null;
    }

    private Map<SysFileBizTypeEnum, List<SysFileItemVO>> buildWorkOrderFileMap(Long workOrderId) {
        if (workOrderId == null) {
            return Collections.emptyMap();
        }
        return sysFileService.listBizFileMap(WORK_ORDER_FILE_BIZ_TYPES, workOrderId);
    }

    private void fillAttachmentDetail(WorkOrderDetailVO detail,
                                      Map<SysFileBizTypeEnum, List<SysFileItemVO>> fileMap) {
        if (detail == null) {
            return;
        }
        Map<SysFileBizTypeEnum, List<SysFileItemVO>> safeFileMap = fileMap == null ? Collections.emptyMap() : fileMap;
        detail.setFaultImageFiles(safeFileMap.getOrDefault(SysFileBizTypeEnum.WORK_ORDER_FAULT_IMAGE, Collections.emptyList()));
        detail.setFaultVideoFiles(safeFileMap.getOrDefault(SysFileBizTypeEnum.WORK_ORDER_FAULT_VIDEO, Collections.emptyList()));
        detail.setFaultVoiceFiles(safeFileMap.getOrDefault(SysFileBizTypeEnum.WORK_ORDER_FAULT_VOICE, Collections.emptyList()));
        detail.setSenderVoucherFiles(safeFileMap.getOrDefault(SysFileBizTypeEnum.WORK_ORDER_SENDER_VOUCHER, Collections.emptyList()));
        detail.setReturnVoucherFiles(safeFileMap.getOrDefault(SysFileBizTypeEnum.WORK_ORDER_RETURN_VOUCHER, Collections.emptyList()));
    }

    private void replaceWorkOrderCreateFiles(Long workOrderId, List<Long> faultImageFileIds, List<Long> faultVideoFileIds,
                                             List<Long> faultVoiceFileIds, List<Long> senderVoucherFileIds,
                                             Long companyId, Long operatorUserId) {
        sysFileService.replaceBizFiles(
                SysFileBizTypeEnum.WORK_ORDER_FAULT_IMAGE,
                workOrderId,
                faultImageFileIds,
                companyId,
                operatorUserId,
                SysFileUploadUserTypeEnum.SYSTEM,
                null
        );
        sysFileService.replaceBizFiles(
                SysFileBizTypeEnum.WORK_ORDER_FAULT_VIDEO,
                workOrderId,
                faultVideoFileIds,
                companyId,
                operatorUserId,
                SysFileUploadUserTypeEnum.SYSTEM,
                null
        );
        sysFileService.replaceBizFiles(
                SysFileBizTypeEnum.WORK_ORDER_FAULT_VOICE,
                workOrderId,
                faultVoiceFileIds,
                companyId,
                operatorUserId,
                SysFileUploadUserTypeEnum.SYSTEM,
                null
        );
        sysFileService.replaceBizFiles(
                SysFileBizTypeEnum.WORK_ORDER_SENDER_VOUCHER,
                workOrderId,
                senderVoucherFileIds,
                companyId,
                operatorUserId,
                SysFileUploadUserTypeEnum.SYSTEM,
                null
        );
    }

    /**
     * 派单对象必须是当前受理公司名下、状态正常且具备接单权限的系统用户。
     *
     * @param userId 派单对象ID
     * @param companyId 当前受理公司ID
     */
    private void validateAssignedUser(Long userId, Long companyId) {
        if (userId == null || !listCompanyAcceptEnabledUserIds(companyId).contains(userId)) {
            throw new ServiceException("\u6d3e\u5355\u5bf9\u8c61\u5fc5\u987b\u662f\u5f53\u524d\u53d7\u7406\u516c\u53f8\u4e0b\u53ef\u63a5\u5355\u7684\u542f\u7528\u7528\u6237");
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new ServiceException("\u6d3e\u5355\u5bf9\u8c61\u5fc5\u987b\u662f\u542f\u7528\u72b6\u6001\u7684\u53ef\u63a5\u5355\u7528\u6237");
        }
    }

    /**
     * 转单前校验目标公司是否允许被当前工单流转到。
     *
     * @param workOrder 工单实体
     * @param targetCompanyId 目标公司ID
     */
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
        vo.setBarcode(normalizeNullableText(barcodeArchive == null ? null : barcodeArchive.getBarcode()));
        vo.setProductCode(normalizeNullableText(barcodeArchive == null ? null : barcodeArchive.getProductCode()));
        vo.setProductName(normalizeNullableText(barcodeArchive == null ? null : barcodeArchive.getProductName()));
        vo.setProductModel(normalizeNullableText(barcodeArchive == null ? null : barcodeArchive.getProductModel()));
        vo.setMachineNo(normalizeNullableText(barcodeArchive == null ? null : barcodeArchive.getMachineNo()));
        vo.setBrandCode(normalizeNullableText(barcodeArchive == null ? null : barcodeArchive.getBrandCode()));
        vo.setWarrantyStatus(resolveBarcodeWarrantyStatus(barcodeArchive, null));
        if (hqCompanyId != null) {
            SysCompany hqCompany = requireActiveHqCompany(hqCompanyId);
            vo.setHqCompanyId(hqCompany.getId());
            vo.setHqCompanyName(hqCompany.getCompanyName());
            vo.setFaultOptions(buildCreateFaultOptions(
                    hqCompany.getId(),
                    barcodeArchive == null ? null : barcodeArchive.getProductCode(),
                    barcodeArchive == null ? null : barcodeArchive.getProductModel()
            ));
            vo.setOtherFaultLabel(OTHER_FAULT_LABEL);
        }
        vo.setTargetCompanyOptions(targetOptions == null ? Collections.emptyList() : targetOptions);
        vo.setDefaultTargetCompanyId(defaultTargetCompanyId);
        return vo;
    }

    /**
     * 条码建单时要求条码档案存在且状态有效。
     *
     * @param barcode 机器条码
     * @return 条码档案
     */
    private MachineBarcode requireActiveMachineBarcode(String barcode) {
        MachineBarcode barcodeArchive = findActiveMachineBarcode(barcode);
        if (barcodeArchive == null) {
            throw new ServiceException("\u5f53\u524d\u6761\u7801\u672a\u7ef4\u62a4\u6863\u6848\u4fe1\u606f");
        }
        return barcodeArchive;
    }

    private MachineBarcode findCreateBarcodeArchive(String barcode) {
        return StrUtil.isBlank(barcode) ? null : requireActiveMachineBarcode(barcode);
    }

    private MachineBarcode findActiveMachineBarcode(String barcode) {
        String normalizedBarcode = normalizeRequiredText(barcode, "\u673a\u5668\u6761\u7801\u4e0d\u80fd\u4e3a\u7a7a");
        LambdaQueryWrapper<MachineBarcode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MachineBarcode::getBarcode, normalizedBarcode)
                .eq(MachineBarcode::getStatus, 1)
                .last("LIMIT 1");
        return machineBarcodeMapper.selectOne(wrapper);
    }

    private String resolveBarcodeWarrantyStatus(MachineBarcode barcodeArchive, String fallbackStatus) {
        String archiveWarrantyStatus = normalizeNullableText(barcodeArchive == null ? null : barcodeArchive.getWarrantyStatus());
        return MachineBarcodeWarrantyResolver.resolveWarrantyStatus(
                barcodeArchive == null ? null : barcodeArchive.getBarcode(),
                barcodeArchive == null ? null : barcodeArchive.getDealerOutDate(),
                barcodeArchive == null ? null : barcodeArchive.getScanDate(),
                archiveWarrantyStatus != null ? archiveWarrantyStatus : normalizeNullableText(fallbackStatus)
        );
    }

    private SysCompany requireActiveHqCompany(Long hqCompanyId) {
        SysCompany company = sysCompanyMapper.selectById(hqCompanyId);
        if (company == null || (company.getStatus() != null && company.getStatus() == 0)) {
            throw new ServiceException("\u5f52\u5c5e\u603b\u90e8\u4e0d\u5b58\u5728");
        }
        if ("SITE_FIRST".equals(company.getTypeCode()) || "SITE_SECOND".equals(company.getTypeCode())) {
            throw new ServiceException("\u5f52\u5c5e\u603b\u90e8\u7c7b\u578b\u4e0d\u6b63\u786e");
        }
        return company;
    }

    private Long resolveDefaultHqCompanyId() {
        String configValue = normalizeNullableText(sysConfigService == null ? null : sysConfigService.getValueByKey("default.hq.company.id"));
        if (configValue == null) {
            throw new ServiceException("默认归属总部未配置");
        }
        Long hqCompanyId;
        try {
            hqCompanyId = Long.valueOf(configValue);
        } catch (NumberFormatException ex) {
            throw new ServiceException("默认归属总部配置错误");
        }
        return requireActiveHqCompany(hqCompanyId).getId();
    }

    private CustomerCreateIdentity resolveProxyCreateCustomerIdentity(WorkOrderProxyCreateDTO dto) {
        String customerMobile = normalizeRequiredText(dto == null ? null : dto.getCustomerMobile(), "客户手机号不能为空");
        String customerName = normalizeNullableText(dto == null ? null : dto.getCustomerName());
        if (customerName == null) {
            customerName = customerMobile;
        }
        return new CustomerCreateIdentity(customerName, customerMobile);
    }

    private CustomerCreateIdentity resolveUpstreamCreateCustomerIdentity() {
        Long currentUserId = SecurityContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new ServiceException("当前登录用户不存在");
        }
        SysUser currentUser = sysUserMapper.selectById(currentUserId);
        if (currentUser == null) {
            throw new ServiceException("当前登录用户不存在");
        }
        String customerMobile = normalizeNullableText(currentUser.getPhone());
        if (customerMobile == null) {
            throw new ServiceException("当前登录账号未维护手机号，无法提交上级报修");
        }
        String customerName = normalizeNullableText(currentUser.getRealName());
        if (customerName == null) {
            customerName = customerMobile;
        }
        return new CustomerCreateIdentity(customerName, customerMobile);
    }

    /**
     * 组合当前公司和条码档案，推导唯一可用的归属总部。
     *
     * @param currentCompanyId 当前公司ID
     * @param barcodeArchive 条码档案
     * @return 归属总部ID
     */
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

    /**
     * 按当前公司角色推导建单时可选的归属总部列表。
     *
     * @param currentCompanyId 当前公司ID
     * @return 可选总部ID列表
     */
    private List<Long> resolveCreateHqCompanyIds(Long currentCompanyId) {
        if (currentCompanyId == null) {
            return Collections.emptyList();
        }
        if ("HQ".equals(SecurityContext.getCurrentSubjectType())) {
            return Collections.singletonList(currentCompanyId);
        }
        String currentTypeCode = requireCompanyTypeCode(currentCompanyId);
        if ("SITE_FIRST".equals(currentTypeCode)) {
            LambdaQueryWrapper<HqFirstContract> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(HqFirstContract::getFirstCompanyId, currentCompanyId)
                    .eq(HqFirstContract::getStatus, 1)
                    .orderByAsc(HqFirstContract::getId);
            return resolveDistinctHqIds(hqFirstContractMapper.selectList(wrapper));
        }
        if ("SITE_SECOND".equals(currentTypeCode)) {
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

    /**
     * 结合条码归属和当前公司角色，计算允许上报佳士的总部范围。
     *
     * @param currentCompanyId 当前公司ID
     * @param barcodeArchive 条码档案
     * @return 总部选项列表
     */
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
        List<String> normalizedFaultItems = normalizeFaultItems(faultItems);
        boolean hasProductScope = hasCreateProductScope(productCode, productModel);
        if (normalizedFaultItems.isEmpty()) {
            if (!hasProductScope) {
                return new CustomerFaultSelection(null, normalizeNullableText(faultRemark));
            }
            throw new ServiceException("\u8bf7\u9009\u62e9\u6545\u969c\u63cf\u8ff0");
        }
        LinkedHashSet<String> allowedFaultOptions = new LinkedHashSet<>();
        if (hasProductScope) {
            List<String> configuredFaultOptions = listConfiguredFaultOptions(hqCompanyId, productCode, productModel);
            if (configuredFaultOptions.isEmpty()) {
                throw new ServiceException("\u5f53\u524d\u4ea7\u54c1\u672a\u914d\u7f6e\u6545\u969c\u9879\uff0c\u4e0d\u80fd\u5efa\u5355\uff0c\u8bf7\u8054\u7cfb\u7ba1\u7406\u5458\u5b8c\u5584\u914d\u7f6e");
            }
            allowedFaultOptions.addAll(configuredFaultOptions);
        }
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

    /**
     * 建单时根据总部和产品配置生成可选故障描述。
     *
     * @param hqCompanyId 归属总部ID
     * @param productCode 产品编码
     * @param productModel 产品型号
     * @return 故障描述选项
     */
    private List<String> buildCreateFaultOptions(Long hqCompanyId, String productCode, String productModel) {
        if (!hasCreateProductScope(productCode, productModel)) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        result.addAll(listConfiguredFaultOptions(hqCompanyId, productCode, productModel));
        if (result.isEmpty()) {
            throw new ServiceException("\u5f53\u524d\u4ea7\u54c1\u672a\u914d\u7f6e\u6545\u969c\u9879\uff0c\u4e0d\u80fd\u5efa\u5355\uff0c\u8bf7\u8054\u7cfb\u7ba1\u7406\u5458\u5b8c\u5584\u914d\u7f6e");
        }
        result.add(OTHER_FAULT_LABEL);
        return new ArrayList<>(result);
    }

    private boolean hasCreateProductScope(String productCode, String productModel) {
        return normalizeNullableText(productCode) != null || normalizeNullableText(productModel) != null;
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

    /**
     * 根据当前受理公司的层级关系推导允许转单的目标公司范围。
     *
     * @param workOrder 工单实体
     * @return 目标公司ID列表
     */
    private List<Long> resolveTransferTargetCompanyIds(WorkOrder workOrder) {
        Long currentCompanyId = workOrder.getCurrentAcceptCompanyId();
        String currentTypeCode = requireCompanyTypeCode(currentCompanyId);
        if ("SITE_SECOND".equals(currentTypeCode)) {
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
        if ("SITE_FIRST".equals(currentTypeCode)) {
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

    /**
     * 统一清洗并校验必填文本，避免前端只传空白字符时绕过校验。
     *
     * @param value 原始文本
     * @param message 为空时提示语
     * @return 规范化后的文本
     */
    private String normalizeRequiredText(String value, String message) {
        String normalized = normalizeNullableText(value);
        if (normalized == null) {
            throw new ServiceException(message);
        }
        return normalized;
    }

    /**
     * 报价故障判定只允许取“有故障/无故障”两个稳定值。
     *
     * @param faultJudge 原始故障判定
     * @param blankMessage 为空时提示语
     * @return 规范化后的故障判定
     */
    private String normalizeFaultJudge(String faultJudge, String blankMessage) {
        String normalized = normalizeRequiredText(faultJudge, blankMessage);
        if (FAULT_JUDGE_HAS_FAULT.equals(normalized) || FAULT_JUDGE_NO_FAULT.equals(normalized)) {
            return normalized;
        }
        throw new ServiceException("\u6545\u969c\u5224\u5b9a\u53ea\u80fd\u4e3a\u6709\u6545\u969c\u6216\u65e0\u6545\u969c");
    }

    /**
     * 服务方式只允许 MAIL / STORE 两个稳定编码。
     *
     * @param serviceMode 原始服务方式编码
     * @return 规范化后的服务方式编码
     */
    private String normalizeServiceMode(String serviceMode) {
        String normalized = normalizeRequiredText(serviceMode, "\u670d\u52a1\u65b9\u5f0f\u4e0d\u80fd\u4e3a\u7a7a");
        if (ServiceModeEnum.getByCode(normalized) != null) {
            return normalized;
        }
        throw new ServiceException("\u670d\u52a1\u65b9\u5f0f\u4ec5\u652f\u6301 MAIL \u6216 STORE");
    }

    private String normalizeNullableText(String value) {
        if (isBlank(value)) {
            return null;
        }
        return value.trim();
    }

    /**
     * 复检结果只允许按“通过/继续维修”两个业务分支流转。
     *
     * @param reviewResult 原始复检结果
     * @return 规范化后的复检结果
     */
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

    /**
     * 机器返还方式仅允许“回寄/自提”两种固定选项。
     *
     * @param returnMethod 原始返回方式
     * @return 规范化后的返回方式
     */
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

    /**
     * 保存维修登记中的故障明细，保留提交顺序作为展示顺序。
     *
     * @param workOrderId 工单ID
     * @param repairId 维修记录ID
     * @param companyId 当前受理公司ID
     * @param faults 故障明细
     */
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

    /**
     * 把维修登记的故障明细规范成可落库格式，并校验故障描述、维修说明是否落在配置范围内。
     *
     * @param workOrder 工单实体
     * @param faults 原始故障明细
     * @return 规范化后的故障明细
     */
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

    /**
     * 构造“故障描述 -> 允许维修说明集合”的映射，用于维修登记校验。
     *
     * @param workOrder 工单实体
     * @return 故障与维修说明映射
     */
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

    /**
     * 校验维修说明去重且仍落在当前故障的允许范围内。
     *
     * @param repairItems 维修说明列表
     * @param allowedOptions 允许的维修说明集合
     */
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
     * 查询当前公司下已启用且具备接单权限的用户ID集合。
     *
     * @param companyId 公司ID
     * @return 用户ID集合
     */
    private Set<Long> listCompanyAcceptEnabledUserIds(Long companyId) {
        Set<Long> companyUserIds = listCompanyUserIds(companyId);
        if (companyUserIds.isEmpty()) {
            return Collections.emptySet();
        }
        List<SysUser> users = sysUserMapper.selectBatchIds(companyUserIds);
        if (users == null || users.isEmpty()) {
            return Collections.emptySet();
        }
        return users.stream()
                .filter(user -> user != null
                        && user.getId() != null
                        && user.getStatus() != null
                        && user.getStatus() == 1)
                .map(SysUser::getId)
                .filter(userId -> hasCompanyPermission(userId, companyId, WORKORDER_ACCEPT_PERMISSION))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean hasCompanyPermission(Long userId, Long companyId, String permission) {
        if (userId == null || companyId == null || StrUtil.isBlank(permission)) {
            return false;
        }
        Set<String> perms = sysMenuMapper.selectPermsByUserIdAndCompanyId(userId, companyId);
        return perms != null && perms.contains(permission);
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

    private static class CustomerCreateIdentity {

        private final String customerName;

        private final String customerMobile;

        private CustomerCreateIdentity(String customerName, String customerMobile) {
            this.customerName = customerName;
            this.customerMobile = customerMobile;
        }

        private String getCustomerName() {
            return customerName;
        }

        private String getCustomerMobile() {
            return customerMobile;
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
