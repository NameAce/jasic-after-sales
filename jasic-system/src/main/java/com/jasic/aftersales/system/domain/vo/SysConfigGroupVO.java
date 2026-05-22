package com.jasic.aftersales.system.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 系统配置分组返回对象。
 *
 * <p>该对象用于前端一次性展示系统配置分组。它只表达当前已经确认的轻量分组结果，
 * 不代表引入配置分组表、动态表单元数据或完整配置中心模型。</p>
 *
 * @author Zoro
 * @date 2026/05/21
 */
@ApiModel(description = "系统配置分组返回对象")
@Data
public class SysConfigGroupVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 配置分组标识，固定取 org、wechat、work_order、legacy。 */
    @ApiModelProperty(value = "配置分组标识")
    private String groupKey;

    /** 配置分组展示名称，用于前端 tab、分组标题或折叠面板标题展示。 */
    @ApiModelProperty(value = "配置分组展示名称")
    private String groupName;

    /** 是否历史废弃分组，true 表示仅用于隔离历史配置，不应作为新功能配置入口。 */
    @ApiModelProperty(value = "是否历史废弃分组")
    private Boolean legacy;

    /** 分组下的配置项列表；即使当前分组暂无配置，也返回空列表以便前端稳定渲染分组结构。 */
    @ApiModelProperty(value = "配置项列表")
    private List<SysConfigVO> configs;
}
