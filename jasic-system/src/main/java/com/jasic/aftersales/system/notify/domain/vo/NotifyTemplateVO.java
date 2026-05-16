package com.jasic.aftersales.system.notify.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知模板返回对象。
 *
 * <p>该对象用于后台模板配置列表和详情展示，
 * 场景、通知类型、接收对象等只读元数据全部从场景注册表补齐，
 * 避免前端继续维护历史组合字段映射。</p>
 *
 * @author Codex
 * @date 2026/05/15
 */
@ApiModel(description = "通知模板返回对象")
@Data
public class NotifyTemplateVO implements Serializable {

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
     * 通知场景名称。
     */
    @ApiModelProperty(value = "通知场景名称")
    private String sceneName;

    /**
     * 模板名称。
     */
    @ApiModelProperty(value = "模板名称")
    private String templateName;

    /**
     * 通知类型编码。
     */
    @ApiModelProperty(value = "通知类型编码")
    private String notifyType;

    /**
     * 通知类型说明。
     */
    @ApiModelProperty(value = "通知类型说明")
    private String notifyTypeDesc;

    @ApiModelProperty(value = "渠道类型编码")
    private String channelType;

    @ApiModelProperty(value = "渠道类型说明")
    private String channelTypeDesc;

    /**
     * 接收对象类型编码。
     */
    @ApiModelProperty(value = "接收对象类型编码")
    private String receiverType;

    /**
     * 接收对象类型说明。
     */
    @ApiModelProperty(value = "接收对象类型说明")
    private String receiverTypeDesc;

    /**
     * 接收对象展示说明。
     */
    @ApiModelProperty(value = "接收对象说明")
    private String receiverDesc;

    /**
     * 标题模板。
     */
    @ApiModelProperty(value = "标题模板")
    private String titleTemplate;

    /**
     * 内容模板。
     */
    @ApiModelProperty(value = "内容模板")
    private String contentTemplate;

    /**
     * 跳转类型。
     */
    @ApiModelProperty(value = "跳转类型")
    private String routeType;

    /**
     * 跳转值模板。
     */
    @ApiModelProperty(value = "跳转值模板")
    private String routeValueTemplate;

    /**
     * 状态。
     */
    @ApiModelProperty(value = "状态：1启用，0停用")
    private Integer status;

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

