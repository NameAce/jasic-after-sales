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
     * ???????
     *
     * @param targetCompanyId ????ID
     * @return ????
     */
    @Resource
    private CompanyDataAccessContext companyDataAccessContext;

    @Resource
    private SysCompanyMapper sysCompanyMapper;

    @Resource
    private ISysCompanyTypeService companyTypeService;

    public Long resolveCurrentCompanyTarget(Long targetCompanyId) {
        if (SecurityContext.isPlatformUser()) {
            if (targetCompanyId == null) {
                throw new ServiceException("缺少目标公司上下文");
            }
            // ?????????????????????????????
            validateEnabledCompany(targetCompanyId, "目标公司不存在", "目标公司已停用");
            return targetCompanyId;
        }
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
     * ???????
     *
     * @param ownerHqId ????ID
     * @return ????
     */
    public Long resolveOwnerHqTarget(Long ownerHqId) {
        Long targetCompanyId = resolveCurrentCompanyTarget(ownerHqId);
        validateEnabledHqCompany(targetCompanyId);
        return targetCompanyId;
    }

    /**
     * ?? runWithCurrentCompanyTarget ?????
     *
     * @param targetCompanyId ????ID
     * @param supplier ????
     * @return ????
     */
    public <T> T runWithCurrentCompanyTarget(Long targetCompanyId, Supplier<T> supplier) {
        Long resolvedTargetCompanyId = resolveCurrentCompanyTarget(targetCompanyId);
        return companyDataAccessContext.runWithTargetCompany(resolvedTargetCompanyId, supplier);
    }

    /**
     * ?? runWithCurrentCompanyTarget ?????
     *
     * @param targetCompanyId ????ID
     * @param runnable ????
     */
    public void runWithCurrentCompanyTarget(Long targetCompanyId, Runnable runnable) {
        runWithCurrentCompanyTarget(targetCompanyId, () -> {
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
            runnable.run();
            return null;
        });
    }

    /**
     * ?? runWithOwnerHqTarget ?????
     *
     * @param ownerHqId ????ID
     * @param supplier ????
     * @return ????
     */
    public <T> T runWithOwnerHqTarget(Long ownerHqId, Supplier<T> supplier) {
        Long resolvedOwnerHqId = resolveOwnerHqTarget(ownerHqId);
        return companyDataAccessContext.runWithTargetCompany(resolvedOwnerHqId, supplier);
    }

    /**
     * ?? runWithOwnerHqTarget ?????
     *
     * @param ownerHqId ????ID
     * @param runnable ????
     */
    public void runWithOwnerHqTarget(Long ownerHqId, Runnable runnable) {
        runWithOwnerHqTarget(ownerHqId, () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * ???????
     *
     * @param companyId ??ID
     * @param missingMessage ??
     * @param disabledMessage ??
     */
    private void validateEnabledCompany(Long companyId, String missingMessage, String disabledMessage) {
        // ??????????????????????????
        SysCompany company = sysCompanyMapper.selectById(companyId);
        if (company == null) {
            throw new ServiceException(missingMessage);
        }
        if (!Objects.equals(company.getStatus(), STATUS_ENABLED)) {
            throw new ServiceException(disabledMessage);
        }
    }

    /**
     * ???????
     *
     * @param companyId ??ID
     */
    private void validateEnabledHqCompany(Long companyId) {
        // ??????????????????????????
        SysCompany company = sysCompanyMapper.selectById(companyId);
        if (company == null) {
            throw new ServiceException("目标总部不存在");
        }
        if (!Objects.equals(company.getStatus(), STATUS_ENABLED)) {
            throw new ServiceException("目标总部已停用");
        }
        Map<String, String> subjectTypeMap = companyTypeService.listAll().stream()
                .collect(Collectors.toMap(SysCompanyType::getTypeCode, SysCompanyType::getSubjectType, (a, b) -> a));
        if (!SubjectTypeEnum.HQ.getCode().equals(subjectTypeMap.get(company.getTypeCode()))) {
            throw new ServiceException("目标公司不是总部类型");
        }
    }
}
