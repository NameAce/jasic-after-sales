package com.jasic.aftersales.common.constant;

/**
 * 工单状态流转定义。
 *
 * @author Zoro
 * @date 2026/03/31
 */
public class WorkOrderStatusFlow {

    /**
     * 禁止实例化状态流转常量类。
     */
    private WorkOrderStatusFlow() {
    }

    /**
     * 建单后的主状态。
     *
     * @return 主状态编码
     */
    public static String afterCreate() {
        return WorkOrderStatusConstants.MainStatus.PENDING_ASSIGN;
    }

    /**
     * 建单后的评价状态。
     *
     * @return 评价状态编码
     */
    public static String afterCreateEvaluateStatus() {
        return WorkOrderStatusConstants.EvaluateStatus.NOT_OPEN;
    }

    /**
     * 派单后的主状态。
     *
     * @return 主状态编码
     */
    public static String afterAssign() {
        return WorkOrderStatusConstants.MainStatus.PENDING_TECH_ACCEPT;
    }

    /**
     * 维修员接单后的主状态。
     *
     * @return 主状态编码
     */
    public static String afterTechAccept() {
        return WorkOrderStatusConstants.MainStatus.IN_PROGRESS;
    }

    /**
     * 转单后的主状态。
     *
     * @return 主状态编码
     */
    public static String afterTransfer() {
        return WorkOrderStatusConstants.MainStatus.PENDING_ASSIGN;
    }

    /**
     * 维修完成后的主状态。
     *
     * @return 主状态编码
     */
    public static String afterRepairFinish() {
        return WorkOrderStatusConstants.MainStatus.COMPLETED;
    }

    /**
     * 关闭后的主状态。
     *
     * @return 主状态编码
     */
    public static String afterClose() {
        return WorkOrderStatusConstants.MainStatus.CLOSED;
    }

    /**
     * 关闭后的评价状态。
     *
     * @return 评价状态编码
     */
    public static String afterCloseEvaluateStatus() {
        return WorkOrderStatusConstants.EvaluateStatus.PENDING_EVALUATE;
    }

    /**
     * 按是否可评价计算关单后的评价状态。
     *
     * @param canEvaluate 是否可评价
     * @return 评价状态编码
     */
    public static String afterCloseEvaluateStatus(boolean canEvaluate) {
        return canEvaluate
                ? WorkOrderStatusConstants.EvaluateStatus.PENDING_EVALUATE
                : WorkOrderStatusConstants.EvaluateStatus.NOT_OPEN;
    }

    /**
     * 评价后的评价状态。
     *
     * @return 评价状态编码
     */
    public static String afterEvaluate() {
        return WorkOrderStatusConstants.EvaluateStatus.EVALUATED;
    }
}
