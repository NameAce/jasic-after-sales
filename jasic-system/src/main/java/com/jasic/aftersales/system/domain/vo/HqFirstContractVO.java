package com.jasic.aftersales.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 总部-一级签约列表 VO（含关联名称）
 *
 * @author Zoro
 * @date 2026/03/18
 */
@ApiModel(description = "总部-一级签约列表 VO（含关联名称）")
@Data
public class HqFirstContractVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 总部公司ID */
    @ApiModelProperty(value = "总部公司ID")
    private Long hqCompanyId;

    /** 一级网点公司ID */
    @ApiModelProperty(value = "一级网点公司ID")
    private Long firstCompanyId;

    /** 大区ID */
    @ApiModelProperty(value = "大区ID")
    private Long regionId;

    /** 总部公司名称 */
    @ApiModelProperty(value = "总部公司名称")
    private String hqCompanyName;

    /** 一级网点名称 */
    @ApiModelProperty(value = "一级网点名称")
    private String firstCompanyName;

    /** 大区名称 */
    @ApiModelProperty(value = "大区名称")
    private String regionName;

    /** 合同编号 */
    @ApiModelProperty(value = "合同编号")
    private String contractNo;

    /** 状态（1=有效，0=终止） */
    @ApiModelProperty(value = "状态（1=有效，0=终止）")
    private Integer status;

    /** 备注 */
    @ApiModelProperty(value = "备注")
    private String remark;

    /** 签约时间（展示用，取创建时间） */
    @ApiModelProperty(value = "签约时间（展示用，取创建时间）")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime contractTime;

    /** 创建时间 */
    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
