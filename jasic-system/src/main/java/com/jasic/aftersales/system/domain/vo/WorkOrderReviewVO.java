package com.jasic.aftersales.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 工单复检视图
 *
 * @author Codex
 * @date 2026/03/26
 */
@ApiModel(description = "工单复检视图")
@Data
public class WorkOrderReviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 复检记录ID */
    @ApiModelProperty(value = "复检记录ID")
    private Long id;

    /** 复检公司ID */
    @ApiModelProperty(value = "复检公司ID")
    private Long companyId;

    /** 复检公司名称 */
    @ApiModelProperty(value = "复检公司名称")
    private String companyName;

    /** 复检人ID */
    @ApiModelProperty(value = "复检人ID")
    private Long reviewUserId;

    /** 复检人姓名 */
    @ApiModelProperty(value = "复检人姓名")
    private String reviewUserName;

    /** 复检结果 */
    @ApiModelProperty(value = "复检结果")
    private String reviewResult;

    /** 复检说明 */
    @ApiModelProperty(value = "复检说明")
    private String reviewDesc;

    /** 是否继续维修 */
    @ApiModelProperty(value = "是否继续维修")
    private Integer isContinueRepair;

    /** 创建时间 */
    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
