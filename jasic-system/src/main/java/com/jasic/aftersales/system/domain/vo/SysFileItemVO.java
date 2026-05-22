package com.jasic.aftersales.system.domain.vo;

import com.jasic.aftersales.common.enums.SysFileBizTypeEnum;
import lombok.Data;

import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 文件项视图
 *
 * @author Zoro
 * @date 2026/04/07
 */
@ApiModel(description = "文件项视图")
@Data
public class SysFileItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 文件ID */
    @ApiModelProperty(value = "文件ID")
    private Long fileId;

    /** 业务类型 */
    @ApiModelProperty(value = "业务类型")
    private SysFileBizTypeEnum bizType;

    /** 业务ID */
    @ApiModelProperty(value = "业务ID")
    private Long bizId;

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

    /** 排序号 */
    @ApiModelProperty(value = "排序号")
    private Integer sortNum;

    /** 是否主文件 */
    @ApiModelProperty(value = "是否主文件")
    private Integer isPrimary;

    /** 预览地址 */
    @ApiModelProperty(value = "预览地址")
    private String previewUrl;
}
