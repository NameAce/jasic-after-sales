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
 * @author Zoro
 * @date 2026/04/07
 */
@Configuration
@EnableConfigurationProperties(CrmDataSourceProperties.class)
public class CrmJdbcTemplateConfig {

    /**
     * jdbc模板。
     *
     * @param dataSource dataSource，当前业务处理所需的输入值。
     * @return 业务处理结果
     */
    @Bean(name = "jdbcTemplate")
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /**
     * crmJDBC模板。
     *
     * @param properties properties，当前业务处理所需的输入值。
     * @return 业务处理结果
     */
    @Bean(name = "crmJdbcTemplate")
    @ConditionalOnProperty(prefix = "jasic.crm.datasource", name = "url")
    public JdbcTemplate crmJdbcTemplate(CrmDataSourceProperties properties) {
        if (StrUtil.isBlank(properties.getUrl())) {
            throw new ServiceException("客户关系管理（CRM）数据源地址不能为空");
        }
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setPoolName("crm-hikari");
        dataSource.setDriverClassName(StrUtil.blankToDefault(StrUtil.trim(properties.getDriverClassName()), "com.mysql.cj.jdbc.Driver"));
        dataSource.setJdbcUrl(StrUtil.trim(properties.getUrl()));
        dataSource.setUsername(StrUtil.trim(properties.getUsername()));
        dataSource.setPassword(properties.getPassword());
        dataSource.setMinimumIdle(properties.getMinimumIdle());
        dataSource.setMaximumPoolSize(properties.getMaximumPoolSize());
        dataSource.setConnectionTimeout(properties.getConnectionTimeout());
        dataSource.setIdleTimeout(properties.getIdleTimeout());
        dataSource.setMaxLifetime(properties.getMaxLifetime());
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.setFetchSize(1000);
        jdbcTemplate.setQueryTimeout(300);
        return jdbcTemplate;
    }
}


