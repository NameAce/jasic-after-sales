package com.jasic.aftersales.system.service;

import com.jasic.aftersales.common.enums.SubjectTypeEnum;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.framework.datapermission.CompanyDataAccessContext;
import com.jasic.aftersales.framework.security.SecurityContext;
import com.jasic.aftersales.system.domain.entity.SysCompany;
import com.jasic.aftersales.system.domain.entity.SysCompanyType;
import com.jasic.aftersales.system.mapper.SysCompanyMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 公司数据访问目标解析服务。
 *
 * @author Codex
 * @date 2026/05/05
 */
@Service
public class CompanyDataAccessService {

    private static final Integer STATUS_ENABLED = 1;

    /**
     * 公司数据Access上下文字段。
     *
     * @return 处理结果
     */
    @Resource
    private CompanyDataAccessContext companyDataAccessContext;

    @Resource
    private SysCompanyMapper sysCompanyMapper;

    @Resource
    private ISysCompanyTypeService companyTypeService;

    /**
     * 处理resolveCurrentCompanyTarget业务逻辑。
     *
     * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
     * @param targetCompanyId 参数
     * @return 处理结果
     */
    public Long resolveCurrentCompanyTarget(Long targetCompanyId) {
        if (SecurityContext.isPlatformUser()) {
            if (targetCompanyId == null) {
                throw new ServiceException("缺少目标公司上下文");
            }
            // 说明：执行该步骤以保证业务流程正确。
            validateEnabledCompany(targetCompanyId, "目标公司不存在", "目标公司已停用");
            return targetCompanyId;
        }
        // 调用getCurrentCompanyId方法，复用统一能力并保证业务规则一致。
        Long currentCompanyId = SecurityContext.getCurrentCompanyId();
        if (currentCompanyId == null) {
            throw new ServiceException("缺少公司数据访问上下文");
        }
        if (targetCompanyId != null && !Objects.equals(targetCompanyId, currentCompanyId)) {
            throw new ServiceException("无权操作目标公司数据");
        }
        return currentCompanyId;
    }

    /**
     * 解析当前公司页的数据访问目标。
     *
     * <p>适用于“当前公司”语义明确的页面，例如当前登录公司自己的用户、角色等。
     * 平台账号进入这类页面时也应默认回退到 Session 中已选中的当前公司，而不是强制要求显式传目标公司。</p>
     *
     * @param targetCompanyId 显式传入的目标公司ID，可为空
     * @return 当前公司ID
     */
    public Long resolveCurrentCompanyOwnedTarget(Long targetCompanyId) {
        // 调用getCurrentCompanyId方法，复用统一能力并保证业务规则一致。
        Long currentCompanyId = SecurityContext.getCurrentCompanyId();
        if (currentCompanyId == null) {
            throw new ServiceException("缺少公司数据访问上下文");
        }
        if (targetCompanyId != null && !Objects.equals(targetCompanyId, currentCompanyId)) {
            throw new ServiceException("无权操作目标公司数据");
        }
        return currentCompanyId;
    }

    /**
     * 解析Owner总部Target。
     *
     * @return 处理结果
     */
    public Long resolveOwnerHqTarget(Long ownerHqId) {
        // 调用resolveCurrentCompanyTarget方法，复用统一能力并保证业务规则一致。
        Long targetCompanyId = resolveCurrentCompanyTarget(ownerHqId);
        // 调用validateEnabledHqCompany方法，复用统一能力并保证业务规则一致。
        validateEnabledHqCompany(targetCompanyId);
        return targetCompanyId;
    }

    /**
     * runWithCurrent公司Target。
     *
     * @param supplier 参数
     * @return 处理结果
     */
    public <T> T runWithCurrentCompanyTarget(Long targetCompanyId, Supplier<T> supplier) {
        // 调用resolveCurrentCompanyTarget方法，复用统一能力并保证业务规则一致。
        Long resolvedTargetCompanyId = resolveCurrentCompanyTarget(targetCompanyId);
        return companyDataAccessContext.runWithTargetCompany(resolvedTargetCompanyId, supplier);
    }

