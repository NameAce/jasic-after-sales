package com.jasic.aftersales.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jasic.aftersales.common.core.domain.PageQuery;
import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.common.enums.SubjectTypeEnum;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.framework.security.SecurityContext;
import com.jasic.aftersales.system.domain.dto.SysFeedbackAcceptDTO;
import com.jasic.aftersales.system.domain.dto.SysFeedbackCreateDTO;
import com.jasic.aftersales.system.domain.entity.SysCompany;
import com.jasic.aftersales.system.domain.entity.SysCompanyType;
import com.jasic.aftersales.system.domain.entity.SysFeedback;
import com.jasic.aftersales.system.domain.entity.SysUser;
import com.jasic.aftersales.system.domain.entity.WorkOrder;
import com.jasic.aftersales.system.domain.enums.FeedbackManageViewTypeEnum;
import com.jasic.aftersales.system.domain.enums.FeedbackStatusEnum;
import com.jasic.aftersales.system.domain.enums.FeedbackSubmitSourceTypeEnum;
import com.jasic.aftersales.system.domain.enums.FeedbackSubmitterTypeEnum;
import com.jasic.aftersales.system.domain.query.SysFeedbackManageQuery;
import com.jasic.aftersales.system.domain.query.SysFeedbackMyQuery;
import com.jasic.aftersales.system.domain.vo.SysFeedbackVO;
import com.jasic.aftersales.system.mapper.SysCompanyMapper;
import com.jasic.aftersales.system.mapper.SysFeedbackMapper;
import com.jasic.aftersales.system.mapper.SysUserMapper;
import com.jasic.aftersales.system.mapper.WorkOrderMapper;
import com.jasic.aftersales.system.service.ISysCompanyTypeService;
import com.jasic.aftersales.system.service.ISysConfigService;
import com.jasic.aftersales.system.service.ISysFeedbackService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 平台反馈单服务实现。
 *
 * <p>负责反馈提交、我的反馈查询、后台管理查询、首次受理和修改受理。</p>
 *
 * @author Codex
 * @date 2026/05/28
 */
@Service
public class SysFeedbackServiceImpl implements ISysFeedbackService {

    /** 默认总部配置键。 */
    private static final String DEFAULT_HQ_COMPANY_ID_CONFIG_KEY = "default.hq.company.id";

    /** 启用状态值。 */
    private static final Integer STATUS_ENABLED = 1;

    /** 终端用户直接提交时的来源名称。 */
    private static final String CUSTOMER_DIRECT_SOURCE_NAME = "终端用户";

    /** 仅传日期时的格式。 */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 传完整时间时的格式。 */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private SysFeedbackMapper sysFeedbackMapper;

    @Resource
    private WorkOrderMapper workOrderMapper;

    @Resource
    private SysCompanyMapper sysCompanyMapper;

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private ISysConfigService sysConfigService;

    @Resource
    private ISysCompanyTypeService sysCompanyTypeService;

