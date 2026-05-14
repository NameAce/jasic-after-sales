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

    /**
     * OSS字段。
     *
     * @param file 参数
     * @return 处理结果
     */
    private volatile OSS ossClient;

    /**
     * 处理upload业务逻辑。
     *
     * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
     * @param file 参数
     * @param objectKey 参数
     * @return 处理结果
     */
    @Override
    public ObjectStorageUploadResult upload(MultipartFile file, String objectKey) {
        // 说明：执行该步骤以保证业务流程正确。
        validateConfig();
        if (file == null || file.isEmpty()) {
            throw new ServiceException("上传文件不能为空");
        }
        // 调用normalizeObjectKey方法，复用统一能力并保证业务规则一致。
        String normalizedObjectKey = normalizeObjectKey(objectKey);
        try (InputStream inputStream = file.getInputStream()) {
            // 调用ObjectMetadata方法，复用统一能力并保证业务规则一致。
            ObjectMetadata metadata = new ObjectMetadata();
            // 调用getSize方法，复用统一能力并保证业务规则一致。
            metadata.setContentLength(file.getSize());
            // 调用getContentType方法，复用统一能力并保证业务规则一致。
            metadata.setContentType(resolveContentType(file.getContentType()));
            // 调用trim方法，复用统一能力并保证业务规则一致。
            getOssClient().putObject(ossProperties.getBucket().trim(), normalizedObjectKey, inputStream, metadata);
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServiceException("文件上传失败");
        }
        // 调用ObjectStorageUploadResult方法，复用统一能力并保证业务规则一致。
        ObjectStorageUploadResult result = new ObjectStorageUploadResult();
        // 调用trim方法，复用统一能力并保证业务规则一致。
        result.setBucket(ossProperties.getBucket().trim());
        // 调用setObjectKey方法，复用统一能力并保证业务规则一致。
        result.setObjectKey(normalizedObjectKey);
        return result;
    }

    /**
     * 删除阿里云OSS对象存储。
     */
    @Override
    public void delete(String objectKey) {
        // 说明：执行该步骤以保证业务流程正确。
        validateConfig();
        // 调用normalizeObjectKey方法，复用统一能力并保证业务规则一致。
        String normalizedObjectKey = normalizeObjectKey(objectKey);
        try {
            // 调用trim方法，复用统一能力并保证业务规则一致。
            getOssClient().deleteObject(ossProperties.getBucket().trim(), normalizedObjectKey);
        } catch (Exception ex) {
            throw new ServiceException("删除文件失败");
        }
    }

    /**
     * generatePresigned预览Url。
     *
     * @param expireSeconds 参数
     * @return 处理结果
     */
    @Override
    public String generatePresignedPreviewUrl(String objectKey, long expireSeconds) {
        // 说明：执行该步骤以保证业务流程正确。
        validateConfig();
        // 调用normalizeObjectKey方法，复用统一能力并保证业务规则一致。
        String normalizedObjectKey = normalizeObjectKey(objectKey);
        // 调用defaultPreviewExpireSeconds方法，复用统一能力并保证业务规则一致。
        long normalizedExpireSeconds = expireSeconds > 0 ? expireSeconds : defaultPreviewExpireSeconds();
        try {
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                    ossProperties.getBucket().trim(),
                    normalizedObjectKey,
                    HttpMethod.GET
            );
            // 调用currentTimeMillis方法，复用统一能力并保证业务规则一致。
            request.setExpiration(new Date(System.currentTimeMillis() + normalizedExpireSeconds * 1000));
            // 调用generatePresignedUrl方法，复用统一能力并保证业务规则一致。
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

    /**
     * calculateSha256。
     *
     * @param file 参数
     * @return 处理结果
     */
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

    /**
     * 获取Bucket。
     *
     * @return 处理结果
     */
    @Override
    public String getBucket() {
        // 调用validateConfig方法，复用统一能力并保证业务规则一致。
        validateConfig();
        return ossProperties.getBucket().trim();
    }

    /**
     * shutdown。
     */
    @PreDestroy
    public void shutdown() {
        if (ossClient != null) {
            // 调用shutdown方法，复用统一能力并保证业务规则一致。
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
        // 调用trimToNull方法，复用统一能力并保证业务规则一致。
        String normalized = StrUtil.trimToNull(objectKey);
        if (normalized == null) {
            throw new ServiceException("对象键不能为空");
        }
        // 调用replace方法，复用统一能力并保证业务规则一致。
        normalized = normalized.replace("\\", "/");
        // 调用replaceAll方法，复用统一能力并保证业务规则一致。
        normalized = normalized.replaceAll("/+", "/");
        // 调用replaceAll方法，复用统一能力并保证业务规则一致。
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
        // 调用getPreviewExpireSeconds方法，复用统一能力并保证业务规则一致。
        Long previewExpireSeconds = ossProperties.getPreviewExpireSeconds();
        return previewExpireSeconds == null || previewExpireSeconds <= 0 ? 1800L : previewExpireSeconds;
    }
}




