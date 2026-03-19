package com.jasic.aftersales.framework.security;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.jasic.aftersales.common.enums.SubjectTypeEnum;

import java.util.Collections;
import java.util.List;

/**
 * 当前用户上下文工具类，从 Sa-Token Session 中获取用户信息
 *
 * @author Zoro
 * @date 2026/03/18
 */
public class SecurityContext {

    private static final String KEY_COMPANY_ID = "currentCompanyId";
    private static final String KEY_SUBJECT_TYPE = "currentSubjectType";
    private static final String KEY_TYPE_CODE = "currentTypeCode";
    private static final String KEY_REGION_IDS = "currentRegionIds";

    private SecurityContext() {
    }

    /**
     * 获取当前登录用户ID
     *
     * @return 用户ID
     */
    public static Long getCurrentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    /**
     * 获取当前操作公司ID
     *
     * @return 公司ID
     */
    public static Long getCurrentCompanyId() {
        return getSession().getLong(KEY_COMPANY_ID);
    }

    /**
     * 设置当前操作公司ID
     *
     * @param companyId 公司ID
     */
    public static void setCurrentCompanyId(Long companyId) {
        getSession().set(KEY_COMPANY_ID, companyId);
    }

    /**
     * 获取当前主体类型（PLATFORM/HQ/SERVICE）
     *
     * @return 主体类型编码
     */
    public static String getCurrentSubjectType() {
        return getSession().getString(KEY_SUBJECT_TYPE);
    }

    /**
     * 设置当前主体类型
     *
     * @param subjectType 主体类型编码
     */
    public static void setCurrentSubjectType(String subjectType) {
        getSession().set(KEY_SUBJECT_TYPE, subjectType);
    }

    /**
     * 获取当前公司类型编码（PLATFORM/HQ_A/HQ_B/.../FIRST/SECOND）
     *
     * @return 公司类型编码
     */
    public static String getCurrentTypeCode() {
        return getSession().getString(KEY_TYPE_CODE);
    }

    /**
     * 设置当前公司类型编码
     *
     * @param typeCode 类型编码
     */
    public static void setCurrentTypeCode(String typeCode) {
        getSession().set(KEY_TYPE_CODE, typeCode);
    }

    /**
     * 获取当前用户负责的大区ID列表
     *
     * @return 大区ID列表
     */
    @SuppressWarnings("unchecked")
    public static List<Long> getCurrentRegionIds() {
        Object regionIds = getSession().get(KEY_REGION_IDS);
        if (regionIds instanceof List) {
            return (List<Long>) regionIds;
        }
        return Collections.emptyList();
    }

    /**
     * 设置当前用户负责的大区ID列表
     *
     * @param regionIds 大区ID列表
     */
    public static void setCurrentRegionIds(List<Long> regionIds) {
        getSession().set(KEY_REGION_IDS, regionIds);
    }

    /**
     * 获取当前用户的有效数据范围编码
     *
     * @return 数据范围编码（ALL/REGION/SELF），可能为 null
     */
    public static String getEffectiveDataScope() {
        return getSession().getString("effectiveDataScope");
    }

    /**
     * 设置当前用户的有效数据范围编码
     *
     * @param dataScope 数据范围编码
     */
    public static void setEffectiveDataScope(String dataScope) {
        getSession().set("effectiveDataScope", dataScope);
    }

    /**
     * 判断当前登录用户是否为平台管理员
     *
     * @return true 是平台管理员
     */
    public static boolean isPlatformUser() {
        return SubjectTypeEnum.PLATFORM.getCode().equals(getCurrentSubjectType());
    }

    /**
     * 获取当前 Sa-Token Session
     *
     * @return Session
     */
    private static SaSession getSession() {
        return StpUtil.getSession();
    }
}
