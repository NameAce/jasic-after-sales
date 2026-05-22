package com.jasic.aftersales.system.domain.vo.dashboard;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 首页趋势序列。
 *
 * <p>趋势序列按“事件发生时间”聚合，不表示当前状态存量。
 * 每个 values 数组长度必须和 HomeTrendVO.days 保持一致，由服务端补齐没有事件的日期。</p>
 *
 * @author Zoro
 * @date 2026/05/21
 */
@ApiModel(description = "首页趋势序列")
@Data
public class HomeTrendSeriesVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 序列编码。
     */
    @ApiModelProperty(value = "序列编码")
    private String code;

    /**
     * 序列名称。
     */
    @ApiModelProperty(value = "序列名称")
    private String name;

    /**
     * 每日事件数量。
     */
    @ApiModelProperty(value = "每日事件数量")
    private List<Long> values = new ArrayList<>();
}
