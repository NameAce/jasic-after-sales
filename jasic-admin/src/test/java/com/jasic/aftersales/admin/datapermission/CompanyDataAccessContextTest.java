package com.jasic.aftersales.admin.datapermission;

import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.framework.datapermission.CompanyDataAccessContext;
import org.junit.Assert;
import org.junit.Test;

/**
 * 公司数据访问上下文测试。
 *
 * @author Zoro
 * @date 2026/05/05
 */
public class CompanyDataAccessContextTest {

    /**验证ResolveTargetCompanyInsideScopedCall，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldResolveTargetCompanyInsideScopedCall() {
        CompanyDataAccessContext context = new CompanyDataAccessContext();
        Long companyId = context.runWithTargetCompany(1001L, context::resolveCompanyId);
        Assert.assertEquals(Long.valueOf(1001L), companyId);
        Assert.assertNull(context.getTargetCompanyId());
    }

    /**验证RejectMissingTargetCompanyForScopedCall，保证相关业务规则在回归场景下保持稳定。*/
    @Test
    public void shouldRejectMissingTargetCompanyForScopedCall() {
        CompanyDataAccessContext context = new CompanyDataAccessContext();
        try {
            context.runWithTargetCompany(null, () -> null);
            Assert.fail("缺少目标公司应拒绝");
        } catch (ServiceException ex) {
            Assert.assertEquals("缺少目标公司上下文", ex.getMessage());
        }
    }
}
