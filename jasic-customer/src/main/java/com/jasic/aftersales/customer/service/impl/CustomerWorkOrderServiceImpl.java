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
        // 说明：执行该步骤以保证业务流程正确。
        Long customerId = requireCustomerId();
        // 调用requireCustomer方法，复用统一能力并保证业务规则一致。
        CUser customer = requireCustomer(customerId);
        // 调用getBrandType方法，复用统一能力并保证业务规则一致。
        BrandTypeEnum brandType = dto == null ? null : dto.getBrandType();
        // 调用getBarcode方法，复用统一能力并保证业务规则一致。
        boolean hasBarcode = normalizeText(dto == null ? null : dto.getBarcode()) != null;
        // 调用isJasic方法，复用统一能力并保证业务规则一致。
        boolean jasicBarcodeCreate = brandType != null && brandType.isJasic() && hasBarcode;
        // 调用validateCreateRequest方法，复用统一能力并保证业务规则一致。
        validateCreateRequest(dto, brandType, hasBarcode);
        // 调用getServiceCompanyId方法，复用统一能力并保证业务规则一致。
        SysCompany serviceCompany = requireServiceCompany(dto.getServiceCompanyId());
        // 调用getBarcode方法，复用统一能力并保证业务规则一致。
        String barcode = jasicBarcodeCreate ? normalizeRequiredText(dto.getBarcode(), "机器条码不能为空") : null;
        // 调用findActiveMachineBarcode方法，复用统一能力并保证业务规则一致。
        MachineBarcode barcodeArchive = jasicBarcodeCreate ? findActiveMachineBarcode(barcode) : null;
        // 调用resolveCreateHqCompanyId方法，复用统一能力并保证业务规则一致。
        Long hqCompanyId = resolveCreateHqCompanyId(brandType, hasBarcode, serviceCompany, barcodeArchive);
        String productCode = jasicBarcodeCreate
                ? resolveArchiveText(barcodeArchive == null ? null : barcodeArchive.getProductCode(), dto.getProductCode())
                : null;
        String productModel = jasicBarcodeCreate
                ? resolveArchiveText(barcodeArchive == null ? null : barcodeArchive.getProductModel(), dto.getProductModel())
                // 调用getProductModel方法，复用统一能力并保证业务规则一致。
                : normalizeText(dto.getProductModel());
        // 调用resolveCustomerFaultSelection方法，复用统一能力并保证业务规则一致。
        CustomerFaultSelection faultSelection = resolveCustomerFaultSelection(dto, brandType, hasBarcode, hqCompanyId, productCode, productModel);

        // 调用WorkOrder方法，复用统一能力并保证业务规则一致。
        WorkOrder workOrder = new WorkOrder();
        // 调用nextOrderNo方法，复用统一能力并保证业务规则一致。
        workOrder.setOrderNo(workOrderNoGenerator.nextOrderNo());
        // 调用setCustomerId方法，复用统一能力并保证业务规则一致。
        workOrder.setCustomerId(customerId);
        // 调用resolveCustomerName方法，复用统一能力并保证业务规则一致。
        workOrder.setCustomerName(resolveCustomerName(customer));
        // 调用getPhone方法，复用统一能力并保证业务规则一致。
        workOrder.setCustomerMobile(normalizeRequiredText(customer.getPhone(), "当前客户手机号不能为空"));
        // 调用setReportSubjectType方法，复用统一能力并保证业务规则一致。
        workOrder.setReportSubjectType(WorkOrderReportSubjectConstants.CUSTOMER);
        // 调用setReportCompanyId方法，复用统一能力并保证业务规则一致。
        workOrder.setReportCompanyId(null);
        // 调用setBarcode方法，复用统一能力并保证业务规则一致。
        workOrder.setBarcode(barcode);
        // 调用setProductCode方法，复用统一能力并保证业务规则一致。
        workOrder.setProductCode(productCode);
        // 调用getProductName方法，复用统一能力并保证业务规则一致。
        workOrder.setProductName(jasicBarcodeCreate ? normalizeText(barcodeArchive == null ? null : barcodeArchive.getProductName()) : null);
        // 调用setProductModel方法，复用统一能力并保证业务规则一致。
        workOrder.setProductModel(productModel);
        // 调用getMachineNo方法，复用统一能力并保证业务规则一致。
        workOrder.setMachineNo(jasicBarcodeCreate ? normalizeText(barcodeArchive == null ? null : barcodeArchive.getMachineNo()) : null);
        // 调用setBrandType方法，复用统一能力并保证业务规则一致。
        workOrder.setBrandType(brandType);
        // 调用resolveCreateBrandCode方法，复用统一能力并保证业务规则一致。
        workOrder.setBrandCode(resolveCreateBrandCode(brandType, jasicBarcodeCreate, barcodeArchive, dto));
        // 调用resolveCreateBrandName方法，复用统一能力并保证业务规则一致。
        workOrder.setBrandName(resolveCreateBrandName(brandType, dto));
        // 调用getServiceMode方法，复用统一能力并保证业务规则一致。
        String serviceMode = normalizeServiceMode(dto.getServiceMode());
        // 调用setServiceMode方法，复用统一能力并保证业务规则一致。
        workOrder.setServiceMode(serviceMode);
        // 调用resolveBarcodeLastOutDate方法，复用统一能力并保证业务规则一致。
        workOrder.setLastOutDate(jasicBarcodeCreate ? resolveBarcodeLastOutDate(barcodeArchive) : null);
        workOrder.setWarrantyStatus(jasicBarcodeCreate
                ? resolveBarcodeWarrantyStatus(barcodeArchive, dto.getWarrantyStatus())
                // 调用getWarrantyStatus方法，复用统一能力并保证业务规则一致。
                : normalizeText(dto.getWarrantyStatus()));
        // 调用getFaultDesc方法，复用统一能力并保证业务规则一致。
        workOrder.setFaultDesc(faultSelection.getFaultDesc());
        // 调用getFaultRemark方法，复用统一能力并保证业务规则一致。
        workOrder.setFaultRemark(faultSelection.getFaultRemark());
        // 调用getSenderName方法，复用统一能力并保证业务规则一致。
        workOrder.setSenderName(resolveSendField(serviceMode, dto.getSenderName()));
        // 调用getSenderMobile方法，复用统一能力并保证业务规则一致。
        workOrder.setSenderMobile(resolveSendField(serviceMode, dto.getSenderMobile()));
        // 调用getSenderAddress方法，复用统一能力并保证业务规则一致。
        workOrder.setSenderAddress(resolveSendField(serviceMode, dto.getSenderAddress()));
        // 调用getSendExpressNo方法，复用统一能力并保证业务规则一致。
        workOrder.setSendExpressNo(resolveSendField(serviceMode, dto.getSendExpressNo()));
        // 调用afterCreate方法，复用统一能力并保证业务规则一致。
        workOrder.setMainStatus(WorkOrderStatusFlow.afterCreate());
        // 调用afterCreateEvaluateStatus方法，复用统一能力并保证业务规则一致。
        workOrder.setEvaluateStatus(WorkOrderStatusFlow.afterCreateEvaluateStatus());
        // 调用setCurrentAcceptSubjectType方法，复用统一能力并保证业务规则一致。
        workOrder.setCurrentAcceptSubjectType("SERVICE");
        // 调用getId方法，复用统一能力并保证业务规则一致。
        workOrder.setCurrentAcceptCompanyId(serviceCompany.getId());
        // 调用getId方法，复用统一能力并保证业务规则一致。
        workOrder.setCreateCompanyId(serviceCompany.getId());
        // 调用setHqCompanyId方法，复用统一能力并保证业务规则一致。
        workOrder.setHqCompanyId(hqCompanyId);
        // 调用resolveCreateFaultRepairConfigId方法，复用统一能力并保证业务规则一致。
        workOrder.setFaultRepairConfigId(resolveCreateFaultRepairConfigId(barcodeArchive, hqCompanyId));
        // 调用setHasTransfer方法，复用统一能力并保证业务规则一致。
        workOrder.setHasTransfer(0);
        // 调用setTransferCount方法，复用统一能力并保证业务规则一致。
        workOrder.setTransferCount(0);
        // 说明：执行该步骤以保证业务流程正确。
        workOrderMapper.insert(workOrder);
        replaceWorkOrderCreateFiles(workOrder.getId(), dto.getFaultImageFileIds(), dto.getFaultVideoFileIds(),
                // 调用getSenderVoucherFileIds方法，复用统一能力并保证业务规则一致。
                dto.getFaultVoiceFileIds(), dto.getSenderVoucherFileIds(), customerId);

        // 调用getMainStatus方法，复用统一能力并保证业务规则一致。
        saveCreateFlow(workOrder.getId(), customerId, serviceCompany.getId(), workOrder.getMainStatus());
        // 调用initParticipants方法，复用统一能力并保证业务规则一致。
        workOrderParticipantService.initParticipants(workOrder, "SERVICE");
        return workOrder.getId();
    }

    /**
     * 解析创建故障维修配置ID。
     *
     * @param barcodeArchive 参数
     * @param hqCompanyId hq Company ID
     * @return 处理结果
     */
    private Long resolveCreateFaultRepairConfigId(MachineBarcode barcodeArchive, Long hqCompanyId) {
        if (barcodeArchive == null || hqCompanyId == null || faultRepairConfigService == null) {
            return null;
        }
        return faultRepairConfigService.findEnabledConfigIdForResolvedHq(
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
        // 调用getFirstLevelTypeCodes方法，复用统一能力并保证业务规则一致。
        typeCodes.addAll(CompanyCategoryEnum.getFirstLevelTypeCodes());
        // 调用getSecondLevelTypeCodes方法，复用统一能力并保证业务规则一致。
        typeCodes.addAll(CompanyCategoryEnum.getSecondLevelTypeCodes());
        if (typeCodes.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<SysCompany> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysCompany::getTypeCode, typeCodes)
                .eq(SysCompany::getStatus, 1)
                .orderByAsc(SysCompany::getCompanyName)
                // 调用orderByAsc方法，复用统一能力并保证业务规则一致。
                .orderByAsc(SysCompany::getId);
        // 说明：执行该步骤以保证业务流程正确。
        List<SysCompany> companies = sysCompanyMapper.selectList(wrapper);
        if (companies.isEmpty()) {
            return Collections.emptyList();
        }
        // 调用size方法，复用统一能力并保证业务规则一致。
        List<CustomerServiceCompanyOptionVO> result = new ArrayList<>(companies.size());
        for (SysCompany company : companies) {
            // 调用buildServiceCompanyOption方法，复用统一能力并保证业务规则一致。
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
        // 说明：执行该步骤以保证业务流程正确。
        validateCoordinate(longitude, latitude);
        // 调用requireCustomerId方法，复用统一能力并保证业务规则一致。
        Long customerId = requireCustomerId();
        // 调用normalizeNearbyLimit方法，复用统一能力并保证业务规则一致。
        int normalizedLimit = normalizeNearbyLimit(limit);
        // 调用listNearbyEnabledServiceCompanies方法，复用统一能力并保证业务规则一致。
        List<SysCompany> companies = listNearbyEnabledServiceCompanies();
        if (companies.isEmpty()) {
            return Collections.emptyList();
        }
        // 调用buildRepairHistoryMap方法，复用统一能力并保证业务规则一致。
        Map<Long, RepairHistorySummary> repairHistoryMap = buildRepairHistoryMap(customerId, companies);
        List<CustomerNearbyServiceCompanyVO> options = companies.stream()
                .map(company -> buildNearbyServiceCompanyOption(company, longitude, latitude))
                .peek(option -> option.setHasRepairHistory(repairHistoryMap.containsKey(option.getId())))
                .sorted((left, right) -> compareNearbyServiceCompany(left, right, repairHistoryMap))
                .limit(normalizedLimit)
                // 调用toList方法，复用统一能力并保证业务规则一致。
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
        // 调用normalizeRequiredText方法，复用统一能力并保证业务规则一致。
        String normalizedBarcode = normalizeRequiredText(barcode, "机器条码不能为空");
        // 说明：执行该步骤以保证业务流程正确。
        MachineBarcode barcodeArchive = requireActiveMachineBarcode(normalizedBarcode);
        // 调用getHqCompanyId方法，复用统一能力并保证业务规则一致。
        SysCompany hqCompany = requireHqCompany(barcodeArchive.getHqCompanyId());
        // 调用CustomerBarcodeInfoVO方法，复用统一能力并保证业务规则一致。
        CustomerBarcodeInfoVO vo = new CustomerBarcodeInfoVO();
        // 调用getBarcode方法，复用统一能力并保证业务规则一致。
        vo.setBarcode(barcodeArchive.getBarcode());
        // 调用getProductCode方法，复用统一能力并保证业务规则一致。
        vo.setProductCode(normalizeText(barcodeArchive.getProductCode()));
        // 调用getProductName方法，复用统一能力并保证业务规则一致。
        vo.setProductName(normalizeText(barcodeArchive.getProductName()));
        // 调用getProductModel方法，复用统一能力并保证业务规则一致。
        vo.setProductModel(normalizeText(barcodeArchive.getProductModel()));
        // 调用getMachineNo方法，复用统一能力并保证业务规则一致。
        vo.setMachineNo(normalizeText(barcodeArchive.getMachineNo()));
        // 调用getBrandCode方法，复用统一能力并保证业务规则一致。
        vo.setBrandCode(resolveBrandCode(barcodeArchive.getBrandCode()));
        // 调用resolveBarcodeLastOutDate方法，复用统一能力并保证业务规则一致。
        vo.setLastOutDate(resolveBarcodeLastOutDate(barcodeArchive));
        // 调用resolveBarcodeWarrantyStatus方法，复用统一能力并保证业务规则一致。
        vo.setWarrantyStatus(resolveBarcodeWarrantyStatus(barcodeArchive, null));
        // 调用getId方法，复用统一能力并保证业务规则一致。
        vo.setHqCompanyId(hqCompany.getId());
        // 调用getCompanyName方法，复用统一能力并保证业务规则一致。
        vo.setHqCompanyName(hqCompany.getCompanyName());
        vo.setFaultOptions(buildCustomerFaultOptions(
                hqCompany.getId(),
                barcodeArchive.getProductCode(),
                barcodeArchive.getProductModel()
        ));
        // 调用setOtherFaultLabel方法，复用统一能力并保证业务规则一致。
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
        // 说明：执行该步骤以保证业务流程正确。
        Long customerId = requireCustomerId();
        // 调用getPageSize方法，复用统一能力并保证业务规则一致。
        Page<WorkOrder> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<WorkOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkOrder::getCustomerId, customerId)
                // 调用orderByDesc方法，复用统一能力并保证业务规则一致。
                .orderByDesc(WorkOrder::getCreateTime);
        // 调用getTabStatus方法，复用统一能力并保证业务规则一致。
        applyTabStatusFilter(wrapper, query.getTabStatus());
        // 说明：执行该步骤以保证业务流程正确。
        Page<WorkOrder> result = workOrderMapper.selectPage(page, wrapper);
        // 调用getRecords方法，复用统一能力并保证业务规则一致。
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
                // 调用toList方法，复用统一能力并保证业务规则一致。
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
        // 说明：执行该步骤以保证业务流程正确。
        Long customerId = requireCustomerId();
        // 调用findLatestUnclosedWorkOrder方法，复用统一能力并保证业务规则一致。
        WorkOrder workOrder = findLatestUnclosedWorkOrder(customerId);
        if (workOrder == null) {
            // 调用findLatestWorkOrder方法，复用统一能力并保证业务规则一致。
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
        // 说明：执行该步骤以保证业务流程正确。
        Long customerId = requireCustomerId();
        // 调用CustomerWorkOrderStatusCountVO方法，复用统一能力并保证业务规则一致。
        CustomerWorkOrderStatusCountVO vo = new CustomerWorkOrderStatusCountVO();
        // 调用countByStatuses方法，复用统一能力并保证业务规则一致。
        vo.setAllCount(countByStatuses(customerId));
        vo.setWaitAcceptCount(countByStatuses(customerId,
                WorkOrderStatusConstants.MainStatus.PENDING_ASSIGN,
                WorkOrderStatusConstants.MainStatus.PENDING_TECH_ACCEPT));
        // 调用countByStatuses方法，复用统一能力并保证业务规则一致。
        vo.setInProgressCount(countByStatuses(customerId, WorkOrderStatusConstants.MainStatus.IN_PROGRESS));
        // 调用countByStatuses方法，复用统一能力并保证业务规则一致。
        vo.setCompletedCount(countByStatuses(customerId, WorkOrderStatusConstants.MainStatus.COMPLETED));
        // 调用countByStatuses方法，复用统一能力并保证业务规则一致。
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
        // 说明：执行该步骤以保证业务流程正确。
        Long customerId = requireCustomerId();
        // 调用requireCustomerWorkOrder方法，复用统一能力并保证业务规则一致。
        WorkOrder workOrder = requireCustomerWorkOrder(workOrderId, customerId);
        // 调用CustomerWorkOrderDetailVO方法，复用统一能力并保证业务规则一致。
        CustomerWorkOrderDetailVO detail = new CustomerWorkOrderDetailVO();
        // 调用getId方法，复用统一能力并保证业务规则一致。
        detail.setId(workOrder.getId());
        // 调用getOrderNo方法，复用统一能力并保证业务规则一致。
        detail.setOrderNo(workOrder.getOrderNo());
        // 调用getCustomerId方法，复用统一能力并保证业务规则一致。
        detail.setCustomerId(workOrder.getCustomerId());
        // 调用getCustomerName方法，复用统一能力并保证业务规则一致。
        detail.setCustomerName(workOrder.getCustomerName());
        // 调用getCustomerMobile方法，复用统一能力并保证业务规则一致。
        detail.setCustomerMobile(workOrder.getCustomerMobile());
        // 调用getBarcode方法，复用统一能力并保证业务规则一致。
        detail.setBarcode(workOrder.getBarcode());
        // 调用getProductCode方法，复用统一能力并保证业务规则一致。
        detail.setProductCode(workOrder.getProductCode());
        // 调用getProductName方法，复用统一能力并保证业务规则一致。
        detail.setProductName(workOrder.getProductName());
        // 调用getProductModel方法，复用统一能力并保证业务规则一致。
        detail.setProductModel(workOrder.getProductModel());
        // 调用getMachineNo方法，复用统一能力并保证业务规则一致。
        detail.setMachineNo(workOrder.getMachineNo());
        // 调用getBrandType方法，复用统一能力并保证业务规则一致。
        detail.setBrandType(workOrder.getBrandType());
        // 调用getLabel方法，复用统一能力并保证业务规则一致。
        detail.setBrandTypeLabel(workOrder.getBrandType() == null ? null : workOrder.getBrandType().getLabel());
        // 调用getBrandCode方法，复用统一能力并保证业务规则一致。
        detail.setBrandCode(workOrder.getBrandCode());
        // 调用getBrandName方法，复用统一能力并保证业务规则一致。
        detail.setBrandName(workOrder.getBrandName());
        // 调用getServiceMode方法，复用统一能力并保证业务规则一致。
        detail.setServiceMode(workOrder.getServiceMode());
        // 调用getServiceMode方法，复用统一能力并保证业务规则一致。
        detail.setServiceModeLabel(ServiceModeEnum.resolveLabel(workOrder.getServiceMode()));
        // 调用getLastOutDate方法，复用统一能力并保证业务规则一致。
        detail.setLastOutDate(workOrder.getLastOutDate());
        // 调用getWarrantyStatus方法，复用统一能力并保证业务规则一致。
        detail.setWarrantyStatus(workOrder.getWarrantyStatus());
        // 调用getFaultDesc方法，复用统一能力并保证业务规则一致。
        detail.setFaultDesc(workOrder.getFaultDesc());
        // 调用getFaultRemark方法，复用统一能力并保证业务规则一致。
        detail.setFaultRemark(workOrder.getFaultRemark());
        // 调用getSenderName方法，复用统一能力并保证业务规则一致。
        detail.setSenderName(workOrder.getSenderName());
        // 调用getSenderMobile方法，复用统一能力并保证业务规则一致。
        detail.setSenderMobile(workOrder.getSenderMobile());
        // 调用getSenderAddress方法，复用统一能力并保证业务规则一致。
        detail.setSenderAddress(workOrder.getSenderAddress());
        // 调用getSendExpressNo方法，复用统一能力并保证业务规则一致。
        detail.setSendExpressNo(workOrder.getSendExpressNo());
        // 调用getMainStatus方法，复用统一能力并保证业务规则一致。
        detail.setMainStatus(workOrder.getMainStatus());
        // 调用getMainStatus方法，复用统一能力并保证业务规则一致。
        detail.setDisplayStatus(resolveCustomerDisplayStatus(workOrder.getMainStatus()));
        // 调用getEvaluateStatus方法，复用统一能力并保证业务规则一致。
        detail.setEvaluateStatus(workOrder.getEvaluateStatus());
        // 调用getEvaluateStatus方法，复用统一能力并保证业务规则一致。
        detail.setEvaluateStatusLabel(WorkOrderStatusConstants.resolveEvaluateStatusLabel(workOrder.getEvaluateStatus()));
        // 调用getHqCompanyId方法，复用统一能力并保证业务规则一致。
        detail.setHqCompanyId(workOrder.getHqCompanyId());
        // 调用getReturnMethod方法，复用统一能力并保证业务规则一致。
        detail.setReturnMethod(workOrder.getReturnMethod());
        // 调用getReturnExpressNo方法，复用统一能力并保证业务规则一致。
        detail.setReturnExpressNo(workOrder.getReturnExpressNo());
        // 调用getCloseReason方法，复用统一能力并保证业务规则一致。
        detail.setCloseReason(workOrder.getCloseReason());
        // 调用canEvaluate方法，复用统一能力并保证业务规则一致。
        detail.setCanEvaluate(canEvaluate(workOrder));
        // 调用canEditSendInfo方法，复用统一能力并保证业务规则一致。
        detail.setCanEditSendInfo(canEditSendInfo(workOrder));
        // 调用getCompletedTime方法，复用统一能力并保证业务规则一致。
        detail.setCompletedTime(workOrder.getCompletedTime());
        // 调用getClosedTime方法，复用统一能力并保证业务规则一致。
        detail.setClosedTime(workOrder.getClosedTime());
        // 调用getCreateTime方法，复用统一能力并保证业务规则一致。
        detail.setCreateTime(workOrder.getCreateTime());
        // 调用buildWorkOrderFileMap方法，复用统一能力并保证业务规则一致。
        fillAttachmentDetail(detail, buildWorkOrderFileMap(workOrderId));

        // 调用getCurrentAcceptCompanyId方法，复用统一能力并保证业务规则一致。
        Map<Long, SysCompany> companyMap = buildCompanyMap(Collections.singleton(workOrder.getCurrentAcceptCompanyId()));
        // 调用getAssignedUserId方法，复用统一能力并保证业务规则一致。
        Map<Long, String> userNameMap = buildUserNameMap(Collections.singleton(workOrder.getAssignedUserId()));
        // 调用getCurrentAcceptCompanyId方法，复用统一能力并保证业务规则一致。
        SysCompany currentAcceptCompany = companyMap.get(workOrder.getCurrentAcceptCompanyId());
        // 调用getCompanyName方法，复用统一能力并保证业务规则一致。
        detail.setCurrentAcceptCompanyName(currentAcceptCompany == null ? null : currentAcceptCompany.getCompanyName());
        // 调用getContactPhone方法，复用统一能力并保证业务规则一致。
        detail.setCurrentAcceptCompanyPhone(currentAcceptCompany == null ? null : currentAcceptCompany.getContactPhone());
        // 调用getAssignedUserId方法，复用统一能力并保证业务规则一致。
        detail.setAssignedUserName(userNameMap.get(workOrder.getAssignedUserId()));
        // 调用listQuoteVos方法，复用统一能力并保证业务规则一致。
        detail.setQuotes(listQuoteVos(workOrderId));
        // 调用listRepairVos方法，复用统一能力并保证业务规则一致。
        detail.setRepairs(listRepairVos(workOrderId));
        // 调用getEvaluationVo方法，复用统一能力并保证业务规则一致。
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
        // 说明：执行该步骤以保证业务流程正确。
        Long customerId = requireCustomerId();
        // 调用getWorkOrderId方法，复用统一能力并保证业务规则一致。
        WorkOrder workOrder = requireCustomerWorkOrder(dto.getWorkOrderId(), customerId);
        if (!canEditSendInfo(workOrder)) {
            throw new ServiceException("当前工单不允许修改寄修信息");
        }
        // 调用getSenderName方法，复用统一能力并保证业务规则一致。
        workOrder.setSenderName(normalizeText(dto.getSenderName()));
        // 调用getSenderMobile方法，复用统一能力并保证业务规则一致。
        workOrder.setSenderMobile(normalizeText(dto.getSenderMobile()));
        // 调用getSenderAddress方法，复用统一能力并保证业务规则一致。
        workOrder.setSenderAddress(normalizeText(dto.getSenderAddress()));
        // 调用getSendExpressNo方法，复用统一能力并保证业务规则一致。
        workOrder.setSendExpressNo(normalizeText(dto.getSendExpressNo()));
        // 说明：执行该步骤以保证业务流程正确。
        workOrderMapper.updateById(workOrder);
        // 说明：执行该步骤以保证业务流程正确。
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
        // 说明：执行该步骤以保证业务流程正确。
        Long customerId = requireCustomerId();
        // 调用getWorkOrderId方法，复用统一能力并保证业务规则一致。
        WorkOrder workOrder = requireCustomerWorkOrder(dto.getWorkOrderId(), customerId);
        if (!canEditSendInfo(workOrder)) {
            throw new ServiceException("当前工单不允许上传寄件凭证");
        }
        if (hasSenderVoucher(workOrder.getId())) {
            throw new ServiceException("当前工单已上传寄件凭证");
        }
        // 说明：执行该步骤以保证业务流程正确。
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
        // 说明：执行该步骤以保证业务流程正确。
        Long customerId = requireCustomerId();
        // 调用getWorkOrderId方法，复用统一能力并保证业务规则一致。
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
        // 调用getId方法，复用统一能力并保证业务规则一致。
        wrapper.eq(WorkOrderEvaluation::getWorkOrderId, workOrder.getId());
        // 说明：执行该步骤以保证业务流程正确。
        if (workOrderEvaluationMapper.selectCount(wrapper) > 0) {
            throw new ServiceException("当前工单已完成评价");
        }

        // 调用WorkOrderEvaluation方法，复用统一能力并保证业务规则一致。
        WorkOrderEvaluation evaluation = new WorkOrderEvaluation();
        // 调用getId方法，复用统一能力并保证业务规则一致。
        evaluation.setWorkOrderId(workOrder.getId());
        // 调用setCustomerId方法，复用统一能力并保证业务规则一致。
        evaluation.setCustomerId(customerId);
        // 调用getCurrentAcceptCompanyId方法，复用统一能力并保证业务规则一致。
        evaluation.setCompanyId(workOrder.getCurrentAcceptCompanyId());
        // 调用getTimelinessScore方法，复用统一能力并保证业务规则一致。
        evaluation.setTimelinessScore(dto.getTimelinessScore());
        // 调用getQualityScore方法，复用统一能力并保证业务规则一致。
        evaluation.setQualityScore(dto.getQualityScore());
        // 调用getSatisfactionScore方法，复用统一能力并保证业务规则一致。
        evaluation.setSatisfactionScore(dto.getSatisfactionScore());
        // 调用getTags方法，复用统一能力并保证业务规则一致。
        evaluation.setTags(dto.getTags());
        // 调用getContent方法，复用统一能力并保证业务规则一致。
        evaluation.setContent(dto.getContent());
        // 说明：执行该步骤以保证业务流程正确。
        workOrderEvaluationMapper.insert(evaluation);

        // 调用afterEvaluate方法，复用统一能力并保证业务规则一致。
        workOrder.setEvaluateStatus(WorkOrderStatusFlow.afterEvaluate());
        // 调用updateById方法，复用统一能力并保证业务规则一致。
        workOrderMapper.updateById(workOrder);

        // 调用WorkOrderFlow方法，复用统一能力并保证业务规则一致。
        WorkOrderFlow flow = new WorkOrderFlow();
        // 调用getId方法，复用统一能力并保证业务规则一致。
        flow.setWorkOrderId(workOrder.getId());
        // 调用setActionType方法，复用统一能力并保证业务规则一致。
        flow.setActionType("EVALUATE");
        // 调用getMainStatus方法，复用统一能力并保证业务规则一致。
        flow.setBeforeStatus(workOrder.getMainStatus());
        // 调用getMainStatus方法，复用统一能力并保证业务规则一致。
        flow.setAfterStatus(workOrder.getMainStatus());
        // 调用getCurrentAcceptCompanyId方法，复用统一能力并保证业务规则一致。
        flow.setFromCompanyId(workOrder.getCurrentAcceptCompanyId());
        // 调用getCurrentAcceptCompanyId方法，复用统一能力并保证业务规则一致。
        flow.setToCompanyId(workOrder.getCurrentAcceptCompanyId());
        // 调用getCurrentAcceptCompanyId方法，复用统一能力并保证业务规则一致。
        flow.setOperatorCompanyId(workOrder.getCurrentAcceptCompanyId());
        // 调用setOperatorUserId方法，复用统一能力并保证业务规则一致。
        flow.setOperatorUserId(customerId);
        // 调用getContent方法，复用统一能力并保证业务规则一致。
        flow.setRemark(dto.getContent());
        // 调用insert方法，复用统一能力并保证业务规则一致。
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
        // 说明：执行该步骤以保证业务流程正确。
        return sysFileService.listBizFileMap(WORK_ORDER_FILE_BIZ_TYPES, workOrderId);
    }

    /**
     * 构建维修文件Map。
     *
     * @param repairId repair ID
     * @return 处理结果
     */
    private Map<SysFileBizTypeEnum, List<SysFileItemVO>> buildRepairFileMap(Long repairId) {
        if (repairId == null) {
            return Collections.emptyMap();
        }
        // 说明：执行该步骤以保证业务流程正确。
        return sysFileService.listBizFileMap(WORK_ORDER_REPAIR_FILE_BIZ_TYPES, repairId);
    }

    /**
     * fillAttachment详情。
     *
     * @param detail 参数
     * @param fileMap 参数
     */
    private void fillAttachmentDetail(CustomerWorkOrderDetailVO detail,
                                      Map<SysFileBizTypeEnum, List<SysFileItemVO>> fileMap) {
        if (detail == null) {
            return;
        }
        // 调用emptyMap方法，复用统一能力并保证业务规则一致。
        Map<SysFileBizTypeEnum, List<SysFileItemVO>> safeFileMap = fileMap == null ? Collections.emptyMap() : fileMap;
        // 调用emptyList方法，复用统一能力并保证业务规则一致。
        detail.setFaultImageFiles(safeFileMap.getOrDefault(SysFileBizTypeEnum.WORK_ORDER_FAULT_IMAGE, Collections.emptyList()));
        // 调用emptyList方法，复用统一能力并保证业务规则一致。
        detail.setFaultVideoFiles(safeFileMap.getOrDefault(SysFileBizTypeEnum.WORK_ORDER_FAULT_VIDEO, Collections.emptyList()));
        // 调用emptyList方法，复用统一能力并保证业务规则一致。
        detail.setFaultVoiceFiles(safeFileMap.getOrDefault(SysFileBizTypeEnum.WORK_ORDER_FAULT_VOICE, Collections.emptyList()));
        // 调用emptyList方法，复用统一能力并保证业务规则一致。
        detail.setSenderVoucherFiles(safeFileMap.getOrDefault(SysFileBizTypeEnum.WORK_ORDER_SENDER_VOUCHER, Collections.emptyList()));
        // 调用emptyList方法，复用统一能力并保证业务规则一致。
        detail.setReturnVoucherFiles(safeFileMap.getOrDefault(SysFileBizTypeEnum.WORK_ORDER_RETURN_VOUCHER, Collections.emptyList()));
    }

    /**
     * fill维修Attachment详情。
     *
     * @param repair 参数
     * @param fileMap 参数
     */
    private void fillRepairAttachmentDetail(WorkOrderRepairVO repair,
                                            Map<SysFileBizTypeEnum, List<SysFileItemVO>> fileMap) {
        if (repair == null) {
            return;
        }
        // 调用emptyMap方法，复用统一能力并保证业务规则一致。
        Map<SysFileBizTypeEnum, List<SysFileItemVO>> safeFileMap = fileMap == null ? Collections.emptyMap() : fileMap;
        // 调用emptyList方法，复用统一能力并保证业务规则一致。
        repair.setFaultOldImageFiles(safeFileMap.getOrDefault(SysFileBizTypeEnum.WORK_ORDER_REPAIR_OLD_IMAGE, Collections.emptyList()));
        // 调用emptyList方法，复用统一能力并保证业务规则一致。
        repair.setFaultNewImageFiles(safeFileMap.getOrDefault(SysFileBizTypeEnum.WORK_ORDER_REPAIR_NEW_IMAGE, Collections.emptyList()));
        // 调用emptyList方法，复用统一能力并保证业务规则一致。
        repair.setMachineImageFiles(safeFileMap.getOrDefault(SysFileBizTypeEnum.WORK_ORDER_REPAIR_MACHINE_IMAGE, Collections.emptyList()));
        // 调用emptyList方法，复用统一能力并保证业务规则一致。
        repair.setMachineBarcodeImageFiles(safeFileMap.getOrDefault(SysFileBizTypeEnum.WORK_ORDER_REPAIR_BARCODE_IMAGE, Collections.emptyList()));
        // 调用emptyList方法，复用统一能力并保证业务规则一致。
        repair.setOtherImageFiles(safeFileMap.getOrDefault(SysFileBizTypeEnum.WORK_ORDER_REPAIR_OTHER_IMAGE, Collections.emptyList()));
    }

    /**
     * 替换工单创建文件。
     *
     * @param customerId customer ID
     */
    private void replaceWorkOrderCreateFiles(Long workOrderId, List<Long> faultImageFileIds, List<Long> faultVideoFileIds,
                                             List<Long> faultVoiceFileIds, List<Long> senderVoucherFileIds,
                                             Long customerId) {
        // 说明：执行该步骤以保证业务流程正确。
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
        // 调用checkLogin方法，复用统一能力并保证业务规则一致。
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
        // 说明：执行该步骤以保证业务流程正确。
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
        // 说明：执行该步骤以保证业务流程正确。
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
        // 调用getServiceMode方法，复用统一能力并保证业务规则一致。
        normalizeServiceMode(dto.getServiceMode());
        if (brandType.isJasic() && hasBarcode) {
            // 调用getBarcode方法，复用统一能力并保证业务规则一致。
            normalizeRequiredText(dto.getBarcode(), "机器条码不能为空");
        }
        if (brandType.isNonJasic() && hasBarcode) {
            throw new ServiceException("非佳士报修不支持填写机器条码");
        }
        // 说明：执行该步骤以保证业务流程正确。
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
        // 说明：执行该步骤以保证业务流程正确。
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
        // 说明：执行该步骤以保证业务流程正确。
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
                // 调用getValueByKey方法，复用统一能力并保证业务规则一致。
                : sysConfigService.getValueByKey(WorkOrderConfigConstants.DEFAULT_HQ_COMPANY_ID));
        if (configValue == null) {
            throw new ServiceException("默认归属总部未配置");
        }
        Long hqCompanyId;
        try {
            // 调用valueOf方法，复用统一能力并保证业务规则一致。
            hqCompanyId = Long.valueOf(configValue);
        } catch (NumberFormatException ex) {
            throw new ServiceException("默认归属总部配置不正确");
        }
        try {
            // 说明：执行该步骤以保证业务流程正确。
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
        // 调用resolveBarcodeArchiveHqCompanyId方法，复用统一能力并保证业务规则一致。
        Long archiveHqCompanyId = resolveBarcodeArchiveHqCompanyId(barcodeArchive);
        if (archiveHqCompanyId != null) {
            return archiveHqCompanyId;
        }
        // 调用resolveFirstCompanyIds方法，复用统一能力并保证业务规则一致。
        List<Long> firstCompanyIds = resolveFirstCompanyIds(serviceCompany);
        if (firstCompanyIds.isEmpty()) {
            throw new ServiceException("当前服务网点暂未关联可用总部，无法提交报修单");
        }
        // 调用resolveActiveHqCompanyIds方法，复用统一能力并保证业务规则一致。
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
        // 说明：执行该步骤以保证业务流程正确。
        return requireHqCompany(barcodeArchive.getHqCompanyId()).getId();
    }

    /**
     * 条码报修时要求条码档案存在且状态正常。
     *
     * @param barcode 机器条码
     * @return 条码档案
     */
    private MachineBarcode requireActiveMachineBarcode(String barcode) {
        // 调用findActiveMachineBarcode方法，复用统一能力并保证业务规则一致。
        MachineBarcode barcodeArchive = findActiveMachineBarcode(barcode);
        if (barcodeArchive == null) {
            throw new ServiceException("当前条码未维护档案信息");
        }
        return barcodeArchive;
    }

    /**
     * findActive机器条码。
     *
     * @param barcode 参数
     * @return 处理结果
     */
    private MachineBarcode findActiveMachineBarcode(String barcode) {
        // 调用normalizeText方法，复用统一能力并保证业务规则一致。
        String normalizedBarcode = normalizeText(barcode);
        if (normalizedBarcode == null) {
            return null;
        }
        LambdaQueryWrapper<MachineBarcode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MachineBarcode::getBarcode, normalizedBarcode)
                .eq(MachineBarcode::getStatus, 1)
                // 调用last方法，复用统一能力并保证业务规则一致。
                .last("LIMIT 1");
        // 说明：执行该步骤以保证业务流程正确。
        return machineBarcodeMapper.selectOne(wrapper);
    }

    /**
     * 解析条码保修状态。
     *
     * @param barcodeArchive 参数
     * @param fallbackStatus 参数
     * @return 处理结果
     */
    private String resolveBarcodeWarrantyStatus(MachineBarcode barcodeArchive, String fallbackStatus) {
        return MachineBarcodeWarrantyResolver.resolveWarrantyStatus(
                barcodeArchive == null ? null : barcodeArchive.getBarcode(),
                barcodeArchive == null ? null : barcodeArchive.getLastOutDate(),
                barcodeArchive == null ? null : barcodeArchive.getScanDate(),
                normalizeText(fallbackStatus)
        );
    }

    /**
     * 解析条码LastOutDate。
     *
     * @param barcodeArchive 参数
     * @return 处理结果
     */
    private LocalDateTime resolveBarcodeLastOutDate(MachineBarcode barcodeArchive) {
        return MachineBarcodeWarrantyResolver.resolveLastOutDate(barcodeArchive);
    }

    /**
     * 解析一级公司Ids。
     *
     * @param serviceCompany 参数
     * @return 处理结果
     */
    private List<Long> resolveFirstCompanyIds(SysCompany serviceCompany) {
        if ("SITE_FIRST".equals(serviceCompany.getTypeCode())) {
            return Collections.singletonList(serviceCompany.getId());
        }
        LambdaQueryWrapper<FirstSecondRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FirstSecondRelation::getSecondCompanyId, serviceCompany.getId())
                // 调用eq方法，复用统一能力并保证业务规则一致。
                .eq(FirstSecondRelation::getStatus, 1);
        // 说明：执行该步骤以保证业务流程正确。
        List<FirstSecondRelation> relations = firstSecondRelationMapper.selectList(wrapper);
        if (relations.isEmpty()) {
            return Collections.emptyList();
        }
        return relations.stream()
                .map(FirstSecondRelation::getFirstCompanyId)
                .filter(id -> id != null)
                .distinct()
                // 调用toList方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toList());
    }

    /**
     * 解析Active总部公司Ids。
     *
     * @return 处理结果
     */
    private List<Long> resolveActiveHqCompanyIds(List<Long> firstCompanyIds) {
        if (firstCompanyIds == null || firstCompanyIds.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<HqFirstContract> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(HqFirstContract::getFirstCompanyId, firstCompanyIds)
                // 调用eq方法，复用统一能力并保证业务规则一致。
                .eq(HqFirstContract::getStatus, 1);
        // 说明：执行该步骤以保证业务流程正确。
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
                // 调用toList方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toList());
    }

    /**
     * 分页查询Active服务Companies列表。
     *
     * @return 处理结果
     */
    private List<SysCompany> listActiveServiceCompanies() {
        Set<String> typeCodes = new LinkedHashSet<>();
        // 调用getFirstLevelTypeCodes方法，复用统一能力并保证业务规则一致。
        typeCodes.addAll(CompanyCategoryEnum.getFirstLevelTypeCodes());
        // 调用getSecondLevelTypeCodes方法，复用统一能力并保证业务规则一致。
        typeCodes.addAll(CompanyCategoryEnum.getSecondLevelTypeCodes());
        if (typeCodes.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<SysCompany> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysCompany::getTypeCode, typeCodes)
                .eq(SysCompany::getStatus, 1)
                .orderByAsc(SysCompany::getCompanyName)
                // 调用orderByAsc方法，复用统一能力并保证业务规则一致。
                .orderByAsc(SysCompany::getId);
        // 说明：执行该步骤以保证业务流程正确。
        return sysCompanyMapper.selectList(wrapper);
    }

    /**
     * 分页查询NearbyEnabled服务Companies列表。
     *
     * @return 处理结果
     */
    private List<SysCompany> listNearbyEnabledServiceCompanies() {
        Set<String> typeCodes = new LinkedHashSet<>();
        // 调用getFirstLevelTypeCodes方法，复用统一能力并保证业务规则一致。
        typeCodes.addAll(CompanyCategoryEnum.getFirstLevelTypeCodes());
        // 调用getSecondLevelTypeCodes方法，复用统一能力并保证业务规则一致。
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
                // 调用orderByAsc方法，复用统一能力并保证业务规则一致。
                .orderByAsc(SysCompany::getId);
        // 说明：执行该步骤以保证业务流程正确。
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
        // 调用WorkOrderFlow方法，复用统一能力并保证业务规则一致。
        WorkOrderFlow flow = new WorkOrderFlow();
        // 调用setWorkOrderId方法，复用统一能力并保证业务规则一致。
        flow.setWorkOrderId(workOrderId);
        // 调用setActionType方法，复用统一能力并保证业务规则一致。
        flow.setActionType("CREATE");
        // 调用setBeforeStatus方法，复用统一能力并保证业务规则一致。
        flow.setBeforeStatus(null);
        // 调用setAfterStatus方法，复用统一能力并保证业务规则一致。
        flow.setAfterStatus(afterStatus);
        // 调用setFromCompanyId方法，复用统一能力并保证业务规则一致。
        flow.setFromCompanyId(null);
        // 调用setToCompanyId方法，复用统一能力并保证业务规则一致。
        flow.setToCompanyId(serviceCompanyId);
        // 调用setOperatorCompanyId方法，复用统一能力并保证业务规则一致。
        flow.setOperatorCompanyId(serviceCompanyId);
        // 调用setOperatorUserId方法，复用统一能力并保证业务规则一致。
        flow.setOperatorUserId(customerId);
        // 调用setRemark方法，复用统一能力并保证业务规则一致。
        flow.setRemark("客户提交报修");
        // 说明：执行该步骤以保证业务流程正确。
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
            // 调用eq方法，复用统一能力并保证业务规则一致。
            wrapper.eq(WorkOrder::getMainStatus, WorkOrderStatusConstants.MainStatus.IN_PROGRESS);
            return;
        }
        if (WorkOrderStatusConstants.DisplayStatus.COMPLETED.equals(tabStatus)) {
            // 调用eq方法，复用统一能力并保证业务规则一致。
            wrapper.eq(WorkOrder::getMainStatus, WorkOrderStatusConstants.MainStatus.COMPLETED);
            return;
        }
        if (WorkOrderStatusConstants.DisplayStatus.CLOSED.equals(tabStatus)) {
            // 调用eq方法，复用统一能力并保证业务规则一致。
            wrapper.eq(WorkOrder::getMainStatus, WorkOrderStatusConstants.MainStatus.CLOSED);
        }
    }

    /**
     * countByStatuses。
     *
     * @param customerId customer ID
     * @param statuses 参数
     * @return 处理结果
     */
    private Long countByStatuses(Long customerId, String... statuses) {
        LambdaQueryWrapper<WorkOrder> wrapper = new LambdaQueryWrapper<>();
        // 调用eq方法，复用统一能力并保证业务规则一致。
        wrapper.eq(WorkOrder::getCustomerId, customerId);
        if (statuses != null && statuses.length > 0) {
            // 调用in方法，复用统一能力并保证业务规则一致。
            wrapper.in(WorkOrder::getMainStatus, (Object[]) statuses);
        }
        // 说明：执行该步骤以保证业务流程正确。
        return workOrderMapper.selectCount(wrapper);
    }

    /**
     * 构建列表Vo。
     *
     * @param workOrder 参数
     * @param companyMap 参数
     * @param userNameMap 参数
     * @param currentQuoteAmountMap 参数
     * @return 处理结果
     */
    private CustomerWorkOrderListVO buildListVo(WorkOrder workOrder, Map<Long, SysCompany> companyMap,
                                                Map<Long, String> userNameMap,
                                                Set<Long> senderVoucherWorkOrderIds,
                                                Map<Long, BigDecimal> currentQuoteAmountMap) {
        // 调用CustomerWorkOrderListVO方法，复用统一能力并保证业务规则一致。
        CustomerWorkOrderListVO vo = new CustomerWorkOrderListVO();
        // 调用getId方法，复用统一能力并保证业务规则一致。
        vo.setId(workOrder.getId());
        // 调用getOrderNo方法，复用统一能力并保证业务规则一致。
        vo.setOrderNo(workOrder.getOrderNo());
        // 调用getCustomerName方法，复用统一能力并保证业务规则一致。
        vo.setCustomerName(workOrder.getCustomerName());
        // 调用getCustomerMobile方法，复用统一能力并保证业务规则一致。
        vo.setCustomerMobile(workOrder.getCustomerMobile());
        // 调用getBarcode方法，复用统一能力并保证业务规则一致。
        vo.setBarcode(workOrder.getBarcode());
        // 调用getProductModel方法，复用统一能力并保证业务规则一致。
        vo.setProductModel(workOrder.getProductModel());
        // 调用getBrandType方法，复用统一能力并保证业务规则一致。
        vo.setBrandType(workOrder.getBrandType());
        // 调用getLabel方法，复用统一能力并保证业务规则一致。
        vo.setBrandTypeLabel(workOrder.getBrandType() == null ? null : workOrder.getBrandType().getLabel());
        // 调用getServiceMode方法，复用统一能力并保证业务规则一致。
        vo.setServiceMode(workOrder.getServiceMode());
        // 调用getServiceMode方法，复用统一能力并保证业务规则一致。
        vo.setServiceModeLabel(ServiceModeEnum.resolveLabel(workOrder.getServiceMode()));
        // 调用getMainStatus方法，复用统一能力并保证业务规则一致。
        vo.setMainStatus(workOrder.getMainStatus());
        // 调用getMainStatus方法，复用统一能力并保证业务规则一致。
        vo.setDisplayStatus(resolveCustomerDisplayStatus(workOrder.getMainStatus()));
        // 调用getEvaluateStatus方法，复用统一能力并保证业务规则一致。
        vo.setEvaluateStatus(workOrder.getEvaluateStatus());
        // 调用getEvaluateStatus方法，复用统一能力并保证业务规则一致。
        vo.setEvaluateStatusLabel(WorkOrderStatusConstants.resolveEvaluateStatusLabel(workOrder.getEvaluateStatus()));
        // 调用getCurrentAcceptCompanyId方法，复用统一能力并保证业务规则一致。
        SysCompany currentAcceptCompany = companyMap == null ? null : companyMap.get(workOrder.getCurrentAcceptCompanyId());
        // 调用getCompanyName方法，复用统一能力并保证业务规则一致。
        vo.setCurrentAcceptCompanyName(currentAcceptCompany == null ? null : currentAcceptCompany.getCompanyName());
        // 调用getContactPhone方法，复用统一能力并保证业务规则一致。
        vo.setCurrentAcceptCompanyPhone(currentAcceptCompany == null ? null : currentAcceptCompany.getContactPhone());
        // 调用getAssignedUserId方法，复用统一能力并保证业务规则一致。
        vo.setAssignedUserName(userNameMap.get(workOrder.getAssignedUserId()));
        // 调用getHasTransfer方法，复用统一能力并保证业务规则一致。
        vo.setHasTransfer(workOrder.getHasTransfer());
        // 调用canEvaluate方法，复用统一能力并保证业务规则一致。
        vo.setCanEvaluate(canEvaluate(workOrder));
        vo.setCanUploadSendExpress(canUploadSendExpress(workOrder,
                // 调用getId方法，复用统一能力并保证业务规则一致。
                senderVoucherWorkOrderIds != null && senderVoucherWorkOrderIds.contains(workOrder.getId())));
        // 调用getId方法，复用统一能力并保证业务规则一致。
        vo.setQuoteAmount(currentQuoteAmountMap == null ? null : currentQuoteAmountMap.get(workOrder.getId()));
        // 调用getCreateTime方法，复用统一能力并保证业务规则一致。
        vo.setCreateTime(workOrder.getCreateTime());
        // 调用getClosedTime方法，复用统一能力并保证业务规则一致。
        vo.setClosedTime(workOrder.getClosedTime());
        return vo;
    }

    /**
     * 构建CurrentValidQuoteAmountMap。
     *
     * @return 处理结果
     */
    private Map<Long, BigDecimal> buildCurrentValidQuoteAmountMap(List<Long> workOrderIds) {
        if (workOrderIds == null || workOrderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> validIds = workOrderIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                // 调用toList方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toList());
        if (validIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<WorkOrderQuote> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(WorkOrderQuote::getWorkOrderId, validIds)
                .eq(WorkOrderQuote::getIsCurrentValid, 1)
                .orderByDesc(WorkOrderQuote::getCreateTime)
                // 调用orderByDesc方法，复用统一能力并保证业务规则一致。
                .orderByDesc(WorkOrderQuote::getId);
        // 说明：执行该步骤以保证业务流程正确。
        List<WorkOrderQuote> quotes = workOrderQuoteMapper.selectList(wrapper);
        if (quotes == null || quotes.isEmpty()) {
            return Collections.emptyMap();
        }
        quotes.sort(Comparator
                .comparing(WorkOrderQuote::getCreateTime, Comparator.nullsLast(Comparator.reverseOrder()))
                // 调用reverseOrder方法，复用统一能力并保证业务规则一致。
                .thenComparing(WorkOrderQuote::getId, Comparator.nullsLast(Comparator.reverseOrder())));
        Map<Long, BigDecimal> result = new HashMap<>();
        for (WorkOrderQuote quote : quotes) {
            if (quote == null
                    || quote.getWorkOrderId() == null
                    || !Integer.valueOf(1).equals(quote.getIsCurrentValid())
                    || result.containsKey(quote.getWorkOrderId())) {
                continue;
            }
            // 调用getQuoteAmount方法，复用统一能力并保证业务规则一致。
            result.put(quote.getWorkOrderId(), quote.getQuoteAmount());
        }
        return result;
    }

    /**
     * 构建LatestSummaryVo。
     *
     * @param workOrder 参数
     * @return 处理结果
     */
    private CustomerWorkOrderLatestSummaryVO buildLatestSummaryVo(WorkOrder workOrder) {
        // 调用CustomerWorkOrderLatestSummaryVO方法，复用统一能力并保证业务规则一致。
        CustomerWorkOrderLatestSummaryVO vo = new CustomerWorkOrderLatestSummaryVO();
        // 调用getId方法，复用统一能力并保证业务规则一致。
        vo.setId(workOrder.getId());
        // 调用getOrderNo方法，复用统一能力并保证业务规则一致。
        vo.setOrderNo(workOrder.getOrderNo());
        // 调用getProductName方法，复用统一能力并保证业务规则一致。
        vo.setProductName(workOrder.getProductName());
        // 调用getProductModel方法，复用统一能力并保证业务规则一致。
        vo.setProductModel(workOrder.getProductModel());
        // 调用getFaultDesc方法，复用统一能力并保证业务规则一致。
        vo.setFaultDesc(workOrder.getFaultDesc());
        // 调用getBrandType方法，复用统一能力并保证业务规则一致。
        vo.setBrandType(workOrder.getBrandType());
        // 调用getLabel方法，复用统一能力并保证业务规则一致。
        vo.setBrandTypeLabel(workOrder.getBrandType() == null ? null : workOrder.getBrandType().getLabel());
        // 调用getServiceMode方法，复用统一能力并保证业务规则一致。
        vo.setServiceMode(workOrder.getServiceMode());
        // 调用getServiceMode方法，复用统一能力并保证业务规则一致。
        vo.setServiceModeLabel(ServiceModeEnum.resolveLabel(workOrder.getServiceMode()));
        // 调用getMainStatus方法，复用统一能力并保证业务规则一致。
        vo.setDisplayStatus(resolveCustomerDisplayStatus(workOrder.getMainStatus()));
        // 调用getCreateTime方法，复用统一能力并保证业务规则一致。
        vo.setCreateTime(workOrder.getCreateTime());
        return vo;
    }

    /**
     * 查询最新未关闭工单。
     *
     * @param customerId customer ID
     * @return 处理结果
     */
    private WorkOrder findLatestUnclosedWorkOrder(Long customerId) {
        LambdaQueryWrapper<WorkOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkOrder::getCustomerId, customerId)
                .ne(WorkOrder::getMainStatus, WorkOrderStatusConstants.MainStatus.CLOSED)
                .orderByDesc(WorkOrder::getCreateTime)
                // 调用last方法，复用统一能力并保证业务规则一致。
                .last("limit 1");
        // 说明：执行该步骤以保证业务流程正确。
        List<WorkOrder> records = workOrderMapper.selectList(wrapper);
        return records == null || records.isEmpty() ? null : records.get(0);
    }

    /**
     * 查询最新工单。
     *
     * @param customerId customer ID
     * @return 处理结果
     */
    private WorkOrder findLatestWorkOrder(Long customerId) {
        LambdaQueryWrapper<WorkOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkOrder::getCustomerId, customerId)
                .orderByDesc(WorkOrder::getCreateTime)
                // 调用last方法，复用统一能力并保证业务规则一致。
                .last("limit 1");
        // 说明：执行该步骤以保证业务流程正确。
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
                // 调用getId方法，复用统一能力并保证业务规则一致。
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
                // 调用getMainStatus方法，复用统一能力并保证业务规则一致。
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

    /**
     * 判断是否存在发送凭证。
     */
    private boolean hasSenderVoucher(Long workOrderId) {
        if (workOrderId == null) {
            return false;
        }
        // 说明：执行该步骤以保证业务流程正确。
        return !sysFileService.listBizFiles(SysFileBizTypeEnum.WORK_ORDER_SENDER_VOUCHER, workOrderId).isEmpty();
    }

    /**
     * 构建发送凭证工单IDSet。
     *
     * @return 处理结果
     */
    private Set<Long> buildSenderVoucherWorkOrderIdSet(Set<Long> workOrderIds) {
        if (workOrderIds == null || workOrderIds.isEmpty()) {
            return Collections.emptySet();
        }
        List<Long> validWorkOrderIds = workOrderIds.stream()
                .filter(id -> id != null)
                // 调用toList方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toList());
        if (validWorkOrderIds.isEmpty()) {
            return Collections.emptySet();
        }
        LambdaQueryWrapper<SysFileBiz> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysFileBiz::getBizType, SysFileBizTypeEnum.WORK_ORDER_SENDER_VOUCHER)
                // 调用in方法，复用统一能力并保证业务规则一致。
                .in(SysFileBiz::getBizId, validWorkOrderIds);
        // 说明：执行该步骤以保证业务流程正确。
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
                // 调用toSet方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toSet());
        if (activeFileIds.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> result = new HashSet<>();
        for (SysFileBiz relation : relations) {
            if (relation != null && relation.getBizId() != null && activeFileIds.contains(relation.getFileId())) {
                // 调用getBizId方法，复用统一能力并保证业务规则一致。
                result.add(relation.getBizId());
            }
        }
        return result;
    }

    /**
     * 解析客户展示状态。
     *
     * @param mainStatus 参数
     * @return 处理结果
     */
    private String resolveCustomerDisplayStatus(String mainStatus) {
        return WorkOrderStatusConstants.resolveDisplayStatusLabel(mainStatus);
    }

    /**
     * 构建公司Map。
     *
     * @return 处理结果
     */
    private Map<Long, SysCompany> buildCompanyMap(Set<Long> companyIds) {
        if (companyIds == null || companyIds.isEmpty()) {
            return Collections.emptyMap();
        }
        // 调用toList方法，复用统一能力并保证业务规则一致。
        List<Long> validCompanyIds = companyIds.stream().filter(id -> id != null).collect(Collectors.toList());
        if (validCompanyIds.isEmpty()) {
            return Collections.emptyMap();
        }
        // 说明：执行该步骤以保证业务流程正确。
        List<SysCompany> companies = sysCompanyMapper.selectBatchIds(validCompanyIds);
        // 调用size方法，复用统一能力并保证业务规则一致。
        Map<Long, SysCompany> map = new HashMap<>(companies.size());
        for (SysCompany company : companies) {
            if (company != null) {
                // 调用getId方法，复用统一能力并保证业务规则一致。
                map.put(company.getId(), company);
            }
        }
        return map;
    }

    /**
     * 构建公司名称Map。
     *
     * @return 处理结果
     */
    private Map<Long, String> buildCompanyNameMap(Set<Long> companyIds) {
        // 调用buildCompanyMap方法，复用统一能力并保证业务规则一致。
        Map<Long, SysCompany> companyMap = buildCompanyMap(companyIds);
        if (companyMap.isEmpty()) {
            return Collections.emptyMap();
        }
        // 调用size方法，复用统一能力并保证业务规则一致。
        Map<Long, String> map = new HashMap<>(companyMap.size());
        for (Map.Entry<Long, SysCompany> entry : companyMap.entrySet()) {
            // 调用getValue方法，复用统一能力并保证业务规则一致。
            SysCompany company = entry.getValue();
            if (company != null) {
                // 调用getCompanyName方法，复用统一能力并保证业务规则一致。
                map.put(entry.getKey(), company.getCompanyName());
            }
        }
        return map;
    }

    /**
     * 构建Nearby服务公司Option。
     *
     * @param company 参数
     * @param longitude 参数
     * @param latitude 参数
     * @return 处理结果
     */
    private CustomerNearbyServiceCompanyVO buildNearbyServiceCompanyOption(SysCompany company, BigDecimal longitude,
                                                                           BigDecimal latitude) {
        // 调用CustomerNearbyServiceCompanyVO方法，复用统一能力并保证业务规则一致。
        CustomerNearbyServiceCompanyVO vo = new CustomerNearbyServiceCompanyVO();
        // 调用getId方法，复用统一能力并保证业务规则一致。
        vo.setId(company.getId());
        // 调用getCompanyName方法，复用统一能力并保证业务规则一致。
        vo.setCompanyName(company.getCompanyName());
        // 调用getCompanyCode方法，复用统一能力并保证业务规则一致。
        vo.setCompanyCode(company.getCompanyCode());
        // 调用getTypeCode方法，复用统一能力并保证业务规则一致。
        vo.setTypeCode(company.getTypeCode());
        // 调用getTypeCode方法，复用统一能力并保证业务规则一致。
        vo.setTypeName(resolveServiceCompanyTypeName(company.getTypeCode()));
        // 调用getContactPhone方法，复用统一能力并保证业务规则一致。
        vo.setContactPhone(company.getContactPhone());
        // 调用resolveCompanyAddress方法，复用统一能力并保证业务规则一致。
        vo.setAddress(resolveCompanyAddress(company));
        // 调用getLongitude方法，复用统一能力并保证业务规则一致。
        vo.setLongitude(company.getLongitude());
        // 调用getLatitude方法，复用统一能力并保证业务规则一致。
        vo.setLatitude(company.getLatitude());
        // 调用getLatitude方法，复用统一能力并保证业务规则一致。
        vo.setDistanceKm(calculateDistanceKm(longitude, latitude, company.getLongitude(), company.getLatitude()));
        // 调用setHasRepairHistory方法，复用统一能力并保证业务规则一致。
        vo.setHasRepairHistory(Boolean.FALSE);
        return vo;
    }

    /**
     * 构建服务公司Option。
     *
     * @param company 参数
     * @return 处理结果
     */
    private CustomerServiceCompanyOptionVO buildServiceCompanyOption(SysCompany company) {
        // 调用CustomerServiceCompanyOptionVO方法，复用统一能力并保证业务规则一致。
        CustomerServiceCompanyOptionVO vo = new CustomerServiceCompanyOptionVO();
        // 调用getId方法，复用统一能力并保证业务规则一致。
        vo.setId(company.getId());
        // 调用getCompanyName方法，复用统一能力并保证业务规则一致。
        vo.setCompanyName(company.getCompanyName());
        // 调用getCompanyCode方法，复用统一能力并保证业务规则一致。
        vo.setCompanyCode(company.getCompanyCode());
        // 调用getTypeCode方法，复用统一能力并保证业务规则一致。
        vo.setTypeCode(company.getTypeCode());
        // 调用getTypeCode方法，复用统一能力并保证业务规则一致。
        vo.setTypeName(resolveServiceCompanyTypeName(company.getTypeCode()));
        // 调用getContactPhone方法，复用统一能力并保证业务规则一致。
        vo.setContactPhone(company.getContactPhone());
        // 调用resolveCompanyAddress方法，复用统一能力并保证业务规则一致。
        vo.setAddress(resolveCompanyAddress(company));
        return vo;
    }

    /**
     * 解析公司Address。
     *
     * @param company 参数
     * @return 处理结果
     */
    private String resolveCompanyAddress(SysCompany company) {
        // 调用getFullAddress方法，复用统一能力并保证业务规则一致。
        String fullAddress = normalizeText(company.getFullAddress());
        if (fullAddress != null) {
            return fullAddress;
        }
        return normalizeText(company.getDetailAddress());
    }

    /**
     * 解析服务公司类型名称。
     *
     * @param typeCode 参数
     * @return 处理结果
     */
    private String resolveServiceCompanyTypeName(String typeCode) {
        if (CompanyCategoryEnum.getFirstLevelTypeCodes().contains(typeCode)) {
            return "一级服务网点";
        }
        if (CompanyCategoryEnum.getSecondLevelTypeCodes().contains(typeCode)) {
            return "二级服务网点";
        }
        return typeCode;
    }

    /**
     * 解析ArchiveText。
     *
     * @param archiveValue 参数
     * @param requestValue 参数
     * @return 处理结果
     */
    private String resolveArchiveText(String archiveValue, String requestValue) {
        // 调用normalizeText方法，复用统一能力并保证业务规则一致。
        String normalizedArchiveValue = normalizeText(archiveValue);
        return normalizedArchiveValue != null ? normalizedArchiveValue : normalizeText(requestValue);
    }

    /**
     * 解析客户名称。
     *
     * @param customer 参数
     * @return 处理结果
     */
    private String resolveCustomerName(CUser customer) {
        if (customer == null) {
            throw new ServiceException("当前客户不存在");
        }
        // 调用getNickname方法，复用统一能力并保证业务规则一致。
        String nickname = normalizeText(customer.getNickname());
        if (nickname != null) {
            return nickname;
        }
        return normalizeRequiredText(customer.getPhone(), "当前客户手机号不能为空");
    }

    /**
     * 解析客户故障Selection。
     *
     * @param dto 参数
     * @param hqCompanyId hq Company ID
     * @param productCode 参数
     * @param productModel 参数
     * @return 处理结果
     */
    private CustomerFaultSelection resolveCustomerFaultSelection(CustomerWorkOrderCreateDTO dto, Long hqCompanyId,
                                                                 String productCode, String productModel) {
        return resolveCustomerFaultSelection(dto, BrandTypeEnum.JASIC, true, hqCompanyId, productCode, productModel);
    }

    /**
     * 解析客户故障Selection。
     *
     * @param dto 参数
     * @param brandType 参数
     * @param hasBarcode 参数
     * @param hqCompanyId hq Company ID
     * @param productCode 参数
     * @param productModel 参数
     * @return 处理结果
     */
    private CustomerFaultSelection resolveCustomerFaultSelection(CustomerWorkOrderCreateDTO dto, BrandTypeEnum brandType,
                                                                 boolean hasBarcode, Long hqCompanyId,
                                                                 String productCode, String productModel) {
        if (brandType == null || !hasBarcode) {
            return resolveOtherOnlyFaultSelection(dto);
        }
        // 调用listConfiguredFaultOptions方法，复用统一能力并保证业务规则一致。
        List<String> configuredFaultOptions = listConfiguredFaultOptions(hqCompanyId, productCode, productModel);
        // 调用getFaultItems方法，复用统一能力并保证业务规则一致。
        List<String> faultItems = normalizeFaultItems(dto == null ? null : dto.getFaultItems());
        // 调用getFaultRemark方法，复用统一能力并保证业务规则一致。
        String faultRemark = normalizeText(dto == null ? null : dto.getFaultRemark());
        // 调用getFaultDesc方法，复用统一能力并保证业务规则一致。
        String legacyFaultDesc = normalizeText(dto == null ? null : dto.getFaultDesc());
        boolean useLegacyFaultDesc = false;
        if (faultItems.isEmpty() && legacyFaultDesc != null) {
            // 调用singletonList方法，复用统一能力并保证业务规则一致。
            faultItems = new ArrayList<>(Collections.singletonList(legacyFaultDesc));
            useLegacyFaultDesc = true;
        }

        if (configuredFaultOptions.isEmpty()) {
            if (faultItems.isEmpty()) {
                // 调用singletonList方法，复用统一能力并保证业务规则一致。
                faultItems = new ArrayList<>(Collections.singletonList(OTHER_FAULT_LABEL));
            } else if (useLegacyFaultDesc && faultItems.size() == 1 && !OTHER_FAULT_LABEL.equals(faultItems.get(0))) {
                // 调用get方法，复用统一能力并保证业务规则一致。
                faultRemark = faultRemark != null ? faultRemark : faultItems.get(0);
                // 调用singletonList方法，复用统一能力并保证业务规则一致。
                faultItems = new ArrayList<>(Collections.singletonList(OTHER_FAULT_LABEL));
            } else if (faultItems.size() != 1 || !OTHER_FAULT_LABEL.equals(faultItems.get(0))) {
                throw new ServiceException("当前产品暂未配置故障项，请选择其它故障");
            }
        } else {
            LinkedHashSet<String> allowedFaultOptions = new LinkedHashSet<>(configuredFaultOptions);
            // 调用add方法，复用统一能力并保证业务规则一致。
            allowedFaultOptions.add(OTHER_FAULT_LABEL);
            if (faultItems.isEmpty()) {
                throw new ServiceException("请选择故障描述");
            }
            if (useLegacyFaultDesc && faultItems.size() == 1 && !allowedFaultOptions.contains(faultItems.get(0))) {
                // 调用get方法，复用统一能力并保证业务规则一致。
                faultRemark = faultRemark != null ? faultRemark : faultItems.get(0);
                // 调用singletonList方法，复用统一能力并保证业务规则一致。
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

    /**
     * 解析OtherOnly故障Selection。
     *
     * @param dto 参数
     * @return 处理结果
     */
    private CustomerFaultSelection resolveOtherOnlyFaultSelection(CustomerWorkOrderCreateDTO dto) {
        // 调用getFaultItems方法，复用统一能力并保证业务规则一致。
        List<String> faultItems = normalizeFaultItems(dto == null ? null : dto.getFaultItems());
        // 调用getFaultRemark方法，复用统一能力并保证业务规则一致。
        String faultRemark = normalizeText(dto == null ? null : dto.getFaultRemark());
        // 调用getFaultDesc方法，复用统一能力并保证业务规则一致。
        String legacyFaultDesc = normalizeText(dto == null ? null : dto.getFaultDesc());
        if (faultItems.isEmpty()) {
            // 调用singletonList方法，复用统一能力并保证业务规则一致。
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
        // 调用listConfiguredFaultOptions方法，复用统一能力并保证业务规则一致。
        LinkedHashSet<String> faultOptions = new LinkedHashSet<>(listConfiguredFaultOptions(hqCompanyId, productCode, productModel));
        // 调用add方法，复用统一能力并保证业务规则一致。
        faultOptions.add(OTHER_FAULT_LABEL);
        return new ArrayList<>(faultOptions);
    }

    /**
     * 分页查询Configured故障Options列表。
     *
     * @param hqCompanyId hq Company ID
     * @param productCode 参数
     * @param productModel 参数
     * @return 处理结果
     */
    private List<String> listConfiguredFaultOptions(Long hqCompanyId, String productCode, String productModel) {
        List<WorkOrderRepairFaultOptionVO> repairFaultOptions = faultRepairConfigService == null
                ? Collections.emptyList()
                // 调用listRepairFaultOptionsForResolvedHq方法，复用统一能力并保证业务规则一致。
                : faultRepairConfigService.listRepairFaultOptionsForResolvedHq(hqCompanyId, productCode, productModel);
        if (repairFaultOptions == null || repairFaultOptions.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (WorkOrderRepairFaultOptionVO repairFaultOption : repairFaultOptions) {
            // 调用getFaultDesc方法，复用统一能力并保证业务规则一致。
            String faultDesc = normalizeText(repairFaultOption.getFaultDesc());
            if (faultDesc != null) {
                // 调用add方法，复用统一能力并保证业务规则一致。
                result.add(faultDesc);
            }
        }
        return new ArrayList<>(result);
    }

    /**
     * 规范化故障Items。
     *
     * @param faultItems 参数
     * @return 处理结果
     */
    private List<String> normalizeFaultItems(List<String> faultItems) {
        if (faultItems == null || faultItems.isEmpty()) {
            return new ArrayList<>();
        }
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String faultItem : faultItems) {
            // 调用normalizeText方法，复用统一能力并保证业务规则一致。
            String normalized = normalizeText(faultItem);
            if (normalized != null) {
                // 调用add方法，复用统一能力并保证业务规则一致。
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

    /**
     * calculateDistanceKm。
     *
     * @param sourceLongitude 参数
     * @param sourceLatitude 参数
     * @param targetLongitude 参数
     * @param targetLatitude 参数
     * @return 处理结果
     */
    private BigDecimal calculateDistanceKm(BigDecimal sourceLongitude, BigDecimal sourceLatitude,
                                           BigDecimal targetLongitude, BigDecimal targetLatitude) {
        if (targetLongitude == null || targetLatitude == null) {
            return null;
        }
        // 调用doubleValue方法，复用统一能力并保证业务规则一致。
        double lat1 = Math.toRadians(sourceLatitude.doubleValue());
        // 调用doubleValue方法，复用统一能力并保证业务规则一致。
        double lng1 = Math.toRadians(sourceLongitude.doubleValue());
        // 调用doubleValue方法，复用统一能力并保证业务规则一致。
        double lat2 = Math.toRadians(targetLatitude.doubleValue());
        // 调用doubleValue方法，复用统一能力并保证业务规则一致。
        double lng2 = Math.toRadians(targetLongitude.doubleValue());
        double deltaLat = lat2 - lat1;
        double deltaLng = lng2 - lng1;
        double haversine = Math.pow(Math.sin(deltaLat / 2), 2)
                // 调用sin方法，复用统一能力并保证业务规则一致。
                + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(deltaLng / 2), 2);
        // 调用sqrt方法，复用统一能力并保证业务规则一致。
        double distance = 2 * EARTH_RADIUS_KM * Math.asin(Math.sqrt(haversine));
        return BigDecimal.valueOf(distance).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * compareDistance。
     *
     * @param leftDistance 参数
     * @param rightDistance 参数
     * @param leftName 参数
     * @param rightName 参数
     * @param leftId left ID
     * @param rightId right ID
     * @return 处理结果
     */
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
        // 调用compareTo方法，复用统一能力并保证业务规则一致。
        int compareResult = leftDistance.compareTo(rightDistance);
        if (compareResult != 0) {
            return compareResult;
        }
        return compareCompanyIdentity(leftName, rightName, leftId, rightId);
    }

    /**
     * 构建维修HistoryMap。
     *
     * @param customerId customer ID
     * @param companies 参数
     * @return 处理结果
     */
    private Map<Long, RepairHistorySummary> buildRepairHistoryMap(Long customerId, List<SysCompany> companies) {
        if (customerId == null || companies == null || companies.isEmpty() || workOrderFlowMapper == null) {
            return Collections.emptyMap();
        }
        Set<Long> companyIds = companies.stream()
                .map(SysCompany::getId)
                .filter(Objects::nonNull)
                // 调用toSet方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toSet());
        if (companyIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<WorkOrderCompanyRepairHistoryStatVO> stats =
                // 说明：执行该步骤以保证业务流程正确。
                workOrderFlowMapper.selectCustomerCreateCompanyRepairHistory(customerId, companyIds);
        if (stats == null || stats.isEmpty()) {
            return Collections.emptyMap();
        }
        // 调用size方法，复用统一能力并保证业务规则一致。
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

    /**
     * compareNearby服务公司。
     *
     * @param left 参数
     * @param right 参数
     * @param repairHistoryMap 参数
     * @return 处理结果
     */
    private int compareNearbyServiceCompany(CustomerNearbyServiceCompanyVO left,
                                            CustomerNearbyServiceCompanyVO right,
                                            Map<Long, RepairHistorySummary> repairHistoryMap) {
        // 调用getId方法，复用统一能力并保证业务规则一致。
        RepairHistorySummary leftHistory = repairHistoryMap.get(left.getId());
        // 调用getId方法，复用统一能力并保证业务规则一致。
        RepairHistorySummary rightHistory = repairHistoryMap.get(right.getId());
        boolean leftHasHistory = leftHistory != null;
        boolean rightHasHistory = rightHistory != null;
        if (leftHasHistory != rightHasHistory) {
            return leftHasHistory ? -1 : 1;
        }
        if (leftHasHistory) {
            // 调用getRepairCount方法，复用统一能力并保证业务规则一致。
            int compareCount = Long.compare(rightHistory.getRepairCount(), leftHistory.getRepairCount());
            if (compareCount != 0) {
                return compareCount;
            }
            int compareTime = compareLastRepairTimeDesc(leftHistory.getLastRepairTime(),
                    // 调用getLastRepairTime方法，复用统一能力并保证业务规则一致。
                    rightHistory.getLastRepairTime());
            if (compareTime != 0) {
                return compareTime;
            }
        }
        return compareDistance(left.getDistanceKm(), right.getDistanceKm(),
                // 调用getId方法，复用统一能力并保证业务规则一致。
                left.getCompanyName(), right.getCompanyName(), left.getId(), right.getId());
    }

    /**
     * compareLast维修Time描述。
     *
     * @param leftTime 参数
     * @param rightTime 参数
     * @return 处理结果
     */
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

        /**
     * Long字段。
     *
     * @param repairCount 参数
     * @param lastRepairTime 参数
     * @return 处理结果
         */
        private final Long repairCount;

        private final LocalDateTime lastRepairTime;

        RepairHistorySummary(Long repairCount, LocalDateTime lastRepairTime) {
            this.repairCount = repairCount == null ? 0L : repairCount;
            this.lastRepairTime = lastRepairTime;
        }

        /**
         * 获取维修Count。
         *
         * @return 处理结果
         */
        Long getRepairCount() {
            return repairCount;
        }

        /**
         * 获取Last维修Time。
         *
         * @return 处理结果
         */
        LocalDateTime getLastRepairTime() {
            return lastRepairTime;
        }
    }

    /**
     * compare公司身份。
     *
     * @param leftName 参数
     * @param rightName 参数
     * @param leftId left ID
     * @param rightId right ID
     * @return 处理结果
     */
    private int compareCompanyIdentity(String leftName, String rightName, Long leftId, Long rightId) {
        String safeLeftName = leftName == null ? "" : leftName;
        String safeRightName = rightName == null ? "" : rightName;
        // 调用compareTo方法，复用统一能力并保证业务规则一致。
        int compareResult = safeLeftName.compareTo(safeRightName);
        if (compareResult != 0) {
            return compareResult;
        }
        long safeLeftId = leftId == null ? Long.MAX_VALUE : leftId;
        long safeRightId = rightId == null ? Long.MAX_VALUE : rightId;
        return Long.compare(safeLeftId, safeRightId);
    }

    /**
     * 构建用户名称Map。
     *
     * @return 处理结果
     */
    private Map<Long, String> buildUserNameMap(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        // 调用toList方法，复用统一能力并保证业务规则一致。
        List<Long> validUserIds = userIds.stream().filter(id -> id != null).collect(Collectors.toList());
        if (validUserIds.isEmpty()) {
            return Collections.emptyMap();
        }
        // 说明：执行该步骤以保证业务流程正确。
        List<SysUser> users = sysUserMapper.selectBatchIds(validUserIds);
        // 调用size方法，复用统一能力并保证业务规则一致。
        Map<Long, String> map = new HashMap<>(users.size());
        for (SysUser user : users) {
            if (user != null) {
                // 调用getRealName方法，复用统一能力并保证业务规则一致。
                map.put(user.getId(), user.getRealName());
            }
        }
        return map;
    }

    /**
     * 分页查询QuoteVos列表。
     *
     * @return 处理结果
     */
    private List<WorkOrderQuoteVO> listQuoteVos(Long workOrderId) {
        LambdaQueryWrapper<WorkOrderQuote> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkOrderQuote::getWorkOrderId, workOrderId)
                // 调用orderByDesc方法，复用统一能力并保证业务规则一致。
                .orderByDesc(WorkOrderQuote::getCreateTime);
        // 说明：执行该步骤以保证业务流程正确。
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
            // 调用WorkOrderQuoteVO方法，复用统一能力并保证业务规则一致。
            WorkOrderQuoteVO vo = new WorkOrderQuoteVO();
            // 调用getId方法，复用统一能力并保证业务规则一致。
            vo.setId(quote.getId());
            // 调用getCompanyId方法，复用统一能力并保证业务规则一致。
            vo.setCompanyId(quote.getCompanyId());
            // 调用getCompanyId方法，复用统一能力并保证业务规则一致。
            vo.setCompanyName(companyNameMap.get(quote.getCompanyId()));
            // 调用getQuotedBy方法，复用统一能力并保证业务规则一致。
            vo.setQuotedBy(quote.getQuotedBy());
            // 调用getQuotedBy方法，复用统一能力并保证业务规则一致。
            vo.setQuotedByName(userNameMap.get(quote.getQuotedBy()));
            // 调用getFaultJudge方法，复用统一能力并保证业务规则一致。
            vo.setFaultJudge(quote.getFaultJudge());
            // 调用getQuoteAmount方法，复用统一能力并保证业务规则一致。
            vo.setQuoteAmount(quote.getQuoteAmount());
            // 调用getQuoteDesc方法，复用统一能力并保证业务规则一致。
            vo.setQuoteDesc(quote.getQuoteDesc());
            // 调用getIsCurrentValid方法，复用统一能力并保证业务规则一致。
            vo.setIsCurrentValid(quote.getIsCurrentValid());
            // 调用getCreateTime方法，复用统一能力并保证业务规则一致。
            vo.setCreateTime(quote.getCreateTime());
            // 调用add方法，复用统一能力并保证业务规则一致。
            result.add(vo);
        }
        return result;
    }

    /**
     * 分页查询维修Vos列表。
     *
     * @return 处理结果
     */
    private List<WorkOrderRepairVO> listRepairVos(Long workOrderId) {
        LambdaQueryWrapper<WorkOrderRepair> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkOrderRepair::getWorkOrderId, workOrderId)
                // 调用orderByDesc方法，复用统一能力并保证业务规则一致。
                .orderByDesc(WorkOrderRepair::getCreateTime);
        // 说明：执行该步骤以保证业务流程正确。
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
        // 调用toSet方法，复用统一能力并保证业务规则一致。
        Set<Long> repairIds = repairs.stream().map(WorkOrderRepair::getId).collect(Collectors.toSet());
        // 调用buildFaultMap方法，复用统一能力并保证业务规则一致。
        Map<Long, List<WorkOrderFaultVO>> faultMap = buildFaultMap(workOrderId, repairIds);
        List<WorkOrderRepairVO> result = new ArrayList<>();
        for (WorkOrderRepair repair : repairs) {
            // 调用WorkOrderRepairVO方法，复用统一能力并保证业务规则一致。
            WorkOrderRepairVO vo = new WorkOrderRepairVO();
            // 调用getId方法，复用统一能力并保证业务规则一致。
            vo.setId(repair.getId());
            // 调用getCompanyId方法，复用统一能力并保证业务规则一致。
            vo.setCompanyId(repair.getCompanyId());
            // 调用getCompanyId方法，复用统一能力并保证业务规则一致。
            vo.setCompanyName(companyNameMap.get(repair.getCompanyId()));
            // 调用getRepairUserId方法，复用统一能力并保证业务规则一致。
            vo.setRepairUserId(repair.getRepairUserId());
            // 调用getRepairUserId方法，复用统一能力并保证业务规则一致。
            vo.setRepairUserName(userNameMap.get(repair.getRepairUserId()));
            // 调用getRegisterStage方法，复用统一能力并保证业务规则一致。
            vo.setRegisterStage(repair.getRegisterStage());
            // 调用getRegisterStage方法，复用统一能力并保证业务规则一致。
            vo.setRegisterStageLabel(REGISTER_STAGE_RECHECK.equals(repair.getRegisterStage()) ? "复检登记" : "维修登记");
            // 调用getIsFinished方法，复用统一能力并保证业务规则一致。
            vo.setIsFinished(repair.getIsFinished());
            // 调用getFinishedTime方法，复用统一能力并保证业务规则一致。
            vo.setFinishedTime(repair.getFinishedTime());
            // 调用getCreateTime方法，复用统一能力并保证业务规则一致。
            vo.setCreateTime(repair.getCreateTime());
            // 调用emptyList方法，复用统一能力并保证业务规则一致。
            vo.setFaults(faultMap.getOrDefault(repair.getId(), Collections.emptyList()));
            // 调用getId方法，复用统一能力并保证业务规则一致。
            fillRepairAttachmentDetail(vo, buildRepairFileMap(repair.getId()));
            // 调用add方法，复用统一能力并保证业务规则一致。
            result.add(vo);
        }
        return result;
    }

    /**
     * 构建故障Map。
     *
     * @return 处理结果
     */
    private Map<Long, List<WorkOrderFaultVO>> buildFaultMap(Long workOrderId, Set<Long> repairIds) {
        if (repairIds == null || repairIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<WorkOrderFault> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkOrderFault::getWorkOrderId, workOrderId)
                .in(WorkOrderFault::getRepairId, repairIds)
                .orderByAsc(WorkOrderFault::getSortNum)
                // 调用orderByAsc方法，复用统一能力并保证业务规则一致。
                .orderByAsc(WorkOrderFault::getId);
        // 说明：执行该步骤以保证业务流程正确。
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
            // 调用WorkOrderFaultVO方法，复用统一能力并保证业务规则一致。
            WorkOrderFaultVO vo = new WorkOrderFaultVO();
            // 调用getId方法，复用统一能力并保证业务规则一致。
            vo.setId(fault.getId());
            // 调用getCompanyId方法，复用统一能力并保证业务规则一致。
            vo.setCompanyId(fault.getCompanyId());
            // 调用getFaultDesc方法，复用统一能力并保证业务规则一致。
            vo.setFaultDesc(fault.getFaultDesc());
            // 调用getRepairDesc方法，复用统一能力并保证业务规则一致。
            vo.setRepairDesc(fault.getRepairDesc());
            // 调用getOtherDesc方法，复用统一能力并保证业务规则一致。
            vo.setOtherDesc(fault.getOtherDesc());
            // 调用emptyList方法，复用统一能力并保证业务规则一致。
            vo.setPartList(partMap.getOrDefault(fault.getId(), Collections.emptyList()));
            // 调用getSortNum方法，复用统一能力并保证业务规则一致。
            vo.setSortNum(fault.getSortNum());
            // 调用getCreatedBy方法，复用统一能力并保证业务规则一致。
            vo.setCreatedBy(fault.getCreatedBy());
            // 调用getCreatedBy方法，复用统一能力并保证业务规则一致。
            vo.setCreatedByName(userNameMap.get(fault.getCreatedBy()));
            // 调用getCreateTime方法，复用统一能力并保证业务规则一致。
            vo.setCreateTime(fault.getCreateTime());
            // 调用add方法，复用统一能力并保证业务规则一致。
            result.computeIfAbsent(fault.getRepairId(), key -> new ArrayList<>()).add(vo);
        }
        return result;
    }

    /**
     * 构建故障PartMap。
     *
     * @return 处理结果
     */
    private Map<Long, List<WorkOrderFaultPartVO>> buildFaultPartMap(Set<Long> faultIds) {
        if (faultIds == null || faultIds.isEmpty()) {
            return Collections.emptyMap();
        }
        LambdaQueryWrapper<WorkOrderFaultPart> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(WorkOrderFaultPart::getFaultId, faultIds)
                .orderByAsc(WorkOrderFaultPart::getSortNum)
                // 调用orderByAsc方法，复用统一能力并保证业务规则一致。
                .orderByAsc(WorkOrderFaultPart::getId);
        // 说明：执行该步骤以保证业务流程正确。
        List<WorkOrderFaultPart> faultParts = workOrderFaultPartMapper.selectList(wrapper);
        if (faultParts.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, List<WorkOrderFaultPartVO>> result = new HashMap<>();
        for (WorkOrderFaultPart faultPart : faultParts) {
            // 调用WorkOrderFaultPartVO方法，复用统一能力并保证业务规则一致。
            WorkOrderFaultPartVO vo = new WorkOrderFaultPartVO();
            // 调用getId方法，复用统一能力并保证业务规则一致。
            vo.setId(faultPart.getId());
            // 调用getPartName方法，复用统一能力并保证业务规则一致。
            vo.setPartName(faultPart.getPartName());
            // 调用getPartQty方法，复用统一能力并保证业务规则一致。
            vo.setPartQty(faultPart.getPartQty());
            // 调用getSortNum方法，复用统一能力并保证业务规则一致。
            vo.setSortNum(faultPart.getSortNum());
            // 调用add方法，复用统一能力并保证业务规则一致。
            result.computeIfAbsent(faultPart.getFaultId(), key -> new ArrayList<>()).add(vo);
        }
        return result;
    }

    /**
     * 获取评价Vo。
     *
     * @return 处理结果
     */
    private WorkOrderEvaluationVO getEvaluationVo(Long workOrderId) {
        LambdaQueryWrapper<WorkOrderEvaluation> wrapper = new LambdaQueryWrapper<>();
        // 调用eq方法，复用统一能力并保证业务规则一致。
        wrapper.eq(WorkOrderEvaluation::getWorkOrderId, workOrderId);
        // 说明：执行该步骤以保证业务流程正确。
        WorkOrderEvaluation evaluation = workOrderEvaluationMapper.selectOne(wrapper);
        if (evaluation == null) {
            return null;
        }
        // 调用WorkOrderEvaluationVO方法，复用统一能力并保证业务规则一致。
        WorkOrderEvaluationVO vo = new WorkOrderEvaluationVO();
        // 调用getId方法，复用统一能力并保证业务规则一致。
        vo.setId(evaluation.getId());
        // 调用getCustomerId方法，复用统一能力并保证业务规则一致。
        vo.setCustomerId(evaluation.getCustomerId());
        // 调用getCompanyId方法，复用统一能力并保证业务规则一致。
        vo.setCompanyId(evaluation.getCompanyId());
        // 调用getTimelinessScore方法，复用统一能力并保证业务规则一致。
        vo.setTimelinessScore(evaluation.getTimelinessScore());
        // 调用getQualityScore方法，复用统一能力并保证业务规则一致。
        vo.setQualityScore(evaluation.getQualityScore());
        // 调用getSatisfactionScore方法，复用统一能力并保证业务规则一致。
        vo.setSatisfactionScore(evaluation.getSatisfactionScore());
        // 调用getTags方法，复用统一能力并保证业务规则一致。
        vo.setTags(evaluation.getTags());
        // 调用getContent方法，复用统一能力并保证业务规则一致。
        vo.setContent(evaluation.getContent());
        // 调用getCreateTime方法，复用统一能力并保证业务规则一致。
        vo.setCreateTime(evaluation.getCreateTime());
        return vo;
    }

    /**
     * 判断是否存在故障For评价。
     */
    private boolean hasFaultForEvaluation(Long workOrderId) {
        if (workOrderId == null) {
            return false;
        }
        LambdaQueryWrapper<WorkOrderQuote> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkOrderQuote::getWorkOrderId, workOrderId)
                .eq(WorkOrderQuote::getIsCurrentValid, 1)
                // 调用orderByDesc方法，复用统一能力并保证业务规则一致。
                .orderByDesc(WorkOrderQuote::getCreateTime);
        // 说明：执行该步骤以保证业务流程正确。
        List<WorkOrderQuote> quotes = workOrderQuoteMapper.selectList(wrapper);
        if (quotes == null || quotes.isEmpty()) {
            return false;
        }
        return !FAULT_JUDGE_NO_FAULT.equals(normalizeText(quotes.get(0).getFaultJudge()));
    }

    private static final class CustomerFaultSelection {

        /**
     * String字段。
     *
     * @param faultDesc 参数
     * @param faultRemark 参数
     * @return 处理结果
         */
        private final String faultDesc;

        private final String faultRemark;

        /**
         * 构造客户故障Selection实例。
         *
         * @param faultDesc 参数
         * @param faultRemark 参数
         * @return 处理结果
         */
        private CustomerFaultSelection(String faultDesc, String faultRemark) {
            this.faultDesc = faultDesc;
            this.faultRemark = faultRemark;
        }

        /**
     * 获取故障描述。
     *
     * @return 处理结果
         */
        private String getFaultDesc() {
            return faultDesc;
        }

        /**
     * 获取故障Remark。
     *
     * @return 处理结果
         */
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
        // 调用normalizeText方法，复用统一能力并保证业务规则一致。
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
        // 调用normalizeText方法，复用统一能力并保证业务规则一致。
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
        // 调用normalizeRequiredText方法，复用统一能力并保证业务规则一致。
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
        // 调用trim方法，复用统一能力并保证业务规则一致。
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }
}




