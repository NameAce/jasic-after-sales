package com.jasic.aftersales.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jasic.aftersales.common.enums.DataScopeEnum;
import com.jasic.aftersales.common.enums.SubjectTypeEnum;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.framework.security.SecurityContext;
import com.jasic.aftersales.system.domain.access.WorkOrderAccessContext;
import com.jasic.aftersales.system.domain.entity.FirstSecondRelation;
import com.jasic.aftersales.system.domain.entity.HqFirstContract;
import com.jasic.aftersales.system.mapper.FirstSecondRelationMapper;
import com.jasic.aftersales.system.mapper.HqFirstContractMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 工单访问上下文解析器。
 *
 * @author Codex
 * @date 2026/05/05
 */
@Service
public class WorkOrderAccessContextResolver {

    /**
     * ???????
     *
     * @return ????
     */
    @Resource
    private HqFirstContractMapper hqFirstContractMapper;

    @Resource
    private FirstSecondRelationMapper firstSecondRelationMapper;

    public WorkOrderAccessContext resolve() {
        WorkOrderAccessContext context = new WorkOrderAccessContext();
        context.setCurrentUserId(SecurityContext.getCurrentUserId());
        context.setSubjectType(SecurityContext.getCurrentSubjectType());
        context.setTypeCode(SecurityContext.getCurrentTypeCode());
        context.setPlatformUser(SecurityContext.isPlatformUser());
        if (context.isPlatformUser()) {
            throw new ServiceException("平台账号不参与工单业务");
        }
        Long currentCompanyId = SecurityContext.getCurrentCompanyId();
        if (currentCompanyId == null) {
            throw new ServiceException("缺少公司数据访问上下文");
        }
        context.setCurrentCompanyId(currentCompanyId);
        DataScopeEnum dataScope = DataScopeEnum.normalize(SecurityContext.getEffectiveDataScope(), context.getSubjectType());
        context.setDataScopeEnum(dataScope);
        context.setDataScope(dataScope.getCode());
        context.setCurrentRegionIds(SecurityContext.getCurrentRegionIds());
        context.setRelatedCompanyIds(resolveRelatedCompanyIds(context));
        return context;
    }

    /**
     * ???????
     *
     * @param context ?????
     * @return ????
     */
    private List<Long> resolveRelatedCompanyIds(WorkOrderAccessContext context) {
        if (context == null || context.getCurrentCompanyId() == null) {
            return Collections.emptyList();
        }
        if (requiresRelatedCompanyLimit(context)) {
            return resolveRegionCompanyIds(context.getCurrentCompanyId(), context.getCurrentRegionIds());
        }
        if (SubjectTypeEnum.SERVICE.getCode().equals(context.getSubjectType())
                && "SITE_FIRST".equals(context.getTypeCode())
                && DataScopeEnum.ALL == context.getDataScopeEnum()) {
            return resolveFirstLevelCompanyScope(context.getCurrentCompanyId());
        }
        return Collections.emptyList();
    }

    /**
     * ??????????
     *
     * @param context ?????
     * @return true ??????
     */
    private boolean requiresRelatedCompanyLimit(WorkOrderAccessContext context) {
        return SubjectTypeEnum.HQ.getCode().equals(context.getSubjectType())
                && DataScopeEnum.REGION == context.getDataScopeEnum();
    }

    /**
     * ???????
     *
     * @param currentCompanyId ????ID
     * @param currentRegionIds ????ID??
     * @return ????
     */
    private List<Long> resolveRegionCompanyIds(Long currentCompanyId, List<Long> currentRegionIds) {
        if (currentRegionIds == null || currentRegionIds.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<HqFirstContract> contractWrapper = new LambdaQueryWrapper<>();
        contractWrapper.eq(HqFirstContract::getHqCompanyId, currentCompanyId)
                .eq(HqFirstContract::getStatus, 1)
                .in(HqFirstContract::getRegionId, currentRegionIds);
        // ??????????????????????????
        List<HqFirstContract> contracts = hqFirstContractMapper.selectList(contractWrapper);
        if (contracts == null || contracts.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> relatedCompanyIds = contracts.stream()
                .map(HqFirstContract::getFirstCompanyId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return appendSecondLevelCompanies(relatedCompanyIds);
    }

    /**
     * ???????
     *
     * @param currentCompanyId ????ID
     * @return ????
     */
    private List<Long> resolveFirstLevelCompanyScope(Long currentCompanyId) {
        Set<Long> relatedCompanyIds = new LinkedHashSet<>();
        relatedCompanyIds.add(currentCompanyId);
        return appendSecondLevelCompanies(relatedCompanyIds);
    }

    /**
     * ?? appendSecondLevelCompanies ?????
     *
     * @param relatedCompanyIds related Company ID??
     * @return ????
     */
    private List<Long> appendSecondLevelCompanies(Set<Long> relatedCompanyIds) {
        if (relatedCompanyIds == null || relatedCompanyIds.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<FirstSecondRelation> relationWrapper = new LambdaQueryWrapper<>();
        relationWrapper.eq(FirstSecondRelation::getStatus, 1)
                .in(FirstSecondRelation::getFirstCompanyId, relatedCompanyIds);
        // ??????????????????????????
        List<FirstSecondRelation> relations = firstSecondRelationMapper.selectList(relationWrapper);
        if (relations == null || relations.isEmpty()) {
            return new ArrayList<>(relatedCompanyIds);
        }
        for (FirstSecondRelation relation : relations) {
            if (relation.getSecondCompanyId() != null) {
                relatedCompanyIds.add(relation.getSecondCompanyId());
            }
        }
        return new ArrayList<>(relatedCompanyIds);
    }
}
