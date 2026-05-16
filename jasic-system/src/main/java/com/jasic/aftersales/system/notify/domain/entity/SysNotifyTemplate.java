package com.jasic.aftersales.system.notify.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jasic.aftersales.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通知模板配置实体。
 *
 * <p>该实体对应 `sys_notify_template`，负责承载某个通知场景的模板内容和启停状态。
 * 场景对应的业务类型、通知类型、接收对象、默认模板和变量元数据
 * 统一由 `NotifySceneRegistry` 维护，不再冗余存储在模板表中。</p>
 *
 * @author Codex
 * @date 2026/05/15
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_notify_template")
public class SysNotifyTemplate extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 通知场景编码。
     */
    private String sceneCode;

    /**
     * 模板名称。
     */
    private String templateName;

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
     * 状态：1 启用，0 停用。
     */
    private Integer status;

    /**
     * 备注。
     */
    private String remark;
}

