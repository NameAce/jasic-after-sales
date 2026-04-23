package com.jasic.aftersales.customer.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jasic.aftersales.common.constant.WorkOrderConfigConstants;
import com.jasic.aftersales.common.constant.WorkOrderReportSubjectConstants;
import com.jasic.aftersales.common.constant.WorkOrderStatusConstants;
import com.jasic.aftersales.common.constant.WorkOrderStatusFlow;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.enums.BrandTypeEnum;
import com.jasic.aftersales.common.enums.CompanyCategoryEnum;
import com.jasic.aftersales.common.enums.ServiceModeEnum;
import com.jasic.aftersales.common.enums.SysFileBizTypeEnum;
import com.jasic.aftersales.common.enums.SysFileStatusEnum;
import com.jasic.aftersales.common.enums.SysFileUploadUserTypeEnum;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.customer.domain.dto.CustomerWorkOrderCreateDTO;
import com.jasic.aftersales.customer.domain.dto.CustomerWorkOrderEvaluateDTO;
import com.jasic.aftersales.customer.domain.dto.CustomerWorkOrderSendInfoDTO;
import com.jasic.aftersales.customer.domain.dto.CustomerWorkOrderSenderVoucherDTO;
import com.jasic.aftersales.customer.domain.entity.CUser;
import com.jasic.aftersales.customer.domain.query.CustomerWorkOrderQuery;
import com.jasic.aftersales.customer.domain.vo.CustomerBarcodeInfoVO;
import com.jasic.aftersales.customer.domain.vo.CustomerNearbyServiceCompanyVO;
import com.jasic.aftersales.customer.domain.vo.CustomerServiceCompanyOptionVO;
import com.jasic.aftersales.customer.domain.vo.CustomerWorkOrderDetailVO;
import com.jasic.aftersales.customer.domain.vo.CustomerWorkOrderLatestSummaryVO;
import com.jasic.aftersales.customer.domain.vo.CustomerWorkOrderListVO;
import com.jasic.aftersales.customer.domain.vo.CustomerWorkOrderStatusCountVO;
import com.jasic.aftersales.customer.mapper.CUserMapper;
import com.jasic.aftersales.customer.service.ICustomerWorkOrderService;
import com.jasic.aftersales.framework.security.StpCustomerUtil;
import com.jasic.aftersales.system.domain.entity.FirstSecondRelation;
import com.jasic.aftersales.system.domain.entity.HqFirstContract;
import com.jasic.aftersales.system.domain.entity.MachineBarcode;
import com.jasic.aftersales.system.domain.entity.SysCompany;
import com.jasic.aftersales.system.domain.entity.SysFile;
import com.jasic.aftersales.system.domain.entity.SysFileBiz;
import com.jasic.aftersales.system.domain.entity.SysUser;
import com.jasic.aftersales.system.domain.entity.WorkOrder;
import com.jasic.aftersales.system.domain.entity.WorkOrderEvaluation;
import com.jasic.aftersales.system.domain.entity.WorkOrderFault;
import com.jasic.aftersales.system.domain.entity.WorkOrderFaultPart;
import com.jasic.aftersales.system.domain.entity.WorkOrderFlow;
import com.jasic.aftersales.system.domain.entity.WorkOrderQuote;
import com.jasic.aftersales.system.domain.entity.WorkOrderRepair;
import com.jasic.aftersales.system.domain.vo.WorkOrderCompanyRepairHistoryStatVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderEvaluationVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderFaultPartVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderFaultVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderQuoteVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderRepairFaultOptionVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderRepairVO;
import com.jasic.aftersales.system.domain.vo.SysFileItemVO;
import com.jasic.aftersales.system.mapper.FirstSecondRelationMapper;
import com.jasic.aftersales.system.mapper.HqFirstContractMapper;
import com.jasic.aftersales.system.mapper.MachineBarcodeMapper;
import com.jasic.aftersales.system.mapper.SysCompanyMapper;
import com.jasic.aftersales.system.mapper.SysFileBizMapper;
import com.jasic.aftersales.system.mapper.SysFileMapper;
import com.jasic.aftersales.system.service.ISysConfigService;
import com.jasic.aftersales.system.mapper.SysUserMapper;
import com.jasic.aftersales.system.mapper.WorkOrderEvaluationMapper;
import com.jasic.aftersales.system.mapper.WorkOrderFaultMapper;
import com.jasic.aftersales.system.mapper.WorkOrderFaultPartMapper;
import com.jasic.aftersales.system.mapper.WorkOrderFlowMapper;
import com.jasic.aftersales.system.mapper.WorkOrderMapper;
import com.jasic.aftersales.system.mapper.WorkOrderQuoteMapper;
import com.jasic.aftersales.system.mapper.WorkOrderRepairMapper;
import com.jasic.aftersales.system.service.IFaultRepairConfigService;
import com.jasic.aftersales.system.service.WorkOrderParticipantService;
import com.jasic.aftersales.system.service.SysFileService;
import com.jasic.aftersales.system.service.support.MachineBarcodeWarrantyResolver;
import com.jasic.aftersales.system.service.support.WorkOrderNoGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * C端工单 Service 实现
 *
 * @author Codex
 * @date 2026/03/26
 */
@Service
public class CustomerWorkOrderServiceImpl implements ICustomerWorkOrderService {

    private static final int DEFAULT_NEARBY_LIMIT = 20;
    private static final int MAX_NEARBY_LIMIT = 50;
    private static final String GEOCODE_STATUS_SUCCESS = "SUCCESS";
    private static final BigDecimal MIN_LONGITUDE = BigDecimal.valueOf(-180);
    private static final BigDecimal MAX_LONGITUDE = BigDecimal.valueOf(180);
    private static final BigDecimal MIN_LATITUDE = BigDecimal.valueOf(-90);
    private static final BigDecimal MAX_LATITUDE = BigDecimal.valueOf(90);
    private static final String FAULT_JUDGE_NO_FAULT = "无故障";
    private static final String REGISTER_STAGE_RECHECK = "RECHECK";
    private static final double EARTH_RADIUS_KM = 6371.0088D;
    private static final String OTHER_FAULT_LABEL = "其它故障";
    private static final String FAULT_DESC_SEPARATOR = "；";
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

    @Resource
    private WorkOrderMapper workOrderMapper;

    @Resource
    private CUserMapper cUserMapper;

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
    private WorkOrderFlowMapper workOrderFlowMapper;

    @Resource
    private SysCompanyMapper sysCompanyMapper;

    @Resource
    private SysFileBizMapper sysFileBizMapper;

    @Resource
    private SysFileMapper sysFileMapper;

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private WorkOrderParticipantService workOrderParticipantService;

    @Resource
    private HqFirstContractMapper hqFirstContractMapper;

    @Resource
    private FirstSecondRelationMapper firstSecondRelationMapper;

    @Resource
    private MachineBarcodeMapper machineBarcodeMapper;

    @Resource
    private IFaultRepairConfigService faultRepairConfigService;

    @Resource
    private ISysConfigService sysConfigService;

    @Resource
    private SysFileService sysFileService;

    @Resource
    private WorkOrderNoGenerator workOrderNoGenerator;

