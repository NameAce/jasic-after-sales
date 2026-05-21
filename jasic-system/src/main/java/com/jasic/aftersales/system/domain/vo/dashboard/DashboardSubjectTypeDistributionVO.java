package com.jasic.aftersales.system.domain.vo.dashboard;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 平台首页主体类型分布。
 *
 * <p>该对象把平台、总部、服务主体三类公司数量固定成首页字段，
 * 避免前端再从字典和公司列表里自行归并。</p>
 *
 * @author Codex
 * @date 2026/05/20
 */
@ApiModel(description = "平台首页主体类型分布")
@Data
public class DashboardSubjectTypeDistributionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 平台主体数量。
     */
    @ApiModelProperty(value = "平台主体数量")
    private Long platformCount;

    /**
     * 总部主体数量。
     */
    @ApiModelProperty(value = "总部主体数量")
    private Long hqCount;

    /**
     * 服务主体数量。
     */
    @ApiModelProperty(value = "服务主体数量")
    private Long serviceCount;
}