    /**
     * 终端用户提交反馈。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createForCustomer(Long customerId, String submitterName, String contactPhone, String content) {
        if (customerId == null) {
            throw new ServiceException("当前终端用户不存在");
        }
        String normalizedContent = normalizeAndValidateContent(content);
        CustomerFeedbackSourceSnapshot sourceSnapshot = resolveCustomerFeedbackSourceSnapshot(customerId);

        SysFeedback entity = new SysFeedback();
        entity.setSubmitterType(FeedbackSubmitterTypeEnum.CUSTOMER.getCode());
        entity.setSubmitterId(customerId);
        entity.setSubmitterName(normalizeNullableText(submitterName));
        entity.setSubmitCompanyId(null);
        entity.setSubmitSourceType(sourceSnapshot.getSubmitSourceType());
        entity.setSubmitSourceName(sourceSnapshot.getSubmitSourceName());
        entity.setContactPhone(normalizeNullableText(contactPhone));
        entity.setRelatedWorkOrderId(sourceSnapshot.getRelatedWorkOrderId());
        entity.setHqCompanyId(sourceSnapshot.getHqCompanyId());
        entity.setContent(normalizedContent);
        entity.setStatus(FeedbackStatusEnum.UNACCEPTED.getCode());
        entity.setAcceptUserId(null);
        entity.setAcceptUserName(null);
        entity.setAcceptTime(null);
        entity.setAcceptReply(null);
        sysFeedbackMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 查询终端用户自己的反馈列表。
     */
    @Override
    public PageResult<SysFeedbackVO> listCustomerPage(Long customerId, SysFeedbackMyQuery query) {
        if (customerId == null) {
            throw new ServiceException("当前终端用户不存在");
        }
        SysFeedbackMyQuery actualQuery = query == null ? new SysFeedbackMyQuery() : query;
        Page<SysFeedback> page = new Page<>(actualQuery.getPageNum(), actualQuery.getPageSize());
        LambdaQueryWrapper<SysFeedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysFeedback::getSubmitterType, FeedbackSubmitterTypeEnum.CUSTOMER.getCode())
                .eq(SysFeedback::getSubmitterId, customerId)
                .orderByDesc(SysFeedback::getCreateTime);
        return buildPageResult(sysFeedbackMapper.selectPage(page, wrapper), actualQuery);
    }

    /**
     * 查询终端用户自己的反馈详情。
     */
    @Override
    public SysFeedbackVO getCustomerDetail(Long customerId, Long feedbackId) {
        if (customerId == null) {
            throw new ServiceException("当前终端用户不存在");
        }
        SysFeedback feedback = sysFeedbackMapper.selectOne(new LambdaQueryWrapper<SysFeedback>()
                .eq(SysFeedback::getId, feedbackId)
                .eq(SysFeedback::getSubmitterType, FeedbackSubmitterTypeEnum.CUSTOMER.getCode())
                .eq(SysFeedback::getSubmitterId, customerId)
                .last("LIMIT 1"));
        if (feedback == null) {
            throw new ServiceException("反馈不存在");
        }
        return convertToVO(feedback);
    }

    /**
     * 网点用户提交反馈。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createForCurrentServiceUser(SysFeedbackCreateDTO dto) {
        SysUser currentUser = requireCurrentSystemUserBySubject(SubjectTypeEnum.SERVICE, "当前主体不是网点用户，不能提交反馈");
        Long currentCompanyId = SecurityContext.getCurrentCompanyId();
        if (currentCompanyId == null) {
            throw new ServiceException("当前网点上下文缺失");
        }
        SysCompany currentCompany = requireEnabledCompany(currentCompanyId, "当前网点不存在或已停用");
        String normalizedContent = normalizeAndValidateContent(dto == null ? null : dto.getContent());

        SysFeedback entity = new SysFeedback();
        entity.setSubmitterType(FeedbackSubmitterTypeEnum.SERVICE_COMPANY_USER.getCode());
        entity.setSubmitterId(currentUser.getId());
        entity.setSubmitterName(resolveSystemUserSnapshotName(currentUser));
        entity.setSubmitCompanyId(currentCompanyId);
        entity.setSubmitSourceType(FeedbackSubmitSourceTypeEnum.SERVICE_COMPANY.getCode());
        entity.setSubmitSourceName(normalizeNullableText(currentCompany.getCompanyName()));
        entity.setContactPhone(normalizeNullableText(currentUser.getPhone()));
        entity.setRelatedWorkOrderId(null);
        entity.setHqCompanyId(resolveDefaultHqCompanyId());
        entity.setContent(normalizedContent);
        entity.setStatus(FeedbackStatusEnum.UNACCEPTED.getCode());
        entity.setAcceptUserId(null);
        entity.setAcceptUserName(null);
        entity.setAcceptTime(null);
        entity.setAcceptReply(null);
        sysFeedbackMapper.insert(entity);
        return entity.getId();
    }

    /**
     * 查询网点用户自己的反馈列表。
     */
    @Override
    public PageResult<SysFeedbackVO> listCurrentServiceUserPage(SysFeedbackMyQuery query) {
        SysUser currentUser = requireCurrentSystemUserBySubject(SubjectTypeEnum.SERVICE, "当前主体不是网点用户，不能查看我的反馈");
        SysFeedbackMyQuery actualQuery = query == null ? new SysFeedbackMyQuery() : query;
        Page<SysFeedback> page = new Page<>(actualQuery.getPageNum(), actualQuery.getPageSize());
        LambdaQueryWrapper<SysFeedback> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysFeedback::getSubmitterType, FeedbackSubmitterTypeEnum.SERVICE_COMPANY_USER.getCode())
                .eq(SysFeedback::getSubmitterId, currentUser.getId())
                .orderByDesc(SysFeedback::getCreateTime);
        return buildPageResult(sysFeedbackMapper.selectPage(page, wrapper), actualQuery);
    }

    /**
     * 查询网点用户自己的反馈详情。
     */
    @Override
    public SysFeedbackVO getCurrentServiceUserDetail(Long feedbackId) {
        SysUser currentUser = requireCurrentSystemUserBySubject(SubjectTypeEnum.SERVICE, "当前主体不是网点用户，不能查看我的反馈");
        SysFeedback feedback = sysFeedbackMapper.selectOne(new LambdaQueryWrapper<SysFeedback>()
                .eq(SysFeedback::getId, feedbackId)
                .eq(SysFeedback::getSubmitterType, FeedbackSubmitterTypeEnum.SERVICE_COMPANY_USER.getCode())
                .eq(SysFeedback::getSubmitterId, currentUser.getId())
                .last("LIMIT 1"));
        if (feedback == null) {
            throw new ServiceException("反馈不存在");
        }
        return convertToVO(feedback);
    }

    /**
     * 查询后台管理列表。
     */
    @Override
    public PageResult<SysFeedbackVO> listManagePage(SysFeedbackManageQuery query) {
        requireCurrentSystemUserBySubject(SubjectTypeEnum.HQ, "当前主体不是总部，不能查看反馈管理列表");
        SysFeedbackManageQuery actualQuery = query == null ? new SysFeedbackManageQuery() : query;
        FeedbackManageViewTypeEnum viewType = resolveManageViewType(actualQuery.getViewType());

        Page<SysFeedback> page = new Page<>(actualQuery.getPageNum(), actualQuery.getPageSize());
        LambdaQueryWrapper<SysFeedback> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(actualQuery.getContactPhone())) {
            wrapper.like(SysFeedback::getContactPhone, actualQuery.getContactPhone().trim());
        }
        if (StringUtils.hasText(actualQuery.getSubmitSourceName())) {
            wrapper.like(SysFeedback::getSubmitSourceName, actualQuery.getSubmitSourceName().trim());
        }

        LocalDateTime beginCreateTime = parseRangeDateTime(actualQuery.getBeginCreateTime(), false, "提交开始时间格式不正确");
        LocalDateTime endCreateTime = parseRangeDateTime(actualQuery.getEndCreateTime(), true, "提交结束时间格式不正确");
        LocalDateTime beginAcceptTime = parseRangeDateTime(actualQuery.getBeginAcceptTime(), false, "受理开始时间格式不正确");
        LocalDateTime endAcceptTime = parseRangeDateTime(actualQuery.getEndAcceptTime(), true, "受理结束时间格式不正确");
        wrapper.ge(beginCreateTime != null, SysFeedback::getCreateTime, beginCreateTime)
                .le(endCreateTime != null, SysFeedback::getCreateTime, endCreateTime)
                .ge(beginAcceptTime != null, SysFeedback::getAcceptTime, beginAcceptTime)
                .le(endAcceptTime != null, SysFeedback::getAcceptTime, endAcceptTime);

        if (FeedbackManageViewTypeEnum.UNACCEPTED == viewType) {
            wrapper.eq(SysFeedback::getStatus, FeedbackStatusEnum.UNACCEPTED.getCode())
                    .orderByAsc(SysFeedback::getCreateTime);
        } else if (FeedbackManageViewTypeEnum.ACCEPTED == viewType) {
            wrapper.eq(SysFeedback::getStatus, FeedbackStatusEnum.ACCEPTED.getCode())
                    .orderByDesc(SysFeedback::getAcceptTime);
        } else {
            wrapper.orderByDesc(SysFeedback::getCreateTime);
        }

        return buildPageResult(sysFeedbackMapper.selectPage(page, wrapper), actualQuery);
    }

    /**
     * 查询后台详情。
     */
    @Override
    public SysFeedbackVO getManageDetail(Long feedbackId) {
        requireCurrentSystemUserBySubject(SubjectTypeEnum.HQ, "当前主体不是总部，不能查看反馈详情");
        SysFeedback feedback = sysFeedbackMapper.selectById(feedbackId);
        if (feedback == null) {
            throw new ServiceException("反馈不存在");
        }
        return convertToVO(feedback);
    }

    /**
     * 首次受理反馈。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void accept(SysFeedbackAcceptDTO dto) {
        SysUser currentUser = requireCurrentSystemUserBySubject(SubjectTypeEnum.HQ, "当前主体不是总部，不能受理反馈");
        if (dto == null || dto.getId() == null) {
            throw new ServiceException("反馈ID不能为空");
        }
        String normalizedAcceptReply = normalizeAndValidateAcceptReply(dto.getAcceptReply());
        SysFeedback updateEntity = buildAcceptUpdateEntity(currentUser, normalizedAcceptReply);
        int rows = sysFeedbackMapper.update(updateEntity, new LambdaUpdateWrapper<SysFeedback>()
                .eq(SysFeedback::getId, dto.getId())
                .eq(SysFeedback::getStatus, FeedbackStatusEnum.UNACCEPTED.getCode()));
        if (rows > 0) {
            return;
        }

        SysFeedback existing = sysFeedbackMapper.selectById(dto.getId());
        if (existing == null) {
            throw new ServiceException("反馈不存在");
        }
        if (FeedbackStatusEnum.ACCEPTED.getCode().equals(existing.getStatus())) {
            throw new ServiceException("当前反馈已受理");
        }
        throw new ServiceException("反馈受理失败");
    }

    /**
     * 修改已受理反馈。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAccept(SysFeedbackAcceptDTO dto) {
        SysUser currentUser = requireCurrentSystemUserBySubject(SubjectTypeEnum.HQ, "当前主体不是总部，不能修改受理");
        if (dto == null || dto.getId() == null) {
            throw new ServiceException("反馈ID不能为空");
        }
        String normalizedAcceptReply = normalizeAndValidateAcceptReply(dto.getAcceptReply());
        SysFeedback updateEntity = buildAcceptUpdateEntity(currentUser, normalizedAcceptReply);
        int rows = sysFeedbackMapper.update(updateEntity, new LambdaUpdateWrapper<SysFeedback>()
                .eq(SysFeedback::getId, dto.getId())
                .eq(SysFeedback::getStatus, FeedbackStatusEnum.ACCEPTED.getCode()));
        if (rows > 0) {
            return;
        }

        SysFeedback existing = sysFeedbackMapper.selectById(dto.getId());
        if (existing == null) {
            throw new ServiceException("反馈不存在");
        }
        if (FeedbackStatusEnum.UNACCEPTED.getCode().equals(existing.getStatus())) {
            throw new ServiceException("当前反馈未受理，不能修改受理");
        }
        throw new ServiceException("修改受理失败");
    }

    /**
     * 构建受理更新实体。
     */
    private SysFeedback buildAcceptUpdateEntity(SysUser currentUser, String acceptReply) {
        SysFeedback updateEntity = new SysFeedback();
        updateEntity.setStatus(FeedbackStatusEnum.ACCEPTED.getCode());
        updateEntity.setAcceptUserId(currentUser.getId());
        updateEntity.setAcceptUserName(resolveSystemUserSnapshotName(currentUser));
        updateEntity.setAcceptTime(LocalDateTime.now());
        updateEntity.setAcceptReply(acceptReply);
        return updateEntity;
    }

    /**
     * 构建分页结果。
     */
    private PageResult<SysFeedbackVO> buildPageResult(Page<SysFeedback> result, PageQuery query) {
        List<SysFeedbackVO> records = result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        return PageResult.of(records, result.getTotal(), query.getPageNum(), query.getPageSize());
    }

    /**
     * 实体转返回对象。
     */
    private SysFeedbackVO convertToVO(SysFeedback entity) {
        SysFeedbackVO vo = new SysFeedbackVO();
        BeanUtil.copyProperties(entity, vo);
        return vo;
    }

    /**
     * 解析终端用户来源快照。
     */
    private CustomerFeedbackSourceSnapshot resolveCustomerFeedbackSourceSnapshot(Long customerId) {
        Long defaultHqCompanyId = resolveDefaultHqCompanyId();
        WorkOrder latestWorkOrder = workOrderMapper.selectOne(new LambdaQueryWrapper<WorkOrder>()
                .eq(WorkOrder::getCustomerId, customerId)
                .orderByDesc(WorkOrder::getCreateTime)
                .last("LIMIT 1"));
        if (latestWorkOrder == null) {
            return buildCustomerDirectSnapshot(defaultHqCompanyId);
        }

        Long serviceCompanyId = latestWorkOrder.getCreateCompanyId() != null
                ? latestWorkOrder.getCreateCompanyId()
                : latestWorkOrder.getCurrentAcceptCompanyId();
        SysCompany serviceCompany = findEnabledCompany(serviceCompanyId);
        if (serviceCompany == null || !StringUtils.hasText(serviceCompany.getCompanyName())) {
            return buildCustomerDirectSnapshot(defaultHqCompanyId);
        }

        CustomerFeedbackSourceSnapshot snapshot = new CustomerFeedbackSourceSnapshot();
        snapshot.setSubmitSourceType(FeedbackSubmitSourceTypeEnum.CUSTOMER_WORK_ORDER.getCode());
        snapshot.setSubmitSourceName(serviceCompany.getCompanyName().trim());
        snapshot.setRelatedWorkOrderId(latestWorkOrder.getId());
        snapshot.setHqCompanyId(resolveWorkOrderHqCompanyId(latestWorkOrder, defaultHqCompanyId));
        return snapshot;
    }

    /**
     * 构建终端用户直接提交快照。
     */
    private CustomerFeedbackSourceSnapshot buildCustomerDirectSnapshot(Long defaultHqCompanyId) {
        CustomerFeedbackSourceSnapshot snapshot = new CustomerFeedbackSourceSnapshot();
        snapshot.setSubmitSourceType(FeedbackSubmitSourceTypeEnum.CUSTOMER_DIRECT.getCode());
        snapshot.setSubmitSourceName(CUSTOMER_DIRECT_SOURCE_NAME);
        snapshot.setRelatedWorkOrderId(null);
        snapshot.setHqCompanyId(defaultHqCompanyId);
        return snapshot;
    }

    /**
     * 解析工单总部归属。
     */
    private Long resolveWorkOrderHqCompanyId(WorkOrder workOrder, Long defaultHqCompanyId) {
        if (workOrder == null || workOrder.getHqCompanyId() == null) {
            return defaultHqCompanyId;
        }
        SysCompany hqCompany = findEnabledCompany(workOrder.getHqCompanyId());
        if (hqCompany == null || !isHqCompany(hqCompany)) {
            return defaultHqCompanyId;
        }
        return workOrder.getHqCompanyId();
    }

    /**
     * 解析后台视图类型。
     */
    private FeedbackManageViewTypeEnum resolveManageViewType(String viewTypeCode) {
        if (!StringUtils.hasText(viewTypeCode)) {
            return FeedbackManageViewTypeEnum.ALL;
        }
        FeedbackManageViewTypeEnum viewType = FeedbackManageViewTypeEnum.getByCode(viewTypeCode.trim());
        if (viewType == null) {
            throw new ServiceException("反馈视图类型不正确");
        }
        return viewType;
    }

    /**
     * 解析区间时间。
     */
    private LocalDateTime parseRangeDateTime(String text, boolean endOfDay, String errorMessage) {
        String normalized = normalizeNullableText(text);
        if (normalized == null) {
            return null;
        }
        try {
            if (normalized.length() == 10) {
                LocalDate date = LocalDate.parse(normalized, DATE_FORMATTER);
                return endOfDay ? date.atTime(LocalTime.of(23, 59, 59)) : date.atStartOfDay();
            }
            return LocalDateTime.parse(normalized, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new ServiceException(errorMessage);
        }
    }

    /**
     * 校验反馈内容。
     */
    private String normalizeAndValidateContent(String content) {
        String normalized = normalizeNullableText(content);
        if (normalized == null) {
            throw new ServiceException("反馈内容不能为空");
        }
        if (normalized.length() < 5) {
            throw new ServiceException("反馈内容长度不能少于5个字符");
        }
        if (normalized.length() > 500) {
            throw new ServiceException("反馈内容长度不能超过500个字符");
        }
        return normalized;
    }

    /**
     * 校验受理回复。
     */
    private String normalizeAndValidateAcceptReply(String acceptReply) {
        String normalized = normalizeNullableText(acceptReply);
        if (normalized == null) {
            throw new ServiceException("受理回复不能为空");
        }
        if (normalized.length() > 200) {
            throw new ServiceException("受理回复长度不能超过200个字符");
        }
        return normalized;
    }

    /**
     * 解析系统用户姓名快照。
     */
    private String resolveSystemUserSnapshotName(SysUser user) {
        String realName = normalizeNullableText(user == null ? null : user.getRealName());
        if (realName != null) {
            return realName;
        }
        return normalizeNullableText(user == null ? null : user.getUsername());
    }

    /**
     * 校验当前系统用户。
     */
    private SysUser requireCurrentSystemUserBySubject(SubjectTypeEnum subjectType, String invalidMessage) {
        if (subjectType == null || !subjectType.getCode().equals(SecurityContext.getCurrentSubjectType())) {
            throw new ServiceException(invalidMessage);
        }
        Long currentUserId = SecurityContext.getCurrentUserId();
        if (currentUserId == null) {
            throw new ServiceException("当前系统用户不存在");
        }
        SysUser currentUser = sysUserMapper.selectById(currentUserId);
        if (currentUser == null) {
            throw new ServiceException("当前系统用户不存在");
        }
        if (!Objects.equals(currentUser.getStatus(), STATUS_ENABLED)) {
            throw new ServiceException("当前系统用户已停用");
        }
        return currentUser;
    }

    /**
     * 解析默认总部。
     */
    private Long resolveDefaultHqCompanyId() {
        String configValue = normalizeNullableText(sysConfigService == null ? null : sysConfigService.getValueByKey(DEFAULT_HQ_COMPANY_ID_CONFIG_KEY));
        if (configValue == null) {
            throw new ServiceException("默认总部配置缺失");
        }
        Long companyId;
        try {
            companyId = Long.valueOf(configValue);
        } catch (NumberFormatException ex) {
            throw new ServiceException("默认总部配置缺失");
        }
        SysCompany company = findEnabledCompany(companyId);
        if (company == null || !isHqCompany(company)) {
            throw new ServiceException("默认总部配置缺失");
        }
        return companyId;
    }

    /**
     * 查询并校验公司。
     */
    private SysCompany requireEnabledCompany(Long companyId, String missingMessage) {
        SysCompany company = findEnabledCompany(companyId);
        if (company == null) {
            throw new ServiceException(missingMessage);
        }
        return company;
    }

    /**
     * 查询启用中的公司。
     */
    private SysCompany findEnabledCompany(Long companyId) {
        if (companyId == null) {
            return null;
        }
        SysCompany company = sysCompanyMapper.selectById(companyId);
        if (company == null || !Objects.equals(company.getStatus(), STATUS_ENABLED)) {
            return null;
        }
        return company;
    }

    /**
     * 判断是否总部公司。
     */
    private boolean isHqCompany(SysCompany company) {
        if (company == null || !StringUtils.hasText(company.getTypeCode())) {
            return false;
        }
        Map<String, String> subjectTypeMap = sysCompanyTypeService.listAll().stream()
                .collect(Collectors.toMap(SysCompanyType::getTypeCode, SysCompanyType::getSubjectType, (a, b) -> a));
        return SubjectTypeEnum.HQ.getCode().equals(subjectTypeMap.get(company.getTypeCode()));
    }

    /**
     * 规范化可空文本。
     */
    private String normalizeNullableText(String text) {
        return StringUtils.hasText(text) ? text.trim() : null;
    }

    /**
     * 终端用户来源快照。
     */
    private static class CustomerFeedbackSourceSnapshot {

        private String submitSourceType;

        private String submitSourceName;

        private Long relatedWorkOrderId;

        private Long hqCompanyId;

        public String getSubmitSourceType() {
            return submitSourceType;
        }

        public void setSubmitSourceType(String submitSourceType) {
            this.submitSourceType = submitSourceType;
        }

        public String getSubmitSourceName() {
            return submitSourceName;
        }

        public void setSubmitSourceName(String submitSourceName) {
            this.submitSourceName = submitSourceName;
        }

        public Long getRelatedWorkOrderId() {
            return relatedWorkOrderId;
        }

        public void setRelatedWorkOrderId(Long relatedWorkOrderId) {
            this.relatedWorkOrderId = relatedWorkOrderId;
        }

        public Long getHqCompanyId() {
            return hqCompanyId;
        }

        public void setHqCompanyId(Long hqCompanyId) {
            this.hqCompanyId = hqCompanyId;
        }
    }
}
