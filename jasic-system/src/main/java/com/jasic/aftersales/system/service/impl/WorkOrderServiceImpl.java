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
import com.jasic.aftersales.common.enums.WorkOrderUserParticipationActionEnum;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.framework.security.SecurityContext;
import com.jasic.aftersales.system.domain.dto.WorkOrderAssignDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderCloseDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderFaultPartItemDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderProxyCreateDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderRepairDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderSendExpressDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderTechAcceptDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderReviewDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderTransferDTO;
import com.jasic.aftersales.system.domain.dto.WorkOrderUpdateProductModelDTO;
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
import com.jasic.aftersales.system.domain.entity.WorkOrderFaultPart;
import com.jasic.aftersales.system.domain.entity.WorkOrderFlow;
import com.jasic.aftersales.system.domain.entity.WorkOrderQuote;
import com.jasic.aftersales.system.domain.entity.WorkOrderRepair;
import com.jasic.aftersales.system.domain.query.WorkOrderHqSiteInternalQuery;
import com.jasic.aftersales.system.domain.query.WorkOrderHqSiteOrderQuery;
import com.jasic.aftersales.system.domain.query.WorkOrderHqSiteSummaryQuery;
import com.jasic.aftersales.system.domain.query.WorkOrderQuery;
import com.jasic.aftersales.system.domain.vo.WorkOrderCreateBarcodeInfoVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderDetailVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderEvaluationVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderFaultPartVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderFaultVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderFlowVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderHqSiteSummaryVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderListVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderQuoteVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderRepairFaultOptionVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderRepairVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderStatusCountVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderUserOptionVO;
import com.jasic.aftersales.system.domain.vo.SysFileItemVO;
import com.jasic.aftersales.system.domain.vo.SysCompanySimpleVO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyAssignedEventDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyEvaluationInviteEventDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyReadByBizDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyTodoCompleteDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyTodoInvalidateDTO;
import com.jasic.aftersales.system.notify.domain.enums.NotifyBizTypeEnum;
import com.jasic.aftersales.system.notify.domain.enums.NotifyInvalidReasonEnum;
import com.jasic.aftersales.system.notify.service.WorkOrderNotifyFacade;
import com.jasic.aftersales.system.notify.support.NotifyConstants;
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
import com.jasic.aftersales.system.mapper.WorkOrderFaultPartMapper;
import com.jasic.aftersales.system.mapper.WorkOrderFlowMapper;
import com.jasic.aftersales.system.mapper.WorkOrderMapper;
import com.jasic.aftersales.system.mapper.WorkOrderCustomerMapper;
import com.jasic.aftersales.system.mapper.WorkOrderQuoteMapper;
import com.jasic.aftersales.system.mapper.WorkOrderRepairMapper;
import com.jasic.aftersales.system.service.IFaultRepairConfigService;
import com.jasic.aftersales.system.service.ISysConfigService;
import com.jasic.aftersales.system.service.IWorkOrderService;
import com.jasic.aftersales.system.service.WorkOrderParticipantService;
import com.jasic.aftersales.system.service.WorkOrderPermissionService;
import com.jasic.aftersales.system.service.WorkOrderUserParticipantService;
import com.jasic.aftersales.system.service.SysFileService;
import com.jasic.aftersales.system.service.support.MachineBarcodeWarrantyResolver;
import com.jasic.aftersales.system.service.support.WorkOrderNoGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    private static final String RETURN_METHOD_MAIL = "回寄";
    private static final String RETURN_METHOD_PICKUP = "自提";
    private static final String FAULT_JUDGE_HAS_FAULT = "有故障";
    private static final String FAULT_JUDGE_NO_FAULT = "无故障";
    private static final String OTHER_REPAIR_OPTION = "其它维修说明";
    private static final String WORKORDER_ACCEPT_PERMISSION = "workorder:accept";
    private static final String REGISTER_STAGE_REPAIR = "REPAIR";
    private static final String REGISTER_STAGE_RECHECK = "RECHECK";
    private static final List<SysFileBizTypeEnum> WORK_ORDER_FILE_BIZ_TYPES = Arrays.asList(
            SysFileBizTypeEnum.WORK_ORDER_FAULT_IMAGE,
            SysFileBizTypeEnum.WORK_ORDER_FAULT_VIDEO,
            SysFileBizTypeEnum.WORK_ORDER_FAULT_VOICE,
            SysFileBizTypeEnum.WORK_ORDER_SENDER_VOUCHER,
            SysFileBizTypeEnum.WORK_ORDER_RETURN_VOUCHER
    );
    private static final List<SysFileBizTypeEnum> WORK_ORDER_REPAIR_FILE_BIZ_TYPES = Arrays.asList(
            SysFileBizTypeEnum.WORK_ORDER_REPAIR_OLD_IMAGE,
            SysFileBizTypeEnum.WORK_ORDER_REPAIR_NEW_IMAGE,
            SysFileBizTypeEnum.WORK_ORDER_REPAIR_MACHINE_IMAGE,
            SysFileBizTypeEnum.WORK_ORDER_REPAIR_BARCODE_IMAGE,
            SysFileBizTypeEnum.WORK_ORDER_REPAIR_OTHER_IMAGE
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
    private WorkOrderFaultPartMapper workOrderFaultPartMapper;

    @Resource
    private WorkOrderEvaluationMapper workOrderEvaluationMapper;

    @Resource
    private WorkOrderPermissionService workOrderPermissionService;

    @Resource
    private WorkOrderParticipantService workOrderParticipantService;

    @Resource
    private WorkOrderUserParticipantService workOrderUserParticipantService;

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

    @Resource
    private WorkOrderNotifyFacade workOrderNotifyFacade;

    @Resource
    private WorkOrderNoGenerator workOrderNoGenerator;

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
        boolean fillActionInfo = "CURRENT".equals(query.getViewScope());
        for (WorkOrderListVO record : records) {
            fillListSnapshot(record, currentQuoteAmountMap, fillActionInfo);
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
        result.add(buildStatusCountVo("ALL", "全部", countMap.values().stream().mapToLong(Long::longValue).sum()));
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
     * 查询总部网点工单汇总。总部工程师的 SELF 数据范围默认不开放该汇总入口。
     *
     * @param query 查询参数
     * @return 网点汇总
     */
    @Override
    public List<WorkOrderHqSiteSummaryVO> listHqSiteSummary(WorkOrderHqSiteSummaryQuery query) {
        WorkOrderHqSiteInternalQuery internalQuery = buildHqSiteSummaryQuery(query);
        normalizeHqSiteQuery(internalQuery);
        if ("SELF".equals(internalQuery.getDataScope())) {
            return Collections.emptyList();
        }
        List<WorkOrderHqSiteSummaryVO> list = workOrderMapper.selectHqSiteSummary(internalQuery);
        if (list == null) {
            return Collections.emptyList();
        }
        for (WorkOrderHqSiteSummaryVO item : list) {
            if (item == null) {
                continue;
            }
            item.setTotalCount(defaultLong(item.getTotalCount()));
            item.setWaitAcceptCount(defaultLong(item.getWaitAcceptCount()));
            item.setInProgressCount(defaultLong(item.getInProgressCount()));
            item.setCompletedCount(defaultLong(item.getCompletedCount()));
        }
        return list;
    }

    /**
     * 分页查询总部网点工单只读列表。列表数据仍复用工单列表快照补齐逻辑。
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @Override
    public PageResult<WorkOrderListVO> listHqSiteOrders(WorkOrderHqSiteOrderQuery query) {
        WorkOrderHqSiteInternalQuery internalQuery = buildHqSiteOrderQuery(query);
        normalizeHqSiteQuery(internalQuery);
        if ("SELF".equals(internalQuery.getDataScope()) || internalQuery.getSiteCompanyId() == null) {
            return PageResult.of(Collections.emptyList(), 0L, internalQuery.getPageNum(), internalQuery.getPageSize());
        }
        Page<WorkOrderListVO> page = new Page<>(internalQuery.getPageNum(), internalQuery.getPageSize());
        IPage<WorkOrderListVO> result = workOrderMapper.selectHqSiteOrderPage(page, internalQuery);
        List<WorkOrderListVO> records = result.getRecords();
        Map<Long, BigDecimal> currentQuoteAmountMap = buildCurrentValidQuoteAmountMap(
                records.stream().map(WorkOrderListVO::getId).collect(Collectors.toList())
        );
        for (WorkOrderListVO record : records) {
            fillListSnapshot(record, currentQuoteAmountMap, false);
        }
        return PageResult.of(records, result.getTotal(), internalQuery.getPageNum(), internalQuery.getPageSize());
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
            throw new ServiceException("工单不存在");
        }
        if (!workOrderPermissionService.canView(entity)) {
            throw new ServiceException("无权查看该工单");
        }
        WorkOrderDetailVO detail = workOrderMapper.selectDetailById(workOrderId);
        if (detail == null) {
            throw new ServiceException("工单详情不存在");
        }
        markWorkOrderTodoRead(workOrderId);
        detail.setParticipants(workOrderMapper.selectParticipantList(workOrderId));
        detail.setQuotes(listQuoteVos(workOrderId));
        detail.setRepairs(listRepairVos(workOrderId));
        detail.setFlows(listFlowVos(workOrderId));
        detail.setEvaluation(getEvaluationVo(workOrderId));
        detail.setAvailableActions(workOrderPermissionService.listAvailableActions(entity));
        fillListSnapshot(detail, entity, buildCurrentValidQuoteAmountMap(Collections.singletonList(workOrderId)), false);
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
            throw new ServiceException("当前工单不允许派单");
        }
        validateAssignedUser(dto.getAssignedUserId(), workOrder.getCurrentAcceptCompanyId());
        Long oldAssignedUserId = workOrder.getAssignedUserId();
        String operationId = IdUtil.fastSimpleUUID();
        String beforeStatus = workOrder.getMainStatus();
        workOrder.setAssignedUserId(dto.getAssignedUserId());
        workOrder.setMainStatus(WorkOrderStatusFlow.afterAssign());
        workOrderMapper.updateById(workOrder);
        saveFlow(workOrder.getId(), WorkOrderActionEnum.ASSIGN.getCode(), beforeStatus, workOrder.getMainStatus(),
                workOrder.getCurrentAcceptCompanyId(), workOrder.getCurrentAcceptCompanyId(),
                workOrder.getCurrentAcceptCompanyId(), null);
        publishAssignedNotifyEvent(workOrder, oldAssignedUserId, dto.getAssignedUserId(), operationId);
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
            throw new ServiceException("当前工单不允许接单");
        }
        String faultJudge = normalizeFaultJudge(dto.getFaultJudge(), "故障判定不能为空");
        String beforeStatus = workOrder.getMainStatus();
        String acceptedStatus = WorkOrderStatusFlow.afterTechAccept();
        LocalDateTime actionTime = LocalDateTime.now();
        WorkOrderQuote quote = replaceCurrentQuote(workOrder, faultJudge, dto.getQuoteAmount(), dto.getQuoteDesc());
        workOrder.setMainStatus(acceptedStatus);
        if (FAULT_JUDGE_NO_FAULT.equals(faultJudge)) {
            String returnMethod = normalizeReturnMethod(dto.getReturnMethod());
            String closeReason = normalizeRequiredText(dto.getCloseReason(), "关闭原因不能为空");
            validateCloseReturnInfo(returnMethod, dto.getReturnVoucherFileIds());
            LocalDateTime now = actionTime;
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
        recordUserParticipation(workOrder.getId(), workOrder.getCurrentAcceptCompanyId(), SecurityContext.getCurrentUserId(),
                WorkOrderUserParticipationActionEnum.TECH_ACCEPT, actionTime);
        recordUserParticipation(workOrder.getId(), workOrder.getCurrentAcceptCompanyId(), SecurityContext.getCurrentUserId(),
                WorkOrderUserParticipationActionEnum.QUOTE, actionTime);
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
        completeWorkOrderTodoByTechAccept(workOrder.getId());
        if (FAULT_JUDGE_NO_FAULT.equals(faultJudge)) {
            invalidateWorkOrderTodo(workOrder.getId(), NotifyInvalidReasonEnum.WORK_ORDER_CLOSED.getCode());
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
            throw new ServiceException("当前工单不允许转单");
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
     * 提交维修登记。
     *
     * @param dto 维修参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRepair(WorkOrderRepairDTO dto) {
        WorkOrder workOrder = requireWorkOrder(dto.getWorkOrderId());
        if (!workOrderPermissionService.canSaveRepair(workOrder)) {
            throw new ServiceException("当前工单不允许登记维修");
        }
        validateRepairProductModelBeforeRegister(workOrder, resolveRegisterStageLabel(REGISTER_STAGE_REPAIR));
        validateRepairConfigBindingBeforeRegister(workOrder);
        RepairFaultSelection faultSelection = resolveRepairFaultSelectionForSaveRepair(workOrder, dto.getFaultItems(), dto.getFaultRemark());
        NormalizedRepairContent repairContent = validateRepairContent(workOrder, faultSelection.getFaultItems(),
                dto.getRepairDesc(), dto.getRepairItems(), dto.getOtherDesc(),
                dto.getPartList(),
                dto.getFaultOldImageFileIds(), dto.getFaultNewImageFileIds(),
                dto.getMachineImageFileIds(), dto.getMachineBarcodeImageFileIds(), dto.getOtherImageFileIds());
        LocalDateTime actionTime = LocalDateTime.now();
        boolean quoteAdjusted = saveRepairQuoteIfNeeded(workOrder, dto);
        WorkOrderRepair repair = createRepairRecord(workOrder, REGISTER_STAGE_REPAIR, actionTime);
        saveFault(repair.getId(), workOrder.getId(), workOrder.getCurrentAcceptCompanyId(), faultSelection,
                repairContent);
        bindRepairFiles(repair.getId(), workOrder.getCurrentAcceptCompanyId(),
                dto.getFaultOldImageFileIds(), dto.getFaultNewImageFileIds(),
                dto.getMachineImageFileIds(), dto.getMachineBarcodeImageFileIds(), dto.getOtherImageFileIds());
        String repairRemark = buildRepairRemark(faultSelection.getFaultDesc(), faultSelection.getFaultRemark(),
                repairContent.getRepairDesc(), repairContent.getRepairItems());

        String beforeStatus = workOrder.getMainStatus();
        workOrder.setMainStatus(WorkOrderStatusFlow.afterRepairFinish());
        workOrder.setCompletedTime(actionTime);
        workOrderMapper.updateById(workOrder);
        saveFlow(workOrder.getId(), WorkOrderActionEnum.REPAIR_FINISH.getCode(), beforeStatus, workOrder.getMainStatus(),
                workOrder.getCurrentAcceptCompanyId(), workOrder.getCurrentAcceptCompanyId(),
                workOrder.getCurrentAcceptCompanyId(), repairRemark);
        if (quoteAdjusted) {
            recordUserParticipation(workOrder.getId(), workOrder.getCurrentAcceptCompanyId(), SecurityContext.getCurrentUserId(),
                    WorkOrderUserParticipationActionEnum.QUOTE, actionTime);
        }
        recordUserParticipation(workOrder.getId(), workOrder.getCurrentAcceptCompanyId(), SecurityContext.getCurrentUserId(),
                WorkOrderUserParticipationActionEnum.REPAIR, actionTime);
        invalidateWorkOrderTodo(workOrder.getId(), NotifyInvalidReasonEnum.WORK_ORDER_COMPLETED.getCode());
    }

    /**
     * 保存复检登记。
     *
     * @param dto 复检参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveReview(WorkOrderReviewDTO dto) {
        WorkOrder workOrder = requireWorkOrder(dto.getWorkOrderId());
        if (!workOrderPermissionService.canReview(workOrder)) {
            throw new ServiceException("当前工单不允许复检");
        }
        validateRepairProductModelBeforeRegister(workOrder, resolveRegisterStageLabel(REGISTER_STAGE_RECHECK));
        validateRepairConfigBindingBeforeRegister(workOrder);
        RepairFaultSelection faultSelection = resolveRepairFaultSelectionForReview(workOrder);
        NormalizedRepairContent repairContent = validateRepairContent(workOrder, faultSelection.getFaultItems(),
                dto.getRepairDesc(), dto.getRepairItems(), dto.getOtherDesc(),
                dto.getPartList(),
                dto.getFaultOldImageFileIds(), dto.getFaultNewImageFileIds(),
                dto.getMachineImageFileIds(), dto.getMachineBarcodeImageFileIds(), dto.getOtherImageFileIds());
        LocalDateTime actionTime = LocalDateTime.now();
        String beforeStatus = workOrder.getMainStatus();
        WorkOrderRepair repair = createRepairRecord(workOrder, REGISTER_STAGE_RECHECK, actionTime);
        saveFault(repair.getId(), workOrder.getId(), workOrder.getCurrentAcceptCompanyId(), faultSelection,
                repairContent);
        bindRepairFiles(repair.getId(), workOrder.getCurrentAcceptCompanyId(),
                dto.getFaultOldImageFileIds(), dto.getFaultNewImageFileIds(),
                dto.getMachineImageFileIds(), dto.getMachineBarcodeImageFileIds(), dto.getOtherImageFileIds());
        String repairRemark = buildRepairRemark(faultSelection.getFaultDesc(), faultSelection.getFaultRemark(),
                repairContent.getRepairDesc(), repairContent.getRepairItems());
        saveFlow(workOrder.getId(), WorkOrderActionEnum.REVIEW.getCode(), beforeStatus, workOrder.getMainStatus(),
                workOrder.getCurrentAcceptCompanyId(), workOrder.getCurrentAcceptCompanyId(),
                workOrder.getCurrentAcceptCompanyId(), repairRemark);
        recordUserParticipation(workOrder.getId(), workOrder.getCurrentAcceptCompanyId(), SecurityContext.getCurrentUserId(),
                WorkOrderUserParticipationActionEnum.REVIEW, actionTime);
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
            throw new ServiceException("当前工单不允许上传寄件快递单号");
        }
        workOrder.setSendExpressNo(normalizeRequiredText(dto.getSendExpressNo(), "寄件快递单号不能为空"));
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
            throw new ServiceException("当前工单不允许关闭");
        }
        String returnMethod = normalizeReturnMethod(dto.getReturnMethod());
        String closeReason = normalizeRequiredText(dto.getCloseReason(), "关闭原因不能为空");
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
        invalidateWorkOrderTodo(workOrder.getId(), NotifyInvalidReasonEnum.WORK_ORDER_CLOSED.getCode());
        publishEvaluationInviteNotifyEvent(workOrder);
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
            throw new ServiceException("当前工单不允许派单");
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
            throw new ServiceException("当前工单不允许转单");
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
     * 查询当前工单在维修/复检登记时可选择的故障和维修说明配置。
     *
     * @param workOrderId 工单ID
     * @return 故障配置选项
     */
    @Override
    public List<WorkOrderRepairFaultOptionVO> listRepairFaultOptions(Long workOrderId) {
        WorkOrder workOrder = requireWorkOrder(workOrderId);
        if (workOrder.getFaultRepairConfigId() == null) {
            return Collections.emptyList();
        }
        return faultRepairConfigService.listRepairFaultOptionsByConfigId(workOrder.getFaultRepairConfigId());
    }

    /**
     * 查询维修/复检前可补录的机器型号选项。
     *
     * @param workOrderId 工单ID
     * @param keyword 机型关键字
     * @return 机型选项
     */
    @Override
    public List<String> listRepairProductModelOptions(Long workOrderId, String keyword) {
        WorkOrder workOrder = requireWorkOrder(workOrderId);
        validateRepairProductModelPreparationAllowed(workOrder);
        ensureRepairProductModelCanBeSupplemented(workOrder);
        return faultRepairConfigService.listEnabledProductModels(workOrder.getHqCompanyId(), keyword);
    }

    /**
     * 补录维修/复检前缺失的机器型号。
     *
     * @param dto 补录参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRepairProductModel(WorkOrderUpdateProductModelDTO dto) {
        WorkOrder workOrder = requireWorkOrder(dto.getWorkOrderId());
        validateRepairProductModelPreparationAllowed(workOrder);
        ensureRepairProductModelCanBeSupplemented(workOrder);
        String productModel = normalizeRequiredText(dto.getProductModel(), "机器型号不能为空");
        List<String> enabledProductModels = faultRepairConfigService.listEnabledProductModels(workOrder.getHqCompanyId(), null);
        if (enabledProductModels.isEmpty()) {
            throw new ServiceException("当前归属总部未配置启用机型，请先维护故障与维修配置");
        }
        boolean matched = enabledProductModels.stream()
                .map(this::normalizeNullableText)
                .anyMatch(item -> StrUtil.equals(item, productModel));
        if (!matched) {
            throw new ServiceException("请选择当前归属总部已启用的机器型号");
        }
        Long faultRepairConfigId = faultRepairConfigService.findEnabledConfigId(
                workOrder.getHqCompanyId(),
                workOrder.getProductCode(),
                productModel
        );
        if (faultRepairConfigId == null) {
            throw new ServiceException("当前总部未配置故障与维修配置，请先维护");
        }
        workOrder.setProductModel(productModel);
        workOrder.setFaultRepairConfigId(faultRepairConfigId);
        workOrderMapper.updateById(workOrder);
    }

    /**
     * 补齐查询权限口径，并给缺省视图范围兜底。
     *
     * @param query 查询参数
     */
    private void normalizeQuery(WorkOrderQuery query) {
        workOrderPermissionService.fillQueryScope(query);
        if (!SecurityContext.isPlatformUser() && query.getCompanyId() == null) {
            throw new ServiceException("当前公司不能为空");
        }
        if (query.getViewScope() == null || query.getViewScope().trim().isEmpty()) {
            query.setViewScope("CURRENT");
        }
    }

    /**
     * 补齐总部网点工单查询权限口径，并限制为总部主体访问。
     *
     * @param query 查询参数
     */
    private void normalizeHqSiteQuery(WorkOrderHqSiteInternalQuery query) {
        workOrderPermissionService.fillQueryScope(query);
        if (query.getCompanyId() == null) {
            throw new ServiceException("当前公司不能为空");
        }
        if (!"HQ".equals(query.getSubjectType())) {
            throw new ServiceException("当前账号不是总部账号");
        }
        if (query.getPageNum() == null || query.getPageNum() < 1) {
            query.setPageNum(1);
        }
        if (query.getPageSize() == null || query.getPageSize() < 1) {
            query.setPageSize(10);
        }
    }

    /**
     * 将前端汇总查询参数转换为内部查询对象。权限上下文字段只从服务端登录态补齐。
     *
     * @param request 前端查询参数
     * @return 内部查询对象
     */
    private WorkOrderHqSiteInternalQuery buildHqSiteSummaryQuery(WorkOrderHqSiteSummaryQuery source) {
        WorkOrderHqSiteInternalQuery query = new WorkOrderHqSiteInternalQuery();
        query.setCompanyId(SecurityContext.getCurrentCompanyId());
        if (source != null) {
            query.setSiteName(source.getSiteName());
        }
        return query;
    }

    /**
     * 将前端明细列表查询参数转换为内部查询对象。权限上下文字段只从服务端登录态补齐。
     *
     * @param source 前端查询参数
     * @return 内部查询对象
     */
    private WorkOrderHqSiteInternalQuery buildHqSiteOrderQuery(WorkOrderHqSiteOrderQuery source) {
        WorkOrderHqSiteInternalQuery query = new WorkOrderHqSiteInternalQuery();
        query.setCompanyId(SecurityContext.getCurrentCompanyId());
        if (source != null) {
            query.setSiteCompanyId(source.getSiteCompanyId());
            query.setDisplayStatus(source.getDisplayStatus());
            query.setOrderNo(source.getOrderNo());
            query.setCustomerName(source.getCustomerName());
            query.setCustomerMobile(source.getCustomerMobile());
            query.setBarcode(source.getBarcode());
            query.setPageNum(source.getPageNum());
            query.setPageSize(source.getPageSize());
        }
        return query;
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
        target.setDisplayStatus(query.getDisplayStatus());
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

    private Long defaultLong(Long value) {
        return value == null ? 0L : value;
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

    private String resolveRegisterStageLabel(String registerStage) {
        if (REGISTER_STAGE_RECHECK.equals(registerStage)) {
            return "复检登记";
        }
        return "维修登记";
    }

    private void fillListSnapshot(WorkOrderListVO target) {
        fillListSnapshot(
                target,
                buildWorkOrderSnapshot(target),
                buildCurrentValidQuoteAmountMap(target == null || target.getId() == null
                        ? Collections.emptyList()
                        : Collections.singletonList(target.getId())),
                true
        );
    }

    private void fillListSnapshot(WorkOrderListVO target, Map<Long, BigDecimal> currentQuoteAmountMap) {
        fillListSnapshot(target, currentQuoteAmountMap, true);
    }

    private void fillListSnapshot(WorkOrderListVO target, Map<Long, BigDecimal> currentQuoteAmountMap,
                                  boolean fillActionInfo) {
        fillListSnapshot(target, buildWorkOrderSnapshot(target), currentQuoteAmountMap, fillActionInfo);
    }

    private void fillListSnapshot(WorkOrderListVO target, WorkOrder workOrder, Map<Long, BigDecimal> currentQuoteAmountMap) {
        fillListSnapshot(target, workOrder, currentQuoteAmountMap, true);
    }

    private void fillListSnapshot(WorkOrderListVO target, WorkOrder workOrder,
                                  Map<Long, BigDecimal> currentQuoteAmountMap, boolean fillActionInfo) {
        if (target == null) {
            return;
        }
        target.setMainStatusLabel(resolveMainStatusLabel(target.getMainStatus()));
        target.setDisplayStatus(resolveDisplayStatus(target.getMainStatus()));
        target.setBrandTypeLabel(target.getBrandType() == null ? null : target.getBrandType().getLabel());
        target.setServiceModeLabel(ServiceModeEnum.resolveLabel(target.getServiceMode()));
        target.setQuoteAmount(currentQuoteAmountMap == null ? null : currentQuoteAmountMap.get(target.getId()));
        if (workOrder == null) {
            return;
        }
        if (fillActionInfo) {
            List<String> availableActions = workOrderPermissionService.listAvailableActions(workOrder);
            target.setAvailableActions(availableActions);
            target.setReadonlyReason(workOrderPermissionService.getReadonlyReason(workOrder, availableActions));
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
            vo.setRegisterStage(repair.getRegisterStage());
            vo.setRegisterStageLabel(resolveRegisterStageLabel(repair.getRegisterStage()));
            vo.setIsFinished(repair.getIsFinished());
            vo.setFinishedTime(repair.getFinishedTime());
            vo.setCreateTime(repair.getCreateTime());
            vo.setFaults(faultMap.getOrDefault(repair.getId(), Collections.emptyList()));
            fillRepairAttachmentDetail(vo, buildRepairFileMap(repair.getId()));
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
        Map<Long, List<WorkOrderFaultPartVO>> partMap = buildFaultPartMap(
                faults.stream().map(WorkOrderFault::getId).collect(Collectors.toCollection(LinkedHashSet::new))
        );
        Map<Long, String> userNameMap = buildUserNameMap(faults.stream().map(WorkOrderFault::getCreatedBy).collect(Collectors.toSet()));
        Map<Long, List<WorkOrderFaultVO>> result = new HashMap<>();
        for (WorkOrderFault fault : faults) {
            WorkOrderFaultVO vo = new WorkOrderFaultVO();
            vo.setId(fault.getId());
            vo.setCompanyId(fault.getCompanyId());
            vo.setFaultDesc(fault.getFaultDesc());
            vo.setFaultRemark(fault.getFaultRemark());
            vo.setRepairDesc(fault.getRepairDesc());
            vo.setOtherDesc(fault.getOtherDesc());
            vo.setPartList(partMap.getOrDefault(fault.getId(), Collections.emptyList()));
            vo.setSortNum(fault.getSortNum());
            vo.setCreatedBy(fault.getCreatedBy());
            vo.setCreatedByName(userNameMap.get(fault.getCreatedBy()));
            vo.setCreateTime(fault.getCreateTime());
            result.computeIfAbsent(fault.getRepairId(), key -> new ArrayList<>()).add(vo);
        }
        return result;
    }

    private Map<Long, List<WorkOrderFaultPartVO>> buildFaultPartMap(Set<Long> faultIds) {
        if (faultIds == null || faultIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<WorkOrderFaultPart> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(WorkOrderFaultPart::getFaultId, faultIds)
                .orderByAsc(WorkOrderFaultPart::getSortNum)
                .orderByAsc(WorkOrderFaultPart::getId);
        List<WorkOrderFaultPart> faultParts = workOrderFaultPartMapper.selectList(wrapper);
        if (faultParts.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, List<WorkOrderFaultPartVO>> result = new HashMap<>();
        for (WorkOrderFaultPart faultPart : faultParts) {
            WorkOrderFaultPartVO vo = new WorkOrderFaultPartVO();
            vo.setId(faultPart.getId());
            vo.setPartName(faultPart.getPartName());
            vo.setPartQty(faultPart.getPartQty());
            vo.setSortNum(faultPart.getSortNum());
            result.computeIfAbsent(faultPart.getFaultId(), key -> new ArrayList<>()).add(vo);
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
            throw new ServiceException("工单不存在");
        }
        return workOrder;
    }

    private void publishAssignedNotifyEvent(WorkOrder workOrder, Long oldAssignedUserId, Long newAssignedUserId, String operationId) {
        String assignType = resolveAssignType(oldAssignedUserId, newAssignedUserId);
        if (assignType == null) {
            return;
        }
        NotifyAssignedEventDTO eventDTO = new NotifyAssignedEventDTO();
        eventDTO.setWorkOrderId(workOrder.getId());
        eventDTO.setOrderNo(workOrder.getOrderNo());
        eventDTO.setOldAssignedUserId(oldAssignedUserId);
        eventDTO.setNewAssignedUserId(newAssignedUserId);
        eventDTO.setOperatorId(SecurityContext.getCurrentUserId());
        eventDTO.setAssignType(assignType);
        eventDTO.setOperationId(operationId);
        workOrderNotifyFacade.publishAssignedEvent(eventDTO);
    }

    private void publishEvaluationInviteNotifyEvent(WorkOrder workOrder) {
        if (workOrder == null) {
            return;
        }
        if (!WorkOrderStatusConstants.EvaluateStatus.PENDING_EVALUATE.equals(workOrder.getEvaluateStatus())) {
            return;
        }
        if (workOrder.getCustomerId() == null) {
            return;
        }
        WorkOrderCustomer customer = workOrderCustomerMapper.selectById(workOrder.getCustomerId());
        SysCompany company = workOrder.getCurrentAcceptCompanyId() == null
                ? null : sysCompanyMapper.selectById(workOrder.getCurrentAcceptCompanyId());
        NotifyEvaluationInviteEventDTO eventDTO = new NotifyEvaluationInviteEventDTO();
        eventDTO.setWorkOrderId(workOrder.getId());
        eventDTO.setOrderNo(workOrder.getOrderNo());
        eventDTO.setCustomerId(workOrder.getCustomerId());
        eventDTO.setCustomerMobile(StrUtil.blankToDefault(
                StrUtil.trim(workOrder.getCustomerMobile()),
                customer == null ? null : StrUtil.trim(customer.getPhone())
        ));
        eventDTO.setCustomerOpenid(customer == null ? null : StrUtil.trim(customer.getOpenid()));
        eventDTO.setCompanyId(workOrder.getCurrentAcceptCompanyId());
        eventDTO.setCompanyName(company == null ? null : StrUtil.trim(company.getCompanyName()));
        eventDTO.setClosedTime(workOrder.getClosedTime());
        workOrderNotifyFacade.publishEvaluationInviteEvent(eventDTO);
    }

    private String resolveAssignType(Long oldAssignedUserId, Long newAssignedUserId) {
        if (newAssignedUserId == null) {
            return null;
        }
        if (oldAssignedUserId == null) {
            return NotifyConstants.ASSIGN_TYPE_ASSIGN;
        }
        if (oldAssignedUserId.equals(newAssignedUserId)) {
            return null;
        }
        return NotifyConstants.ASSIGN_TYPE_TRANSFER;
    }

    private void markWorkOrderTodoRead(Long workOrderId) {
        NotifyReadByBizDTO dto = new NotifyReadByBizDTO();
        dto.setBizType(NotifyBizTypeEnum.WORK_ORDER.getCode());
        dto.setBizId(workOrderId);
        dto.setReceiverId(SecurityContext.getCurrentUserId());
        workOrderNotifyFacade.markReadByBiz(dto);
    }

    private void completeWorkOrderTodoByTechAccept(Long workOrderId) {
        NotifyTodoCompleteDTO dto = new NotifyTodoCompleteDTO();
        dto.setBizType(NotifyBizTypeEnum.WORK_ORDER.getCode());
        dto.setBizId(workOrderId);
        dto.setReceiverId(SecurityContext.getCurrentUserId());
        dto.setActionCode(NotifyConstants.ACTION_TECH_ACCEPT);
        workOrderNotifyFacade.completeTodoByBizAndReceiver(dto);
    }

    private void invalidateWorkOrderTodo(Long workOrderId, String invalidReason) {
        NotifyTodoInvalidateDTO dto = new NotifyTodoInvalidateDTO();
        dto.setBizType(NotifyBizTypeEnum.WORK_ORDER.getCode());
        dto.setBizId(workOrderId);
        dto.setInvalidReason(invalidReason);
        workOrderNotifyFacade.invalidateTodoByBiz(dto);
    }

    /**
     * 维修/复检前补录机型沿用实例级维修/复检权限判断，避免额外放开其它人可改工单基础信息。
     *
     * @param workOrder 工单实体
     */
    private void validateRepairProductModelPreparationAllowed(WorkOrder workOrder) {
        if (workOrderPermissionService.canSaveRepair(workOrder) || workOrderPermissionService.canReview(workOrder)) {
            return;
        }
        throw new ServiceException("当前工单不允许补录机器型号");
    }

    private void ensureRepairProductModelCanBeSupplemented(WorkOrder workOrder) {
        if (requiresRepairProductModelSupplement(workOrder)) {
            return;
        }
        if (normalizeNullableText(workOrder == null ? null : workOrder.getProductModel()) != null) {
            throw new ServiceException("当前工单已存在机器型号，不能重复补录");
        }
        throw new ServiceException("当前工单无需补录机器型号");
    }

    private void validateRepairProductModelBeforeRegister(WorkOrder workOrder, String registerStageLabel) {
        if (!requiresRepairProductModelSupplement(workOrder)) {
            return;
        }
        throw new ServiceException("佳士品牌工单缺少机器型号，请先补录机器型号后再进行" + registerStageLabel);
    }

    private void validateRepairConfigBindingBeforeRegister(WorkOrder workOrder) {
        if (workOrder == null || !BrandTypeEnum.JASIC.equals(workOrder.getBrandType())) {
            return;
        }
        if (workOrder.getFaultRepairConfigId() != null) {
            return;
        }
        throw new ServiceException("当前总部未配置故障与维修配置，请先维护");
    }

    private boolean requiresRepairProductModelSupplement(WorkOrder workOrder) {
        return workOrder != null
                && BrandTypeEnum.JASIC.equals(workOrder.getBrandType())
                && normalizeNullableText(workOrder.getProductModel()) == null;
    }

    /**
     * 非平台用户必须具备当前公司上下文，否则无法计算权限和数据范围。
     *
     * @return 当前公司ID
     */
    private Long requireCurrentCompanyId() {
        Long currentCompanyId = SecurityContext.getCurrentCompanyId();
        if (currentCompanyId == null) {
            throw new ServiceException("当前公司不能为空");
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
        String normalizedCustomerName = normalizeRequiredText(customerName, "客户姓名不能为空");
        String normalizedCustomerMobile = normalizeRequiredText(customerMobile, "客户手机号不能为空");
        String normalizedBarcode = normalizeNullableText(barcode);
        String normalizedServiceMode = normalizeServiceMode(serviceMode);
        validateCreateSendInfo(normalizedServiceMode, senderName, senderMobile, senderAddress);
        CustomerFaultSelection faultSelection = resolveCreateFaultSelection(
                faultItems, faultRemark, hqCompanyId,
                barcodeArchive == null ? null : barcodeArchive.getProductCode(),
                barcodeArchive == null ? null : barcodeArchive.getProductModel()
        );

        WorkOrder entity = new WorkOrder();
        entity.setOrderNo(workOrderNoGenerator.nextOrderNo());
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
        entity.setLastOutDate(resolveBarcodeLastOutDate(barcodeArchive));
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
        entity.setFaultRepairConfigId(resolveCreateFaultRepairConfigId(barcodeArchive, hqCompanyId));
        entity.setHasTransfer(0);
        entity.setTransferCount(0);
        workOrderMapper.insert(entity);
        replaceWorkOrderCreateFiles(entity.getId(), faultImageFileIds, faultVideoFileIds, faultVoiceFileIds,
                senderVoucherFileIds, currentCompanyId, currentUserId);

        saveFlow(entity.getId(), WorkOrderActionEnum.CREATE.getCode(), null, entity.getMainStatus(), null, targetCompanyId, currentCompanyId, null);
        workOrderParticipantService.initParticipants(entity, resolveCreateCompanySubjectType(currentCompanyId));
        return entity.getId();
    }

    private Long resolveCreateFaultRepairConfigId(MachineBarcode barcodeArchive, Long hqCompanyId) {
        if (barcodeArchive == null || hqCompanyId == null || faultRepairConfigService == null) {
            return null;
        }
        return faultRepairConfigService.findEnabledConfigId(
                hqCompanyId,
                barcodeArchive.getProductCode(),
                barcodeArchive.getProductModel()
        );
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
            throw new ServiceException("寄修工单必须填写寄件人姓名");
        }
        if (isBlank(senderMobile)) {
            throw new ServiceException("寄修工单必须填写寄件人手机号");
        }
        if (isBlank(senderAddress)) {
            throw new ServiceException("寄修工单必须填写寄件地址");
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
            throw new ServiceException("回寄时必须上传回寄凭证");
        }
    }

    /**
     * 维修登记时允许顺带调整报价，但必须先存在一条有效报价记录。
     *
     * @param workOrder 工单实体
     * @param dto 维修参数
     */
    private boolean saveRepairQuoteIfNeeded(WorkOrder workOrder, WorkOrderRepairDTO dto) {
        WorkOrderQuote currentQuote = getCurrentValidQuote(workOrder.getId());
        BigDecimal nextQuoteAmount = dto == null ? null : dto.getQuoteAmount();
        String nextQuoteDesc = normalizeNullableText(dto == null ? null : dto.getQuoteDesc());
        if (currentQuote == null) {
            if (nextQuoteAmount != null || nextQuoteDesc != null) {
                throw new ServiceException("请先提交报价，再在维修登记中调整报价");
            }
            return false;
        }
        if (!isQuoteChanged(currentQuote, nextQuoteAmount, nextQuoteDesc)) {
            return false;
        }
        String faultJudge = normalizeFaultJudge(
                currentQuote.getFaultJudge(),
                "当前有效报价的故障判定不能为空"
        );
        WorkOrderQuote quote = replaceCurrentQuote(workOrder, faultJudge, nextQuoteAmount, nextQuoteDesc);
        saveFlow(workOrder.getId(), WorkOrderActionEnum.QUOTE.getCode(), workOrder.getMainStatus(), workOrder.getMainStatus(),
                workOrder.getCurrentAcceptCompanyId(), workOrder.getCurrentAcceptCompanyId(),
                workOrder.getCurrentAcceptCompanyId(), quote.getQuoteDesc());
        return true;
    }

    private NormalizedRepairContent validateRepairContent(WorkOrder workOrder, List<String> faultItems,
                                                          String repairDesc, List<String> repairItems,
                                                          String otherDesc, List<WorkOrderFaultPartItemDTO> partList,
                                                          List<Long> faultOldImageFileIds, List<Long> faultNewImageFileIds,
                                                          List<Long> machineImageFileIds, List<Long> machineBarcodeImageFileIds,
                                                          List<Long> otherImageFileIds) {
        if (workOrder == null) {
            throw new ServiceException("工单不存在");
        }
        if (!hasRepairContent(repairDesc, repairItems, otherDesc, partList,
                faultOldImageFileIds, faultNewImageFileIds, machineImageFileIds, machineBarcodeImageFileIds, otherImageFileIds)) {
            throw new ServiceException("请至少填写一项维修内容");
        }
        validateSingleImageLimit(faultOldImageFileIds, "故障处旧图片");
        validateSingleImageLimit(faultNewImageFileIds, "故障处新图片");
        validateSingleImageLimit(machineImageFileIds, "机器正面照片");
        validateSingleImageLimit(machineBarcodeImageFileIds, "机器条码照片");
        validateSingleImageLimit(otherImageFileIds, "其他图片");
        return normalizeRepairContent(workOrder, faultItems, repairDesc, repairItems, otherDesc, partList);
    }

    private boolean hasRepairContent(String repairDesc, List<String> repairItems, String otherDesc,
                                     List<WorkOrderFaultPartItemDTO> partList, List<Long> faultOldImageFileIds,
                                     List<Long> faultNewImageFileIds,
                                     List<Long> machineImageFileIds, List<Long> machineBarcodeImageFileIds,
                                     List<Long> otherImageFileIds) {
        return !isBlank(repairDesc)
                || (repairItems != null && !repairItems.isEmpty())
                || !isBlank(otherDesc)
                || hasPartContent(partList)
                || hasFileContent(faultOldImageFileIds)
                || hasFileContent(faultNewImageFileIds)
                || hasFileContent(machineImageFileIds)
                || hasFileContent(machineBarcodeImageFileIds)
                || hasFileContent(otherImageFileIds);
    }

    private boolean hasPartContent(List<WorkOrderFaultPartItemDTO> partList) {
        if (partList == null || partList.isEmpty()) {
            return false;
        }
        for (WorkOrderFaultPartItemDTO partItem : partList) {
            if (partItem == null) {
                continue;
            }
            if (!isBlank(partItem.getPartName()) || partItem.getPartQty() != null) {
                return true;
            }
        }
        return false;
    }

    private boolean hasFileContent(List<Long> fileIds) {
        return fileIds != null && fileIds.stream().anyMatch(item -> item != null);
    }

    private void validateSingleImageLimit(List<Long> fileIds, String fieldName) {
        if (fileIds == null) {
            return;
        }
        long count = fileIds.stream().filter(item -> item != null).count();
        if (count > 1) {
            throw new ServiceException(fieldName + "最多只能上传1张");
        }
    }

    private NormalizedRepairContent normalizeRepairContent(WorkOrder workOrder, List<String> faultItems,
                                                           String repairDesc, List<String> repairItems,
                                                           String otherDesc, List<WorkOrderFaultPartItemDTO> partList) {
        List<NormalizedFaultPart> normalizedPartList = normalizeFaultPartList(partList);
        if (normalizedPartList.isEmpty()) {
            throw new ServiceException("请至少填写一条配件明细");
        }
        Map<String, Set<String>> optionMap = buildRepairOptionMap(workOrder);
        Set<String> allowedOptions = buildAllowedRepairOptions(faultItems, optionMap);
        List<String> normalizedRepairItems = normalizeRepairItems(repairItems);
        String normalizedOtherDesc = normalizeNullableText(otherDesc);
        String storedRepairDesc = normalizeNullableText(repairDesc);
        if (!allowedOptions.isEmpty()) {
            if (normalizedRepairItems.isEmpty()) {
                throw new ServiceException("请选择维修说明");
            }
            validateRepairItems(normalizedRepairItems, allowedOptions);
            storedRepairDesc = String.join(FAULT_DESC_SEPARATOR, normalizedRepairItems);
        } else if (!normalizedRepairItems.isEmpty()) {
            validateRepairItems(normalizedRepairItems, Collections.emptySet());
            storedRepairDesc = String.join(FAULT_DESC_SEPARATOR, normalizedRepairItems);
        } else {
            storedRepairDesc = normalizeRequiredText(storedRepairDesc, "维修说明不能为空");
        }
        if (normalizedRepairItems.contains(OTHER_REPAIR_OPTION) && normalizedOtherDesc == null) {
            throw new ServiceException("选择其它维修说明时，其他维修说明不能为空");
        }
        if (!normalizedRepairItems.contains(OTHER_REPAIR_OPTION)) {
            normalizedOtherDesc = null;
        }
        return new NormalizedRepairContent(storedRepairDesc, normalizedRepairItems, normalizedOtherDesc, normalizedPartList);
    }

    private List<NormalizedFaultPart> normalizeFaultPartList(List<WorkOrderFaultPartItemDTO> partList) {
        if (partList == null || partList.isEmpty()) {
            return Collections.emptyList();
        }
        List<NormalizedFaultPart> result = new ArrayList<>();
        for (WorkOrderFaultPartItemDTO partItem : partList) {
            if (partItem == null) {
                continue;
            }
            String normalizedPartName = normalizeNullableText(partItem.getPartName());
            Integer partQty = partItem.getPartQty();
            if (normalizedPartName == null && partQty == null) {
                continue;
            }
            if (normalizedPartName == null) {
                throw new ServiceException("配件名称不能为空");
            }
            if (partQty == null || partQty <= 0) {
                throw new ServiceException("配件数量必须是正整数");
            }
            result.add(new NormalizedFaultPart(normalizedPartName, partQty));
        }
        return result;
    }

    private String buildRepairRemark(String faultDesc, String faultRemark, String repairDesc, List<String> repairItems) {
        String normalizedFaultDesc = normalizeNullableText(faultDesc);
        String normalizedFaultRemark = normalizeNullableText(faultRemark);
        String normalizedRepairDesc = normalizeNullableText(repairDesc);
        if (repairItems != null && !repairItems.isEmpty()) {
            normalizedRepairDesc = String.join(FAULT_DESC_SEPARATOR, normalizeRepairItems(repairItems));
        }
        if (normalizedFaultDesc != null && normalizedFaultRemark != null) {
            normalizedFaultDesc = normalizedFaultDesc + "（" + normalizedFaultRemark + "）";
        }
        if (normalizedFaultDesc == null) {
            return normalizedRepairDesc;
        }
        if (normalizedRepairDesc == null) {
            return normalizedFaultDesc;
        }
        return normalizedFaultDesc + "：" + normalizedRepairDesc;
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

    private void recordUserParticipation(Long workOrderId, Long companyId, Long userId,
                                         WorkOrderUserParticipationActionEnum action, LocalDateTime actionTime) {
        if (workOrderUserParticipantService == null) {
            return;
        }
        workOrderUserParticipantService.recordAction(workOrderId, companyId, userId, action, actionTime);
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

    private Map<SysFileBizTypeEnum, List<SysFileItemVO>> buildRepairFileMap(Long repairId) {
        if (repairId == null) {
            return Collections.emptyMap();
        }
        return sysFileService.listBizFileMap(WORK_ORDER_REPAIR_FILE_BIZ_TYPES, repairId);
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

    private void fillRepairAttachmentDetail(WorkOrderRepairVO repair,
                                            Map<SysFileBizTypeEnum, List<SysFileItemVO>> fileMap) {
        if (repair == null) {
            return;
        }
        Map<SysFileBizTypeEnum, List<SysFileItemVO>> safeFileMap = fileMap == null ? Collections.emptyMap() : fileMap;
        repair.setFaultOldImageFiles(safeFileMap.getOrDefault(SysFileBizTypeEnum.WORK_ORDER_REPAIR_OLD_IMAGE, Collections.emptyList()));
        repair.setFaultNewImageFiles(safeFileMap.getOrDefault(SysFileBizTypeEnum.WORK_ORDER_REPAIR_NEW_IMAGE, Collections.emptyList()));
        repair.setMachineImageFiles(safeFileMap.getOrDefault(SysFileBizTypeEnum.WORK_ORDER_REPAIR_MACHINE_IMAGE, Collections.emptyList()));
        repair.setMachineBarcodeImageFiles(safeFileMap.getOrDefault(SysFileBizTypeEnum.WORK_ORDER_REPAIR_BARCODE_IMAGE, Collections.emptyList()));
        repair.setOtherImageFiles(safeFileMap.getOrDefault(SysFileBizTypeEnum.WORK_ORDER_REPAIR_OTHER_IMAGE, Collections.emptyList()));
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
            throw new ServiceException("派单对象必须是当前受理公司下可接单的启用用户");
        }
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new ServiceException("派单对象必须是启用状态的可接单用户");
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
            throw new ServiceException("目标公司不能为空");
        }
        if (targetCompanyId.equals(workOrder.getCurrentAcceptCompanyId())) {
            throw new ServiceException("目标公司不能和当前受理公司相同");
        }
        List<Long> targetCompanyIds = resolveTransferTargetCompanyIds(workOrder);
        if (!targetCompanyIds.contains(targetCompanyId)) {
            throw new ServiceException("当前工单不允许转到该目标公司");
        }
    }

    private void validateCreateHqCompany(Long currentCompanyId, Long hqCompanyId) {
        List<Long> hqCompanyIds = resolveCreateHqCompanyIds(currentCompanyId);
        if (hqCompanyIds.isEmpty()) {
            throw new ServiceException("当前公司没有可选归属总部");
        }
        if (!hqCompanyIds.contains(hqCompanyId)) {
            throw new ServiceException("当前公司不允许归属到该总部");
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
        vo.setLastOutDate(resolveBarcodeLastOutDate(barcodeArchive));
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
            throw new ServiceException("当前条码未维护档案信息");
        }
        return barcodeArchive;
    }

    private MachineBarcode findCreateBarcodeArchive(String barcode) {
        return StrUtil.isBlank(barcode) ? null : requireActiveMachineBarcode(barcode);
    }

    private MachineBarcode findActiveMachineBarcode(String barcode) {
        String normalizedBarcode = normalizeRequiredText(barcode, "机器条码不能为空");
        LambdaQueryWrapper<MachineBarcode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MachineBarcode::getBarcode, normalizedBarcode)
                .eq(MachineBarcode::getStatus, 1)
                .last("LIMIT 1");
        return machineBarcodeMapper.selectOne(wrapper);
    }

    private String resolveBarcodeWarrantyStatus(MachineBarcode barcodeArchive, String fallbackStatus) {
        return MachineBarcodeWarrantyResolver.resolveWarrantyStatus(
                barcodeArchive == null ? null : barcodeArchive.getBarcode(),
                barcodeArchive == null ? null : barcodeArchive.getLastOutDate(),
                barcodeArchive == null ? null : barcodeArchive.getScanDate(),
                normalizeNullableText(fallbackStatus)
        );
    }

    private LocalDateTime resolveBarcodeLastOutDate(MachineBarcode barcodeArchive) {
        return MachineBarcodeWarrantyResolver.resolveLastOutDate(barcodeArchive);
    }

    private SysCompany requireActiveHqCompany(Long hqCompanyId) {
        SysCompany company = sysCompanyMapper.selectById(hqCompanyId);
        if (company == null || (company.getStatus() != null && company.getStatus() == 0)) {
            throw new ServiceException("归属总部不存在");
        }
        if ("SITE_FIRST".equals(company.getTypeCode()) || "SITE_SECOND".equals(company.getTypeCode())) {
            throw new ServiceException("归属总部类型不正确");
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
            throw new ServiceException("当前条码归属总部暂无法自动识别，请联系管理员完善条码配置");
        }
        if (hqCompanyIds.size() > 1) {
            throw new ServiceException("当前条码归属总部存在多个候选项，请联系管理员完善条码配置");
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
            throw new ServiceException("当前没有可选的上游受理公司");
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
            throw new ServiceException("选择的上游受理公司不在允许范围内");
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
            throw new ServiceException("当前公司未配置可报修的一级公司");
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
            throw new ServiceException("当前条码未匹配到可报修的一级公司");
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
            throw new ServiceException("当前公司未配置可报修的佳士总部");
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
            throw new ServiceException("请选择故障描述");
        }
        LinkedHashSet<String> allowedFaultOptions = new LinkedHashSet<>();
        if (hasProductScope) {
            List<String> configuredFaultOptions = listConfiguredFaultOptions(hqCompanyId, productCode, productModel);
            if (configuredFaultOptions.isEmpty()) {
                throw new ServiceException("当前产品未配置故障项，不能建单，请联系管理员完善配置");
            }
            allowedFaultOptions.addAll(configuredFaultOptions);
        }
        allowedFaultOptions.add(OTHER_FAULT_LABEL);
        for (String faultItem : normalizedFaultItems) {
            if (!allowedFaultOptions.contains(faultItem)) {
                throw new ServiceException("故障描述不在可选范围内");
            }
        }
        String normalizedFaultRemark = normalizeNullableText(faultRemark);
        if (normalizedFaultItems.contains(OTHER_FAULT_LABEL) && normalizedFaultRemark == null) {
            throw new ServiceException("选择其它故障时必须填写故障说明");
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
            throw new ServiceException("当前产品未配置故障项，不能建单，请联系管理员完善配置");
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
            throw new ServiceException("目标公司不存在");
        }
        LambdaQueryWrapper<SysCompanyType> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysCompanyType::getTypeCode, company.getTypeCode());
        SysCompanyType companyType = sysCompanyTypeMapper.selectOne(wrapper);
        if (companyType == null) {
            throw new ServiceException("目标公司类型不存在");
        }
        return companyType.getSubjectType();
    }

    private String requireCompanyTypeCode(Long companyId) {
        SysCompany company = sysCompanyMapper.selectById(companyId);
        if (company == null) {
            throw new ServiceException("当前受理公司不存在");
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
        throw new ServiceException("故障判定只能为有故障或无故障");
    }

    /**
     * 服务方式只允许 MAIL / STORE 两个稳定编码。
     *
     * @param serviceMode 原始服务方式编码
     * @return 规范化后的服务方式编码
     */
    private String normalizeServiceMode(String serviceMode) {
        String normalized = normalizeRequiredText(serviceMode, "服务方式不能为空");
        if (ServiceModeEnum.getByCode(normalized) != null) {
            return normalized;
        }
        throw new ServiceException("服务方式仅支持 MAIL 或 STORE");
    }

    private String normalizeNullableText(String value) {
        if (isBlank(value)) {
            return null;
        }
        return value.trim();
    }

    /**
     * 机器返还方式仅允许“回寄/自提”两种固定选项。
     *
     * @param returnMethod 原始返回方式
     * @return 规范化后的返回方式
     */
    private String normalizeReturnMethod(String returnMethod) {
        String normalized = normalizeRequiredText(returnMethod, "机器返回方式不能为空");
        if (RETURN_METHOD_PICKUP.equals(normalized) || RETURN_METHOD_MAIL.equals(normalized)) {
            return normalized;
        }
        throw new ServiceException("机器返回方式不合法");
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
        snapshot.setServiceMode(target.getServiceMode());
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

    private WorkOrderRepair createRepairRecord(WorkOrder workOrder, String registerStage, LocalDateTime actionTime) {
        WorkOrderRepair repair = new WorkOrderRepair();
        repair.setWorkOrderId(workOrder.getId());
        repair.setCompanyId(workOrder.getCurrentAcceptCompanyId());
        repair.setRepairUserId(SecurityContext.getCurrentUserId());
        repair.setRegisterStage(registerStage);
        repair.setIsFinished(1);
        repair.setFinishedTime(actionTime);
        workOrderRepairMapper.insert(repair);
        return repair;
    }

    private void saveFault(Long repairId, Long workOrderId, Long companyId, RepairFaultSelection faultSelection,
                           NormalizedRepairContent repairContent) {
        if (repairContent == null) {
            throw new ServiceException("维修内容不能为空");
        }
        WorkOrderFault fault = new WorkOrderFault();
        fault.setWorkOrderId(workOrderId);
        fault.setRepairId(repairId);
        fault.setCompanyId(companyId);
        fault.setFaultDesc(normalizeRequiredText(faultSelection.getFaultDesc(), "工单故障描述不能为空"));
        fault.setFaultRemark(faultSelection.getFaultRemark());
        fault.setRepairDesc(repairContent.getRepairDesc());
        fault.setOtherDesc(repairContent.getOtherDesc());
        fault.setSortNum(1);
        fault.setCreatedBy(SecurityContext.getCurrentUserId());
        workOrderFaultMapper.insert(fault);
        saveFaultParts(workOrderId, fault.getId(), companyId, repairContent.getPartList());
    }

    private void saveFaultParts(Long workOrderId, Long faultId, Long companyId, List<NormalizedFaultPart> partList) {
        if (partList == null || partList.isEmpty()) {
            return;
        }
        Long currentUserId = SecurityContext.getCurrentUserId();
        for (int i = 0; i < partList.size(); i++) {
            NormalizedFaultPart partItem = partList.get(i);
            WorkOrderFaultPart faultPart = new WorkOrderFaultPart();
            faultPart.setWorkOrderId(workOrderId);
            faultPart.setFaultId(faultId);
            faultPart.setCompanyId(companyId);
            faultPart.setPartName(partItem.getPartName());
            faultPart.setPartQty(partItem.getPartQty());
            faultPart.setSortNum(i + 1);
            faultPart.setCreatedBy(currentUserId);
            workOrderFaultPartMapper.insert(faultPart);
        }
    }

    private void bindRepairFiles(Long repairId, Long companyId, List<Long> faultOldImageFileIds,
                                 List<Long> faultNewImageFileIds, List<Long> machineImageFileIds,
                                 List<Long> machineBarcodeImageFileIds, List<Long> otherImageFileIds) {
        Long currentUserId = SecurityContext.getCurrentUserId();
        replaceRepairFiles(SysFileBizTypeEnum.WORK_ORDER_REPAIR_OLD_IMAGE, repairId, faultOldImageFileIds, companyId, currentUserId);
        replaceRepairFiles(SysFileBizTypeEnum.WORK_ORDER_REPAIR_NEW_IMAGE, repairId, faultNewImageFileIds, companyId, currentUserId);
        replaceRepairFiles(SysFileBizTypeEnum.WORK_ORDER_REPAIR_MACHINE_IMAGE, repairId, machineImageFileIds, companyId, currentUserId);
        replaceRepairFiles(SysFileBizTypeEnum.WORK_ORDER_REPAIR_BARCODE_IMAGE, repairId, machineBarcodeImageFileIds, companyId, currentUserId);
        replaceRepairFiles(SysFileBizTypeEnum.WORK_ORDER_REPAIR_OTHER_IMAGE, repairId, otherImageFileIds, companyId, currentUserId);
    }

    private void replaceRepairFiles(SysFileBizTypeEnum bizType, Long repairId, List<Long> fileIds,
                                    Long companyId, Long operatorUserId) {
        sysFileService.replaceBizFiles(
                bizType,
                repairId,
                fileIds,
                companyId,
                operatorUserId,
                SysFileUploadUserTypeEnum.SYSTEM,
                null
        );
    }

    /**
     * 构造“故障描述 -> 允许维修说明集合”的映射，用于维修登记校验。
     *
     * @param workOrder 工单实体
     * @return 故障与维修说明映射
     */
    private Map<String, Set<String>> buildRepairOptionMap(WorkOrder workOrder) {
        if (workOrder == null || workOrder.getFaultRepairConfigId() == null) {
            return Collections.emptyMap();
        }
        List<WorkOrderRepairFaultOptionVO> options = faultRepairConfigService.listRepairFaultOptionsByConfigId(
                workOrder.getFaultRepairConfigId()
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

    private RepairFaultSelection resolveRepairFaultSelectionForSaveRepair(WorkOrder workOrder, List<String> faultItems,
                                                                         String faultRemark) {
        Map<String, Set<String>> optionMap = buildRepairOptionMap(workOrder);
        if (optionMap.isEmpty()) {
            return buildFallbackRepairFaultSelection(workOrder);
        }
        List<String> normalizedFaultItems = normalizeFaultItems(faultItems);
        if (normalizedFaultItems.isEmpty()) {
            throw new ServiceException("请选择故障描述");
        }
        Set<String> duplicateCheck = new HashSet<>();
        for (String faultItem : normalizedFaultItems) {
            if (!duplicateCheck.add(faultItem)) {
                throw new ServiceException("故障描述不能重复");
            }
            if (!optionMap.containsKey(faultItem) && !OTHER_FAULT_LABEL.equals(faultItem)) {
                throw new ServiceException("故障描述不在当前配置范围内");
            }
        }
        String normalizedFaultRemark = normalizeNullableText(faultRemark);
        if (normalizedFaultItems.contains(OTHER_FAULT_LABEL) && normalizedFaultRemark == null) {
            throw new ServiceException("选择其它故障时必须填写其它故障说明");
        }
        if (!normalizedFaultItems.contains(OTHER_FAULT_LABEL)) {
            normalizedFaultRemark = null;
        }
        return new RepairFaultSelection(String.join(FAULT_DESC_SEPARATOR, normalizedFaultItems),
                normalizedFaultItems,
                normalizedFaultRemark);
    }

    private RepairFaultSelection resolveRepairFaultSelectionForReview(WorkOrder workOrder) {
        Map<String, Set<String>> optionMap = buildRepairOptionMap(workOrder);
        if (optionMap.isEmpty()) {
            return buildFallbackRepairFaultSelection(workOrder);
        }
        WorkOrderFault firstRepairFault = findFirstRepairFault(workOrder.getId());
        if (firstRepairFault == null) {
            throw new ServiceException("未找到首次维修确认故障，无法提交复检登记");
        }
        List<String> normalizedFaultItems = splitFaultDescSelections(firstRepairFault.getFaultDesc());
        if (normalizedFaultItems.isEmpty()) {
            throw new ServiceException("首次维修确认故障缺失，无法提交复检登记");
        }
        return new RepairFaultSelection(firstRepairFault.getFaultDesc(),
                normalizedFaultItems,
                normalizeNullableText(firstRepairFault.getFaultRemark()));
    }

    private RepairFaultSelection buildFallbackRepairFaultSelection(WorkOrder workOrder) {
        if (workOrder == null) {
            throw new ServiceException("工单不存在");
        }
        String faultDesc = normalizeNullableText(workOrder.getFaultDesc());
        String faultRemark = normalizeNullableText(workOrder.getFaultRemark());
        if (faultDesc == null) {
            faultDesc = OTHER_FAULT_LABEL;
        }
        return new RepairFaultSelection(faultDesc,
                splitFaultDescSelections(faultDesc),
                faultRemark);
    }

    private WorkOrderFault findFirstRepairFault(Long workOrderId) {
        if (workOrderId == null) {
            return null;
        }
        LambdaQueryWrapper<WorkOrderRepair> repairWrapper = new LambdaQueryWrapper<>();
        repairWrapper.eq(WorkOrderRepair::getWorkOrderId, workOrderId)
                .eq(WorkOrderRepair::getRegisterStage, REGISTER_STAGE_REPAIR)
                .orderByAsc(WorkOrderRepair::getCreateTime)
                .orderByAsc(WorkOrderRepair::getId);
        List<WorkOrderRepair> repairs = workOrderRepairMapper.selectList(repairWrapper);
        if (repairs == null || repairs.isEmpty()) {
            return null;
        }
        Long repairId = repairs.get(0).getId();
        if (repairId == null) {
            return null;
        }
        LambdaQueryWrapper<WorkOrderFault> faultWrapper = new LambdaQueryWrapper<>();
        faultWrapper.eq(WorkOrderFault::getWorkOrderId, workOrderId)
                .eq(WorkOrderFault::getRepairId, repairId)
                .orderByAsc(WorkOrderFault::getSortNum)
                .orderByAsc(WorkOrderFault::getId);
        List<WorkOrderFault> faults = workOrderFaultMapper.selectList(faultWrapper);
        return faults == null || faults.isEmpty() ? null : faults.get(0);
    }

    private Set<String> buildAllowedRepairOptions(List<String> faultItems, Map<String, Set<String>> optionMap) {
        if (faultItems == null || faultItems.isEmpty() || optionMap == null || optionMap.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String faultDesc : faultItems) {
            result.addAll(optionMap.getOrDefault(faultDesc, Collections.emptySet()));
        }
        return result;
    }

    private List<String> splitFaultDescSelections(String faultDesc) {
        String normalized = normalizeNullableText(faultDesc);
        if (normalized == null) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (String item : normalized.split(FAULT_DESC_SEPARATOR)) {
            String trimmed = normalizeNullableText(item);
            if (trimmed != null) {
                result.add(trimmed);
            }
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
            throw new ServiceException("维修说明不能为空");
        }
        Set<String> duplicateCheck = new HashSet<>();
        for (String repairItem : repairItems) {
            if (!duplicateCheck.add(repairItem)) {
                throw new ServiceException("维修说明不能重复");
            }
            if (!allowedOptions.isEmpty()
                    && !allowedOptions.contains(repairItem)
                    && !OTHER_REPAIR_OPTION.equals(repairItem)) {
                throw new ServiceException("维修说明不在当前故障配置范围内");
            }
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

    private static class RepairFaultSelection {

        private final String faultDesc;

        private final List<String> faultItems;

        private final String faultRemark;

        private RepairFaultSelection(String faultDesc, List<String> faultItems, String faultRemark) {
            this.faultDesc = faultDesc;
            this.faultItems = faultItems;
            this.faultRemark = faultRemark;
        }

        private String getFaultDesc() {
            return faultDesc;
        }

        private List<String> getFaultItems() {
            return faultItems;
        }

        private String getFaultRemark() {
            return faultRemark;
        }
    }

    private static class NormalizedRepairContent {

        private final String repairDesc;

        private final List<String> repairItems;

        private final String otherDesc;

        private final List<NormalizedFaultPart> partList;

        private NormalizedRepairContent(String repairDesc, List<String> repairItems, String otherDesc,
                                        List<NormalizedFaultPart> partList) {
            this.repairDesc = repairDesc;
            this.repairItems = repairItems;
            this.otherDesc = otherDesc;
            this.partList = partList;
        }

        private String getRepairDesc() {
            return repairDesc;
        }

        private List<String> getRepairItems() {
            return repairItems;
        }

        private String getOtherDesc() {
            return otherDesc;
        }

        private List<NormalizedFaultPart> getPartList() {
            return partList;
        }
    }

    private static class NormalizedFaultPart {

        private final String partName;

        private final Integer partQty;

        private NormalizedFaultPart(String partName, Integer partQty) {
            this.partName = partName;
            this.partQty = partQty;
        }

        private String getPartName() {
            return partName;
        }

        private Integer getPartQty() {
            return partQty;
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

}
