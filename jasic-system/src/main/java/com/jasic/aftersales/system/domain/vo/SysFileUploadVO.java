package com.jasic.aftersales.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 文件上传结果视图
 *
 * @author Zoro
 * @date 2026/04/07
 */
@ApiModel(description = "文件上传结果视图")
@Data
public class SysFileUploadVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 文件ID */
    @ApiModelProperty(value = "文件ID")
    private Long fileId;

    /** 原始文件名 */
    @ApiModelProperty(value = "原始文件名")
    private String originalName;

    /** 内容类型 */
    @ApiModelProperty(value = "内容类型")
    private String contentType;

    /** 文件大小 */
    @ApiModelProperty(value = "文件大小")
    private Long fileSize;

    /** 文件扩展名 */
    @ApiModelProperty(value = "文件扩展名")
    private String fileExt;

    /** 文件哈希 */
    @ApiModelProperty(value = "文件哈希")
    private String fileHash;

    /** 预览地址 */
    @ApiModelProperty(value = "预览地址")
    private String previewUrl;
}
