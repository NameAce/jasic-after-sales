package com.jasic.aftersales.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 字典类型 VO
 *
 * @author Codex
 * @date 2026/03/19
 */
@ApiModel(description = "字典类型 VO")
@Data
public class SysDictTypeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 字典名称 */
    @ApiModelProperty(value = "字典名称")
    private String dictName;

    /** 字典类型 */
    @ApiModelProperty(value = "字典类型")
    private String dictType;

    /** 状态 */
    @ApiModelProperty(value = "状态")
    private Integer status;

    /** 备注 */
    @ApiModelProperty(value = "备注")
    private String remark;

    /** 创建时间 */
    @ApiModelProperty(value = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
