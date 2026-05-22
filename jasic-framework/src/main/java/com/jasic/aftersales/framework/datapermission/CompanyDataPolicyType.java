package com.jasic.aftersales.framework.datapermission;

/**
 * 公司数据权限表策略类型。
 *
 * @author Zoro
 * @date 2026/05/05
 */
public enum CompanyDataPolicyType {

    /**
     * 当前登录公司归属表，由 TenantLine 自动拼接 company_id。
     */
    CURRENT_COMPANY,

    /**
     * 总部归属表，由服务层按 ownerHq / resolvedHq 显式过滤。
     */
    HQ_OWNER,

    /**
     * 领域策略表，由领域权限组件控制。
     */
    DOMAIN_POLICY,

    /**
     * 关系事实表，不可作为独立权限入口。
     */
    RELATION_FACT,

    /**
     * 全局配置或基础资料表，不按公司隔离。
     */
    GLOBAL
}
