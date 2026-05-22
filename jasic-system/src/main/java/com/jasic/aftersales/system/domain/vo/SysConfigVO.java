package com.jasic.aftersales.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 参数设置 VO。
 *
 * <p>用于旧“参数设置”页和后续配置页改造读取配置项。新增 groupKey 只作为后端分组能力输出，
 * 不要求当前前端页面改变布局、结构或交互。</p>
 *
 * @author Zoro
 * @date 2026/03/19
 */
@ApiModel(description = "参数设置 VO")
@Data
public class SysConfigVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键，唯一标识一条参数设置记录。 */
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 参数名称，用于页面展示配置项业务含义。 */
    @ApiModelProperty(value = "参数名称")
    private String configName;

    /** 参数键名，业务代码读取配置值时使用的稳定 key。 */
    @ApiModelProperty(value = "参数键名")
    private String configKey;

    /** 参数键值，返回当前环境下已维护的配置内容。 */
    @ApiModelProperty(value = "参数键值")
    private String configValue;

    /** 是否内置（1=是，0=否），用于页面判断删除限制。 */
    @ApiModelProperty(value = "是否内置")
    private Integer configType;

    /** 配置分组标识，用于后续系统配置聚合页按业务分组展示。 */
    @ApiModelProperty(value = "配置分组标识")
    private String groupKey;

    /** 备注，说明配置项业务用途、默认值口径或环境维护要求。 */
    @ApiModelProperty(value = "备注")
    private String remark;

    /** 创建时间，用于页面展示配置项初始化或新增时间。 */
    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
