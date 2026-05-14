package com.jasic.aftersales.system.notify.domain.query;

import com.jasic.aftersales.common.core.domain.PageQuery;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通知消息查询参数。
 *
 * @author Codex
 * @date 2026/04/18
 */
@ApiModel(description = "通知消息查询参数")
@Data
@EqualsAndHashCode(callSuper = true)
public class NotifyMessageQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 消息盒子（TODO/HISTORY） */
    @ApiModelProperty(value = "消息盒子（TODO/HISTORY）")
    private String box;

    /** 接收人ID */
    @ApiModelProperty(value = "接收人ID")
    private Long receiverId;

    /** 接收公司ID */
    @ApiModelProperty(value = "接收公司ID")
    private Long receiverCompanyId;

    /** 业务类型 */
    @ApiModelProperty(value = "业务类型")
    private String bizType;

    /** 业务ID */
    @ApiModelProperty(value = "业务ID")
    private Long bizId;
}
