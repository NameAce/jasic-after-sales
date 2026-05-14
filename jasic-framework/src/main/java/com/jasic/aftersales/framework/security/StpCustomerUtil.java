package com.jasic.aftersales.framework.security;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;

/**
 * C端客户多账号体系工具类，与 B端 StpUtil 隔离
 * <p>
 * 使用方式与 StpUtil 完全一致，如：
 * StpCustomerUtil.login(cUserId)
 * StpCustomerUtil.checkLogin()
 * </p>
 *
 * @author Zoro
 * @date 2026/03/18
 */
public class StpCustomerUtil {

    public static final String TYPE = "customer";

    private static final StpLogic STP_LOGIC = new StpLogic(TYPE);

    /**
     * 构造Stp客户实例。
     */
    private StpCustomerUtil() {
    }

    /**
     * 获取 StpLogic 实例
     *
     * @return StpLogic
     */
    public static StpLogic getStpLogic() {
        return STP_LOGIC;
    }

    /**
     * C端用户登录
     *
     * @param id 客户ID
     */
    public static void login(Object id) {
        // 调用login方法，复用统一能力并保证业务规则一致。
        STP_LOGIC.login(id);
    }

    /**
     * C端用户登出
     */
    public static void logout() {
        // 调用logout方法，复用统一能力并保证业务规则一致。
        STP_LOGIC.logout();
    }

    /**
     * 检查C端用户是否已登录
     */
    public static void checkLogin() {
        // 调用checkLogin方法，复用统一能力并保证业务规则一致。
        STP_LOGIC.checkLogin();
    }

    /**
     * 获取C端当前登录用户ID
     *
     * @return 客户ID
     */
    public static Long getLoginIdAsLong() {
        return STP_LOGIC.getLoginIdAsLong();
    }

    /**
     * 获取C端 Token 值
     *
     * @return Token
     */
    public static String getTokenValue() {
        return STP_LOGIC.getTokenValue();
    }

    /**
     * 强制指定C端用户下线
     *
     * @param id 客户ID
     */
    public static void kickout(Object id) {
        // 调用kickout方法，复用统一能力并保证业务规则一致。
        STP_LOGIC.kickout(id);
    }
}


