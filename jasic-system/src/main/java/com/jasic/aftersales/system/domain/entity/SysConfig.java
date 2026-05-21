package com.jasic.aftersales.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jasic.aftersales.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 参数设置实体。
 *
 * <p>该实体对应 sys_config 单表配置项，当前仍服务于旧“参数设置”页和各业务模块按 key 读取配置的能力。
 * 本轮分组改造仅在现有配置项上增加 groupKey，用于标识配置所属业务分组，不引入配置中心、分组表或复杂元数据。</p>
 *
 * @author Codex
 * @date 2026/03/19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_config")
public class SysConfig extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键，作为参数设置记录的唯一标识。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 参数名称，用于旧参数设置页展示配置项的业务含义。 */
    private String configName;

    /** 参数键名，作为代码读取配置值和缓存配置值时使用的稳定业务 key。 */
    private String configKey;

    /** 参数键值，保存具体环境下的配置内容，允许按环境留空后再补充。 */
    private String configValue;

    /** 是否内置（1=是，0=否），用于限制内置配置被旧参数设置页删除。 */
    private Integer configType;

    /** 配置分组标识，仅允许使用后端约定的 org、wechat、work_order、legacy。 */
    private String groupKey;

    /** 备注，用于说明配置项的业务用途、默认值口径或环境维护要求。 */
    private String remark;
}
