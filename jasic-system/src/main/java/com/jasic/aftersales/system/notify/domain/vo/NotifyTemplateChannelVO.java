package com.jasic.aftersales.system.notify.domain.vo;

import com.jasic.aftersales.system.notify.domain.dto.NotifyChannelFieldMappingDTO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 通知模板渠道配置返回对象。
 *
 * <p>该对象用于后台渠道配置查询和运行时渠道快照读取，
 * 既保留原始 `config_json`，也展开常用字段，便于后台直接编辑和运行时快速判断。</p>
 *
 * @author Codex
 * @date 2026/05/15
 */
@ApiModel(description = "通知模板渠道配置返回对象")
@Data
public class NotifyTemplateChannelVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键。
     */
    @ApiModelProperty(value = "主键")
    private Long id;

    /**
     * 通知场景编码。
     */
    @ApiModelProperty(value = "通知场景编码")
    private String sceneCode;

    /**
     * 渠道类型编码。
     */
    @ApiModelProperty(value = "渠道类型编码")
    private String channelType;

    /**
     * 渠道类型说明。
     */
    @ApiModelProperty(value = "渠道类型说明")
    private String channelTypeDesc;

    /**
     * 渠道启停状态。
     */
    @ApiModelProperty(value = "渠道状态：1启用，0停用")
    private Integer channelEnabled;

    /**
     * 小程序订阅消息模板 ID。
     */
    @ApiModelProperty(value = "小程序订阅消息模板ID")
    private String templateId;

    /**
     * 小程序场景。
     */
    @ApiModelProperty(value = "小程序场景，B/C")
    private String channelScene;

    /**
     * 小程序场景说明。
     */
    @ApiModelProperty(value = "小程序场景说明")
    private String channelSceneDesc;

    /**
     * 页面路径模板。
     */
    @ApiModelProperty(value = "页面路径模板")
    private String pagePathTemplate;

    /**
     * 字段映射。
     */
    @ApiModelProperty(value = "字段映射")
    private List<NotifyChannelFieldMappingDTO> fieldMapping;

    /**
     * 原始配置 JSON。
     */
    @ApiModelProperty(value = "原始配置JSON")
    private String configJson;

    /**
     * 备注。
     */
    @ApiModelProperty(value = "备注")
    private String remark;

    /**
     * 创建时间。
     */
    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;

    /**
     * 更新时间。
     */
    @ApiModelProperty(value = "更新时间")
    private LocalDateTime updateTime;
}
