package com.jasic.aftersales.framework.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * 对象存储服务
 *
 * @author Codex
 * @date 2026/04/07
 */
public interface ObjectStorageService {

    /**
     * 上传文件到指定对象键
     *
     * @param file      文件
     * @param objectKey 对象键
     * @return 上传结果
     */
    ObjectStorageUploadResult upload(MultipartFile file, String objectKey);

    /**
     * 删除对象
     *
     * @param objectKey 对象键
     */
    void delete(String objectKey);

    /**
     * 生成预签名预览地址
     *
     * @param objectKey      对象键
     * @param expireSeconds  过期秒数
     * @return 预签名地址
     */
    String generatePresignedPreviewUrl(String objectKey, long expireSeconds);

    /**
     * 计算文件哈希
     *
     * @param file 文件
     * @return SHA-256 哈希
     */
    String calculateSha256(MultipartFile file);

    /**
     * 获取存储桶名称
     *
     * @return 存储桶名称
     */
    String getBucket();
}
