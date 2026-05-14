package com.jasic.aftersales.system.notify.support;

/**
 * Notify module constants.
 *
 * @author Codex
 * @date 2026/04/18
 */
public final class NotifyConstants {

    public static final String EVENT_KEY_PREFIX_WORK_ORDER_ASSIGNED = "WORK_ORDER_ASSIGNED";

    public static final String EVENT_KEY_PREFIX_WORK_ORDER_EVALUATION_INVITE = "WORK_ORDER_EVALUATION_INVITE";

    public static final String MESSAGE_TYPE_TODO = "TODO";

    public static final String MESSAGE_TYPE_EXTERNAL_NOTIFY = "EXTERNAL_NOTIFY";

    public static final String ROUTE_TYPE_WORK_ORDER_DETAIL = "WORK_ORDER_DETAIL";

    public static final String ROUTE_TYPE_WORK_ORDER_EVALUATE = "WORK_ORDER_EVALUATE";

    public static final String BOX_TODO = "TODO";

    public static final String BOX_HISTORY = "HISTORY";

    public static final String ACTION_TECH_ACCEPT = "TECH_ACCEPT";

    public static final String ASSIGN_TYPE_ASSIGN = "ASSIGN";

    public static final String ASSIGN_TYPE_TRANSFER = "TRANSFER";

    public static final String TEMPLATE_CODE_WORK_ORDER_ASSIGNED = "WORK_ORDER_ASSIGNED";

    public static final String TEMPLATE_CODE_WORK_ORDER_EVALUATION_INVITE = "WORK_ORDER_EVALUATION_INVITE";

    public static final String TEMPLATE_SOURCE_BUILT_IN = "BUILT_IN";

    public static final String TEMPLATE_SOURCE_CUSTOM = "CUSTOM";

    public static final String TODO_TITLE_ASSIGNED = "Assigned work order todo";

    public static final int EVENT_CONSUME_BATCH_SIZE = 20;

    public static final long EVENT_RETRY_DELAY_MINUTES = 5L;

    public static final int DISPATCH_SEND_BATCH_SIZE = 20;

    public static final int DISPATCH_RETRY_MAX_COUNT = 3;

    public static final long DISPATCH_RETRY_DELAY_MINUTES = 5L;

    /**
     * 禁止实例化通知常量类。
     */
    private NotifyConstants() {
    }
}
