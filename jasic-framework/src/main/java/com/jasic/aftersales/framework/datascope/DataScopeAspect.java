package com.jasic.aftersales.framework.datascope;

import cn.hutool.core.util.StrUtil;
import com.jasic.aftersales.common.annotation.DataScope;
import com.jasic.aftersales.common.enums.DataScopeEnum;
import com.jasic.aftersales.common.enums.SubjectTypeEnum;
import com.jasic.aftersales.framework.security.SecurityContext;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据权限 AOP 切面
 * <p>
 * 在标记了 @DataScope 的方法执行前，根据当前用户的 subjectType 和 dataScope
 * 拼接 SQL WHERE 条件并写入 ThreadLocal，供 DataScopeHandler 读取。
 * 方法执行后自动清理 ThreadLocal。
 * </p>
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Slf4j
@Aspect
@Component
public class DataScopeAspect {

    private static final ThreadLocal<String> DATA_SCOPE_SQL = new ThreadLocal<>();

    /**
     * 标记当前线程是否由 @DataScope 接管数据过滤，
     * 为 true 时 TenantLineInnerInterceptor 应让路，避免重复/冲突注入。
     */
    private static final ThreadLocal<Boolean> DATA_SCOPE_ACTIVE = new ThreadLocal<>();

    /**
     * 方法执行前设置数据过滤条件
     *
     * @param joinPoint 切入点
     * @param dataScope 数据权限注解
     */
    @Before("@annotation(dataScope)")
    public void doBefore(JoinPoint joinPoint, DataScope dataScope) {
        clearDataScope();
        DATA_SCOPE_ACTIVE.set(true);

        String subjectType = SecurityContext.getCurrentSubjectType();
        Long companyId = SecurityContext.getCurrentCompanyId();
        Long userId = SecurityContext.getCurrentUserId();

        if (subjectType == null || companyId == null) {
            return;
        }

        String companyAlias = dataScope.companyAlias();
        String userAlias = dataScope.userAlias();
        String prefix = StrUtil.isNotBlank(companyAlias) ? companyAlias + "." : "";
        String userPrefix = StrUtil.isNotBlank(userAlias) ? userAlias + "." : "";

        StringBuilder sql = new StringBuilder();

        if (SubjectTypeEnum.PLATFORM.getCode().equals(subjectType)) {
            // 平台管理员：不限制数据，可看所有
            return;
        }

        if (SubjectTypeEnum.HQ.getCode().equals(subjectType)) {
            buildHqSql(sql, prefix, userPrefix, companyId, userId);
        } else if (SubjectTypeEnum.SERVICE.getCode().equals(subjectType)) {
            buildServiceSql(sql, prefix, companyId);
        }

        if (sql.length() > 0) {
            DATA_SCOPE_SQL.set(sql.toString());
        }
    }

    /**
     * 方法执行后清理 ThreadLocal
     *
     * @param joinPoint 切入点
     * @param dataScope 注解
     */
    @After("@annotation(dataScope)")
    public void doAfter(JoinPoint joinPoint, DataScope dataScope) {
        clearDataScope();
        DATA_SCOPE_ACTIVE.remove();
    }

    /**
     * 构建总部数据权限SQL
     *
     * @param sql        SQL构建器
     * @param prefix     公司字段别名前缀
     * @param userPrefix 用户字段别名前缀
     * @param companyId  当前公司ID
     * @param userId     当前用户ID
     */
    private void buildHqSql(StringBuilder sql, String prefix, String userPrefix, Long companyId, Long userId) {
        DataScopeEnum effectiveScope = DataScopeEnum.ALL;
        try {
            String scopeCode = SecurityContext.getEffectiveDataScope();
            if (StrUtil.isNotBlank(scopeCode)) {
                effectiveScope = DataScopeEnum.getByCode(scopeCode);
            }
        } catch (Exception e) {
            log.warn("获取有效数据范围失败，使用默认ALL", e);
        }

        switch (effectiveScope) {
            case ALL:
                sql.append(prefix).append("hq_company_id = ").append(companyId);
                break;
            case REGION:
                List<Long> regionIds = SecurityContext.getCurrentRegionIds();
                if (regionIds != null && !regionIds.isEmpty()) {
                    String idStr = regionIds.stream().map(String::valueOf).collect(Collectors.joining(","));
                    sql.append(prefix).append("hq_company_id = ").append(companyId)
                       .append(" AND ").append(prefix).append("company_id IN (")
                       .append("SELECT first_company_id FROM hq_first_contract WHERE region_id IN (").append(idStr).append(")")
                       .append(" UNION ")
                       .append("SELECT fsr.second_company_id FROM first_second_relation fsr ")
                       .append("INNER JOIN hq_first_contract hfc ON fsr.first_company_id = hfc.first_company_id ")
                       .append("WHERE hfc.region_id IN (").append(idStr).append(")")
                       .append(")");
                } else {
                    sql.append("1 = 0");
                }
                break;
            case SELF:
                sql.append(prefix).append("hq_company_id = ").append(companyId)
                   .append(" AND ").append(userPrefix).append("assigned_user_id = ").append(userId);
                break;
            default:
                break;
        }
    }

    /**
     * 构建服务网点数据权限SQL
     *
     * @param sql       SQL构建器
     * @param prefix    公司字段别名前缀
     * @param companyId 当前公司ID
     */
    private void buildServiceSql(StringBuilder sql, String prefix, Long companyId) {
        String typeCode = SecurityContext.getCurrentTypeCode();
        if ("FIRST".equals(typeCode)) {
            sql.append("(").append(prefix).append("company_id = ").append(companyId)
               .append(" OR ").append(prefix).append("company_id IN (")
               .append("SELECT second_company_id FROM first_second_relation WHERE first_company_id = ").append(companyId)
               .append(" AND status = 1")
               .append("))");
        } else if ("SECOND".equals(typeCode)) {
            sql.append("(").append(prefix).append("company_id = ").append(companyId)
               .append(" OR ").append(prefix).append("origin_company_id = ").append(companyId).append(")");
        }
    }

    /**
     * 获取当前线程的数据过滤SQL
     *
     * @return SQL条件片段
     */
    public static String getDataScopeSql() {
        return DATA_SCOPE_SQL.get();
    }

    /**
     * 当前线程是否由 @DataScope 接管数据过滤
     *
     * @return true 表示 @DataScope 已激活
     */
    public static boolean isDataScopeActive() {
        return Boolean.TRUE.equals(DATA_SCOPE_ACTIVE.get());
    }

    /**
     * 清除数据过滤SQL
     */
    public static void clearDataScope() {
        DATA_SCOPE_SQL.remove();
    }

}
