package com.jasic.aftersales.common.constant;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 角色常量
 *
 * @author Codex
 * @date 2026/03/31
 */
public class RoleConstants {

    private RoleConstants() {
    }

    /** 平台管理员角色标识 */
    public static final String ADMIN_ROLE_KEY = "admin";

    /** 公司管理员角色标识 */
    public static final String JASIC_ADMIN_ROLE_KEY = "js-admin";

    /** 维修员角色标识 */
    public static final String REPAIRER_ROLE_KEY = "repairer";

    /** 系统保留角色标识集合 */
    public static final Set<String> RESERVED_ROLE_KEYS = Collections.unmodifiableSet(
            new LinkedHashSet<>(Arrays.asList(JASIC_ADMIN_ROLE_KEY, ADMIN_ROLE_KEY, REPAIRER_ROLE_KEY))
    );
}
