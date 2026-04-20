package com.jasic.aftersales.system.notify.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知消息分页项。
 *
 * @author Codex
 * @date 2026/04/18
 */
@ApiModel(description = "通知消息分页项")
@Data
public class NotifyMessagePageVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 消息ID */
    @ApiModelProperty(value = "消息ID")
    private Long id;

    /** 标题 */
    @ApiModelProperty(value = "标题")
    private String title;

    /** 摘要 */
    @ApiModelProperty(value = "摘要")
    private String summary;

    /** 业务类型 */
    @ApiModelProperty(value = "业务类型")
    private String bizType;

    /** 业务ID */
    @ApiModelProperty(value = "业务ID")
    private Long bizId;

    /** 业务编号 */
    @ApiModelProperty(value = "业务编号")
    private String bizNo;

    /** 跳转类型 */
    @ApiModelProperty(value = "跳转类型")
    private String routeType;

    /** 跳转值 */
    @ApiModelProperty(value = "跳转值")
    private String routeValue;

    /** 待办状态 */
    @ApiModelProperty(value = "待办状态")
    private String todoStatus;

    /** 失效原因 */
    @ApiModelProperty(value = "失效原因")
    private String invalidReason;

    /** 创建时间 */
    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