    /**
     * 创建我的工单
     *
     * @param dto 建单参数
     * @return 工单ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CustomerWorkOrderCreateDTO dto) {
        Long customerId = requireCustomerId();
        CUser customer = requireCustomer(customerId);
        BrandTypeEnum brandType = dto == null ? null : dto.getBrandType();
        boolean hasBarcode = normalizeText(dto == null ? null : dto.getBarcode()) != null;
        boolean jasicBarcodeCreate = brandType != null && brandType.isJasic() && hasBarcode;
        validateCreateRequest(dto, brandType, hasBarcode);
        SysCompany serviceCompany = requireServiceCompany(dto.getServiceCompanyId());
        String barcode = jasicBarcodeCreate ? normalizeRequiredText(dto.getBarcode(), "机器条码不能为空") : null;
        MachineBarcode barcodeArchive = jasicBarcodeCreate ? findActiveMachineBarcode(barcode) : null;
        Long hqCompanyId = resolveCreateHqCompanyId(brandType, hasBarcode, serviceCompany, barcodeArchive);
        String productCode = jasicBarcodeCreate
                ? resolveArchiveText(barcodeArchive == null ? null : barcodeArchive.getProductCode(), dto.getProductCode())
                : null;
        String productModel = jasicBarcodeCreate
                ? resolveArchiveText(barcodeArchive == null ? null : barcodeArchive.getProductModel(), dto.getProductModel())
                : normalizeText(dto.getProductModel());
        CustomerFaultSelection faultSelection = resolveCustomerFaultSelection(dto, brandType, hasBarcode, hqCompanyId, productCode, productModel);

        WorkOrder workOrder = new WorkOrder();
        workOrder.setOrderNo(workOrderNoGenerator.nextOrderNo());
        workOrder.setCustomerId(customerId);
        workOrder.setCustomerName(resolveCustomerName(customer));
        workOrder.setCustomerMobile(normalizeRequiredText(customer.getPhone(), "当前客户手机号不能为空"));
        workOrder.setReportSubjectType(WorkOrderReportSubjectConstants.CUSTOMER);
        workOrder.setReportCompanyId(null);
        workOrder.setBarcode(barcode);
        workOrder.setProductCode(productCode);
        workOrder.setProductName(jasicBarcodeCreate ? normalizeText(barcodeArchive == null ? null : barcodeArchive.getProductName()) : null);
        workOrder.setProductModel(productModel);
        workOrder.setMachineNo(jasicBarcodeCreate ? normalizeText(barcodeArchive == null ? null : barcodeArchive.getMachineNo()) : null);
        workOrder.setBrandType(brandType);
        workOrder.setBrandCode(resolveCreateBrandCode(brandType, jasicBarcodeCreate, barcodeArchive, dto));
        workOrder.setBrandName(resolveCreateBrandName(brandType, dto));
        String serviceMode = normalizeServiceMode(dto.getServiceMode());
        workOrder.setServiceMode(serviceMode);
        workOrder.setWarrantyStatus(jasicBarcodeCreate
                ? resolveBarcodeWarrantyStatus(barcodeArchive, dto.getWarrantyStatus())
                : normalizeText(dto.getWarrantyStatus()));
        workOrder.setFaultDesc(faultSelection.getFaultDesc());
        workOrder.setFaultRemark(faultSelection.getFaultRemark());
        workOrder.setSenderName(resolveSendField(serviceMode, dto.getSenderName()));
        workOrder.setSenderMobile(resolveSendField(serviceMode, dto.getSenderMobile()));
        workOrder.setSenderAddress(resolveSendField(serviceMode, dto.getSenderAddress()));
        workOrder.setSendExpressNo(resolveSendField(serviceMode, dto.getSendExpressNo()));
        workOrder.setMainStatus(WorkOrderStatusFlow.afterCreate());
        workOrder.setEvaluateStatus(WorkOrderStatusFlow.afterCreateEvaluateStatus());
        workOrder.setCurrentAcceptSubjectType("SERVICE");
        workOrder.setCurrentAcceptCompanyId(serviceCompany.getId());
        workOrder.setCreateCompanyId(serviceCompany.getId());
        workOrder.setHqCompanyId(hqCompanyId);
        workOrder.setFaultRepairConfigId(resolveCreateFaultRepairConfigId(barcodeArchive, hqCompanyId));
        workOrder.setHasTransfer(0);
        workOrder.setTransferCount(0);
        workOrderMapper.insert(workOrder);
        replaceWorkOrderCreateFiles(workOrder.getId(), dto.getFaultImageFileIds(), dto.getFaultVideoFileIds(),
                dto.getFaultVoiceFileIds(), dto.getSenderVoucherFileIds(), customerId);

        saveCreateFlow(workOrder.getId(), customerId, serviceCompany.getId(), workOrder.getMainStatus());
        workOrderParticipantService.initParticipants(workOrder, "SERVICE");
        return workOrder.getId();
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
     * 查询 C 端可选服务网点列表
     *
     * @return 服务网点选项
     */
    @Override
    public List<CustomerServiceCompanyOptionVO> listServiceCompanyOptions() {
        Set<String> typeCodes = new LinkedHashSet<>();
        typeCodes.addAll(CompanyCategoryEnum.getFirstLevelTypeCodes());
        typeCodes.addAll(CompanyCategoryEnum.getSecondLevelTypeCodes());
        if (typeCodes.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<SysCompany> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysCompany::getTypeCode, typeCodes)
                .eq(SysCompany::getStatus, 1)
                .orderByAsc(SysCompany::getCompanyName)
                .orderByAsc(SysCompany::getId);
        List<SysCompany> companies = sysCompanyMapper.selectList(wrapper);
        if (companies.isEmpty()) {
            return Collections.emptyList();
        }
        List<CustomerServiceCompanyOptionVO> result = new ArrayList<>(companies.size());
        for (SysCompany company : companies) {
            result.add(buildServiceCompanyOption(company));
        }
        return result;
    }

