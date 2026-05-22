package com.jasic.aftersales.system.domain.vo.dashboard;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 平台治理看板原始统计结果。
 *
 * <p>该对象只承接平台首页“组织、账号、基础配置”三块所需的聚合 SQL 结果。
 * 它不包含工单、CRM 同步、消息治理、日志趋势或基础配置完整度字段，确保平台首页不会误接入本期明确不做的内容。</p>
 *
 * @author Zoro
 * @date 2026/05/21
 */
@ApiModel(description = "平台治理看板原始统计结果")
@Data
public class DashboardPlatformGovernanceStatsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 总部主体数量。
     */
    @ApiModelProperty(value = "总部主体数量")
    private Long hqCompanyCount;

    /**
     * 服务网点主体数量。
     */
    @ApiModelProperty(value = "服务网点主体数量")
    private Long serviceCompanyCount;

    /**
     * 启用总部与服务网点主体数量。
     */
    @ApiModelProperty(value = "启用总部与服务网点主体数量")
    private Long enabledSubjectCount;

    /**
     * 停用总部与服务网点主体数量。
     */
    @ApiModelProperty(value = "停用总部与服务网点主体数量")
    private Long disabledSubjectCount;

    /**
     * B 端用户总数。
     */
    @ApiModelProperty(value = "B端用户总数")
    private Long userTotal;

    /**
     * 启用用户数量。
     */
    @ApiModelProperty(value = "启用用户数量")
    private Long enabledUserCount;

    /**
     * 停用用户数量。
     */
    @ApiModelProperty(value = "停用用户数量")
    private Long disabledUserCount;

    /**
     * 角色数量。
     */
    @ApiModelProperty(value = "角色数量")
    private Long roleCount;

    /**
     * 产品资料数量。
     */
    @ApiModelProperty(value = "产品资料数量")
    private Long productCount;

    /**
     * 服务类型配置数量。
     */
    @ApiModelProperty(value = "服务类型配置数量")
    private Long serviceTypeCount;

    /**
     * 字典配置项数量。
     */
    @ApiModelProperty(value = "字典配置项数量")
    private Long dictItemCount;

    /**
     * 区域配置数量。
     */
    @ApiModelProperty(value = "区域配置数量")
    private Long regionCount;
}
