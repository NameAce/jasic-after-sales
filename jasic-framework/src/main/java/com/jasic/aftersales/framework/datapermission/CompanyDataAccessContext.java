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
 * @author Zoro
 * @date 2026/05/05
 */
@Component
public class CompanyDataAccessContext {

    /**TARGET_COMPANY_ID 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    private static final ThreadLocal<Long> TARGET_COMPANY_ID = new ThreadLocal<>();

    /**
     * 获取Target公司ID。
     *
     * @return 业务处理结果
     */
    public Long getTargetCompanyId() {
        return TARGET_COMPANY_ID.get();
    }

    /**
     * 解析公司ID。
     *
     * @return 业务处理结果
     */
    public Long resolveCompanyId() {
        Long targetCompanyId = getTargetCompanyId();
        if (targetCompanyId != null) {
            return targetCompanyId;
        }
        Long currentCompanyId = SecurityContext.getCurrentCompanyId();
        if (currentCompanyId == null) {
            throw new ServiceException("缺少公司数据访问上下文");
        }
        return currentCompanyId;
    }

    /**
     * runWithTarget公司。
     *
     * @param supplier supplier，当前业务处理所需的输入值。
     * @return 业务处理结果
     */
    public <T> T runWithTargetCompany(Long targetCompanyId, Supplier<T> supplier) {
        if (targetCompanyId == null) {
            throw new ServiceException("缺少目标公司上下文");
        }
        Long previous = TARGET_COMPANY_ID.get();
        TARGET_COMPANY_ID.set(targetCompanyId);
        try {
            return supplier.get();
        } finally {
            restore(previous);
        }
    }

    /**
     * runWithTarget公司。
     *
     * @param runnable runnable，当前业务处理所需的输入值。
     */
    public void runWithTargetCompany(Long targetCompanyId, Runnable runnable) {
        runWithTargetCompany(targetCompanyId, () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * clear。
     */
    public void clear() {
        TARGET_COMPANY_ID.remove();
    }

    /**
     * restore。
     *
     * @param previous previous，当前业务处理所需的输入值。
     */
    private void restore(Long previous) {
        if (previous == null) {
            TARGET_COMPANY_ID.remove();
        } else {
            TARGET_COMPANY_ID.set(previous);
        }
    }
}


