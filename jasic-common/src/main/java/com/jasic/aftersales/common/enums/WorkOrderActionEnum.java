package com.jasic.aftersales.common.enums;

import lombok.Getter;

/**
 * 工单动作枚举。
 *
 * <p>该枚举统一了工单流程动作的编码口径，覆盖三类场景：</p>
 * <p>1. 前端详情页 `availableActions` 返回给按钮层的动作编码。</p>
 * <p>2. 后端工单流转日志 `actionType` 持久化时使用的动作编码。</p>
 * <p>3. 日志展示、通知事件关联、状态流说明时需要的中文动作名称。</p>
 *
 * <p>通过统一枚举，业务代码可以避免到处散落 `"ASSIGN"`、`"QUOTE"` 这类魔法字符串，
 * 也能减少按钮编码、流转日志编码、基础权限码和展示名称不一致的风险。</p>
 *
 * <p>这里的 `permissionCode` 表示该动作对应的“基础菜单权限点”。它只回答
 * “当前账号原则上是否具备这类动作能力”，并不直接等价于“当前这张工单上就一定能做”。
 * 真正放行时，后端仍然需要再叠加工单关系、工单状态、服务模式等实例条件。</p>
 *
 * @author Codex
 * @date 2026/04/11
 */
@Getter
public enum WorkOrderActionEnum {

    /** 创建工单，通常对应建单入口完成后的首条流转记录。 */
    CREATE("CREATE", "建单", "workorder:add"),

    /** 派单给当前受理公司的维修员，把工单推进到待接单阶段。 */
    ASSIGN("ASSIGN", "派单", "workorder:assign"),

    /** 维修员接单，确认开始处理当前工单。 */
    TECH_ACCEPT("TECH_ACCEPT", "维修员接单", "workorder:accept"),

    /** 将工单转交到其他服务公司，并切换当前受理公司。 */
    TRANSFER("TRANSFER", "转单", "workorder:transfer"),

    /** 报价留痕动作，接单首报或维修登记改价都会落该动作。 */
    QUOTE("QUOTE", "报价", ""),

    /** 提交维修登记，并把工单推进到已完成阶段。 */
    REPAIR_FINISH("REPAIR_FINISH", "维修登记", "workorder:repair"),

    /** 管理岗复检，决定通过关闭流或打回继续维修。 */
    REVIEW("REVIEW", "复检", "workorder:review"),

    /**
     * 上传寄修场景的寄件快递单号，补充送修物流信息。
     *
     * <p>该动作本轮临时重构为“建单人本人补录”口径，不再挂接派单权限点；
     * 后端放行只看工单可见性、待接单状态窗口、寄修服务方式以及首条 CREATE 流转识别出的建单人。</p>
     */
    UPLOAD_SEND_EXPRESS("UPLOAD_SEND_EXPRESS", "上传寄件单号", ""),

    /** 选择返还方式，用于关闭前或无故障直接闭单场景的返件信息记录。 */
    RETURN_METHOD("RETURN_METHOD", "选择返回方式", "workorder:close"),

    /** 关闭工单，结束当前售后服务流程。 */
    CLOSE("CLOSE", "关闭工单", "workorder:close");

    /** 编码 */
    private final String code;

    /** 展示名称 */
    private final String label;

    /**
     * 基础权限标识。
     *
     * <p>该字段仅表达“这类动作在系统权限层面是否开放”，
     * 不负责实例级校验，实例级约束由工单权限服务统一叠加判断。</p>
     */
    private final String permissionCode;

    /**
     * 构造工单动作实例。
     *
     * @param code 参数
     * @param label 参数
     * @param permissionCode 参数
     */
    WorkOrderActionEnum(String code, String label, String permissionCode) {
        this.code = code;
        this.label = label;
        this.permissionCode = permissionCode;
    }

    /**
     * 根据编码获取枚举。
     *
     * <p>当数据库流转记录或接口入参以字符串形式传递动作编码时，
     * 可通过该方法回收到统一枚举进行处理。</p>
     *
     * @param code 编码
     * @return 枚举值；未命中时返回 null
     */
    public static WorkOrderActionEnum getByCode(String code) {
        if (code == null) {
            return null;
        }
        for (WorkOrderActionEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }

    /**
     * 根据编码解析展示名称。
     *
     * <p>主要给流转记录展示层使用：数据库只存动作编码，前端或 VO 需要显示中文动作名时，
     * 统一通过该方法做映射，避免在服务层维护一长串 `if/else`。</p>
     *
     * @param code 动作编码
     * @return 展示名称；未命中时返回原值
     */
    public static String resolveLabel(String code) {
        // 调用getByCode方法，复用统一能力并保证业务规则一致。
        WorkOrderActionEnum action = getByCode(code);
        return action == null ? code : action.getLabel();
    }
}




