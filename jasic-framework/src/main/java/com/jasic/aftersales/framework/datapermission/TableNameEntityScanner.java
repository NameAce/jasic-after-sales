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
 * @author Zoro
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
     * @param basePackage basePackage，当前业务处理所需的输入值。
     * @return 业务处理结果
     */
    public static Map<String, Set<String>> scanTableEntities(String basePackage) {
        ClassPathScanningCandidateComponentProvider scanner =
                /**
                 * ClassPathScanningCandidateComponentProvider。
                 *
                 * @param false false，当前业务处理所需的输入值。
                 * @return 业务处理结果
                 */
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(TableName.class));

        Map<String, Set<String>> result = new LinkedHashMap<>();
        for (BeanDefinition definition : scanner.findCandidateComponents(basePackage)) {
            String className = definition.getBeanClassName();
            if (className == null) {
                continue;
            }
            Class<?> entityClass = loadClass(className);
            TableName tableName = entityClass.getAnnotation(TableName.class);
            if (tableName == null) {
                continue;
            }
            String normalized = CompanyDataPolicyRegistry.normalize(tableName.value());
            result.computeIfAbsent(normalized, key -> new LinkedHashSet<>()).add(entityClass.getName());
        }
        return result;
    }

    /**
     * loadClass。
     *
     * @param className className，当前业务处理所需的输入值。
     * @return 业务处理结果
     */
    private static Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("加载 TableName 实体失败：" + className, e);
        }
    }
}




