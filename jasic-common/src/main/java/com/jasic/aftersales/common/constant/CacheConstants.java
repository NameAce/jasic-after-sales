package com.jasic.aftersales.common.constant;

/**
 * 缓存 Key 常量
 *
 * @author Zoro
 * @date 2026/03/18
 */
public class CacheConstants {

    private CacheConstants() {
    }

    /** 用户权限集合前缀，完整 key: user:perms:{userId}:{companyId} */
    public static final String USER_PERMS_KEY = "user:perms:";

    /** 用户菜单树前缀，完整 key: user:menus:{userId}:{companyId} */
    public static final String USER_MENUS_KEY = "user:menus:";

    /** 用户基本信息前缀，完整 key: user:info:{userId} */
    public static final String USER_INFO_KEY = "user:info:";

    /** 公司下级公司ID列表前缀，完整 key: company:children:{companyId} */
    public static final String COMPANY_CHILDREN_KEY = "company:children:";

    /** 大区下网点ID列表前缀，完整 key: region:companies:{regionId} */
    public static final String REGION_COMPANIES_KEY = "region:companies:";
}
