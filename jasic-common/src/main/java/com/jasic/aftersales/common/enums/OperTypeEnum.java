package com.jasic.aftersales.common.enums;

import lombok.Getter;

/**
 * 操作类型枚举
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Getter
public enum OperTypeEnum {

    /** 其他 */
    OTHER(0, "其他"),

    /** 新增 */
    INSERT(1, "新增"),

    /** 修改 */
    UPDATE(2, "修改"),

    /** 删除 */
    DELETE(3, "删除"),

    /** 授权 */
    GRANT(4, "授权"),

    /** 导出 */
    EXPORT(5, "导出"),

    /** 登录 */
    LOGIN(6, "登录"),

    /** 登出 */
    LOGOUT(7, "登出"),

    /** 强制下线 */
    KICKOUT(8, "强制下线");

    /** 类型编码 */
    private final int code;

    /** 描述 */
    private final String desc;

    OperTypeEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
