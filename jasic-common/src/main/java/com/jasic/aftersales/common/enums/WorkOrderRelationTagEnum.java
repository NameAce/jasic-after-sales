package com.jasic.aftersales.common.enums;

import lombok.Getter;

/**
 * 工单关系标签枚举。
 *
 * <p>该枚举表达的是“当前登录人与某张工单之间已经成立的事实关系标签”，
 * 重点是描述客观事实，而不是直接承担按钮放行结果。它与
 * {@link WorkOrderRelationTypeEnum} 的职责不同：</p>
 *
 * <p>1. `WorkOrderRelationTypeEnum` 是单值、偏展示语义，适合前端展示一个主身份。</p>
 * <p>2. `WorkOrderRelationTagEnum` 是多值、偏授权语义，适合表达一人多岗、多重关系并存。</p>
 *
 * <p>例如同一用户在当前受理公司下，既可能是这张单的被派维修员，
 * 又同时拥有转单权限。在这种场景里，单一 relationType 很难完整表达全部能力，
 * 但关系标签可以同时具备 `CURRENT_ACCEPT_COMPANY` 与 `ASSIGNEE`。</p>
 *
 * <p>后端推荐的授权顺序为：</p>
 * <p>1. 先判断能否查看工单。</p>
 * <p>2. 再解析工单关系标签集合。</p>
 * <p>3. 最后结合动作对应的基础权限点和工单状态做实例级放行。</p>
 *
 * @author Codex
 * @date 2026/04/11
 */
@Getter
public enum WorkOrderRelationTagEnum {

    /**
     * 平台管理员视角。
     *
     * <p>平台账号通常不依赖工单参与记录即可查看平台范围内工单，
     * 因此单独打上该标签，便于后续动作策略做平台特例扩展。</p>
     */
    PLATFORM_ADMIN("PLATFORM_ADMIN", "平台管理员"),

    /**
     * 当前登录公司就是工单当前受理公司。
     *
     * <p>这是大多数管理动作的核心前提，例如派单、转单、复检、关闭等。
     * 是否最终允许执行，还需要再看当前用户是否拥有对应动作的基础权限点。</p>
     */
    CURRENT_ACCEPT_COMPANY("CURRENT_ACCEPT_COMPANY", "当前受理公司"),

    /**
     * 当前登录人就是这张工单当前被指派的维修员。
     *
     * <p>接单、报价、维修记录保存/完成等典型技师动作通常依赖该标签。</p>
     */
    ASSIGNEE("ASSIGNEE", "当前维修员"),

    /**
     * 当前登录公司是建单公司。
     *
     * <p>该标签当前主要用于补充关系事实，为后续扩展建单方补资料、
     * 回看历史或补充评价等能力预留挂载点。</p>
     */
    CREATOR_COMPANY("CREATOR_COMPANY", "建单公司"),

    /**
     * 总部观察者。
     *
     * <p>该标签来源于工单参与方记录中的总部只读参与关系，
     * 默认只有查看权，不直接附带写入类动作能力。</p>
     */
    HQ_OBSERVER("HQ_OBSERVER", "总部观察者"),

    /**
     * 历史参与方。
     *
     * <p>表示当前公司曾经处理或参与过这张单，但现在已经不是当前受理方。
     * 该标签默认用于历史可见和只读展示，不直接表示具备管理动作。</p>
     */
    HISTORY_PARTICIPANT("HISTORY_PARTICIPANT", "历史参与方");

    /** 编码 */
    private final String code;

    /** 说明 */
    private final String desc;

    WorkOrderRelationTagEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
