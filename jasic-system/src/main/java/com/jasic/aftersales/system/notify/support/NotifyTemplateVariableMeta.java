package com.jasic.aftersales.system.notify.support;

/**
 * 通知模板变量元数据。
 *
 * <p>用于给模板配置页和预览能力提供统一的变量说明。
 * 该对象只描述变量含义和示例，不负责变量实际取值解析。</p>
 *
 * @author Zoro
 * @date 2026/05/15
 */
public class NotifyTemplateVariableMeta {

    /**
     * 变量名。
     */
    private final String name;

    /**
     * 变量说明。
     */
    private final String desc;

    /**
     * 示例值。
     */
    private final String example;

    /**
     * 构造通知模板变量元数据。
     *
     * @param name 变量名
     * @param desc 变量说明
     * @param example 示例值
     */
    public NotifyTemplateVariableMeta(String name, String desc, String example) {
        this.name = name;
        this.desc = desc;
        this.example = example;
    }

    /**
     * 获取变量名。
     *
     * @return 变量名
     */
    public String getName() {
        return name;
    }

    /**
     * 获取变量说明。
     *
     * @return 变量说明
     */
    public String getDesc() {
        return desc;
    }

    /**
     * 获取示例值。
     *
     * @return 示例值
     */
    public String getExample() {
        return example;
    }
}
