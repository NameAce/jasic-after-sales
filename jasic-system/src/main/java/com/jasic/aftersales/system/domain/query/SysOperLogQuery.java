package com.jasic.aftersales.system.domain.query;

import com.jasic.aftersales.common.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 操作日志查询参数
 *
 * @author Zoro
 * @date 2026/03/18
 */
@ApiModel(description = "操作日志查询参数")
@Data
@EqualsAndHashCode(callSuper = true)
public class SysOperLogQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 操作模块（模糊） */
    @ApiModelProperty(value = "操作模块（模糊）")
    private String title;

    /** 操作类型 */
    @ApiModelProperty(value = "操作类型")
    private Integer operType;

    /** 操作人用户名（模糊） */
    @ApiModelProperty(value = "操作人用户名（模糊）")
    private String username;

    /** 操作状态（1=成功，0=失败） */
    @ApiModelProperty(value = "操作状态（1=成功，0=失败）")
    private Integer status;

    /** 开始时间（yyyy-MM-dd HH:mm:ss） */
    @ApiModelProperty(value = "开始时间（yyyy-MM-dd HH:mm:ss）")
    private String beginTime;

    /** 结束时间（yyyy-MM-dd HH:mm:ss） */
    @ApiModelProperty(value = "结束时间（yyyy-MM-dd HH:mm:ss）")
    private String endTime;
}
