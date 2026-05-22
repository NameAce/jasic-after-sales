package com.jasic.aftersales.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 工单参与方视图
 *
 * @author Zoro
 * @date 2026/03/26
 */
@ApiModel(description = "工单参与方视图")
@Data
public class WorkOrderParticipantVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 参与公司ID */
    @ApiModelProperty(value = "参与公司ID")
    private Long companyId;

    /** 参与公司名称 */
    @ApiModelProperty(value = "参与公司名称")
    private String companyName;

    /** 主体类型 */
    @ApiModelProperty(value = "主体类型")
    private String subjectType;

    /** 参与类型 */
    @ApiModelProperty(value = "参与类型")
    private String participateType;

    /** 是否当前受理方 */
    @ApiModelProperty(value = "是否当前受理方")
    private Integer isCurrentHandler;

    /** 首次参与时间 */
    @ApiModelProperty(value = "首次参与时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime firstParticipateTime;

    /** 最后参与时间 */
    @ApiModelProperty(value = "最后参与时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastParticipateTime;
}
