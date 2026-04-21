package com.jasic.aftersales.system.notify.support;

/**
 * 通知模块固定常量。
 *
 * @author Codex
 * @date 2026/04/18
 */
public final class NotifyConstants {

    public static final String EVENT_KEY_PREFIX_WORK_ORDER_ASSIGNED = "WORK_ORDER_ASSIGNED";

    public static final String MESSAGE_TYPE_TODO = "TODO";

    public static final String ROUTE_TYPE_WORK_ORDER_DETAIL = "WORK_ORDER_DETAIL";

    public static final String BOX_TODO = "TODO";

    public static final String BOX_HISTORY = "HISTORY";

    public static final String ACTION_TECH_ACCEPT = "TECH_ACCEPT";

    public static final String ASSIGN_TYPE_ASSIGN = "ASSIGN";

    public static final String ASSIGN_TYPE_TRANSFER = "TRANSFER";

    public static final String TEMPLATE_CODE_WORK_ORDER_ASSIGNED = "WORK_ORDER_ASSIGNED";

    public static final String TEMPLATE_SOURCE_BUILT_IN = "BUILT_IN";

    public static final String TEMPLATE_SOURCE_CUSTOM = "CUSTOM";

    public static final String TODO_TITLE_ASSIGNED = "你有新的工单待处理";

    public static final int EVENT_CONSUME_BATCH_SIZE = 20;

    public static final long EVENT_RETRY_DELAY_MINUTES = 5L;

    private NotifyConstants() {
    }
}
