package com.jasic.aftersales.framework.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.jasic.aftersales.framework.datascope.CompanyIsolationRegistrar;
import com.jasic.aftersales.framework.datascope.DataScopeAspect;
import com.jasic.aftersales.framework.security.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import org.apache.ibatis.reflection.MetaObject;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 配置（含分页插件、数据权限插件、自动填充）
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Slf4j
@EnableAsync
@Configuration
@MapperScan({"com.jasic.aftersales.system.mapper", "com.jasic.aftersales.customer.mapper"})
public class MybatisPlusConfig implements MetaObjectHandler {

    /**
     * MyBatis-Plus 插件配置
     * <p>
     * 拦截器执行顺序：TenantLine（公司隔离） → DataPermission（业务数据权限） → Pagination（分页）
     * </p>
     *
     * @param registrar 公司隔离表注册器（自动探测含 company_id 的表）
     * @return 插件拦截器
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(CompanyIsolationRegistrar registrar) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // ====== 第一层：公司级自动隔离 ======
        TenantLineInnerInterceptor tenantInterceptor = new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                Long companyId = SecurityContext.getCurrentCompanyId();
                return companyId != null ? new LongValue(companyId) : new NullValue();
            }

            @Override
            public String getTenantIdColumn() {
                return "company_id";
            }

            @Override
            public boolean ignoreTable(String tableName) {
                try {
                    Long companyId = SecurityContext.getCurrentCompanyId();
                    if (companyId == null) {
                        return true;
                    }
                    if (SecurityContext.isPlatformUser()) {
                        return true;
                    }
                } catch (Exception e) {
                    return true;
                }
                if (DataScopeAspect.isDataScopeActive()) {
                    return true;
                }
                return !registrar.shouldIsolate(tableName);
            }
        });
        interceptor.addInnerInterceptor(tenantInterceptor);

        // ====== 第二层：业务数据权限（@DataScope 触发） ======
        DataPermissionInterceptor dataPermissionInterceptor = new DataPermissionInterceptor();
        dataPermissionInterceptor.setDataPermissionHandler((where, mappedStatementId) -> {
            String dataScopeSql = DataScopeAspect.getDataScopeSql();
            if (dataScopeSql == null || dataScopeSql.isEmpty()) {
                return where;
            }
            try {
                Expression dataScopeExpression = CCJSqlParserUtil.parseCondExpression(dataScopeSql);
                if (where == null) {
                    return dataScopeExpression;
                }
                return new AndExpression(where, dataScopeExpression);
            } catch (Exception e) {
                log.error("解析数据权限SQL异常: {}", dataScopeSql, e);
                return where;
            }
        });
        interceptor.addInnerInterceptor(dataPermissionInterceptor);

        // ====== 第三层：分页 ======
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
