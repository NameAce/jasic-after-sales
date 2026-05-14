package com.jasic.aftersales.common.enums;

import lombok.Getter;

/**
 * 工单关系类型枚举。
 *
 * <p>该枚举描述的是“当前登录人与一张工单的业务关系”，不是纯粹的菜单权限。
 * 在售后工单场景里，同一个人即便拥有某个系统权限点，也不一定能对所有可见工单执行动作，
 * 还要先判断他在这张工单里到底是当前受理方管理岗、当前维修员、历史参与方还是总部观察者。</p>
 *
 * <p>后端通常按“能否查看工单 -> 解析 relationType -> 结合主状态和权限点返回动作列表”的顺序做校验。
 * 因此该枚举既用于列表/详情展示当前身份，也用于控制 `availableActions`、只读标记和单个动作接口的放行逻辑。</p>
 *
 * @author Codex
 * @date 2026/04/11
 */
@Getter
public enum WorkOrderRelationTypeEnum {

    /** 平台管理员，可按平台数据权限查看工单；通常不受网点参与历史限制。 */
    PLATFORM_ADMIN("PLATFORM_ADMIN", "平台管理员"),

    /** 当前受理公司下已被明确派单到该工单的维修员，可执行接单、报价、维修等技师动作。 */
    CURRENT_ASSIGNEE("CURRENT_ASSIGNEE", "当前维修员"),

    /** 当前受理公司的管理岗，可执行派单、转单、复检、关闭等管理动作。 */
    CURRENT_OWNER_MANAGER("CURRENT_OWNER_MANAGER", "当前受理公司管理岗"),

    /** 当前受理公司的普通成员，可查看当前受理工单，但默认不具备该工单的管理或技师动作。 */
    CURRENT_OWNER_MEMBER("CURRENT_OWNER_MEMBER", "当前受理公司普通成员"),

    /** 总部观察者，可因总部视角或参与记录看到工单，但在该工单上只读。 */
    HQ_OBSERVER("HQ_OBSERVER", "总部观察者"),

    /** 历史参与公司成员，可见本公司历史经办过的工单，但当前不再是受理方，因此只读。 */
    HISTORY_PARTICIPANT_READONLY("HISTORY_PARTICIPANT_READONLY", "历史参与方只读"),

    /** 当前用户与工单没有可识别业务关系，通常既不应展示动作，也可能无法查看详情。 */
    NONE("NONE", "无关系");

    /** 编码 */
    private final String code;

    /** 说明 */
        private final String desc;

    /**
     * 构造工单关系类型实例。
     */
    WorkOrderRelationTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据编码获取枚举。
     *
     * <p>该方法主要用于接口层或持久化层把字符串编码回收为统一的业务语义，
     * 避免在业务代码里继续散落字符串常量比较。</p>
     *
     * @param code 编码
     * @return 枚举值；未命中时返回 {@link #NONE}
     */
    public static WorkOrderRelationTypeEnum getByCode(String code) {
        if (code == null) {
            return NONE;
        }
        for (WorkOrderRelationTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return NONE;
    }

    /**
     * 当前关系是否只读。
     *
     * <p>只读的含义是：当前用户可以查看工单内容、流转记录、报价/维修等信息，
     * 但不应该获得改变工单状态或写入业务记录的动作按钮。</p>
     *
     * @return true 表示只读
     */
    public boolean isReadonly() {
        return this == HQ_OBSERVER || this == HISTORY_PARTICIPANT_READONLY;
    }

    /**
     * 当前关系是否属于已建立工单关系的用户。
     *
     * <p>这里的“有关系”指当前用户和工单存在明确业务关联，不论是当前受理方、
     * 当前维修员、历史参与方还是总部观察者。该判断通常用于列表只读标记或前端身份展示。</p>
     *
     * @return true 表示存在工单关系
     */
    public boolean hasRelation() {
        return this != NONE;
    }
}





