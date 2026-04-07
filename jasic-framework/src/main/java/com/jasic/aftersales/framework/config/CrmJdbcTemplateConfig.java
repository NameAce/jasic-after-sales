package com.jasic.aftersales.framework.config;

import cn.hutool.core.util.StrUtil;
import com.jasic.aftersales.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

/**
 * CRM JdbcTemplate 配置
 *
 * @author Codex
 * @date 2026/04/07
 */
@Configuration
public class CrmJdbcTemplateConfig {

    @Bean(name = "crmJdbcTemplate")
    @ConditionalOnProperty(prefix = "jasic.crm.datasource", name = "url")
    public JdbcTemplate crmJdbcTemplate(@Value("${jasic.crm.datasource.url}") String url,
                                        @Value("${jasic.crm.datasource.username}") String username,
                                        @Value("${jasic.crm.datasource.password}") String password,
                                        @Value("${jasic.crm.datasource.driver-class-name:com.mysql.cj.jdbc.Driver}") String driverClassName) {
        if (StrUtil.isBlank(url)) {
            throw new ServiceException("CRM 数据源地址不能为空");
        }
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(StrUtil.blankToDefault(StrUtil.trim(driverClassName), "com.mysql.cj.jdbc.Driver"));
        dataSource.setUrl(StrUtil.trim(url));
        dataSource.setUsername(StrUtil.trim(username));
        dataSource.setPassword(password);
        return new JdbcTemplate(dataSource);
    }
}
