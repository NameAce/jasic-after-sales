package com.jasic.aftersales.system.notify.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 排障视角下的状态计数视图。
 *
 * <p>该对象用于把同一通知目标下的多条产物状态按统一口径聚合后返回给页面，
 * 让排障页可以明确看到“失败几条、跳过几条、死信几条”，
 * 而不是只能看到一条最新记录。</p>
 *
 * @author Codex
 * @date 2026/05/16
 */
@ApiModel(description = "通知排障状态计数视图")
@Data
public class NotifyTraceStatusCountVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态编码 */
    @ApiModelProperty(value = "状态编码")
    private String status;

    /** 状态说明 */
    @ApiModelProperty(value = "状态说明")
    private String statusDesc;

    /** 状态数量 */
    @ApiModelProperty(value = "状态数量")
    private Integer count;
}
