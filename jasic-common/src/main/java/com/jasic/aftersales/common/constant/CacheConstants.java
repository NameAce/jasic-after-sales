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

    /** 字典数据缓存前缀，完整 key: dict:data:{dictType} */
    public static final String DICT_DATA_KEY = "dict:data:";

    /** 参数缓存前缀，完整 key: config:key:{configKey} */
    public static final String CONFIG_KEY = "config:key:";

    /** 微信 access_token 缓存前缀，完整 key: wechat:access-token:{scene} */
    public static final String WECHAT_ACCESS_TOKEN_KEY = "wechat:access-token:";

    /** 微信绑定码缓存前缀，完整 key: wechat:bind-code:{bindCode} */
    public static final String WECHAT_BIND_CODE_KEY = "wechat:bind-code:";

    /** 用户待绑定会话缓存前缀，完整 key: wechat:bind-user:{userId} */
    public static final String WECHAT_BIND_USER_KEY = "wechat:bind-user:";
}
