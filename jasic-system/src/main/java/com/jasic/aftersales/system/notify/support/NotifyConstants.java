package com.jasic.aftersales.system.notify.support;

/**
 * 通知模块常量。
 *
 * <p>这里保留通知模板、待办类型和可靠性默认值，
 * 其中重试和超时相关参数允许被配置项覆盖。</p>
 *
 * @author Zoro
 * @date 2026/04/18
 */
public final class NotifyConstants {

    /**EVENT_KEY_PREFIX_WORK_ORDER_ASSIGNED 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    public static final String EVENT_KEY_PREFIX_WORK_ORDER_ASSIGNED = "WORK_ORDER_ASSIGNED";

    /**EVENT_KEY_PREFIX_WORK_ORDER_ACCEPT 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    public static final String EVENT_KEY_PREFIX_WORK_ORDER_ACCEPT = "WORK_ORDER_ACCEPT";

    /**EVENT_KEY_PREFIX_WORK_ORDER_TRANSFER_IN 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    public static final String EVENT_KEY_PREFIX_WORK_ORDER_TRANSFER_IN = "WORK_ORDER_TRANSFER_IN";

    /**EVENT_KEY_PREFIX_WORK_ORDER_ACCEPTED 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    public static final String EVENT_KEY_PREFIX_WORK_ORDER_ACCEPTED = "WORK_ORDER_ACCEPTED";

    /**EVENT_KEY_PREFIX_WORK_ORDER_TRANSFER_NOTICE 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    public static final String EVENT_KEY_PREFIX_WORK_ORDER_TRANSFER_NOTICE = "WORK_ORDER_TRANSFER_NOTICE";

    /**EVENT_KEY_PREFIX_WORK_ORDER_EVALUATION_INVITE 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    public static final String EVENT_KEY_PREFIX_WORK_ORDER_EVALUATION_INVITE = "WORK_ORDER_EVALUATION_INVITE";

    /**EVENT_KEY_PREFIX_WORK_ORDER_EVALUATED 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    public static final String EVENT_KEY_PREFIX_WORK_ORDER_EVALUATED = "WORK_ORDER_EVALUATED";

    /**MESSAGE_TYPE_TODO 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    public static final String MESSAGE_TYPE_TODO = "TODO";

    /**ROUTE_TYPE_WORK_ORDER_DETAIL 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    public static final String ROUTE_TYPE_WORK_ORDER_DETAIL = "WORK_ORDER_DETAIL";

    /**ROUTE_TYPE_WORK_ORDER_EVALUATE 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    public static final String ROUTE_TYPE_WORK_ORDER_EVALUATE = "WORK_ORDER_EVALUATE";

    /**BOX_TODO 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    public static final String BOX_TODO = "TODO";

    /**BOX_HISTORY 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    public static final String BOX_HISTORY = "HISTORY";

    /**ACTION_TECH_ACCEPT 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    public static final String ACTION_TECH_ACCEPT = "TECH_ACCEPT";

    /**ASSIGN_TYPE_ASSIGN 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    public static final String ASSIGN_TYPE_ASSIGN = "ASSIGN";

    /**ASSIGN_TYPE_TRANSFER 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    public static final String ASSIGN_TYPE_TRANSFER = "TRANSFER";

    /**
     * 通知事件表 `receiver_id` 的兼容占位值。
     *
     * <p>新通知链路里，部分事件只表达“发生了什么”，并不天然存在单一接收人，
     * 但部分历史库实例仍保留 `receiver_id NOT NULL` 约束。
     * 因此当事件本身不需要真实接收人时，先写入该占位值，避免主事务因历史表结构失败。</p>
     */
    public static final long EVENT_RECEIVER_ID_PLACEHOLDER = 0L;

    /**EVENT_CONSUME_BATCH_SIZE 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    public static final int EVENT_CONSUME_BATCH_SIZE = 20;

    /**EVENT_RETRY_MAX_COUNT 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    public static final int EVENT_RETRY_MAX_COUNT = 3;

    /**EVENT_RETRY_DELAY_MINUTES 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    public static final long EVENT_RETRY_DELAY_MINUTES = 5L;

    /**EVENT_PROCESSING_TIMEOUT_MINUTES 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    public static final long EVENT_PROCESSING_TIMEOUT_MINUTES = 10L;

    /**DISPATCH_SEND_BATCH_SIZE 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    public static final int DISPATCH_SEND_BATCH_SIZE = 20;

    /**DISPATCH_RETRY_MAX_COUNT 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    public static final int DISPATCH_RETRY_MAX_COUNT = 3;

    /**DISPATCH_RETRY_DELAY_MINUTES 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    public static final long DISPATCH_RETRY_DELAY_MINUTES = 5L;

    /**DISPATCH_PROCESSING_TIMEOUT_MINUTES 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    public static final long DISPATCH_PROCESSING_TIMEOUT_MINUTES = 10L;

    /**
     * 禁止实例化通知常量类。
     */
    private NotifyConstants() {
    }
}

