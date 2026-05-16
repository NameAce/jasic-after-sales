package com.jasic.aftersales.admin.datapermission;

import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.framework.datapermission.CompanyDataPolicyRegistry;
import com.jasic.aftersales.framework.datapermission.CompanyDataPolicyType;
import com.jasic.aftersales.framework.datapermission.CompanyDataPolicyValidator;
import com.jasic.aftersales.framework.datapermission.TableNameEntityScanner;
import org.junit.Assert;
import org.junit.Test;

/**
 * 公司数据权限策略注册测试。
 *
 * @author Codex
 * @date 2026/05/05
 */
public class CompanyDataPolicyRegistryTest {

    @Test
    public void shouldRegisterEveryTableNameEntity() {
        CompanyDataPolicyValidator.validateTablePolicies(
                TableNameEntityScanner.scanTableEntities("com.jasic.aftersales")
        );
    }

    @Test
    public void shouldApplyTenantLineOnlyForCurrentCompanyTables() {
        Assert.assertTrue(CompanyDataPolicyRegistry.useTenantLine("sys_role"));
        Assert.assertTrue(CompanyDataPolicyRegistry.useTenantLine("company_address"));

        Assert.assertFalse(CompanyDataPolicyRegistry.useTenantLine("fault_repair_config"));
        Assert.assertFalse(CompanyDataPolicyRegistry.useTenantLine("work_order"));
        Assert.assertFalse(CompanyDataPolicyRegistry.useTenantLine("sys_user_company"));
        Assert.assertFalse(CompanyDataPolicyRegistry.useTenantLine("sys_menu"));
    }

    @Test
    public void shouldFailClosedForUnknownTablePolicy() {
        try {
            CompanyDataPolicyRegistry.requirePolicy("unknown_table");
            Assert.fail("未知表策略应拒绝访问");
        } catch (ServiceException ex) {
            Assert.assertTrue(ex.getMessage().contains("未知数据权限表策略"));
        }
    }

    @Test
    public void shouldClassifyFaultConfigAsHqOwnerAndWorkOrderAsDomainPolicy() {
        Assert.assertEquals(CompanyDataPolicyType.HQ_OWNER,
                CompanyDataPolicyRegistry.getPolicy("fault_repair_config"));
        Assert.assertEquals(CompanyDataPolicyType.DOMAIN_POLICY,
                CompanyDataPolicyRegistry.getPolicy("work_order"));
        Assert.assertEquals(CompanyDataPolicyType.GLOBAL,
                CompanyDataPolicyRegistry.getPolicy("notify_scene"));
        Assert.assertEquals(CompanyDataPolicyType.GLOBAL,
                CompanyDataPolicyRegistry.getPolicy("notify_scene_target"));
    }
}
