package com.jasic.aftersales.framework.datapermission;

import com.jasic.aftersales.common.exception.ServiceException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 公司数据权限表策略注册表。
 *
 * <p>表策略是安全边界，固定在代码中维护。所有 {@code @TableName} 实体表必须在这里登记。</p>
 *
 * @author Codex
 * @date 2026/05/05
 */
public final class CompanyDataPolicyRegistry {

    private static final Map<String, CompanyDataPolicyType> POLICIES;

    static {
        Map<String, CompanyDataPolicyType> policies = new LinkedHashMap<>();

        register(policies, CompanyDataPolicyType.CURRENT_COMPANY,
                "company_address",
                "sys_oper_log",
                "sys_region",
                "sys_role"
        );

        register(policies, CompanyDataPolicyType.HQ_OWNER,
                "fault_repair_config",
                "fault_repair_config_fault",
                "fault_repair_config_option",
                "machine_barcode"
        );

        register(policies, CompanyDataPolicyType.DOMAIN_POLICY,
                "c_user",
                "customer_address",
                "sys_file",
                "sys_notify_dispatch",
                "sys_notify_event",
                "sys_notify_message",
                "sys_notify_message_log",
                "work_order",
                "work_order_evaluation",
                "work_order_fault",
                "work_order_fault_part",
                "work_order_flow",
                "work_order_participant",
                "work_order_quote",
                "work_order_repair",
                "work_order_user_participant"
        );

        register(policies, CompanyDataPolicyType.RELATION_FACT,
                "first_second_relation",
                "first_second_relation_record",
                "hq_first_contract",
                "hq_first_contract_record",
                "sys_file_biz",
                "sys_role_menu",
                "sys_user_company",
                "sys_user_region",
                "sys_user_role",
                "wechat_bind_record"
        );

        register(policies, CompanyDataPolicyType.GLOBAL,
                "crm_biz_company_snapshot",
                "crm_first_second_relation_snapshot",
                "crm_hq_first_contract_snapshot",
                "crm_warehouse_scan_outstorage_snapshot",
                "sync_task",
                "sync_task_log",
                "sys_area",
                "sys_company",
                "sys_company_type",
                "sys_config",
                "sys_dict_data",
                "sys_dict_type",
                "sys_menu",
                "sys_notify_template",
                "sys_notify_template_channel",
                "sys_role_template",
                "sys_role_template_menu",
                "sys_type_code_menu",
                "sys_user"
        );

        POLICIES = Collections.unmodifiableMap(policies);
    }

    /**
     * 构造公司数据策略实例。
     */
    private CompanyDataPolicyRegistry() {
    }

    /**
     * allPolicies。
     *
     * @return 处理结果
     */
    public static Map<String, CompanyDataPolicyType> allPolicies() {
        return POLICIES;
    }

    /**
     * contains。
     *
     * @param tableName 参数
     */
    public static boolean contains(String tableName) {
        return POLICIES.containsKey(normalize(tableName));
    }

    /**
     * 获取策略。
     *
     * @param tableName 参数
     * @return 处理结果
     */
    public static CompanyDataPolicyType getPolicy(String tableName) {
        return POLICIES.get(normalize(tableName));
    }

    /**
     * require策略。
     *
     * @param tableName 参数
     * @return 处理结果
     */
    public static CompanyDataPolicyType requirePolicy(String tableName) {
        // 调用getPolicy方法，复用统一能力并保证业务规则一致。
        CompanyDataPolicyType policyType = getPolicy(tableName);
        if (policyType == null) {
            throw new ServiceException("未知数据权限表策略：" + tableName);
        }
        return policyType;
    }

    /**
     * useTenantLine。
     *
     * @param tableName 参数
     */
    public static boolean useTenantLine(String tableName) {
        return requirePolicy(tableName) == CompanyDataPolicyType.CURRENT_COMPANY;
    }

    /**
     * 规范化公司数据策略。
     *
     * @param tableName 参数
     * @return 处理结果
     */
    public static String normalize(String tableName) {
        if (tableName == null) {
            return null;
        }
        // 调用trim方法，复用统一能力并保证业务规则一致。
        String normalized = tableName.trim();
        if (normalized.startsWith("`") && normalized.endsWith("`") && normalized.length() > 1) {
            // 调用length方法，复用统一能力并保证业务规则一致。
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    /**
     * register。
     *
     * @param policies 参数
     * @param policyType 参数
     * @param tableNames 参数
     */
    private static void register(Map<String, CompanyDataPolicyType> policies, CompanyDataPolicyType policyType,
                                 String... tableNames) {
        for (String tableName : tableNames) {
            // 调用normalize方法，复用统一能力并保证业务规则一致。
            String normalized = normalize(tableName);
            if (normalized == null || normalized.length() == 0) {
                throw new IllegalStateException("数据权限表名不能为空");
            }
            // 调用putIfAbsent方法，复用统一能力并保证业务规则一致。
            CompanyDataPolicyType exists = policies.putIfAbsent(normalized, policyType);
            if (exists != null) {
                throw new IllegalStateException("重复注册数据权限表策略：" + normalized);
            }
        }
    }
}


