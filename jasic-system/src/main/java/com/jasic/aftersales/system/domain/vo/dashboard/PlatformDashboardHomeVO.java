package com.jasic.aftersales.system.domain.vo.dashboard;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 平台主体首页返回结构。
 *
 * <p>该对象是 `/dashboard/platform/home` 的唯一返回契约。本轮平台首页命名为“治理看板”，
 * 只允许返回组织治理、账号治理和基础配置三块内容，不包含任何工单、CRM 同步、消息治理、趋势图或完整度评分字段。</p>
 *
 * @author Zoro
 * @date 2026/05/21
 */
@ApiModel(description = "平台主体首页返回结构")
@Data
public class PlatformDashboardHomeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 首页标题。
     */
    @ApiModelProperty(value = "首页标题")
    private String title;

    /**
     * 组织治理分区。
     */
    @ApiModelProperty(value = "组织治理分区")
    private HomeSectionVO organization;

    /**
     * 账号治理分区。
     */
    @ApiModelProperty(value = "账号治理分区")
    private HomeSectionVO account;

    /**
     * 基础配置分区。
     */
    @ApiModelProperty(value = "基础配置分区")
    private HomeSectionVO basicConfig;
}
