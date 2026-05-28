package com.jasic.aftersales.system.domain.query;

import com.jasic.aftersales.common.core.domain.PageQuery;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 反馈后台管理分页查询参数。
 *
 * @author Codex
 * @date 2026/05/28
 */
@ApiModel(description = "反馈后台管理分页查询参数")
@Data
@EqualsAndHashCode(callSuper = true)
public class SysFeedbackManageQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 视图类型（UNACCEPTED/ACCEPTED/ALL） */
    @ApiModelProperty(value = "视图类型", allowableValues = "UNACCEPTED,ACCEPTED,ALL")
    private String viewType;

    /** 联系电话（模糊） */
    @ApiModelProperty(value = "联系电话（模糊）")
    private String contactPhone;

    /** 提交来源（模糊） */
    @ApiModelProperty(value = "提交来源（模糊）")
    private String submitSourceName;

    /** 提交开始时间 */
    @ApiModelProperty(value = "提交开始时间，支持 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss")
    private String beginCreateTime;

    /** 提交结束时间 */
    @ApiModelProperty(value = "提交结束时间，支持 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss")
    private String endCreateTime;

    /** 受理开始时间 */
    @ApiModelProperty(value = "受理开始时间，支持 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss")
    private String beginAcceptTime;

    /** 受理结束时间 */
    @ApiModelProperty(value = "受理结束时间，支持 yyyy-MM-dd 或 yyyy-MM-dd HH:mm:ss")
    private String endAcceptTime;
}
