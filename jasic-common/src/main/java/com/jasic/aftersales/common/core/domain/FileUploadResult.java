package com.jasic.aftersales.common.core.domain;

import lombok.Data;

import java.io.Serializable;

/**
 * 文件上传结果
 *
 * @author Codex
 * @date 2026/04/07
 */
@Data
public class FileUploadResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 文件访问地址 */
    private String url;

    /** 对象存储键 */
    private String objectKey;

    /** 原始文件名 */
    private String originalName;

    /** 文件类型 */
    private String contentType;

    /** 文件大小 */
    private Long size;
}
