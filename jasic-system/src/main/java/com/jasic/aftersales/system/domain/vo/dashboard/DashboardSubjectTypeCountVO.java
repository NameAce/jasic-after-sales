package com.jasic.aftersales.system.domain.vo.dashboard;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 主体类型计数原始结果。
 *
 * <p>该对象只用于承接 Mapper 的分组计数结果，
 * 最终仍由 Service 映射成固定的三字段结构。</p>
 *
 * @author Codex
 * @date 2026/05/20
 */
@ApiModel(description = "主体类型计数原始结果")
@Data
public class DashboardSubjectTypeCountVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主体类型编码。
     */
    @ApiModelProperty(value = "主体类型编码")
    private String subjectType;

    /**
     * 数量。
     */
    @ApiModelProperty(value = "数量")
    private Long countNum;
}
