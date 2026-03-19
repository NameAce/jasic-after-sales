package com.jasic.aftersales.framework.datascope;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;

/**
 * MyBatis-Plus 数据权限 Handler
 * <p>
 * 从 DataScopeAspect 的 ThreadLocal 中读取 SQL 条件，
 * 拼接到原始 SQL 的 WHERE 子句中。
 * </p>
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Slf4j
public class DataScopeHandler {

    /**
     * 获取数据过滤条件表达式
     * <p>
     * 由 MybatisPlusConfig 中的 DataPermissionInterceptor 调用
     * </p>
     *
     * @param where     原始 WHERE 条件
     * @param mappedStatementId Mapper 方法全限定名
     * @return 附加数据权限后的 WHERE 条件
     */
    public Expression getSqlSegment(Expression where, String mappedStatementId) {
        String dataScopeSql = DataScopeAspect.getDataScopeSql();
        if (StrUtil.isBlank(dataScopeSql)) {
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
    }
}
