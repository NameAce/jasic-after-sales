package com.jasic.aftersales.framework.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.jasic.aftersales.framework.datapermission.CompanyDataAccessContext;
import com.jasic.aftersales.framework.datapermission.CompanyDataPolicyRegistry;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.apache.ibatis.reflection.MetaObject;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 配置（含分页插件、TenantLine 公司隔离、自动填充）
 *
 * @author Zoro
 * @date 2026/03/18
 */
@EnableAsync
@Configuration
@MapperScan({
        "com.jasic.aftersales.system.mapper",
        "com.jasic.aftersales.system.notify.mapper",
        "com.jasic.aftersales.customer.mapper"
})
public class MybatisPlusConfig implements MetaObjectHandler {

    /**
     * MyBatis-Plus 插件配置
     *
     * @param companyDataAccessContext 公司数据访问上下文
     * @return 插件拦截器
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(CompanyDataAccessContext companyDataAccessContext) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // TenantLine 只作用于显式注册为 CURRENT_COMPANY 的表。
        TenantLineInnerInterceptor tenantInterceptor = new TenantLineInnerInterceptor(new TenantLineHandler() {
            /**
             * ??Tenant Id?
             *
             * @return ????
             */
            @Override
            public Expression getTenantId() {
                return new LongValue(companyDataAccessContext.resolveCompanyId());
            }

            /**
             * ??Tenant Id Column?
             *
             * @return ?????
             */
            @Override
            public String getTenantIdColumn() {
                return "company_id";
            }

            /**
             * ?? ignoreTable ?????
             *
             * @param tableName ??
             * @return true ??????
             */
            @Override
            public boolean ignoreTable(String tableName) {
                return !CompanyDataPolicyRegistry.useTenantLine(tableName);
            }
        });
        interceptor.addInnerInterceptor(tenantInterceptor);

        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    /**
     * 插入时自动填充 createTime 和 updateTime
     *
     * @param metaObject 元对象
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }

    /**
     * 更新时自动填充 updateTime
     *
     * @param metaObject 元对象
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