    /**
     * 按定位查询附近服务网点
     *
     * @param longitude 经度
     * @param latitude  纬度
     * @param limit     返回条数
     * @return 服务网点选项
     */
    @Override
    public List<CustomerNearbyServiceCompanyVO> listNearbyServiceCompanyOptions(BigDecimal longitude, BigDecimal latitude,
                                                                                Integer limit) {
        validateCoordinate(longitude, latitude);
        Long customerId = requireCustomerId();
        int normalizedLimit = normalizeNearbyLimit(limit);
        List<SysCompany> companies = listNearbyEnabledServiceCompanies();
        if (companies.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, RepairHistorySummary> repairHistoryMap = buildRepairHistoryMap(customerId, companies);
        List<CustomerNearbyServiceCompanyVO> options = companies.stream()
                .map(company -> buildNearbyServiceCompanyOption(company, longitude, latitude))
                .peek(option -> option.setHasRepairHistory(repairHistoryMap.containsKey(option.getId())))
                .sorted((left, right) -> compareNearbyServiceCompany(left, right, repairHistoryMap))
                .limit(normalizedLimit)
                .collect(Collectors.toList());
        return options;
    }

    /**
     * 查询条码档案信息
     *
     * @param barcode 机器条码
     * @return 条码信息
     */
    @Override
    public CustomerBarcodeInfoVO getBarcodeInfo(String barcode) {
        String normalizedBarcode = normalizeRequiredText(barcode, "机器条码不能为空");
        MachineBarcode barcodeArchive = requireActiveMachineBarcode(normalizedBarcode);
        SysCompany hqCompany = requireHqCompany(barcodeArchive.getHqCompanyId());
        CustomerBarcodeInfoVO vo = new CustomerBarcodeInfoVO();
        vo.setBarcode(barcodeArchive.getBarcode());
        vo.setProductCode(normalizeText(barcodeArchive.getProductCode()));
        vo.setProductName(normalizeText(barcodeArchive.getProductName()));
        vo.setProductModel(normalizeText(barcodeArchive.getProductModel()));
        vo.setMachineNo(normalizeText(barcodeArchive.getMachineNo()));
        vo.setBrandCode(resolveBrandCode(barcodeArchive.getBrandCode()));
        vo.setWarrantyStatus(resolveBarcodeWarrantyStatus(barcodeArchive, null));
        vo.setHqCompanyId(hqCompany.getId());
        vo.setHqCompanyName(hqCompany.getCompanyName());
        vo.setFaultOptions(buildCustomerFaultOptions(
                hqCompany.getId(),
                barcodeArchive.getProductCode(),
                barcodeArchive.getProductModel()
        ));
        vo.setOtherFaultLabel(OTHER_FAULT_LABEL);
        return vo;
    }

    /**
     * 分页查询我的工单
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @Override
    public PageResult<CustomerWorkOrderListVO> listPage(CustomerWorkOrderQuery query) {
        Long customerId = requireCustomerId();
        Page<WorkOrder> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<WorkOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkOrder::getCustomerId, customerId)
                .orderByDesc(WorkOrder::getCreateTime);
        applyTabStatusFilter(wrapper, query.getTabStatus());
        Page<WorkOrder> result = workOrderMapper.selectPage(page, wrapper);
        List<WorkOrder> records = result.getRecords();
        if (records.isEmpty()) {
            return PageResult.of(Collections.emptyList(), result.getTotal(), query.getPageNum(), query.getPageSize());
        }
        Map<Long, SysCompany> companyMap = buildCompanyMap(
                records.stream().map(WorkOrder::getCurrentAcceptCompanyId).collect(Collectors.toSet())
        );
        Map<Long, String> userNameMap = buildUserNameMap(
                records.stream().map(WorkOrder::getAssignedUserId).collect(Collectors.toSet())
        );
        Set<Long> senderVoucherWorkOrderIds = buildSenderVoucherWorkOrderIdSet(
                records.stream().map(WorkOrder::getId).collect(Collectors.toSet())
        );
        Map<Long, BigDecimal> currentQuoteAmountMap = buildCurrentValidQuoteAmountMap(
                records.stream().map(WorkOrder::getId).collect(Collectors.toList())
        );
        List<CustomerWorkOrderListVO> list = records.stream()
                .map(workOrder -> buildListVo(workOrder, companyMap, userNameMap,
                        senderVoucherWorkOrderIds, currentQuoteAmountMap))
                .collect(Collectors.toList());
        return PageResult.of(list, result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 查询最近一条工单摘要。
     * 优先取最近未关闭工单；若不存在，再回退到最近创建的工单。
     *
     * @return 最近工单摘要，不存在时返回 null
     */
    @Override
    public CustomerWorkOrderLatestSummaryVO getLatestSummary() {
        Long customerId = requireCustomerId();
        WorkOrder workOrder = findLatestUnclosedWorkOrder(customerId);
        if (workOrder == null) {
            workOrder = findLatestWorkOrder(customerId);
        }
        return workOrder == null ? null : buildLatestSummaryVo(workOrder);
    }

    /**
     * 查询我的工单状态计数
     *
     * @return 状态计数
     */
    @Override
    public CustomerWorkOrderStatusCountVO getStatusCount() {
        Long customerId = requireCustomerId();
        CustomerWorkOrderStatusCountVO vo = new CustomerWorkOrderStatusCountVO();
        vo.setAllCount(countByStatuses(customerId));
        vo.setWaitAcceptCount(countByStatuses(customerId,
                WorkOrderStatusConstants.MainStatus.PENDING_ASSIGN,
                WorkOrderStatusConstants.MainStatus.PENDING_TECH_ACCEPT));
        vo.setInProgressCount(countByStatuses(customerId, WorkOrderStatusConstants.MainStatus.IN_PROGRESS));
        vo.setCompletedCount(countByStatuses(customerId, WorkOrderStatusConstants.MainStatus.COMPLETED));
        vo.setClosedCount(countByStatuses(customerId, WorkOrderStatusConstants.MainStatus.CLOSED));
        return vo;
    }

    /**
     * 查询工单详情
     *
     * @param workOrderId 工单ID
     * @return 工单详情
     */
    @Override
    public CustomerWorkOrderDetailVO getById(Long workOrderId) {
        Long customerId = requireCustomerId();
        WorkOrder workOrder = requireCustomerWorkOrder(workOrderId, customerId);
        CustomerWorkOrderDetailVO detail = new CustomerWorkOrderDetailVO();
        detail.setId(workOrder.getId());
        detail.setOrderNo(workOrder.getOrderNo());
        detail.setCustomerId(workOrder.getCustomerId());
        detail.setCustomerName(workOrder.getCustomerName());
        detail.setCustomerMobile(workOrder.getCustomerMobile());
        detail.setBarcode(workOrder.getBarcode());
        detail.setProductCode(workOrder.getProductCode());
        detail.setProductName(workOrder.getProductName());
        detail.setProductModel(workOrder.getProductModel());
        detail.setMachineNo(workOrder.getMachineNo());
        detail.setBrandType(workOrder.getBrandType());
        detail.setBrandTypeLabel(workOrder.getBrandType() == null ? null : workOrder.getBrandType().getLabel());
        detail.setBrandCode(workOrder.getBrandCode());
        detail.setBrandName(workOrder.getBrandName());
        detail.setServiceMode(workOrder.getServiceMode());
        detail.setServiceModeLabel(ServiceModeEnum.resolveLabel(workOrder.getServiceMode()));
        detail.setWarrantyStatus(workOrder.getWarrantyStatus());
        detail.setFaultDesc(workOrder.getFaultDesc());
        detail.setFaultRemark(workOrder.getFaultRemark());
        detail.setSenderName(workOrder.getSenderName());
        detail.setSenderMobile(workOrder.getSenderMobile());
        detail.setSenderAddress(workOrder.getSenderAddress());
        detail.setSendExpressNo(workOrder.getSendExpressNo());
        detail.setMainStatus(workOrder.getMainStatus());
        detail.setDisplayStatus(resolveCustomerDisplayStatus(workOrder.getMainStatus()));
        detail.setEvaluateStatus(workOrder.getEvaluateStatus());
        detail.setEvaluateStatusLabel(WorkOrderStatusConstants.resolveEvaluateStatusLabel(workOrder.getEvaluateStatus()));
        detail.setHqCompanyId(workOrder.getHqCompanyId());
        detail.setReturnMethod(workOrder.getReturnMethod());
        detail.setReturnExpressNo(workOrder.getReturnExpressNo());
        detail.setCloseReason(workOrder.getCloseReason());
        detail.setCanEvaluate(canEvaluate(workOrder));
        detail.setCanEditSendInfo(canEditSendInfo(workOrder));
        detail.setCompletedTime(workOrder.getCompletedTime());
        detail.setClosedTime(workOrder.getClosedTime());
        detail.setCreateTime(workOrder.getCreateTime());
        fillAttachmentDetail(detail, buildWorkOrderFileMap(workOrderId));

        Map<Long, SysCompany> companyMap = buildCompanyMap(Collections.singleton(workOrder.getCurrentAcceptCompanyId()));
        Map<Long, String> userNameMap = buildUserNameMap(Collections.singleton(workOrder.getAssignedUserId()));
        SysCompany currentAcceptCompany = companyMap.get(workOrder.getCurrentAcceptCompanyId());
        detail.setCurrentAcceptCompanyName(currentAcceptCompany == null ? null : currentAcceptCompany.getCompanyName());
        detail.setCurrentAcceptCompanyPhone(currentAcceptCompany == null ? null : currentAcceptCompany.getContactPhone());
        detail.setAssignedUserName(userNameMap.get(workOrder.getAssignedUserId()));
        detail.setQuotes(listQuoteVos(workOrderId));
        detail.setRepairs(listRepairVos(workOrderId));
        detail.setEvaluation(getEvaluationVo(workOrderId));
        return detail;
    }

    /**
     * 更新工单寄修信息
     *
     * @param dto 寄修信息参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSendInfo(CustomerWorkOrderSendInfoDTO dto) {
        Long customerId = requireCustomerId();
        WorkOrder workOrder = requireCustomerWorkOrder(dto.getWorkOrderId(), customerId);
        if (!canEditSendInfo(workOrder)) {
            throw new ServiceException("当前工单不允许修改寄修信息");
        }
        workOrder.setSenderName(normalizeText(dto.getSenderName()));
        workOrder.setSenderMobile(normalizeText(dto.getSenderMobile()));
        workOrder.setSenderAddress(normalizeText(dto.getSenderAddress()));
        workOrder.setSendExpressNo(normalizeText(dto.getSendExpressNo()));
        workOrderMapper.updateById(workOrder);
        sysFileService.replaceBizFiles(
                SysFileBizTypeEnum.WORK_ORDER_SENDER_VOUCHER,
                workOrder.getId(),
                dto.getSenderVoucherFileIds(),
                null,
                customerId,
                SysFileUploadUserTypeEnum.CUSTOMER,
                null
        );
    }

    /**
     * 上传工单寄件凭证
     *
     * @param dto 寄件凭证参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateSenderVoucher(CustomerWorkOrderSenderVoucherDTO dto) {
        Long customerId = requireCustomerId();
        WorkOrder workOrder = requireCustomerWorkOrder(dto.getWorkOrderId(), customerId);
        if (!canEditSendInfo(workOrder)) {
            throw new ServiceException("当前工单不允许上传寄件凭证");
        }
        if (hasSenderVoucher(workOrder.getId())) {
            throw new ServiceException("当前工单已上传寄件凭证");
        }
        sysFileService.replaceBizFiles(
                SysFileBizTypeEnum.WORK_ORDER_SENDER_VOUCHER,
                workOrder.getId(),
                dto.getSenderVoucherFileIds(),
                null,
                customerId,
                SysFileUploadUserTypeEnum.CUSTOMER,
                null
        );
    }

    /**
     * 提交工单评价
     *
     * @param dto 评价参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void evaluate(CustomerWorkOrderEvaluateDTO dto) {
        Long customerId = requireCustomerId();
        WorkOrder workOrder = requireCustomerWorkOrder(dto.getWorkOrderId(), customerId);
        if (!WorkOrderStatusConstants.MainStatus.CLOSED.equals(workOrder.getMainStatus())) {
            throw new ServiceException("当前工单未关闭，不能评价");
        }
        if (!WorkOrderStatusConstants.EvaluateStatus.PENDING_EVALUATE.equals(workOrder.getEvaluateStatus())) {
            throw new ServiceException("当前工单不可重复评价");
        }
        if (!hasFaultForEvaluation(workOrder.getId())) {
            throw new ServiceException("当前工单无故障，不能评价");
        }
        LambdaQueryWrapper<WorkOrderEvaluation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkOrderEvaluation::getWorkOrderId, workOrder.getId());
        if (workOrderEvaluationMapper.selectCount(wrapper) > 0) {
            throw new ServiceException("当前工单已完成评价");
        }

        WorkOrderEvaluation evaluation = new WorkOrderEvaluation();
        evaluation.setWorkOrderId(workOrder.getId());
        evaluation.setCustomerId(customerId);
        evaluation.setCompanyId(workOrder.getCurrentAcceptCompanyId());
        evaluation.setTimelinessScore(dto.getTimelinessScore());
        evaluation.setQualityScore(dto.getQualityScore());
        evaluation.setSatisfactionScore(dto.getSatisfactionScore());
        evaluation.setTags(dto.getTags());
        evaluation.setContent(dto.getContent());
        workOrderEvaluationMapper.insert(evaluation);

        workOrder.setEvaluateStatus(WorkOrderStatusFlow.afterEvaluate());
        workOrderMapper.updateById(workOrder);

        WorkOrderFlow flow = new WorkOrderFlow();
        flow.setWorkOrderId(workOrder.getId());
        flow.setActionType("EVALUATE");
        flow.setBeforeStatus(workOrder.getMainStatus());
        flow.setAfterStatus(workOrder.getMainStatus());
        flow.setFromCompanyId(workOrder.getCurrentAcceptCompanyId());
        flow.setToCompanyId(workOrder.getCurrentAcceptCompanyId());
        flow.setOperatorCompanyId(workOrder.getCurrentAcceptCompanyId());
        flow.setOperatorUserId(customerId);
        flow.setRemark(dto.getContent());
        workOrderFlowMapper.insert(flow);

    }

    /**
     * 汇总工单附件，统一供详情页和建单回显复用。
     *
     * @param workOrderId 工单ID
     * @return 附件映射
     */
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

    private void fillAttachmentDetail(CustomerWorkOrderDetailVO detail,
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
                                             Long customerId) {
        sysFileService.replaceBizFiles(
                SysFileBizTypeEnum.WORK_ORDER_FAULT_IMAGE,
                workOrderId,
                faultImageFileIds,
                null,
                customerId,
                SysFileUploadUserTypeEnum.CUSTOMER,
                null
        );
        sysFileService.replaceBizFiles(
                SysFileBizTypeEnum.WORK_ORDER_FAULT_VIDEO,
                workOrderId,
                faultVideoFileIds,
                null,
                customerId,
                SysFileUploadUserTypeEnum.CUSTOMER,
                null
        );
        sysFileService.replaceBizFiles(
                SysFileBizTypeEnum.WORK_ORDER_FAULT_VOICE,
                workOrderId,
                faultVoiceFileIds,
                null,
                customerId,
                SysFileUploadUserTypeEnum.CUSTOMER,
                null
        );
        sysFileService.replaceBizFiles(
                SysFileBizTypeEnum.WORK_ORDER_SENDER_VOUCHER,
                workOrderId,
                senderVoucherFileIds,
                null,
                customerId,
                SysFileUploadUserTypeEnum.CUSTOMER,
                null
        );
    }

    /**
     * 校验 C 端登录态并返回当前客户ID。
     *
     * @return 当前客户ID
     */
    private Long requireCustomerId() {
        StpCustomerUtil.checkLogin();
        return StpCustomerUtil.getLoginIdAsLong();
    }

    /**
     * 校验工单归属当前客户，避免越权查看或修改其它客户工单。
     *
     * @param workOrderId 工单ID
     * @param customerId 当前客户ID
     * @return 工单实体
     */
    private WorkOrder requireCustomerWorkOrder(Long workOrderId, Long customerId) {
        WorkOrder workOrder = workOrderMapper.selectById(workOrderId);
        if (workOrder == null) {
            throw new ServiceException("工单不存在");
        }
        if (!customerId.equals(workOrder.getCustomerId())) {
            throw new ServiceException("无权查看该工单");
        }
        return workOrder;
    }

    /**
     * 校验客户存在且状态正常。
     *
     * @param customerId 客户ID
     * @return 客户实体
     */
    private CUser requireCustomer(Long customerId) {
        CUser customer = cUserMapper.selectById(customerId);
        if (customer == null) {
            throw new ServiceException("客户不存在");
        }
        if (customer.getStatus() != null && customer.getStatus() == 0) {
            throw new ServiceException("当前客户已停用");
        }
        return customer;
    }

    /**
     * 校验建单入参的核心业务约束，避免在后续多段自调用里重复判空。
     *
     * @param dto 建单参数
     * @param brandType 品牌类型
     * @param hasBarcode 是否填写条码
     */
    private void validateCreateRequest(CustomerWorkOrderCreateDTO dto, BrandTypeEnum brandType, boolean hasBarcode) {
        if (dto == null) {
            throw new ServiceException("建单参数不能为空");
        }
        if (brandType == null) {
            throw new ServiceException("品牌类型不支持");
        }
        normalizeServiceMode(dto.getServiceMode());
        if (brandType.isJasic() && hasBarcode) {
            normalizeRequiredText(dto.getBarcode(), "机器条码不能为空");
        }
        if (brandType.isNonJasic() && hasBarcode) {
            throw new ServiceException("非佳士报修不支持填写机器条码");
        }
        validateSendInfo(dto);
    }

    /**
     * 寄修方式下必须填写寄件信息，到店维修则不要求这些字段。
     *
     * @param dto 建单参数
     */
    private void validateSendInfo(CustomerWorkOrderCreateDTO dto) {
        if (dto == null || !ServiceModeEnum.isMail(normalizeServiceMode(dto.getServiceMode()))) {
            return;
        }
        if (normalizeText(dto.getSenderName()) == null) {
            throw new ServiceException("寄修工单必须填写寄件人姓名");
        }
        if (normalizeText(dto.getSenderMobile()) == null) {
            throw new ServiceException("寄修工单必须填写寄件人手机号");
        }
        if (normalizeText(dto.getSenderAddress()) == null) {
            throw new ServiceException("寄修工单必须填写寄件地址");
        }
    }

    /**
     * 校验所选服务网点存在、启用且属于 C 端可报修范围。
     *
     * @param serviceCompanyId 服务网点ID
     * @return 服务网点实体
     */
    private SysCompany requireServiceCompany(Long serviceCompanyId) {
        SysCompany company = sysCompanyMapper.selectById(serviceCompanyId);
        if (company == null) {
            throw new ServiceException("服务网点不存在");
        }
        if (company.getStatus() != null && company.getStatus() == 0) {
            throw new ServiceException("服务网点已停用");
        }
        if (!"SITE_FIRST".equals(company.getTypeCode()) && !"SITE_SECOND".equals(company.getTypeCode())) {
            throw new ServiceException("当前公司不是可选服务网点");
        }
        return company;
    }

    /**
     * 校验归属总部存在且启用。
     *
     * @param hqCompanyId 总部ID
     * @return 总部实体
     */
    private SysCompany requireHqCompany(Long hqCompanyId) {
        SysCompany company = sysCompanyMapper.selectById(hqCompanyId);
        if (company == null) {
            throw new ServiceException("归属总部不存在");
        }
        if (company.getStatus() != null && company.getStatus() == 0) {
            throw new ServiceException("归属总部已停用");
        }
        if ("SITE_FIRST".equals(company.getTypeCode()) || "SITE_SECOND".equals(company.getTypeCode())) {
            throw new ServiceException("归属总部类型不正确");
        }
        return company;
    }

    // Barcode archives are primary; relation fallback keeps old data usable during backfill.
    /**
     * 兼容旧入口，按条码和服务网点推导默认归属总部。
     *
     * @param barcode 机器条码
     * @param serviceCompany 服务网点
     * @return 归属总部ID
     */
    private Long resolveCreateHqCompanyId(String barcode, SysCompany serviceCompany) {
        return resolveCreateHqCompanyId(serviceCompany, findActiveMachineBarcode(barcode));
    }

    /**
     * 根据品牌类型与是否填写条码决定归属总部的推导路径。
     *
     * @param brandType 品牌类型
     * @param hasBarcode 是否填写条码
     * @param serviceCompany 服务网点
     * @param barcodeArchive 条码档案
     * @return 归属总部ID
     */
    private Long resolveCreateHqCompanyId(BrandTypeEnum brandType, boolean hasBarcode,
                                          SysCompany serviceCompany, MachineBarcode barcodeArchive) {
        if (brandType == null || !brandType.isJasic() || !hasBarcode) {
            return resolveDefaultHqCompanyId();
        }
        return resolveCreateHqCompanyId(serviceCompany, barcodeArchive);
    }

    /**
     * 读取系统默认归属总部配置。
     *
     * @return 默认归属总部ID
     */
    private Long resolveDefaultHqCompanyId() {
        String configValue = normalizeText(sysConfigService == null
                ? null
                : sysConfigService.getValueByKey(WorkOrderConfigConstants.DEFAULT_HQ_COMPANY_ID));
        if (configValue == null) {
            throw new ServiceException("默认归属总部未配置");
        }
        Long hqCompanyId;
        try {
            hqCompanyId = Long.valueOf(configValue);
        } catch (NumberFormatException ex) {
            throw new ServiceException("默认归属总部配置不正确");
        }
        try {
            return requireHqCompany(hqCompanyId).getId();
        } catch (ServiceException ex) {
            throw new ServiceException("默认归属总部配置不正确");
        }
    }

    // Temporary fallback remains for old data until barcode archives are fully backfilled.
    /**
     * 按服务网点与条码档案交叉推导可用的归属总部。
     *
     * @param serviceCompany 服务网点
     * @param barcodeArchive 条码档案
     * @return 归属总部ID
     */
    private Long resolveCreateHqCompanyId(SysCompany serviceCompany, MachineBarcode barcodeArchive) {
        Long archiveHqCompanyId = resolveBarcodeArchiveHqCompanyId(barcodeArchive);
        if (archiveHqCompanyId != null) {
            return archiveHqCompanyId;
        }
        List<Long> firstCompanyIds = resolveFirstCompanyIds(serviceCompany);
        if (firstCompanyIds.isEmpty()) {
            throw new ServiceException("当前服务网点暂未关联可用总部，无法提交报修单");
        }
        List<Long> hqCompanyIds = resolveActiveHqCompanyIds(firstCompanyIds);
        if (hqCompanyIds.isEmpty()) {
            throw new ServiceException("当前机器条码归属总部暂无法自动识别，请联系管理员完善条码归属配置");
        }
        if (hqCompanyIds.size() > 1) {
            throw new ServiceException("当前机器条码归属总部存在多个候选项，暂无法自动识别，请联系管理员完善条码归属配置");
        }
        return hqCompanyIds.get(0);
    }

    /**
     * 优先从条码档案直接识别归属总部。
     *
     * @param barcodeArchive 条码档案
     * @return 归属总部ID
     */
    private Long resolveBarcodeArchiveHqCompanyId(MachineBarcode barcodeArchive) {
        if (barcodeArchive == null || barcodeArchive.getHqCompanyId() == null) {
            return null;
        }
        return requireHqCompany(barcodeArchive.getHqCompanyId()).getId();
    }

    /**
     * 条码报修时要求条码档案存在且状态正常。
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

    private MachineBarcode findActiveMachineBarcode(String barcode) {
        String normalizedBarcode = normalizeText(barcode);
        if (normalizedBarcode == null) {
            return null;
        }
        LambdaQueryWrapper<MachineBarcode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MachineBarcode::getBarcode, normalizedBarcode)
                .eq(MachineBarcode::getStatus, 1)
                .last("LIMIT 1");
        return machineBarcodeMapper.selectOne(wrapper);
    }

    private String resolveBarcodeWarrantyStatus(MachineBarcode barcodeArchive, String fallbackStatus) {
        return MachineBarcodeWarrantyResolver.resolveWarrantyStatus(
                barcodeArchive == null ? null : barcodeArchive.getBarcode(),
                barcodeArchive == null ? null : barcodeArchive.getDealerOutDate(),
                barcodeArchive == null ? null : barcodeArchive.getScanDate(),
                resolveArchiveText(barcodeArchive == null ? null : barcodeArchive.getWarrantyStatus(), fallbackStatus)
        );
    }

    private List<Long> resolveFirstCompanyIds(SysCompany serviceCompany) {
        if ("SITE_FIRST".equals(serviceCompany.getTypeCode())) {
            return Collections.singletonList(serviceCompany.getId());
        }
        LambdaQueryWrapper<FirstSecondRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FirstSecondRelation::getSecondCompanyId, serviceCompany.getId())
                .eq(FirstSecondRelation::getStatus, 1);
        List<FirstSecondRelation> relations = firstSecondRelationMapper.selectList(wrapper);
        if (relations.isEmpty()) {
            return Collections.emptyList();
        }
        return relations.stream()
                .map(FirstSecondRelation::getFirstCompanyId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<Long> resolveActiveHqCompanyIds(List<Long> firstCompanyIds) {
        if (firstCompanyIds == null || firstCompanyIds.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<HqFirstContract> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(HqFirstContract::getFirstCompanyId, firstCompanyIds)
                .eq(HqFirstContract::getStatus, 1);
        List<HqFirstContract> contracts = hqFirstContractMapper.selectList(wrapper);
        if (contracts.isEmpty()) {
            return Collections.emptyList();
        }
        return contracts.stream()
                .map(HqFirstContract::getHqCompanyId)
                .filter(id -> id != null)
                .distinct()
                .map(this::requireHqCompany)
                .map(SysCompany::getId)
                .collect(Collectors.toList());
    }

    private List<SysCompany> listActiveServiceCompanies() {
        Set<String> typeCodes = new LinkedHashSet<>();
        typeCodes.addAll(CompanyCategoryEnum.getFirstLevelTypeCodes());
        typeCodes.addAll(CompanyCategoryEnum.getSecondLevelTypeCodes());
        if (typeCodes.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<SysCompany> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysCompany::getTypeCode, typeCodes)
                .eq(SysCompany::getStatus, 1)
                .orderByAsc(SysCompany::getCompanyName)
                .orderByAsc(SysCompany::getId);
        return sysCompanyMapper.selectList(wrapper);
    }

    private List<SysCompany> listNearbyEnabledServiceCompanies() {
        Set<String> typeCodes = new LinkedHashSet<>();
        typeCodes.addAll(CompanyCategoryEnum.getFirstLevelTypeCodes());
        typeCodes.addAll(CompanyCategoryEnum.getSecondLevelTypeCodes());
        if (typeCodes.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<SysCompany> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysCompany::getTypeCode, typeCodes)
                .eq(SysCompany::getStatus, 1)
                .eq(SysCompany::getGeocodeStatus, GEOCODE_STATUS_SUCCESS)
                .isNotNull(SysCompany::getLongitude)
                .isNotNull(SysCompany::getLatitude)
                .orderByAsc(SysCompany::getCompanyName)
                .orderByAsc(SysCompany::getId);
        return sysCompanyMapper.selectList(wrapper);
    }

    /**
     * 仅在寄修模式下保留寄件字段，其余模式统一清空。
     *
     * @param serviceMode 服务方式
     * @param value 原始字段值
     * @return 清洗后的字段值
     */
    private String resolveSendField(String serviceMode, String value) {
        if (!ServiceModeEnum.isMail(serviceMode)) {
            return null;
        }
        return normalizeText(value);
    }

    /**
     * 记录客户建单时的首条工单流转记录。
     *
     * @param workOrderId 工单ID
     * @param customerId 客户ID
     * @param serviceCompanyId 服务网点ID
     * @param afterStatus 建单后的工单状态
     */
    private void saveCreateFlow(Long workOrderId, Long customerId, Long serviceCompanyId, String afterStatus) {
        WorkOrderFlow flow = new WorkOrderFlow();
        flow.setWorkOrderId(workOrderId);
        flow.setActionType("CREATE");
        flow.setBeforeStatus(null);
        flow.setAfterStatus(afterStatus);
        flow.setFromCompanyId(null);
        flow.setToCompanyId(serviceCompanyId);
        flow.setOperatorCompanyId(serviceCompanyId);
        flow.setOperatorUserId(customerId);
        flow.setRemark("客户提交报修");
        workOrderFlowMapper.insert(flow);
    }

    /**
     * 把前端标签页状态映射为工单主状态筛选条件。
     *
     * @param wrapper 查询条件
     * @param tabStatus 标签页状态
     */
    private void applyTabStatusFilter(LambdaQueryWrapper<WorkOrder> wrapper, String tabStatus) {
        if (tabStatus == null || tabStatus.trim().isEmpty()) {
            return;
        }
        if (WorkOrderStatusConstants.DisplayStatus.WAIT_ACCEPT.equals(tabStatus)) {
            wrapper.in(WorkOrder::getMainStatus,
                    WorkOrderStatusConstants.MainStatus.PENDING_ASSIGN,
                    WorkOrderStatusConstants.MainStatus.PENDING_TECH_ACCEPT);
            return;
        }
        if (WorkOrderStatusConstants.DisplayStatus.IN_PROGRESS.equals(tabStatus)) {
            wrapper.eq(WorkOrder::getMainStatus, WorkOrderStatusConstants.MainStatus.IN_PROGRESS);
            return;
        }
        if (WorkOrderStatusConstants.DisplayStatus.COMPLETED.equals(tabStatus)) {
            wrapper.eq(WorkOrder::getMainStatus, WorkOrderStatusConstants.MainStatus.COMPLETED);
            return;
        }
        if (WorkOrderStatusConstants.DisplayStatus.CLOSED.equals(tabStatus)) {
            wrapper.eq(WorkOrder::getMainStatus, WorkOrderStatusConstants.MainStatus.CLOSED);
        }
    }

    private Long countByStatuses(Long customerId, String... statuses) {
        LambdaQueryWrapper<WorkOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkOrder::getCustomerId, customerId);
        if (statuses != null && statuses.length > 0) {
            wrapper.in(WorkOrder::getMainStatus, (Object[]) statuses);
        }
        return workOrderMapper.selectCount(wrapper);
    }

