package com.jasic.aftersales.system.notify.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jasic.aftersales.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通知场景目标配置实体。
 *
 * <p>该实体对应 `notify_scene_target`，负责持久化“一个场景下的一个通知目标如何配置”。
 * 它统一承载站内消息、站内待办和小程序订阅消息的启停、模板、跳转与渠道专属参数。
 * 该实体不负责接收人规则解析，也不负责运行时发送。</p>
 *
 * @author Zoro
 * @date 2026/05/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notify_scene_target")
public class NotifySceneTarget extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 场景编码。
     */
    private String sceneCode;

    /**
     * 通知目标类型。
     */
    private String targetType;

    /**
     * 是否启用：1 启用，0 停用。
     */
    private Integer enabled;

    /**
     * 标题模板。
     */
    private String titleTemplate;

    /**
     * 内容模板。
     */
    private String contentTemplate;

    /**
     * 跳转类型。
     */
    private String routeType;

    /**
     * 跳转值模板。
     */
    private String routeValueTemplate;

    /**
     * 目标专属配置 JSON。
     */
    private String configJson;

    /**
     * 备注。
     */
    private String remark;
}
