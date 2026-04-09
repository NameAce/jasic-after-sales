package com.jasic.aftersales.framework.storage;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.ObjectMetadata;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.framework.config.OssProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;

/**
 * 阿里云 OSS 对象存储实现
 *
 * @author Codex
 * @date 2026/04/07
 */
@Service
public class AliyunOssObjectStorageService implements ObjectStorageService {

    @Resource
    private OssProperties ossProperties;

    private final Object lock = new Object();

    private volatile OSS ossClient;

    @Override
    public ObjectStorageUploadResult upload(MultipartFile file, String objectKey) {
        validateConfig();
        if (file == null || file.isEmpty()) {
            throw new ServiceException("上传文件不能为空");
        }
        String normalizedObjectKey = normalizeObjectKey(objectKey);
        try (InputStream inputStream = file.getInputStream()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(resolveContentType(file.getContentType()));
            getOssClient().putObject(ossProperties.getBucket().trim(), normalizedObjectKey, inputStream, metadata);
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServiceException("文件上传失败");
        }
        ObjectStorageUploadResult result = new ObjectStorageUploadResult();
        result.setBucket(ossProperties.getBucket().trim());
        result.setObjectKey(normalizedObjectKey);
        return result;
    }

    @Override
    public void delete(String objectKey) {
        validateConfig();
        String normalizedObjectKey = normalizeObjectKey(objectKey);
        try {
            getOssClient().deleteObject(ossProperties.getBucket().trim(), normalizedObjectKey);
        } catch (Exception ex) {
            throw new ServiceException("删除文件失败");
        }
    }

    @Override
    public String generatePresignedPreviewUrl(String objectKey, long expireSeconds) {
        validateConfig();
        String normalizedObjectKey = normalizeObjectKey(objectKey);
        long normalizedExpireSeconds = expireSeconds > 0 ? expireSeconds : defaultPreviewExpireSeconds();
        try {
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                    ossProperties.getBucket().trim(),
                    normalizedObjectKey,
                    HttpMethod.GET
            );
            request.setExpiration(new Date(System.currentTimeMillis() + normalizedExpireSeconds * 1000));
            URL url = getOssClient().generatePresignedUrl(request);
            if (url == null) {
                throw new ServiceException("生成预览地址失败");
            }
            return url.toString();
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServiceException("生成预览地址失败");
        }
    }

    @Override
    public String calculateSha256(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("上传文件不能为空");
        }
        try (InputStream inputStream = file.getInputStream()) {
            return DigestUtil.sha256Hex(inputStream);
        } catch (Exception ex) {
            throw new ServiceException("计算文件哈希失败");
        }
    }

    @Override
    public String getBucket() {
        validateConfig();
        return ossProperties.getBucket().trim();
    }

    @PreDestroy
    public void shutdown() {
        if (ossClient != null) {
            ossClient.shutdown();
        }
    }

    /**
     * 延迟初始化 OSS 客户端，避免未使用场景提前建立连接。
     *
     * @return OSS 客户端
     */
    private OSS getOssClient() {
        if (ossClient != null) {
            return ossClient;
        }
        synchronized (lock) {
            if (ossClient == null) {
                ossClient = new OSSClientBuilder().build(
                        ossProperties.getEndpoint().trim(),
                        ossProperties.getAccessKeyId().trim(),
                        ossProperties.getAccessKeySecret().trim()
                );
            }
            return ossClient;
        }
    }

    /**
     * 校验对象存储配置是否完整可用。
     */
    private void validateConfig() {
        if (!ossProperties.isEnabled()) {
            throw new ServiceException("对象存储（OSS）未启用");
        }
        if (StrUtil.hasBlank(
                ossProperties.getEndpoint(),
                ossProperties.getAccessKeyId(),
                ossProperties.getAccessKeySecret(),
                ossProperties.getBucket())) {
            throw new ServiceException("对象存储（OSS）配置不完整");
        }
    }

    /**
     * 统一规范对象键格式，避免前后斜杠和空白导致同一文件出现多个路径。
     *
     * @param objectKey 原始对象键
     * @return 规范化后的对象键
     */
    private String normalizeObjectKey(String objectKey) {
        String normalized = StrUtil.trimToNull(objectKey);
        if (normalized == null) {
            throw new ServiceException("对象键不能为空");
        }
        normalized = normalized.replace("\\", "/");
        normalized = normalized.replaceAll("/+", "/");
        normalized = normalized.replaceAll("^/+", "");
        if (normalized.isEmpty()) {
            throw new ServiceException("对象键不能为空");
        }
        return normalized;
    }

    /**
     * 缺省时回退为通用二进制类型，避免浏览器侧解析失败。
     *
     * @param contentType 原始内容类型
     * @return 可安全入库的内容类型
     */
    private String resolveContentType(String contentType) {
        return StrUtil.blankToDefault(StrUtil.trimToNull(contentType), "application/octet-stream");
    }

    /**
     * 读取预览地址默认有效期配置。
     *
     * @return 预览地址有效秒数
     */
    private long defaultPreviewExpireSeconds() {
        Long previewExpireSeconds = ossProperties.getPreviewExpireSeconds();
        return previewExpireSeconds == null || previewExpireSeconds <= 0 ? 1800L : previewExpireSeconds;
    }
}
