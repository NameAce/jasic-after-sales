package com.jasic.aftersales.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 文件预览地址视图
 *
 * @author Codex
 * @date 2026/04/07
 */
@ApiModel(description = "文件预览地址视图")
@Data
public class SysFilePreviewVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 文件ID */
    @ApiModelProperty(value = "文件ID")
    private Long fileId;

    /** 预览地址 */
    @ApiModelProperty(value = "预览地址")
    private String previewUrl;

    /** 有效秒数 */
    @ApiModelProperty(value = "有效秒数")
    private Long expireSeconds;
}
