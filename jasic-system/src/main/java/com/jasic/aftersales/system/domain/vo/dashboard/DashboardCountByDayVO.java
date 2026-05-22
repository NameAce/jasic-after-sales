package com.jasic.aftersales.system.domain.vo.dashboard;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 首页按天聚合结果。
 *
 * <p>该对象只承载 Mapper 层返回的按天计数结果，
 * 由 Service 再按最近七天完整补齐缺失日期。</p>
 *
 * @author Zoro
 * @date 2026/05/20
 */
@ApiModel(description = "首页按天聚合结果")
@Data
public class DashboardCountByDayVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 日期键，格式为 yyyy-MM-dd。
     */
    @ApiModelProperty(value = "日期键")
    private String dayKey;

    /**
     * 当天数量。
     */
    @ApiModelProperty(value = "当天数量")
    private Long countNum;
}
