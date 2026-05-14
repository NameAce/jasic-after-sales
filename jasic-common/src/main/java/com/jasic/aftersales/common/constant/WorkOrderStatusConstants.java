package com.jasic.aftersales.common.constant;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 工单状态常量。
 *
 * @author Codex
 * @date 2026/03/31
 */
public class WorkOrderStatusConstants {

    /**
     * 构造工单状态实例。
     */
    private WorkOrderStatusConstants() {
    }

    /** 工单内部主状态。 */
    public static final class MainStatus {

        /** 待派单。 */
        public static final String PENDING_ASSIGN = "PENDING_ASSIGN";

        /** 待接单。 */
        public static final String PENDING_TECH_ACCEPT = "PENDING_TECH_ACCEPT";

        /** 维修中。 */
        public static final String IN_PROGRESS = "IN_PROGRESS";

        /** 已完成。 */
        public static final String COMPLETED = "COMPLETED";

        /** 已关闭。 */
        public static final String CLOSED = "CLOSED";

        /** 待接单聚合态对应的内部状态集合。 */
        public static final Set<String> WAIT_ACCEPT_SET = Collections.unmodifiableSet(
                new LinkedHashSet<>(Arrays.asList(PENDING_ASSIGN, PENDING_TECH_ACCEPT))
        );

        /** 主状态中文映射。 */
        public static final Map<String, String> LABELS = unmodifiableMap(
                entry(PENDING_ASSIGN, "待派单"),
                entry(PENDING_TECH_ACCEPT, "待接单"),
                entry(IN_PROGRESS, "维修中"),
                entry(COMPLETED, "已完成"),
                entry(CLOSED, "已关闭")
        );

        /**
         * 构造主状态实例。
         *
         * @return 处理结果
         */
        private MainStatus() {
        }
    }

    /** 工单外显状态。 */
    public static final class DisplayStatus {

        /** 待接单。 */
        public static final String WAIT_ACCEPT = "WAIT_ACCEPT";

        /** 维修中。 */
        public static final String IN_PROGRESS = MainStatus.IN_PROGRESS;

        /** 已完成。 */
        public static final String COMPLETED = MainStatus.COMPLETED;

        /** 已关闭。 */
        public static final String CLOSED = MainStatus.CLOSED;

        /** 外显状态中文映射。 */
        public static final Map<String, String> LABELS = unmodifiableMap(
                entry(WAIT_ACCEPT, "待接单"),
                entry(IN_PROGRESS, "维修中"),
                entry(COMPLETED, "已完成"),
                entry(CLOSED, "已关闭")
        );

        /**
         * 构造展示状态实例。
         *
         * @return 处理结果
         */
        private DisplayStatus() {
        }
    }

    /** 工单评价状态。 */
    public static final class EvaluateStatus {

        /** 未开启评价。 */
        public static final String NOT_OPEN = "NOT_OPEN";

        /** 待评价。 */
        public static final String PENDING_EVALUATE = "PENDING_EVALUATE";

        /** 已评价。 */
        public static final String EVALUATED = "EVALUATED";

        /** 评价状态中文映射。 */
                public static final Map<String, String> LABELS = unmodifiableMap(
                        entry(NOT_OPEN, "未开启评价"),
                        entry(PENDING_EVALUATE, "待评价"),
                        entry(EVALUATED, "已评价")
                );

        /**
         * 构造评价状态实例。
         */
        private EvaluateStatus() {
        }
    }

    /**
     * 判断主状态是否为待接单聚合态。
     *
     * @param mainStatus 主状态编码
     * @return true 表示属于待接单聚合态
     */
    public static boolean isWaitAcceptMainStatus(String mainStatus) {
        return MainStatus.WAIT_ACCEPT_SET.contains(mainStatus);
    }

    /**
     * 判断主状态是否允许转单。
     *
     * @param mainStatus 主状态编码
     * @return true 表示允许转单
     */
    public static boolean canTransfer(String mainStatus) {
        return MainStatus.IN_PROGRESS.equals(mainStatus) || MainStatus.COMPLETED.equals(mainStatus);
    }

    /**
     * 根据主状态解析内部展示文案。
     *
     * @param mainStatus 主状态编码
     * @return 中文文案
     */
    public static String resolveMainStatusLabel(String mainStatus) {
        return resolveLabel(MainStatus.LABELS, mainStatus);
    }

    /**
     * 根据主状态解析外显状态编码。
     *
     * @param mainStatus 主状态编码
     * @return 外显状态编码
     */
    public static String resolveDisplayStatus(String mainStatus) {
        if (isWaitAcceptMainStatus(mainStatus)) {
            return DisplayStatus.WAIT_ACCEPT;
        }
        return mainStatus;
    }

    /**
     * 根据主状态解析外显中文文案。
     *
     * @param mainStatus 主状态编码
     * @return 中文文案
     */
    public static String resolveDisplayStatusLabel(String mainStatus) {
        return resolveLabel(DisplayStatus.LABELS, resolveDisplayStatus(mainStatus));
    }

    /**
     * 解析评价状态中文文案。
     *
     * @param evaluateStatus 评价状态编码
     * @return 中文文案
     */
    public static String resolveEvaluateStatusLabel(String evaluateStatus) {
        return resolveLabel(EvaluateStatus.LABELS, evaluateStatus);
    }

    /**
     * 解析标签。
     *
     * @param labelMap 参数
     * @param code 参数
     * @return 处理结果
     */
    private static String resolveLabel(Map<String, String> labelMap, String code) {
        if (code == null || code.trim().isEmpty()) {
            return code;
        }
        // 调用get方法，复用统一能力并保证业务规则一致。
        String label = labelMap.get(code);
        return label == null ? code : label;
    }

    /**
     * unmodifiableMap。
     *
     * @param entries 参数
     * @return 处理结果
     */
    @SafeVarargs
    private static Map<String, String> unmodifiableMap(Map.Entry<String, String>... entries) {
        Map<String, String> map = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : entries) {
            // 调用getValue方法，复用统一能力并保证业务规则一致。
            map.put(entry.getKey(), entry.getValue());
        }
        return Collections.unmodifiableMap(map);
    }

    /**
     * entry。
     *
     * @param key 参数
     * @param value 参数
     * @return 处理结果
     */
    private static Map.Entry<String, String> entry(String key, String value) {
        return new AbstractMap.SimpleEntry<>(key, value);
    }
}





