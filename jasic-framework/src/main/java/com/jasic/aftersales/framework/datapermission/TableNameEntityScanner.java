package com.jasic.aftersales.framework.datapermission;

import com.baomidou.mybatisplus.annotation.TableName;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * {@code @TableName} 实体扫描工具。
 *
 * @author Codex
 * @date 2026/05/05
 */
public final class TableNameEntityScanner {

    /**
     * 构造表名称实体实例。
     */
    private TableNameEntityScanner() {
    }

    /**
     * scan表Entities。
     *
     * @param basePackage 参数
     * @return 处理结果
     */
    public static Map<String, Set<String>> scanTableEntities(String basePackage) {
        ClassPathScanningCandidateComponentProvider scanner =
                /**
                 * ClassPathScanningCandidateComponentProvider。
                 *
                 * @param false 参数
                 * @return 处理结果
                 */
                new ClassPathScanningCandidateComponentProvider(false);
        // 调用AnnotationTypeFilter方法，复用统一能力并保证业务规则一致。
        scanner.addIncludeFilter(new AnnotationTypeFilter(TableName.class));

        Map<String, Set<String>> result = new LinkedHashMap<>();
        for (BeanDefinition definition : scanner.findCandidateComponents(basePackage)) {
            // 调用getBeanClassName方法，复用统一能力并保证业务规则一致。
            String className = definition.getBeanClassName();
            if (className == null) {
                continue;
            }
            // 调用loadClass方法，复用统一能力并保证业务规则一致。
            Class<?> entityClass = loadClass(className);
            // 调用getAnnotation方法，复用统一能力并保证业务规则一致。
            TableName tableName = entityClass.getAnnotation(TableName.class);
            if (tableName == null) {
                continue;
            }
            // 调用value方法，复用统一能力并保证业务规则一致。
            String normalized = CompanyDataPolicyRegistry.normalize(tableName.value());
            // 调用getName方法，复用统一能力并保证业务规则一致。
            result.computeIfAbsent(normalized, key -> new LinkedHashSet<>()).add(entityClass.getName());
        }
        return result;
    }

    /**
     * loadClass。
     *
     * @param className 参数
     * @return 处理结果
     */
    private static Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("加载 TableName 实体失败：" + className, e);
        }
    }
}




