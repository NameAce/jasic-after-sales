package com.jasic.aftersales.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 文件业务类型枚举
 *
 * @author Codex
 * @date 2026/04/07
 */
public enum SysFileBizTypeEnum {

    /** 故障图片 */
    WORK_ORDER_FAULT_IMAGE("WORK_ORDER_FAULT_IMAGE", "工单故障图片"),

    /** 故障视频 */
    WORK_ORDER_FAULT_VIDEO("WORK_ORDER_FAULT_VIDEO", "工单故障视频"),

    /** 故障语音 */
    WORK_ORDER_FAULT_VOICE("WORK_ORDER_FAULT_VOICE", "工单故障语音"),

    /** 寄件凭证 */
    WORK_ORDER_SENDER_VOUCHER("WORK_ORDER_SENDER_VOUCHER", "工单寄件凭证"),

    /** 回寄凭证 */
    WORK_ORDER_RETURN_VOUCHER("WORK_ORDER_RETURN_VOUCHER", "工单回寄凭证"),

    /** 维修登记旧故障图片 */
    WORK_ORDER_REPAIR_OLD_IMAGE("WORK_ORDER_REPAIR_OLD_IMAGE", "维修登记旧故障图片"),

    /** 维修登记新故障图片 */
    WORK_ORDER_REPAIR_NEW_IMAGE("WORK_ORDER_REPAIR_NEW_IMAGE", "维修登记新故障图片"),

    /** 维修登记机器正面照片 */
    WORK_ORDER_REPAIR_MACHINE_IMAGE("WORK_ORDER_REPAIR_MACHINE_IMAGE", "维修登记机器正面照片"),

    /** 维修登记机器条码照片 */
    WORK_ORDER_REPAIR_BARCODE_IMAGE("WORK_ORDER_REPAIR_BARCODE_IMAGE", "维修登记机器条码照片"),

    /** 维修登记其他图片 */
    WORK_ORDER_REPAIR_OTHER_IMAGE("WORK_ORDER_REPAIR_OTHER_IMAGE", "维修登记其他图片");

    /**
     * 系统文件业务类型编码。
     *
     * @param code 参数
     * @param desc 参数
     * @return 处理结果
     */
    private final String code;

    private final String desc;

    /**
     * 构造系统文件业务类型实例。
     *
     * @param code 参数
     * @param desc 参数
     */
    SysFileBizTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据编码查询文件业务类型。
     *
     * @param code 参数
     * @return 处理结果
     */
    public static SysFileBizTypeEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        // 调用trim方法，复用统一能力并保证业务规则一致。
        String normalizedCode = code.trim();
        if (normalizedCode.isEmpty()) {
            return null;
        }
        for (SysFileBizTypeEnum value : values()) {
            if (value.code.equals(normalizedCode)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 根据编码解析文件业务类型。
     *
     * @param code 参数
     * @return 处理结果
     */
    @JsonCreator
    public static SysFileBizTypeEnum fromCode(String code) {
        // 调用getByCode方法，复用统一能力并保证业务规则一致。
        SysFileBizTypeEnum value = getByCode(code);
        if (value == null && code != null) {
            throw new IllegalArgumentException("不支持的文件业务类型编码：" + code);
        }
        return value;
    }

    /**
     * 获取文件业务类型编码。
     *
     * @return 处理结果
     */
    @JsonValue
    public String getCode() {
        return code;
    }

    /**
     * 获取文件业务类型描述。
     *
     * @return 处理结果
     */
    public String getDesc() {
        return desc;
    }
}




