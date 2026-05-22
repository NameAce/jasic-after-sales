package com.jasic.aftersales.system.domain.vo.dashboard;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 首页业务分区。
 *
 * <p>三类主体首页都按 section 组织指标：平台拆成组织、账号、基础配置；
 * 总部和服务网点拆成当前承接工单池与已转出。该结构用于避免不同主体首页字段互相混杂。</p>
 *
 * @author Zoro
 * @date 2026/05/21
 */
@ApiModel(description = "首页业务分区")
@Data
public class HomeSectionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分区标题。
     */
    @ApiModelProperty(value = "分区标题")
    private String title;

    /**
     * 分区指标列表。
     */
    @ApiModelProperty(value = "分区指标列表")
    private List<HomeMetricVO> metrics = new ArrayList<>();
}
