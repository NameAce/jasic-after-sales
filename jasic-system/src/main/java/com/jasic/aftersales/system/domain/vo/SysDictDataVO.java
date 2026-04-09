package com.jasic.aftersales.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 字典数据 VO
 *
 * @author Codex
 * @date 2026/03/19
 */
@ApiModel(description = "字典数据 VO")
@Data
public class SysDictDataVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 字典类型 */
    @ApiModelProperty(value = "字典类型")
    private String dictType;

    /** 字典标签 */
    @ApiModelProperty(value = "字典标签")
    private String dictLabel;

    /** 字典键值 */
    @ApiModelProperty(value = "字典键值")
    private String dictValue;

    /** 排序 */
    @ApiModelProperty(value = "排序")
    private Integer dictSort;

    /** 自定义样式 */
    @ApiModelProperty(value = "自定义样式")
    private String cssClass;

    /** 标签样式 */
    @ApiModelProperty(value = "标签样式")
    private String listClass;

    /** 是否默认 */
    @ApiModelProperty(value = "是否默认")
    private Integer isDefault;

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
