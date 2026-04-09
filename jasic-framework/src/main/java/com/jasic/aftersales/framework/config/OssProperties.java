package com.jasic.aftersales.framework.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云 OSS 配置
 *
 * @author Codex
 * @date 2026/04/07
 */
@Data
@Component
@ConfigurationProperties(prefix = "jasic.oss")
public class OssProperties {

    /** 是否启用 */
    private boolean enabled;

    /** 外网访问域名，例如 https://oss-cn-shanghai.aliyuncs.com */
    private String endpoint;

    /** 区域 */
    private String region;

    /** AccessKey ID */
    private String accessKeyId;

    /** AccessKey Secret */
    private String accessKeySecret;

    /** 存储桶 */
    private String bucket;

    /** 公网访问前缀 */
    private String publicUrlPrefix;

    /** 预签名预览有效期，单位秒 */
    private Long previewExpireSeconds;
}
