package com.jasic.aftersales.framework.storage;

import lombok.Data;

import java.io.Serializable;

/**
 * 对象存储上传结果
 *
 * @author Zoro
 * @date 2026/04/07
 */
@Data
public class ObjectStorageUploadResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 存储桶 */
    private String bucket;

    /** 对象键 */
    private String objectKey;
}
