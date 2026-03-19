package com.jasic.aftersales.framework.datascope;

import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 公司隔离表自动探测注册器
 * <p>
 * 启动时扫描所有 MyBatis-Plus 注册的实体，
 * 将包含 company_id 字段的表名记录下来，
 * 供 TenantLineHandler 判断是否需要自动注入公司隔离条件。
 * </p>
 *
 * @author Zoro
 * @date 2026/03/19
 */
@Slf4j
@Component
public class CompanyIsolationRegistrar implements ApplicationRunner {

    private final Set<String> isolatedTables = ConcurrentHashMap.newKeySet();

    @Override
    public void run(ApplicationArguments args) {
        for (TableInfo info : TableInfoHelper.getTableInfos()) {
            boolean hasCompanyId = info.getFieldList().stream()
                    .anyMatch(f -> "company_id".equals(f.getColumn()));
            if (hasCompanyId) {
                isolatedTables.add(info.getTableName());
            }
        }
        log.info("公司隔离自动探测完成，已注册表：{}", isolatedTables);
    }

    /**
     * 判断该表是否需要公司隔离（即表中存在 company_id 字段）
     *
     * @param tableName 表名
     * @return true 需要隔离
     */
    public boolean shouldIsolate(String tableName) {
        return isolatedTables.contains(tableName);
    }
}
