package com.jasic.aftersales.framework.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.jasic.aftersales.framework.datascope.DataScopeAspect;
import net.sf.jsqlparser.expression.Expression;
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
@EnableAsync
@Configuration
@MapperScan({"com.jasic.aftersales.system.mapper", "com.jasic.aftersales.customer.mapper"})
public class MybatisPlusConfig implements MetaObjectHandler {

    /**
     * MyBatis-Plus 插件配置
     *
     * @return 插件拦截器
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

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
                return where;
            }
        });
        interceptor.addInnerInterceptor(dataPermissionInterceptor);

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
