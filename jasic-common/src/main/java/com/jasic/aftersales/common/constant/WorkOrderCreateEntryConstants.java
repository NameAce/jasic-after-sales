package com.jasic.aftersales.common.constant;

/**
 * 工单建单入口类型常量。
 *
 * @author Zoro
 * @date 2026/04/06
 */
public class WorkOrderCreateEntryConstants {

    /**
     * 禁止实例化建单入口常量类。
     */
    private WorkOrderCreateEntryConstants() {
    }

    /** 代客户填写，由当前公司自行受理 */
    public static final String PROXY_SELF = "PROXY_SELF";

    /** 二级报修一级，由上游一级受理 */
    public static final String UPSTREAM_FIRST = "UPSTREAM_FIRST";

    /** 一级报修佳士，由上游总部受理 */
    public static final String UPSTREAM_HQ = "UPSTREAM_HQ";

    /** 客户在 C 端提交报修，由当前服务网点接单处理 */
    public static final String CUSTOMER_REPORT = "CUSTOMER_REPORT";
}
