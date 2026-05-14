package com.jasic.aftersales.framework.datapermission;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 启动期校验所有实体表均已注册数据权限策略。
 *
 * @author Codex
 * @date 2026/05/05
 */
@Component
public class CompanyDataPolicyValidator implements ApplicationRunner {

    private static final String BASE_PACKAGE = "com.jasic.aftersales";

    /**
     * ?? run ?????
     *
     * @param args ??
     */
    @Override
    public void run(ApplicationArguments args) {
        validateTablePolicies(TableNameEntityScanner.scanTableEntities(BASE_PACKAGE));
    }

    /**
     * ???????
     *
     * @param tableEntities ??
     */
    public static void validateTablePolicies(Map<String, Set<String>> tableEntities) {
        List<String> errors = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : tableEntities.entrySet()) {
            String tableName = entry.getKey();
            Set<String> entityClasses = entry.getValue();
            if (entityClasses.size() > 1) {
                errors.add("表名重复：" + tableName + " -> " + entityClasses);
            }
            if (!CompanyDataPolicyRegistry.contains(tableName)) {
                errors.add("实体表未注册数据权限策略：" + tableName + " -> " + entityClasses);
            }
        }
        if (!errors.isEmpty()) {
            throw new IllegalStateException("公司数据权限策略校验失败：" + errors);
        }
    }
}
