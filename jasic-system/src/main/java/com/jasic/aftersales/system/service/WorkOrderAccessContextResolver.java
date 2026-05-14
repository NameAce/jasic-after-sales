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
     * 总部一级合同Mapper数据访问接口。
     *
     * @return 处理结果
     */
    @Resource
    private HqFirstContractMapper hqFirstContractMapper;

    @Resource
    private FirstSecondRelationMapper firstSecondRelationMapper;

    /**
     * 处理resolve业务逻辑。
     *
     * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
     * @return 处理结果
     */
    public WorkOrderAccessContext resolve() {
        // 调用WorkOrderAccessContext方法，复用统一能力并保证业务规则一致。
        WorkOrderAccessContext context = new WorkOrderAccessContext();
        // 调用getCurrentUserId方法，复用统一能力并保证业务规则一致。
        context.setCurrentUserId(SecurityContext.getCurrentUserId());
        // 调用getCurrentSubjectType方法，复用统一能力并保证业务规则一致。
        context.setSubjectType(SecurityContext.getCurrentSubjectType());
        // 调用getCurrentTypeCode方法，复用统一能力并保证业务规则一致。
        context.setTypeCode(SecurityContext.getCurrentTypeCode());
        // 调用isPlatformUser方法，复用统一能力并保证业务规则一致。
        context.setPlatformUser(SecurityContext.isPlatformUser());
        if (context.isPlatformUser()) {
            throw new ServiceException("平台账号不参与工单业务");
        }
        // 调用getCurrentCompanyId方法，复用统一能力并保证业务规则一致。
        Long currentCompanyId = SecurityContext.getCurrentCompanyId();
        if (currentCompanyId == null) {
            throw new ServiceException("缺少公司数据访问上下文");
        }
        // 调用setCurrentCompanyId方法，复用统一能力并保证业务规则一致。
        context.setCurrentCompanyId(currentCompanyId);
        // 调用getSubjectType方法，复用统一能力并保证业务规则一致。
        DataScopeEnum dataScope = DataScopeEnum.normalize(SecurityContext.getEffectiveDataScope(), context.getSubjectType());
        // 调用setDataScopeEnum方法，复用统一能力并保证业务规则一致。
        context.setDataScopeEnum(dataScope);
        // 调用getCode方法，复用统一能力并保证业务规则一致。
        context.setDataScope(dataScope.getCode());
        // 调用getCurrentRegionIds方法，复用统一能力并保证业务规则一致。
        context.setCurrentRegionIds(SecurityContext.getCurrentRegionIds());
        // 调用resolveRelatedCompanyIds方法，复用统一能力并保证业务规则一致。
        context.setRelatedCompanyIds(resolveRelatedCompanyIds(context));
        return context;
    }

    /**
     * 解析Related公司Ids。
     *
     * @param context 参数
     * @return 处理结果
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
     * requiresRelated公司Limit。
     *
     * @param context 参数
     */
    private boolean requiresRelatedCompanyLimit(WorkOrderAccessContext context) {
        return SubjectTypeEnum.HQ.getCode().equals(context.getSubjectType())
                // 调用getDataScopeEnum方法，复用统一能力并保证业务规则一致。
                && DataScopeEnum.REGION == context.getDataScopeEnum();
    }

    /**
     * 解析地区公司Ids。
     *
     * @return 处理结果
     */
    private List<Long> resolveRegionCompanyIds(Long currentCompanyId, List<Long> currentRegionIds) {
        if (currentRegionIds == null || currentRegionIds.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<HqFirstContract> contractWrapper = new LambdaQueryWrapper<>();
        contractWrapper.eq(HqFirstContract::getHqCompanyId, currentCompanyId)
                .eq(HqFirstContract::getStatus, 1)
                // 调用in方法，复用统一能力并保证业务规则一致。
                .in(HqFirstContract::getRegionId, currentRegionIds);
        // 说明：执行该步骤以保证业务流程正确。
        List<HqFirstContract> contracts = hqFirstContractMapper.selectList(contractWrapper);
        if (contracts == null || contracts.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> relatedCompanyIds = contracts.stream()
                .map(HqFirstContract::getFirstCompanyId)
                .filter(id -> id != null)
                // 调用toCollection方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return appendSecondLevelCompanies(relatedCompanyIds);
    }

    /**
     * 解析一级Level公司范围。
     *
     * @return 处理结果
     */
    private List<Long> resolveFirstLevelCompanyScope(Long currentCompanyId) {
        Set<Long> relatedCompanyIds = new LinkedHashSet<>();
        // 调用add方法，复用统一能力并保证业务规则一致。
        relatedCompanyIds.add(currentCompanyId);
        return appendSecondLevelCompanies(relatedCompanyIds);
    }

    /**
     * append二级LevelCompanies。
     *
     * @return 处理结果
     */
    private List<Long> appendSecondLevelCompanies(Set<Long> relatedCompanyIds) {
        if (relatedCompanyIds == null || relatedCompanyIds.isEmpty()) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<FirstSecondRelation> relationWrapper = new LambdaQueryWrapper<>();
        relationWrapper.eq(FirstSecondRelation::getStatus, 1)
                // 调用in方法，复用统一能力并保证业务规则一致。
                .in(FirstSecondRelation::getFirstCompanyId, relatedCompanyIds);
        // 说明：执行该步骤以保证业务流程正确。
        List<FirstSecondRelation> relations = firstSecondRelationMapper.selectList(relationWrapper);
        if (relations == null || relations.isEmpty()) {
            return new ArrayList<>(relatedCompanyIds);
        }
        for (FirstSecondRelation relation : relations) {
            if (relation.getSecondCompanyId() != null) {
                // 调用getSecondCompanyId方法，复用统一能力并保证业务规则一致。
                relatedCompanyIds.add(relation.getSecondCompanyId());
            }
        }
        return new ArrayList<>(relatedCompanyIds);
    }
}