    private CustomerWorkOrderListVO buildListVo(WorkOrder workOrder, Map<Long, SysCompany> companyMap,
                                                Map<Long, String> userNameMap,
                                                Set<Long> senderVoucherWorkOrderIds,
                                                Map<Long, BigDecimal> currentQuoteAmountMap) {
        CustomerWorkOrderListVO vo = new CustomerWorkOrderListVO();
        vo.setId(workOrder.getId());
        vo.setOrderNo(workOrder.getOrderNo());
        vo.setCustomerName(workOrder.getCustomerName());
        vo.setCustomerMobile(workOrder.getCustomerMobile());
        vo.setBarcode(workOrder.getBarcode());
        vo.setProductModel(workOrder.getProductModel());
        vo.setBrandType(workOrder.getBrandType());
        vo.setBrandTypeLabel(workOrder.getBrandType() == null ? null : workOrder.getBrandType().getLabel());
        vo.setServiceMode(workOrder.getServiceMode());
        vo.setServiceModeLabel(ServiceModeEnum.resolveLabel(workOrder.getServiceMode()));
        vo.setMainStatus(workOrder.getMainStatus());
        vo.setDisplayStatus(resolveCustomerDisplayStatus(workOrder.getMainStatus()));
        vo.setEvaluateStatus(workOrder.getEvaluateStatus());
        vo.setEvaluateStatusLabel(WorkOrderStatusConstants.resolveEvaluateStatusLabel(workOrder.getEvaluateStatus()));
        SysCompany currentAcceptCompany = companyMap == null ? null : companyMap.get(workOrder.getCurrentAcceptCompanyId());
        vo.setCurrentAcceptCompanyName(currentAcceptCompany == null ? null : currentAcceptCompany.getCompanyName());
        vo.setCurrentAcceptCompanyPhone(currentAcceptCompany == null ? null : currentAcceptCompany.getContactPhone());
        vo.setAssignedUserName(userNameMap.get(workOrder.getAssignedUserId()));
        vo.setHasTransfer(workOrder.getHasTransfer());
        vo.setCanEvaluate(canEvaluate(workOrder));
        vo.setCanUploadSendExpress(canUploadSendExpress(workOrder,
                senderVoucherWorkOrderIds != null && senderVoucherWorkOrderIds.contains(workOrder.getId())));
        vo.setQuoteAmount(currentQuoteAmountMap == null ? null : currentQuoteAmountMap.get(workOrder.getId()));
        vo.setCreateTime(workOrder.getCreateTime());
        vo.setClosedTime(workOrder.getClosedTime());
        return vo;
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

    private CustomerWorkOrderLatestSummaryVO buildLatestSummaryVo(WorkOrder workOrder) {
        CustomerWorkOrderLatestSummaryVO vo = new CustomerWorkOrderLatestSummaryVO();
        vo.setId(workOrder.getId());
        vo.setOrderNo(workOrder.getOrderNo());
        vo.setProductName(workOrder.getProductName());
        vo.setProductModel(workOrder.getProductModel());
        vo.setFaultDesc(workOrder.getFaultDesc());
        vo.setBrandType(workOrder.getBrandType());
        vo.setBrandTypeLabel(workOrder.getBrandType() == null ? null : workOrder.getBrandType().getLabel());
        vo.setServiceMode(workOrder.getServiceMode());
        vo.setServiceModeLabel(ServiceModeEnum.resolveLabel(workOrder.getServiceMode()));
        vo.setDisplayStatus(resolveCustomerDisplayStatus(workOrder.getMainStatus()));
        vo.setCreateTime(workOrder.getCreateTime());
        return vo;
    }

    private WorkOrder findLatestUnclosedWorkOrder(Long customerId) {
        LambdaQueryWrapper<WorkOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkOrder::getCustomerId, customerId)
                .ne(WorkOrder::getMainStatus, WorkOrderStatusConstants.MainStatus.CLOSED)
                .orderByDesc(WorkOrder::getCreateTime)
                .last("limit 1");
        List<WorkOrder> records = workOrderMapper.selectList(wrapper);
        return records == null || records.isEmpty() ? null : records.get(0);
    }