    /**
     * runWithCurrent公司Target。
     *
     * @param runnable 参数
     */
    public void runWithCurrentCompanyTarget(Long targetCompanyId, Runnable runnable) {
        runWithCurrentCompanyTarget(targetCompanyId, () -> {
            // 调用run方法，复用统一能力并保证业务规则一致。
            runnable.run();
            return null;
        });
    }

    /**
     * 以当前公司页口径执行数据访问逻辑。
     *
     * @param targetCompanyId 显式传入的目标公司ID，可为空
     * @param supplier 业务逻辑
     * @return 业务结果
     */
    public <T> T runWithCurrentCompanyOwnedTarget(Long targetCompanyId, Supplier<T> supplier) {
        // 调用resolveCurrentCompanyOwnedTarget方法，复用统一能力并保证业务规则一致。
        Long resolvedTargetCompanyId = resolveCurrentCompanyOwnedTarget(targetCompanyId);
        return companyDataAccessContext.runWithTargetCompany(resolvedTargetCompanyId, supplier);
    }

    /**
     * 以当前公司页口径执行无返回值逻辑。
     *
     * @param targetCompanyId 显式传入的目标公司ID，可为空
     * @param runnable 业务逻辑
     */
    public void runWithCurrentCompanyOwnedTarget(Long targetCompanyId, Runnable runnable) {
        runWithCurrentCompanyOwnedTarget(targetCompanyId, () -> {
            // 调用run方法，复用统一能力并保证业务规则一致。
            runnable.run();
            return null;
        });
    }

    /**
     * runWithOwner总部Target。
     *
     * @param supplier 参数
     * @return 处理结果
     */
    public <T> T runWithOwnerHqTarget(Long ownerHqId, Supplier<T> supplier) {
        // 调用resolveOwnerHqTarget方法，复用统一能力并保证业务规则一致。
        Long resolvedOwnerHqId = resolveOwnerHqTarget(ownerHqId);
        return companyDataAccessContext.runWithTargetCompany(resolvedOwnerHqId, supplier);
    }

    /**
     * runWithOwner总部Target。
     *
     * @param runnable 参数
     */
    public void runWithOwnerHqTarget(Long ownerHqId, Runnable runnable) {
        runWithOwnerHqTarget(ownerHqId, () -> {
            // 调用run方法，复用统一能力并保证业务规则一致。
            runnable.run();
            return null;
        });
    }

    /**
     * 校验Enabled公司。
     *
     * @param missingMessage 参数
     * @param disabledMessage 参数
     */
    private void validateEnabledCompany(Long companyId, String missingMessage, String disabledMessage) {
        // 说明：执行该步骤以保证业务流程正确。
        SysCompany company = sysCompanyMapper.selectById(companyId);
        if (company == null) {
            throw new ServiceException(missingMessage);
        }
        if (!Objects.equals(company.getStatus(), STATUS_ENABLED)) {
            throw new ServiceException(disabledMessage);
        }
    }

    /**
     * 校验Enabled总部公司。
     */
    private void validateEnabledHqCompany(Long companyId) {
        // 说明：执行该步骤以保证业务流程正确。
        SysCompany company = sysCompanyMapper.selectById(companyId);
        if (company == null) {
            throw new ServiceException("目标总部不存在");
        }
        if (!Objects.equals(company.getStatus(), STATUS_ENABLED)) {
            throw new ServiceException("目标总部已停用");
        }
        Map<String, String> subjectTypeMap = companyTypeService.listAll().stream()
                // 调用toMap方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toMap(SysCompanyType::getTypeCode, SysCompanyType::getSubjectType, (a, b) -> a));
        if (!SubjectTypeEnum.HQ.getCode().equals(subjectTypeMap.get(company.getTypeCode()))) {
            throw new ServiceException("目标公司不是总部类型");
        }
    }
}


