package com.jasic.aftersales.system.notify.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jasic.aftersales.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通知场景实体。
 *
 * <p>该实体对应 `notify_scene`，负责持久化系统注册场景的后台维护态信息。
 * 场景名称、业务类型、事件编码由注册表给出，表中主要承载场景启停状态和后台备注。
 * 该实体不承载具体通知目标配置，也不负责运行时发送。</p>
 *
 * @author Zoro
 * @date 2026/05/16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notify_scene")
public class NotifyScene extends BaseEntity {

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
     * 场景名称。
     */
    private String sceneName;

    /**
     * 业务类型。
     */
    private String bizType;

    /**
     * 事件编码。
     */
    private String eventCode;

    /**
     * 场景状态：1 启用，0 停用。
     */
    private Integer status;

    /**
     * 备注。
     */
    private String remark;
}
