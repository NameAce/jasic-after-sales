package com.jasic.aftersales.framework.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CRM 数据源配置
 *
 * @author Zoro
 * @date 2026/04/12
 */
@Data
@ConfigurationProperties(prefix = "jasic.crm.datasource")
public class CrmDataSourceProperties {

    /** JDBC 地址 */
    private String url;

    /** 用户名 */
    private String username;

    /** 密码 */
    private String password;

    /** 驱动类名 */
    private String driverClassName = "com.mysql.cj.jdbc.Driver";

    /** 最小空闲连接 */
    private Integer minimumIdle = 2;

    /** 最大连接数 */
    private Integer maximumPoolSize = 10;

    /** 连接超时时间 */
    private Long connectionTimeout = 30000L;

    /** 空闲超时时间 */
    private Long idleTimeout = 600000L;

    /** 最大存活时间 */
    private Long maxLifetime = 1800000L;
}
