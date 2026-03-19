package com.jasic.aftersales.framework.web;

/**
 * 错误码常量定义（遵循阿里5位错误码规范）
 * <p>
 * A 类：用户端错误
 * B 类：系统内部错误
 * C 类：第三方服务错误
 * </p>
 *
 * @author Zoro
 * @date 2026/03/18
 */
public class ResultCode {

    private ResultCode() {
    }

    /** 成功 */
    public static final String SUCCESS = "00000";

    /** 用户端错误 - 通用 */
    public static final String USER_ERROR = "A0001";

    /** 未登录 */
    public static final String NOT_LOGIN = "A0100";

    /** 无权限 */
    public static final String NOT_PERMISSION = "A0200";

    /** 用户名或密码错误 */
    public static final String LOGIN_ERROR = "A0110";

    /** 账号已停用 */
    public static final String ACCOUNT_DISABLED = "A0120";

    /** 参数校验失败 */
    public static final String PARAM_ERROR = "A0400";

    /** 数据不存在 */
    public static final String DATA_NOT_FOUND = "A0410";

    /** 数据已存在 */
    public static final String DATA_ALREADY_EXISTS = "A0420";

    /** 系统内部错误 */
    public static final String SYSTEM_ERROR = "B0001";

    /** 第三方服务错误 */
    public static final String THIRD_PARTY_ERROR = "C0001";
}
