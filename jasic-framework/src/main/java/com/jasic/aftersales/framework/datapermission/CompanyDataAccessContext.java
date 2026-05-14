package com.jasic.aftersales.framework.datapermission;

import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.framework.security.SecurityContext;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * 单次调用内的数据访问目标公司上下文。
 *
 * <p>该上下文只用于数据隔离，不改变登录态和审计身份。</p>
 *
 * @author Codex
 * @date 2026/05/05
 */
@Component
public class CompanyDataAccessContext {

    private static final ThreadLocal<Long> TARGET_COMPANY_ID = new ThreadLocal<>();

    /**
     * 获取Target公司ID。
     *
     * @return 处理结果
     */
    public Long getTargetCompanyId() {
        return TARGET_COMPANY_ID.get();
    }

    /**
     * 解析公司ID。
     *
     * @return 处理结果
     */
    public Long resolveCompanyId() {
        // 调用getTargetCompanyId方法，复用统一能力并保证业务规则一致。
        Long targetCompanyId = getTargetCompanyId();
        if (targetCompanyId != null) {
            return targetCompanyId;
        }
        // 调用getCurrentCompanyId方法，复用统一能力并保证业务规则一致。
        Long currentCompanyId = SecurityContext.getCurrentCompanyId();
        if (currentCompanyId == null) {
            throw new ServiceException("缺少公司数据访问上下文");
        }
        return currentCompanyId;
    }

    /**
     * runWithTarget公司。
     *
     * @param supplier 参数
     * @return 处理结果
     */
    public <T> T runWithTargetCompany(Long targetCompanyId, Supplier<T> supplier) {
        if (targetCompanyId == null) {
            throw new ServiceException("缺少目标公司上下文");
        }
        // 调用get方法，复用统一能力并保证业务规则一致。
        Long previous = TARGET_COMPANY_ID.get();
        // 调用set方法，复用统一能力并保证业务规则一致。
        TARGET_COMPANY_ID.set(targetCompanyId);
        try {
            return supplier.get();
        } finally {
            // 调用restore方法，复用统一能力并保证业务规则一致。
            restore(previous);
        }
    }

    /**
     * runWithTarget公司。
     *
     * @param runnable 参数
     */
    public void runWithTargetCompany(Long targetCompanyId, Runnable runnable) {
        runWithTargetCompany(targetCompanyId, () -> {
            // 调用run方法，复用统一能力并保证业务规则一致。
            runnable.run();
            return null;
        });
    }

    /**
     * clear。
     */
    public void clear() {
        // 调用remove方法，复用统一能力并保证业务规则一致。
        TARGET_COMPANY_ID.remove();
    }

    /**
     * restore。
     *
     * @param previous 参数
     */
    private void restore(Long previous) {
        if (previous == null) {
            // 调用remove方法，复用统一能力并保证业务规则一致。
            TARGET_COMPANY_ID.remove();
        } else {
            // 调用set方法，复用统一能力并保证业务规则一致。
            TARGET_COMPANY_ID.set(previous);
        }
    }
}