    private WorkOrder findLatestWorkOrder(Long customerId) {
        LambdaQueryWrapper<WorkOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkOrder::getCustomerId, customerId)
                .orderByDesc(WorkOrder::getCreateTime)
                .last("limit 1");
        List<WorkOrder> records = workOrderMapper.selectList(wrapper);
        return records == null || records.isEmpty() ? null : records.get(0);
    }

    /**
     * 只有已关闭、待评价且存在故障的工单才允许客户评价。
     *
     * @param workOrder 工单实体
     * @return 是否允许评价
     */
    private boolean canEvaluate(WorkOrder workOrder) {
        return workOrder != null
                && WorkOrderStatusConstants.MainStatus.CLOSED.equals(workOrder.getMainStatus())
                && WorkOrderStatusConstants.EvaluateStatus.PENDING_EVALUATE.equals(workOrder.getEvaluateStatus())
                && hasFaultForEvaluation(workOrder.getId());
    }

    /**
     * 客户仅能在服务网点尚未处理前补充或修改寄修信息。
     *
     * @param workOrder 工单实体
     * @return 是否允许修改寄修信息
     */
    private boolean canEditSendInfo(WorkOrder workOrder) {
        return workOrder != null
                && ServiceModeEnum.isMail(workOrder.getServiceMode())
                && WorkOrderStatusConstants.isWaitAcceptMainStatus(workOrder.getMainStatus());
    }

    /**
     * 仅待接单寄修单且当前未上传寄件凭证时，列表才展示上传按钮。
     *
     * @param workOrder 工单实体
     * @param hasSenderVoucher 当前是否已有寄件凭证
     * @return 是否允许上传寄件凭证
     */
    private boolean canUploadSendExpress(WorkOrder workOrder, boolean hasSenderVoucher) {
        return !hasSenderVoucher && canEditSendInfo(workOrder);
    }

    private boolean hasSenderVoucher(Long workOrderId) {
        if (workOrderId == null) {
            return false;
        }
        return !sysFileService.listBizFiles(SysFileBizTypeEnum.WORK_ORDER_SENDER_VOUCHER, workOrderId).isEmpty();
    }

    private Set<Long> buildSenderVoucherWorkOrderIdSet(Set<Long> workOrderIds) {
        if (workOrderIds == null || workOrderIds.isEmpty()) {
            return Collections.emptySet();
        }
        List<Long> validWorkOrderIds = workOrderIds.stream()
                .filter(id -> id != null)
                .collect(Collectors.toList());
        if (validWorkOrderIds.isEmpty()) {
            return Collections.emptySet();
        }
        LambdaQueryWrapper<SysFileBiz> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysFileBiz::getBizType, SysFileBizTypeEnum.WORK_ORDER_SENDER_VOUCHER)
                .in(SysFileBiz::getBizId, validWorkOrderIds);
        List<SysFileBiz> relations = sysFileBizMapper.selectList(wrapper);
        if (relations == null || relations.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> activeFileIds = sysFileMapper.selectBatchIds(relations.stream()
                        .map(SysFileBiz::getFileId)
                        .filter(id -> id != null)
                        .collect(Collectors.toSet()))
                .stream()
                .filter(file -> file != null && SysFileStatusEnum.ACTIVE == file.getStatus())
                .map(SysFile::getId)
                .collect(Collectors.toSet());
        if (activeFileIds.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> result = new HashSet<>();
        for (SysFileBiz relation : relations) {
            if (relation != null && relation.getBizId() != null && activeFileIds.contains(relation.getFileId())) {
                result.add(relation.getBizId());
            }
        }
        return result;
    }

    private String resolveCustomerDisplayStatus(String mainStatus) {
        return WorkOrderStatusConstants.resolveDisplayStatusLabel(mainStatus);
    }

    private Map<Long, SysCompany> buildCompanyMap(Set<Long> companyIds) {
        if (companyIds == null || companyIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> validCompanyIds = companyIds.stream().filter(id -> id != null).collect(Collectors.toList());
        if (validCompanyIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SysCompany> companies = sysCompanyMapper.selectBatchIds(validCompanyIds);
        Map<Long, SysCompany> map = new HashMap<>(companies.size());
        for (SysCompany company : companies) {
            if (company != null) {
                map.put(company.getId(), company);
            }
        }
        return map;
    }

    private Map<Long, String> buildCompanyNameMap(Set<Long> companyIds) {
        Map<Long, SysCompany> companyMap = buildCompanyMap(companyIds);
        if (companyMap.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> map = new HashMap<>(companyMap.size());
        for (Map.Entry<Long, SysCompany> entry : companyMap.entrySet()) {
            SysCompany company = entry.getValue();
            if (company != null) {
                map.put(entry.getKey(), company.getCompanyName());
            }
        }
        return map;
    }

    private CustomerNearbyServiceCompanyVO buildNearbyServiceCompanyOption(SysCompany company, BigDecimal longitude,
                                                                           BigDecimal latitude) {
        CustomerNearbyServiceCompanyVO vo = new CustomerNearbyServiceCompanyVO();
        vo.setId(company.getId());
        vo.setCompanyName(company.getCompanyName());
        vo.setCompanyCode(company.getCompanyCode());
        vo.setTypeCode(company.getTypeCode());
        vo.setTypeName(resolveServiceCompanyTypeName(company.getTypeCode()));
        vo.setContactPhone(company.getContactPhone());
        vo.setAddress(resolveCompanyAddress(company));
        vo.setLongitude(company.getLongitude());
        vo.setLatitude(company.getLatitude());
        vo.setDistanceKm(calculateDistanceKm(longitude, latitude, company.getLongitude(), company.getLatitude()));
        vo.setHasRepairHistory(Boolean.FALSE);
        return vo;
    }

    private CustomerServiceCompanyOptionVO buildServiceCompanyOption(SysCompany company) {
        CustomerServiceCompanyOptionVO vo = new CustomerServiceCompanyOptionVO();
        vo.setId(company.getId());
        vo.setCompanyName(company.getCompanyName());
        vo.setCompanyCode(company.getCompanyCode());
        vo.setTypeCode(company.getTypeCode());
        vo.setTypeName(resolveServiceCompanyTypeName(company.getTypeCode()));
        vo.setContactPhone(company.getContactPhone());
        vo.setAddress(resolveCompanyAddress(company));
        return vo;
    }

    private String resolveCompanyAddress(SysCompany company) {
        String fullAddress = normalizeText(company.getFullAddress());
        if (fullAddress != null) {
            return fullAddress;
        }
        return normalizeText(company.getDetailAddress());
    }

    private String resolveServiceCompanyTypeName(String typeCode) {
        if (CompanyCategoryEnum.getFirstLevelTypeCodes().contains(typeCode)) {
            return "一级服务网点";
        }
        if (CompanyCategoryEnum.getSecondLevelTypeCodes().contains(typeCode)) {
            return "二级服务网点";
        }
        return typeCode;
    }

    private String resolveArchiveText(String archiveValue, String requestValue) {
        String normalizedArchiveValue = normalizeText(archiveValue);
        return normalizedArchiveValue != null ? normalizedArchiveValue : normalizeText(requestValue);
    }

    private String resolveCustomerName(CUser customer) {
        if (customer == null) {
            throw new ServiceException("当前客户不存在");
        }
        String nickname = normalizeText(customer.getNickname());
        if (nickname != null) {
            return nickname;
        }
        return normalizeRequiredText(customer.getPhone(), "当前客户手机号不能为空");
    }

    private CustomerFaultSelection resolveCustomerFaultSelection(CustomerWorkOrderCreateDTO dto, Long hqCompanyId,
                                                                 String productCode, String productModel) {
        return resolveCustomerFaultSelection(dto, BrandTypeEnum.JASIC, true, hqCompanyId, productCode, productModel);
    }

    private CustomerFaultSelection resolveCustomerFaultSelection(CustomerWorkOrderCreateDTO dto, BrandTypeEnum brandType,
                                                                 boolean hasBarcode, Long hqCompanyId,
                                                                 String productCode, String productModel) {
        if (brandType == null || !hasBarcode) {
            return resolveOtherOnlyFaultSelection(dto);
        }
        List<String> configuredFaultOptions = listConfiguredFaultOptions(hqCompanyId, productCode, productModel);
        List<String> faultItems = normalizeFaultItems(dto == null ? null : dto.getFaultItems());
        String faultRemark = normalizeText(dto == null ? null : dto.getFaultRemark());
        String legacyFaultDesc = normalizeText(dto == null ? null : dto.getFaultDesc());
        boolean useLegacyFaultDesc = false;
        if (faultItems.isEmpty() && legacyFaultDesc != null) {
            faultItems = new ArrayList<>(Collections.singletonList(legacyFaultDesc));
            useLegacyFaultDesc = true;
        }

        if (configuredFaultOptions.isEmpty()) {
            if (faultItems.isEmpty()) {
                faultItems = new ArrayList<>(Collections.singletonList(OTHER_FAULT_LABEL));
            } else if (useLegacyFaultDesc && faultItems.size() == 1 && !OTHER_FAULT_LABEL.equals(faultItems.get(0))) {
                faultRemark = faultRemark != null ? faultRemark : faultItems.get(0);
                faultItems = new ArrayList<>(Collections.singletonList(OTHER_FAULT_LABEL));
            } else if (faultItems.size() != 1 || !OTHER_FAULT_LABEL.equals(faultItems.get(0))) {
                throw new ServiceException("当前产品暂未配置故障项，请选择其它故障");
            }
        } else {
            LinkedHashSet<String> allowedFaultOptions = new LinkedHashSet<>(configuredFaultOptions);
            allowedFaultOptions.add(OTHER_FAULT_LABEL);
            if (faultItems.isEmpty()) {
                throw new ServiceException("请选择故障描述");
            }
            if (useLegacyFaultDesc && faultItems.size() == 1 && !allowedFaultOptions.contains(faultItems.get(0))) {
                faultRemark = faultRemark != null ? faultRemark : faultItems.get(0);
                faultItems = new ArrayList<>(Collections.singletonList(OTHER_FAULT_LABEL));
            }
            for (String faultItem : faultItems) {
                if (!allowedFaultOptions.contains(faultItem)) {
                    throw new ServiceException("故障描述不在可选范围内");
                }
            }
        }

        if (faultItems.contains(OTHER_FAULT_LABEL) && faultRemark == null) {
            throw new ServiceException("选择其它故障时必须填写故障说明");
        }
        return new CustomerFaultSelection(String.join(FAULT_DESC_SEPARATOR, faultItems), faultRemark);
    }

    private CustomerFaultSelection resolveOtherOnlyFaultSelection(CustomerWorkOrderCreateDTO dto) {
        List<String> faultItems = normalizeFaultItems(dto == null ? null : dto.getFaultItems());
        String faultRemark = normalizeText(dto == null ? null : dto.getFaultRemark());
        String legacyFaultDesc = normalizeText(dto == null ? null : dto.getFaultDesc());
        if (faultItems.isEmpty()) {
            faultItems = new ArrayList<>(Collections.singletonList(OTHER_FAULT_LABEL));
        }
        if (faultItems.size() != 1 || !OTHER_FAULT_LABEL.equals(faultItems.get(0))) {
            throw new ServiceException("无码报修只能选择其它故障");
        }
        if (faultRemark == null) {
            faultRemark = legacyFaultDesc;
        }
        if (faultRemark == null) {
            throw new ServiceException("选择其它故障时必须填写故障说明");
        }
        return new CustomerFaultSelection(OTHER_FAULT_LABEL, faultRemark);
    }

    /**
     * 根据总部和产品配置生成 C 端可选故障描述，并附带“其它故障”兜底项。
     *
     * @param hqCompanyId 归属总部ID
     * @param productCode 产品编码
     * @param productModel 产品型号
     * @return 故障描述选项
     */
    private List<String> buildCustomerFaultOptions(Long hqCompanyId, String productCode, String productModel) {
        LinkedHashSet<String> faultOptions = new LinkedHashSet<>(listConfiguredFaultOptions(hqCompanyId, productCode, productModel));
        faultOptions.add(OTHER_FAULT_LABEL);
        return new ArrayList<>(faultOptions);
    }

    private List<String> listConfiguredFaultOptions(Long hqCompanyId, String productCode, String productModel) {
        List<WorkOrderRepairFaultOptionVO> repairFaultOptions = faultRepairConfigService == null
                ? Collections.emptyList()
                : faultRepairConfigService.listRepairFaultOptions(hqCompanyId, productCode, productModel);
        if (repairFaultOptions == null || repairFaultOptions.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (WorkOrderRepairFaultOptionVO repairFaultOption : repairFaultOptions) {
            String faultDesc = normalizeText(repairFaultOption.getFaultDesc());
            if (faultDesc != null) {
                result.add(faultDesc);
            }
        }
        return new ArrayList<>(result);
    }

    private List<String> normalizeFaultItems(List<String> faultItems) {
        if (faultItems == null || faultItems.isEmpty()) {
            return new ArrayList<>();
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String faultItem : faultItems) {
            String normalized = normalizeText(faultItem);
            if (normalized != null) {
                result.add(normalized);
            }
        }
        return new ArrayList<>(result);
    }

    /**
     * 校验定位经纬度，避免附近网点排序时出现非法坐标。
     *
     * @param longitude 经度
     * @param latitude 纬度
     */
    private void validateCoordinate(BigDecimal longitude, BigDecimal latitude) {
        if (longitude == null || latitude == null) {
            throw new ServiceException("定位经纬度不能为空");
        }
        if (longitude.compareTo(MIN_LONGITUDE) < 0 || longitude.compareTo(MAX_LONGITUDE) > 0) {
            throw new ServiceException("经度超出有效范围");
        }
        if (latitude.compareTo(MIN_LATITUDE) < 0 || latitude.compareTo(MAX_LATITUDE) > 0) {
            throw new ServiceException("纬度超出有效范围");
        }
    }

    /**
     * 控制附近网点返回条数，避免一次性查询过大。
     *
     * @param limit 前端期望条数
     * @return 规范化后的条数
     */
    private int normalizeNearbyLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_NEARBY_LIMIT;
        }
        if (limit <= 0) {
            throw new ServiceException("返回条数必须大于0");
        }
        return Math.min(limit, MAX_NEARBY_LIMIT);
    }

    private BigDecimal calculateDistanceKm(BigDecimal sourceLongitude, BigDecimal sourceLatitude,
                                           BigDecimal targetLongitude, BigDecimal targetLatitude) {
        if (targetLongitude == null || targetLatitude == null) {
            return null;
        }
        double lat1 = Math.toRadians(sourceLatitude.doubleValue());
        double lng1 = Math.toRadians(sourceLongitude.doubleValue());
        double lat2 = Math.toRadians(targetLatitude.doubleValue());
        double lng2 = Math.toRadians(targetLongitude.doubleValue());
        double deltaLat = lat2 - lat1;
        double deltaLng = lng2 - lng1;
        double haversine = Math.pow(Math.sin(deltaLat / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(deltaLng / 2), 2);
        double distance = 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(haversine));
        return BigDecimal.valueOf(distance).setScale(2, RoundingMode.HALF_UP);
    }

    private int compareDistance(BigDecimal leftDistance, BigDecimal rightDistance,
                                String leftName, String rightName, Long leftId, Long rightId) {
        if (leftDistance == null && rightDistance == null) {
            return compareCompanyIdentity(leftName, rightName, leftId, rightId);
        }
        if (leftDistance == null) {
            return 1;
        }
        if (rightDistance == null) {
            return -1;
        }
        int compareResult = leftDistance.compareTo(rightDistance);
        if (compareResult != 0) {
            return compareResult;
        }
        return compareCompanyIdentity(leftName, rightName, leftId, rightId);
    }

    private Map<Long, RepairHistorySummary> buildRepairHistoryMap(Long customerId, List<SysCompany> companies) {
        if (customerId == null || companies == null || companies.isEmpty() || workOrderFlowMapper == null) {
            return Collections.emptyMap();
        }
        Set<Long> companyIds = companies.stream()
                .map(SysCompany::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (companyIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<WorkOrderCompanyRepairHistoryStatVO> stats =
                workOrderFlowMapper.selectCustomerCreateCompanyRepairHistory(customerId, companyIds);
        if (stats == null || stats.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, RepairHistorySummary> result = new HashMap<>(stats.size());
        for (WorkOrderCompanyRepairHistoryStatVO stat : stats) {
            if (stat == null || stat.getCompanyId() == null) {
                continue;
            }
            result.put(stat.getCompanyId(), new RepairHistorySummary(
                    stat.getRepairCount(),
                    stat.getLastRepairTime()
            ));
        }
        return result;
    }

    private int compareNearbyServiceCompany(CustomerNearbyServiceCompanyVO left,
                                            CustomerNearbyServiceCompanyVO right,
                                            Map<Long, RepairHistorySummary> repairHistoryMap) {
        RepairHistorySummary leftHistory = repairHistoryMap.get(left.getId());
        RepairHistorySummary rightHistory = repairHistoryMap.get(right.getId());
        boolean leftHasHistory = leftHistory != null;
        boolean rightHasHistory = rightHistory != null;
        if (leftHasHistory != rightHasHistory) {
            return leftHasHistory ? -1 : 1;
        }
        if (leftHasHistory) {
            int compareCount = Long.compare(rightHistory.getRepairCount(), leftHistory.getRepairCount());
            if (compareCount != 0) {
                return compareCount;
            }
            int compareTime = compareLastRepairTimeDesc(leftHistory.getLastRepairTime(),
                    rightHistory.getLastRepairTime());
            if (compareTime != 0) {
                return compareTime;
            }
        }
        return compareDistance(left.getDistanceKm(), right.getDistanceKm(),
                left.getCompanyName(), right.getCompanyName(), left.getId(), right.getId());
    }

    private int compareLastRepairTimeDesc(LocalDateTime leftTime, LocalDateTime rightTime) {
        if (leftTime == null && rightTime == null) {
            return 0;
        }
        if (leftTime == null) {
            return 1;
        }
        if (rightTime == null) {
            return -1;
        }
        return rightTime.compareTo(leftTime);
    }

    private static class RepairHistorySummary {

        private final Long repairCount;

        private final LocalDateTime lastRepairTime;

        RepairHistorySummary(Long repairCount, LocalDateTime lastRepairTime) {
            this.repairCount = repairCount == null ? 0L : repairCount;
            this.lastRepairTime = lastRepairTime;
        }

        Long getRepairCount() {
            return repairCount;
        }

        LocalDateTime getLastRepairTime() {
            return lastRepairTime;
        }
    }

    private int compareCompanyIdentity(String leftName, String rightName, Long leftId, Long rightId) {
        String safeLeftName = leftName == null ? "" : leftName;
        String safeRightName = rightName == null ? "" : rightName;
        int compareResult = safeLeftName.compareTo(safeRightName);
        if (compareResult != 0) {
            return compareResult;
        }
        long safeLeftId = leftId == null ? Long.MAX_VALUE : leftId;
        long safeRightId = rightId == null ? Long.MAX_VALUE : rightId;
        return Long.compare(safeLeftId, safeRightId);
    }

    private Map<Long, String> buildUserNameMap(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> validUserIds = userIds.stream().filter(id -> id != null).collect(Collectors.toList());
        if (validUserIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SysUser> users = sysUserMapper.selectBatchIds(validUserIds);
        Map<Long, String> map = new HashMap<>(users.size());
        for (SysUser user : users) {
            if (user != null) {
                map.put(user.getId(), user.getRealName());
            }
        }
        return map;
    }

    private List<WorkOrderQuoteVO> listQuoteVos(Long workOrderId) {
        LambdaQueryWrapper<WorkOrderQuote> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkOrderQuote::getWorkOrderId, workOrderId)
                .orderByDesc(WorkOrderQuote::getCreateTime);
        List<WorkOrderQuote> quotes = workOrderQuoteMapper.selectList(wrapper);
        if (quotes.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, String> companyNameMap = buildCompanyNameMap(
                quotes.stream().map(WorkOrderQuote::getCompanyId).collect(Collectors.toSet())
        );
        Map<Long, String> userNameMap = buildUserNameMap(
                quotes.stream().map(WorkOrderQuote::getQuotedBy).collect(Collectors.toSet())
        );
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
        LambdaQueryWrapper<WorkOrderRepair> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkOrderRepair::getWorkOrderId, workOrderId)
                .orderByDesc(WorkOrderRepair::getCreateTime);
        List<WorkOrderRepair> repairs = workOrderRepairMapper.selectList(wrapper);
        if (repairs.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, String> companyNameMap = buildCompanyNameMap(
                repairs.stream().map(WorkOrderRepair::getCompanyId).collect(Collectors.toSet())
        );
        Map<Long, String> userNameMap = buildUserNameMap(
                repairs.stream().map(WorkOrderRepair::getRepairUserId).collect(Collectors.toSet())
        );
        Set<Long> repairIds = repairs.stream().map(WorkOrderRepair::getId).collect(Collectors.toSet());
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
            vo.setRegisterStageLabel(REGISTER_STAGE_RECHECK.equals(repair.getRegisterStage()) ? "复检登记" : "维修登记");
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
        if (repairIds == null || repairIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<WorkOrderFault> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkOrderFault::getWorkOrderId, workOrderId)
                .in(WorkOrderFault::getRepairId, repairIds)
                .orderByAsc(WorkOrderFault::getSortNum)
                .orderByAsc(WorkOrderFault::getId);
        List<WorkOrderFault> faults = workOrderFaultMapper.selectList(wrapper);
        if (faults.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, List<WorkOrderFaultPartVO>> partMap = buildFaultPartMap(
                faults.stream().map(WorkOrderFault::getId).collect(Collectors.toCollection(LinkedHashSet::new))
        );
        Map<Long, String> userNameMap = buildUserNameMap(
                faults.stream().map(WorkOrderFault::getCreatedBy).collect(Collectors.toSet())
        );
        Map<Long, List<WorkOrderFaultVO>> result = new HashMap<>();
        for (WorkOrderFault fault : faults) {
            WorkOrderFaultVO vo = new WorkOrderFaultVO();
            vo.setId(fault.getId());
            vo.setCompanyId(fault.getCompanyId());
            vo.setFaultDesc(fault.getFaultDesc());
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

    private boolean hasFaultForEvaluation(Long workOrderId) {
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
        return !FAULT_JUDGE_NO_FAULT.equals(normalizeText(quotes.get(0).getFaultJudge()));
    }

    private static final class CustomerFaultSelection {

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

    /**
     * 按品牌类型和条码场景收口品牌编码。
     *
     * @param brandType 品牌类型
     * @param jasicBarcodeCreate 是否为佳士有码建单
     * @param barcodeArchive 条码档案
     * @param dto 建单参数
     * @return 品牌编码
     */
    private String resolveCreateBrandCode(BrandTypeEnum brandType, boolean jasicBarcodeCreate,
                                          MachineBarcode barcodeArchive, CustomerWorkOrderCreateDTO dto) {
        if (brandType == null) {
            return null;
        }
        if (brandType.isNonJasic()) {
            return null;
        }
        if (!jasicBarcodeCreate) {
            return "JASIC";
        }
        return resolveBrandCode(resolveArchiveText(
                barcodeArchive == null ? null : barcodeArchive.getBrandCode(),
                dto == null ? null : dto.getBrandCode()
        ));
    }

    /**
     * 按品牌类型收口品牌名称。
     *
     * @param brandType 品牌类型
     * @param dto 建单参数
     * @return 品牌名称
     */
    private String resolveCreateBrandName(BrandTypeEnum brandType, CustomerWorkOrderCreateDTO dto) {
        if (brandType == null || brandType.isJasic()) {
            return null;
        }
        return normalizeText(dto == null ? null : dto.getBrandName());
    }

    /**
     * 品牌编码为空时回退为空串，避免前端展示出现 null。
     *
     * @param brandCode 品牌编码
     * @return 规范化后的品牌编码
     */
    private String resolveBrandCode(String brandCode) {
        String value = normalizeText(brandCode);
        return value == null ? "JASIC" : value;
    }

    /**
     * 统一清洗并校验必填文本，避免前端只传空白字符时绕过校验。
     *
     * @param value 原始文本
     * @param message 为空时提示语
     * @return 规范化后的文本
     */
    private String normalizeRequiredText(String value, String message) {
        String text = normalizeText(value);
        if (text == null) {
            throw new ServiceException(message);
        }
        return text;
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

    /**
     * 统一清洗可空文本，空白字符按 null 处理。
     *
     * @param value 原始文本
     * @return 规范化后的文本
     */
    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }
}
