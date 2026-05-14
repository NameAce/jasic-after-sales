package com.jasic.aftersales.framework.config;

import cn.hutool.core.util.StrUtil;
import com.jasic.aftersales.common.exception.ServiceException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

/**
 * CRM JdbcTemplate 配置
 *
 * @author Codex
 * @date 2026/04/07
 */
@Configuration
@EnableConfigurationProperties(CrmDataSourceProperties.class)
public class CrmJdbcTemplateConfig {

    /**
     * jdbc模板。
     *
     * @param dataSource 参数
     * @return 处理结果
     */
    @Bean(name = "jdbcTemplate")
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /**
     * crmJDBC模板。
     *
     * @param properties 参数
     * @return 处理结果
     */
    @Bean(name = "crmJdbcTemplate")
    @ConditionalOnProperty(prefix = "jasic.crm.datasource", name = "url")
    public JdbcTemplate crmJdbcTemplate(CrmDataSourceProperties properties) {
        if (StrUtil.isBlank(properties.getUrl())) {
            throw new ServiceException("客户关系管理（CRM）数据源地址不能为空");
        }
        // 调用HikariDataSource方法，复用统一能力并保证业务规则一致。
        HikariDataSource dataSource = new HikariDataSource();
        // 调用setPoolName方法，复用统一能力并保证业务规则一致。
        dataSource.setPoolName("crm-hikari");
        // 调用getDriverClassName方法，复用统一能力并保证业务规则一致。
        dataSource.setDriverClassName(StrUtil.blankToDefault(StrUtil.trim(properties.getDriverClassName()), "com.mysql.cj.jdbc.Driver"));
        // 调用getUrl方法，复用统一能力并保证业务规则一致。
        dataSource.setJdbcUrl(StrUtil.trim(properties.getUrl()));
        // 调用getUsername方法，复用统一能力并保证业务规则一致。
        dataSource.setUsername(StrUtil.trim(properties.getUsername()));
        // 调用getPassword方法，复用统一能力并保证业务规则一致。
        dataSource.setPassword(properties.getPassword());
        // 调用getMinimumIdle方法，复用统一能力并保证业务规则一致。
        dataSource.setMinimumIdle(properties.getMinimumIdle());
        // 调用getMaximumPoolSize方法，复用统一能力并保证业务规则一致。
        dataSource.setMaximumPoolSize(properties.getMaximumPoolSize());
        // 调用getConnectionTimeout方法，复用统一能力并保证业务规则一致。
        dataSource.setConnectionTimeout(properties.getConnectionTimeout());
        // 调用getIdleTimeout方法，复用统一能力并保证业务规则一致。
        dataSource.setIdleTimeout(properties.getIdleTimeout());
        // 调用getMaxLifetime方法，复用统一能力并保证业务规则一致。
        dataSource.setMaxLifetime(properties.getMaxLifetime());
        // 调用JdbcTemplate方法，复用统一能力并保证业务规则一致。
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        // 调用setFetchSize方法，复用统一能力并保证业务规则一致。
        jdbcTemplate.setFetchSize(1000);
        // 调用setQueryTimeout方法，复用统一能力并保证业务规则一致。
        jdbcTemplate.setQueryTimeout(300);
        return jdbcTemplate;
    }
}


