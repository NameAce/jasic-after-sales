package com.jasic.aftersales.customer.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jasic.aftersales.common.constant.WorkOrderStatusConstants;
import com.jasic.aftersales.common.constant.WorkOrderStatusFlow;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.customer.domain.dto.CustomerWorkOrderCreateDTO;
import com.jasic.aftersales.customer.domain.dto.CustomerWorkOrderEvaluateDTO;
import com.jasic.aftersales.customer.domain.dto.CustomerWorkOrderSendInfoDTO;
import com.jasic.aftersales.customer.domain.entity.CUser;
import com.jasic.aftersales.customer.domain.query.CustomerWorkOrderQuery;
import com.jasic.aftersales.customer.domain.vo.CustomerWorkOrderDetailVO;
import com.jasic.aftersales.customer.domain.vo.CustomerWorkOrderListVO;
import com.jasic.aftersales.customer.domain.vo.CustomerWorkOrderStatusCountVO;
import com.jasic.aftersales.customer.mapper.CUserMapper;
import com.jasic.aftersales.customer.service.ICustomerWorkOrderService;
import com.jasic.aftersales.framework.security.StpCustomerUtil;
import com.jasic.aftersales.system.domain.entity.FirstSecondRelation;
import com.jasic.aftersales.system.domain.entity.HqFirstContract;
import com.jasic.aftersales.system.domain.entity.SysCompany;
import com.jasic.aftersales.system.domain.entity.SysUser;
import com.jasic.aftersales.system.domain.entity.WorkOrder;
import com.jasic.aftersales.system.domain.entity.WorkOrderEvaluation;
import com.jasic.aftersales.system.domain.entity.WorkOrderFault;
import com.jasic.aftersales.system.domain.entity.WorkOrderFlow;
import com.jasic.aftersales.system.domain.entity.WorkOrderQuote;
import com.jasic.aftersales.system.domain.entity.WorkOrderRepair;
import com.jasic.aftersales.system.domain.entity.WorkOrderReview;
import com.jasic.aftersales.system.domain.vo.WorkOrderEvaluationVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderFaultVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderQuoteVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderRepairVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderReviewVO;
import com.jasic.aftersales.system.mapper.FirstSecondRelationMapper;
import com.jasic.aftersales.system.mapper.HqFirstContractMapper;
import com.jasic.aftersales.system.mapper.SysCompanyMapper;
import com.jasic.aftersales.system.mapper.SysUserMapper;
import com.jasic.aftersales.system.mapper.WorkOrderEvaluationMapper;
import com.jasic.aftersales.system.mapper.WorkOrderFaultMapper;
import com.jasic.aftersales.system.mapper.WorkOrderFlowMapper;
import com.jasic.aftersales.system.mapper.WorkOrderMapper;
import com.jasic.aftersales.system.mapper.WorkOrderQuoteMapper;
import com.jasic.aftersales.system.mapper.WorkOrderRepairMapper;
import com.jasic.aftersales.system.mapper.WorkOrderReviewMapper;
import com.jasic.aftersales.system.service.WorkOrderNotifyEventService;
import com.jasic.aftersales.system.service.WorkOrderParticipantService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private static final DateTimeFormatter ORDER_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

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
    private WorkOrderReviewMapper workOrderReviewMapper;

    @Resource
    private WorkOrderEvaluationMapper workOrderEvaluationMapper;

    @Resource
    private WorkOrderFlowMapper workOrderFlowMapper;

    @Resource
    private SysCompanyMapper sysCompanyMapper;

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private WorkOrderNotifyEventService workOrderNotifyEventService;

    @Resource
    private WorkOrderParticipantService workOrderParticipantService;

    @Resource
    private HqFirstContractMapper hqFirstContractMapper;

    @Resource
    private FirstSecondRelationMapper firstSecondRelationMapper;

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
        validateCreateRequest(dto);
        SysCompany serviceCompany = requireServiceCompany(dto.getServiceCompanyId());
        validateServiceHqRelation(serviceCompany, dto.getHqCompanyId());

        WorkOrder workOrder = new WorkOrder();
        workOrder.setOrderNo(generateOrderNo());
        workOrder.setCustomerId(customerId);
        workOrder.setCustomerName(normalizeRequiredText(dto.getCustomerName(), "报修人姓名不能为空"));
        workOrder.setCustomerMobile(normalizeRequiredText(customer.getPhone(), "当前客户手机号不能为空"));
        workOrder.setBarcode(normalizeRequiredText(dto.getBarcode(), "机器条码不能为空"));
        workOrder.setProductCode(normalizeText(dto.getProductCode()));
        workOrder.setProductModel(normalizeText(dto.getProductModel()));
        workOrder.setBrandCode(resolveBrandCode(dto.getBrandCode()));
        workOrder.setServiceMode(normalizeRequiredText(dto.getServiceMode(), "服务方式不能为空"));
        workOrder.setWarrantyStatus(normalizeText(dto.getWarrantyStatus()));
        workOrder.setFaultDesc(normalizeText(dto.getFaultDesc()));
        workOrder.setSenderName(resolveSendField(dto.getServiceMode(), dto.getSenderName()));
        workOrder.setSenderMobile(resolveSendField(dto.getServiceMode(), dto.getSenderMobile()));
        workOrder.setSenderAddress(resolveSendField(dto.getServiceMode(), dto.getSenderAddress()));
        workOrder.setSendExpressNo(resolveSendField(dto.getServiceMode(), dto.getSendExpressNo()));
        workOrder.setMainStatus(WorkOrderStatusFlow.afterCreate());
        workOrder.setEvaluateStatus(WorkOrderStatusFlow.afterCreateEvaluateStatus());
        workOrder.setCurrentAcceptSubjectType("SERVICE");
        workOrder.setCurrentAcceptCompanyId(serviceCompany.getId());
        workOrder.setCreateCompanyId(serviceCompany.getId());
        workOrder.setHqCompanyId(dto.getHqCompanyId());
        workOrder.setHasTransfer(0);
        workOrder.setTransferCount(0);
        workOrderMapper.insert(workOrder);

        saveCreateFlow(workOrder.getId(), customerId, serviceCompany.getId(), workOrder.getMainStatus());
        workOrderParticipantService.initParticipants(workOrder);
        return workOrder.getId();
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
        Map<Long, String> companyNameMap = buildCompanyNameMap(
                records.stream().map(WorkOrder::getCurrentAcceptCompanyId).collect(Collectors.toSet())
        );
        Map<Long, String> userNameMap = buildUserNameMap(
                records.stream().map(WorkOrder::getAssignedUserId).collect(Collectors.toSet())
        );
        List<CustomerWorkOrderListVO> list = records.stream()
                .map(workOrder -> buildListVo(workOrder, companyNameMap, userNameMap))
                .collect(Collectors.toList());
        return PageResult.of(list, result.getTotal(), query.getPageNum(), query.getPageSize());
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
        detail.setProductModel(workOrder.getProductModel());
        detail.setBrandCode(workOrder.getBrandCode());
        detail.setServiceMode(workOrder.getServiceMode());
        detail.setWarrantyStatus(workOrder.getWarrantyStatus());
        detail.setFaultDesc(workOrder.getFaultDesc());
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

        Map<Long, String> companyNameMap = buildCompanyNameMap(Collections.singleton(workOrder.getCurrentAcceptCompanyId()));
        Map<Long, String> userNameMap = buildUserNameMap(Collections.singleton(workOrder.getAssignedUserId()));
        detail.setCurrentAcceptCompanyName(companyNameMap.get(workOrder.getCurrentAcceptCompanyId()));
        detail.setAssignedUserName(userNameMap.get(workOrder.getAssignedUserId()));
        detail.setQuotes(listQuoteVos(workOrderId));
        detail.setRepairs(listRepairVos(workOrderId));
        detail.setReviews(listReviewVos(workOrderId));
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
        LambdaQueryWrapper<WorkOrderEvaluation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WorkOrderEvaluation::getWorkOrderId, workOrder.getId());
        if (workOrderEvaluationMapper.selectCount(wrapper) > 0) {
            throw new ServiceException("当前工单已完成评价");
        }

        WorkOrderEvaluation evaluation = new WorkOrderEvaluation();
        evaluation.setWorkOrderId(workOrder.getId());
        evaluation.setCustomerId(customerId);
        evaluation.setCompanyId(workOrder.getCurrentAcceptCompanyId());
        evaluation.setScore(dto.getScore());
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

        workOrderNotifyEventService.recordCustomerEvaluated(workOrder, dto.getScore(), dto.getContent());
    }

    private Long requireCustomerId() {
        StpCustomerUtil.checkLogin();
        return StpCustomerUtil.getLoginIdAsLong();
    }

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

    private void validateCreateRequest(CustomerWorkOrderCreateDTO dto) {
        if (dto == null) {
            throw new ServiceException("建单参数不能为空");
        }
        String serviceMode = normalizeRequiredText(dto.getServiceMode(), "服务方式不能为空");
        if (!"寄修".equals(serviceMode) && !"到店维修".equals(serviceMode)) {
            throw new ServiceException("服务方式仅支持寄修或到店维修");
        }
        validateSendInfo(dto);
    }

    private void validateSendInfo(CustomerWorkOrderCreateDTO dto) {
        if (dto == null || !"寄修".equals(normalizeText(dto.getServiceMode()))) {
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

    private SysCompany requireServiceCompany(Long serviceCompanyId) {
        SysCompany company = sysCompanyMapper.selectById(serviceCompanyId);
        if (company == null) {
            throw new ServiceException("服务网点不存在");
        }
        if (company.getStatus() != null && company.getStatus() == 0) {
            throw new ServiceException("服务网点已停用");
        }
        if (!"FIRST".equals(company.getTypeCode()) && !"SECOND".equals(company.getTypeCode())) {
            throw new ServiceException("当前公司不是可选服务网点");
        }
        return company;
    }

    private SysCompany requireHqCompany(Long hqCompanyId) {
        SysCompany company = sysCompanyMapper.selectById(hqCompanyId);
        if (company == null) {
            throw new ServiceException("归属总部不存在");
        }
        if (company.getStatus() != null && company.getStatus() == 0) {
            throw new ServiceException("归属总部已停用");
        }
        if ("FIRST".equals(company.getTypeCode()) || "SECOND".equals(company.getTypeCode())) {
            throw new ServiceException("归属总部类型不正确");
        }
        return company;
    }

    private void validateServiceHqRelation(SysCompany serviceCompany, Long hqCompanyId) {
        requireHqCompany(hqCompanyId);
        if ("FIRST".equals(serviceCompany.getTypeCode())) {
            LambdaQueryWrapper<HqFirstContract> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(HqFirstContract::getFirstCompanyId, serviceCompany.getId())
                    .eq(HqFirstContract::getHqCompanyId, hqCompanyId)
                    .eq(HqFirstContract::getStatus, 1);
            if (hqFirstContractMapper.selectCount(wrapper) == 0) {
                throw new ServiceException("当前服务网点不支持归属到该总部");
            }
            return;
        }

        LambdaQueryWrapper<FirstSecondRelation> relationWrapper = new LambdaQueryWrapper<>();
        relationWrapper.eq(FirstSecondRelation::getSecondCompanyId, serviceCompany.getId())
                .eq(FirstSecondRelation::getStatus, 1);
        List<FirstSecondRelation> relations = firstSecondRelationMapper.selectList(relationWrapper);
        if (relations.isEmpty()) {
            throw new ServiceException("当前二级网点未关联上级网点");
        }
        List<Long> firstCompanyIds = relations.stream()
                .map(FirstSecondRelation::getFirstCompanyId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        if (firstCompanyIds.isEmpty()) {
            throw new ServiceException("当前二级网点未关联有效上级网点");
        }
        LambdaQueryWrapper<HqFirstContract> contractWrapper = new LambdaQueryWrapper<>();
        contractWrapper.in(HqFirstContract::getFirstCompanyId, firstCompanyIds)
                .eq(HqFirstContract::getHqCompanyId, hqCompanyId)
                .eq(HqFirstContract::getStatus, 1);
        if (hqFirstContractMapper.selectCount(contractWrapper) == 0) {
            throw new ServiceException("当前服务网点不支持归属到该总部");
        }
    }

    private String resolveSendField(String serviceMode, String value) {
        if (!"寄修".equals(normalizeText(serviceMode))) {
            return null;
        }
        return normalizeText(value);
    }

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

    private CustomerWorkOrderListVO buildListVo(WorkOrder workOrder, Map<Long, String> companyNameMap,
                                                Map<Long, String> userNameMap) {
        CustomerWorkOrderListVO vo = new CustomerWorkOrderListVO();
        vo.setId(workOrder.getId());
        vo.setOrderNo(workOrder.getOrderNo());
        vo.setCustomerName(workOrder.getCustomerName());
        vo.setCustomerMobile(workOrder.getCustomerMobile());
        vo.setBarcode(workOrder.getBarcode());
        vo.setProductModel(workOrder.getProductModel());
        vo.setMainStatus(workOrder.getMainStatus());
        vo.setDisplayStatus(resolveCustomerDisplayStatus(workOrder.getMainStatus()));
        vo.setEvaluateStatus(workOrder.getEvaluateStatus());
        vo.setEvaluateStatusLabel(WorkOrderStatusConstants.resolveEvaluateStatusLabel(workOrder.getEvaluateStatus()));
        vo.setCurrentAcceptCompanyName(companyNameMap.get(workOrder.getCurrentAcceptCompanyId()));
        vo.setAssignedUserName(userNameMap.get(workOrder.getAssignedUserId()));
        vo.setHasTransfer(workOrder.getHasTransfer());
        vo.setCanEvaluate(canEvaluate(workOrder));
        vo.setCreateTime(workOrder.getCreateTime());
        vo.setClosedTime(workOrder.getClosedTime());
        return vo;
    }

    private boolean canEvaluate(WorkOrder workOrder) {
        return workOrder != null
                && WorkOrderStatusConstants.MainStatus.CLOSED.equals(workOrder.getMainStatus())
                && WorkOrderStatusConstants.EvaluateStatus.PENDING_EVALUATE.equals(workOrder.getEvaluateStatus());
    }

    private boolean canEditSendInfo(WorkOrder workOrder) {
        return workOrder != null
                && "寄修".equals(workOrder.getServiceMode())
                && WorkOrderStatusConstants.isWaitAcceptMainStatus(workOrder.getMainStatus());
    }

    private String resolveCustomerDisplayStatus(String mainStatus) {
        return WorkOrderStatusConstants.resolveDisplayStatusLabel(mainStatus);
    }

    private Map<Long, String> buildCompanyNameMap(Set<Long> companyIds) {
        if (companyIds == null || companyIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SysCompany> companies = sysCompanyMapper.selectBatchIds(companyIds);
        Map<Long, String> map = new HashMap<>(companies.size());
        for (SysCompany company : companies) {
            if (company != null) {
                map.put(company.getId(), company.getCompanyName());
            }
        }
        return map;
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
        Map<Long, String> companyNameMap = buildCompanyNameMap(
                reviews.stream().map(WorkOrderReview::getCompanyId).collect(Collectors.toSet())
        );
        Map<Long, String> userNameMap = buildUserNameMap(
                reviews.stream().map(WorkOrderReview::getReviewUserId).collect(Collectors.toSet())
        );
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

    private String generateOrderNo() {
        String datePart = LocalDateTime.now().format(ORDER_DATE_FORMATTER);
        String suffix = IdUtil.getSnowflakeNextIdStr();
        suffix = suffix.substring(Math.max(0, suffix.length() - 5));
        return "WO" + datePart + "-" + suffix;
    }

    private String resolveBrandCode(String brandCode) {
        String value = normalizeText(brandCode);
        return value == null ? "JASIC" : value;
    }

    private String normalizeRequiredText(String value, String message) {
        String text = normalizeText(value);
        if (text == null) {
            throw new ServiceException(message);
        }
        return text;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }
}
