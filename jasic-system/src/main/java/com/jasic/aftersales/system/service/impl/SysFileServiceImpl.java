package com.jasic.aftersales.system.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jasic.aftersales.common.enums.SysFileAccessLevelEnum;
import com.jasic.aftersales.common.enums.SysFileBizTypeEnum;
import com.jasic.aftersales.common.enums.SysFileMediaTypeEnum;
import com.jasic.aftersales.common.enums.SysFileStatusEnum;
import com.jasic.aftersales.common.enums.SysFileStorageTypeEnum;
import com.jasic.aftersales.common.enums.SysFileUploadUserTypeEnum;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.framework.config.OssProperties;
import com.jasic.aftersales.framework.storage.ObjectStorageService;
import com.jasic.aftersales.framework.storage.ObjectStorageUploadResult;
import com.jasic.aftersales.system.domain.entity.SysFile;
import com.jasic.aftersales.system.domain.entity.SysFileBiz;
import com.jasic.aftersales.system.domain.vo.SysFileItemVO;
import com.jasic.aftersales.system.domain.vo.SysFileUploadVO;
import com.jasic.aftersales.system.mapper.SysFileBizMapper;
import com.jasic.aftersales.system.mapper.SysFileMapper;
import com.jasic.aftersales.system.service.SysFileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文件中台 Service 实现
 *
 * @author Codex
 * @date 2026/04/07
 */
@Service
public class SysFileServiceImpl implements SysFileService {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    private static final long IMAGE_MAX_SIZE = 10L * 1024 * 1024;
    private static final long VIDEO_MAX_SIZE = 50L * 1024 * 1024;
    private static final long VOICE_MAX_SIZE = 10L * 1024 * 1024;

    private static final Set<String> IMAGE_EXTENSIONS = new LinkedHashSet<>();
    private static final Set<String> VIDEO_EXTENSIONS = new LinkedHashSet<>();
    private static final Set<String> VOICE_EXTENSIONS = new LinkedHashSet<>();

    static {
        Collections.addAll(IMAGE_EXTENSIONS, "jpg", "jpeg", "png", "webp");
        Collections.addAll(VIDEO_EXTENSIONS, "mp4", "mov");
        Collections.addAll(VOICE_EXTENSIONS, "mp3", "wav", "amr", "aac");
    }

    @Resource
    private ObjectStorageService objectStorageService;

    @Resource
    private OssProperties ossProperties;

    @Resource
    private SysFileMapper sysFileMapper;

    @Resource
    private SysFileBizMapper sysFileBizMapper;

    /**
     * 上传文件。
     *
     * @param file 参数
     * @param bizDir 参数
     * @param uploadUserId upload User ID
     * @param uploadUserType 参数
     * @param uploadCompanyId upload Company ID
     * @return 处理结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysFileUploadVO upload(MultipartFile file, String bizDir, Long uploadUserId,
                                  SysFileUploadUserTypeEnum uploadUserType, Long uploadCompanyId) {
        // 说明：执行该步骤以保证业务流程正确。
        validateUploadFile(file);
        // 调用getOriginalFilename方法，复用统一能力并保证业务规则一致。
        String fileExt = resolveFileExtension(file.getOriginalFilename());
        // 调用resolveMediaType方法，复用统一能力并保证业务规则一致。
        SysFileMediaTypeEnum mediaType = resolveMediaType(fileExt);
        // 调用getSize方法，复用统一能力并保证业务规则一致。
        validateFileSize(file.getSize(), mediaType);
        // 调用calculateSha256方法，复用统一能力并保证业务规则一致。
        String fileHash = objectStorageService.calculateSha256(file);
        // 调用buildObjectKey方法，复用统一能力并保证业务规则一致。
        String objectKey = buildObjectKey(bizDir, fileExt);
        // 调用upload方法，复用统一能力并保证业务规则一致。
        ObjectStorageUploadResult uploadResult = objectStorageService.upload(file, objectKey);

        // 调用SysFile方法，复用统一能力并保证业务规则一致。
        SysFile entity = new SysFile();
        // 调用setStorageType方法，复用统一能力并保证业务规则一致。
        entity.setStorageType(SysFileStorageTypeEnum.OSS);
        // 调用getBucket方法，复用统一能力并保证业务规则一致。
        entity.setBucket(uploadResult.getBucket());
        // 调用getObjectKey方法，复用统一能力并保证业务规则一致。
        entity.setObjectKey(uploadResult.getObjectKey());
        // 调用getOriginalFilename方法，复用统一能力并保证业务规则一致。
        entity.setOriginalName(normalizeOriginalName(file.getOriginalFilename(), fileExt));
        // 调用getContentType方法，复用统一能力并保证业务规则一致。
        entity.setContentType(resolveContentType(file.getContentType()));
        // 调用getSize方法，复用统一能力并保证业务规则一致。
        entity.setFileSize(file.getSize());
        // 调用setFileExt方法，复用统一能力并保证业务规则一致。
        entity.setFileExt(fileExt);
        // 调用setFileHash方法，复用统一能力并保证业务规则一致。
        entity.setFileHash(fileHash);
        // 调用setAccessLevel方法，复用统一能力并保证业务规则一致。
        entity.setAccessLevel(SysFileAccessLevelEnum.PRIVATE);
        // 调用setUploadUserId方法，复用统一能力并保证业务规则一致。
        entity.setUploadUserId(uploadUserId);
        // 调用setUploadUserType方法，复用统一能力并保证业务规则一致。
        entity.setUploadUserType(uploadUserType);
        // 调用setUploadCompanyId方法，复用统一能力并保证业务规则一致。
        entity.setUploadCompanyId(uploadCompanyId);
        // 调用setStatus方法，复用统一能力并保证业务规则一致。
        entity.setStatus(SysFileStatusEnum.ACTIVE);
        // 说明：执行该步骤以保证业务流程正确。
        sysFileMapper.insert(entity);

        // 调用SysFileUploadVO方法，复用统一能力并保证业务规则一致。
        SysFileUploadVO vo = new SysFileUploadVO();
        // 调用getId方法，复用统一能力并保证业务规则一致。
        vo.setFileId(entity.getId());
        // 调用getOriginalName方法，复用统一能力并保证业务规则一致。
        vo.setOriginalName(entity.getOriginalName());
        // 调用getContentType方法，复用统一能力并保证业务规则一致。
        vo.setContentType(entity.getContentType());
        // 调用getFileSize方法，复用统一能力并保证业务规则一致。
        vo.setFileSize(entity.getFileSize());
        // 调用getFileExt方法，复用统一能力并保证业务规则一致。
        vo.setFileExt(entity.getFileExt());
        // 调用getFileHash方法，复用统一能力并保证业务规则一致。
        vo.setFileHash(entity.getFileHash());
        // 调用generatePreviewUrl方法，复用统一能力并保证业务规则一致。
        vo.setPreviewUrl(generatePreviewUrl(entity));
        return vo;
    }

    /**
     * 替换业务文件。
     *
     * @param bizType 参数
     * @param operatorUserId operator User ID
     * @param operatorUserType 参数
     * @param remark 参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceBizFiles(SysFileBizTypeEnum bizType, Long bizId, List<Long> fileIds, Long companyId,
                                Long operatorUserId, SysFileUploadUserTypeEnum operatorUserType, String remark) {
        // 说明：执行该步骤以保证业务流程正确。
        validateBizTarget(bizType, bizId);
        LambdaQueryWrapper<SysFileBiz> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SysFileBiz::getBizType, bizType)
                // 调用eq方法，复用统一能力并保证业务规则一致。
                .eq(SysFileBiz::getBizId, bizId);
        // 说明：执行该步骤以保证业务流程正确。
        sysFileBizMapper.delete(deleteWrapper);

        // 调用normalizeFileIds方法，复用统一能力并保证业务规则一致。
        List<Long> normalizedFileIds = normalizeFileIds(fileIds);
        if (normalizedFileIds.isEmpty()) {
            return;
        }
        // 调用loadActiveFileMap方法，复用统一能力并保证业务规则一致。
        Map<Long, SysFile> fileMap = loadActiveFileMap(normalizedFileIds);
        int sortNum = 1;
        for (Long fileId : normalizedFileIds) {
            // 调用get方法，复用统一能力并保证业务规则一致。
            SysFile sysFile = fileMap.get(fileId);
            if (sysFile == null) {
                throw new ServiceException("文件不存在或已失效");
            }
            // 调用SysFileBiz方法，复用统一能力并保证业务规则一致。
            SysFileBiz relation = new SysFileBiz();
            // 调用setFileId方法，复用统一能力并保证业务规则一致。
            relation.setFileId(fileId);
            // 调用setBizType方法，复用统一能力并保证业务规则一致。
            relation.setBizType(bizType);
            // 调用setBizId方法，复用统一能力并保证业务规则一致。
            relation.setBizId(bizId);
            // 调用setSortNum方法，复用统一能力并保证业务规则一致。
            relation.setSortNum(sortNum++);
            // 调用setIsPrimary方法，复用统一能力并保证业务规则一致。
            relation.setIsPrimary(0);
            // 调用setCompanyId方法，复用统一能力并保证业务规则一致。
            relation.setCompanyId(companyId);
            // 调用setOperatorUserId方法，复用统一能力并保证业务规则一致。
            relation.setOperatorUserId(operatorUserId);
            // 调用setOperatorUserType方法，复用统一能力并保证业务规则一致。
            relation.setOperatorUserType(operatorUserType);
            // 调用normalizeNullableText方法，复用统一能力并保证业务规则一致。
            relation.setRemark(normalizeNullableText(remark));
            // 调用insert方法，复用统一能力并保证业务规则一致。
            sysFileBizMapper.insert(relation);
        }
    }

    /**
     * unbind业务文件。
     *
     * @param bizType 参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbindBizFile(SysFileBizTypeEnum bizType, Long bizId, Long fileId) {
        // 说明：执行该步骤以保证业务流程正确。
        validateBizTarget(bizType, bizId);
        if (fileId == null) {
            throw new ServiceException("文件ID不能为空");
        }
        LambdaQueryWrapper<SysFileBiz> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysFileBiz::getBizType, bizType)
                .eq(SysFileBiz::getBizId, bizId)
                // 调用eq方法，复用统一能力并保证业务规则一致。
                .eq(SysFileBiz::getFileId, fileId);
        // 说明：执行该步骤以保证业务流程正确。
        sysFileBizMapper.delete(wrapper);
    }

    /**
     * 分页查询业务Files列表。
     *
     * @param bizType 参数
     * @return 处理结果
     */
    @Override
    public List<SysFileItemVO> listBizFiles(SysFileBizTypeEnum bizType, Long bizId) {
        // 调用validateBizTarget方法，复用统一能力并保证业务规则一致。
        validateBizTarget(bizType, bizId);
        // 调用selectVisibleBizRelations方法，复用统一能力并保证业务规则一致。
        List<SysFileBiz> relations = sysFileBizMapper.selectVisibleBizRelations(bizType, bizId);
        return buildFileItems(relations);
    }

    /**
     * 分页查询业务文件Map列表。
     *
     * @param bizTypes 参数
     * @return 处理结果
     */
    @Override
    public Map<SysFileBizTypeEnum, List<SysFileItemVO>> listBizFileMap(List<SysFileBizTypeEnum> bizTypes, Long bizId) {
        if (bizTypes == null || bizTypes.isEmpty() || bizId == null) {
            return Collections.emptyMap();
        }
        List<SysFileBizTypeEnum> normalizedBizTypes = bizTypes.stream()
                .filter(item -> item != null)
                .distinct()
                // 调用toList方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toList());
        if (normalizedBizTypes.isEmpty()) {
            return Collections.emptyMap();
        }
        // 说明：执行该步骤以保证业务流程正确。
        List<SysFileBiz> relations = sysFileBizMapper.selectVisibleBizRelationsByTypes(normalizedBizTypes, bizId);
        Map<SysFileBizTypeEnum, List<SysFileItemVO>> result = new EnumMap<>(SysFileBizTypeEnum.class);
        for (SysFileBizTypeEnum bizType : normalizedBizTypes) {
            // 调用put方法，复用统一能力并保证业务规则一致。
            result.put(bizType, new ArrayList<>());
        }
        if (relations.isEmpty()) {
            return result;
        }
        Map<Long, SysFile> fileMap = loadFileMap(relations.stream()
                .map(SysFileBiz::getFileId)
                // 调用toList方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toList()));
        for (SysFileBiz relation : relations) {
            // 调用getFileId方法，复用统一能力并保证业务规则一致。
            SysFile sysFile = fileMap.get(relation.getFileId());
            if (sysFile == null || sysFile.getStatus() != SysFileStatusEnum.ACTIVE) {
                continue;
            }
            result.computeIfAbsent(relation.getBizType(), key -> new ArrayList<>())
                    // 调用buildFileItem方法，复用统一能力并保证业务规则一致。
                    .add(buildFileItem(sysFile, relation));
        }
        return result;
    }

    /**
     * 构建文件Items。
     *
     * @param relations 参数
     * @return 处理结果
     */
    private List<SysFileItemVO> buildFileItems(List<SysFileBiz> relations) {
        if (relations == null || relations.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, SysFile> fileMap = loadFileMap(relations.stream()
                .map(SysFileBiz::getFileId)
                // 调用toList方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toList()));
        List<SysFileItemVO> result = new ArrayList<>();
        for (SysFileBiz relation : relations) {
            // 调用getFileId方法，复用统一能力并保证业务规则一致。
            SysFile sysFile = fileMap.get(relation.getFileId());
            if (sysFile == null || sysFile.getStatus() != SysFileStatusEnum.ACTIVE) {
                continue;
            }
            // 调用buildFileItem方法，复用统一能力并保证业务规则一致。
            result.add(buildFileItem(sysFile, relation));
        }
        return result;
    }

    /**
     * 构建文件项。
     *
     * @param sysFile 参数
     * @param relation 参数
     * @return 处理结果
     */
    private SysFileItemVO buildFileItem(SysFile sysFile, SysFileBiz relation) {
        // 调用SysFileItemVO方法，复用统一能力并保证业务规则一致。
        SysFileItemVO vo = new SysFileItemVO();
        // 调用getId方法，复用统一能力并保证业务规则一致。
        vo.setFileId(sysFile.getId());
        // 调用getBizType方法，复用统一能力并保证业务规则一致。
        vo.setBizType(relation.getBizType());
        // 调用getBizId方法，复用统一能力并保证业务规则一致。
        vo.setBizId(relation.getBizId());
        // 调用getOriginalName方法，复用统一能力并保证业务规则一致。
        vo.setOriginalName(sysFile.getOriginalName());
        // 调用getContentType方法，复用统一能力并保证业务规则一致。
        vo.setContentType(sysFile.getContentType());
        // 调用getFileSize方法，复用统一能力并保证业务规则一致。
        vo.setFileSize(sysFile.getFileSize());
        // 调用getFileExt方法，复用统一能力并保证业务规则一致。
        vo.setFileExt(sysFile.getFileExt());
        // 调用getSortNum方法，复用统一能力并保证业务规则一致。
        vo.setSortNum(relation.getSortNum());
        // 调用getIsPrimary方法，复用统一能力并保证业务规则一致。
        vo.setIsPrimary(relation.getIsPrimary());
        // 调用generatePreviewUrl方法，复用统一能力并保证业务规则一致。
        vo.setPreviewUrl(generatePreviewUrl(sysFile));
        return vo;
    }

    /**
     * loadActive文件Map。
     *
     * @return 处理结果
     */
    private Map<Long, SysFile> loadActiveFileMap(List<Long> fileIds) {
        // 调用loadFileMap方法，复用统一能力并保证业务规则一致。
        Map<Long, SysFile> fileMap = loadFileMap(fileIds);
        for (Long fileId : fileIds) {
            // 调用get方法，复用统一能力并保证业务规则一致。
            SysFile sysFile = fileMap.get(fileId);
            if (sysFile == null || sysFile.getStatus() != SysFileStatusEnum.ACTIVE) {
                throw new ServiceException("文件不存在或已失效");
            }
        }
        return fileMap;
    }

    /**
     * load文件Map。
     *
     * @return 处理结果
     */
    private Map<Long, SysFile> loadFileMap(List<Long> fileIds) {
        // 调用normalizeFileIds方法，复用统一能力并保证业务规则一致。
        List<Long> normalizedFileIds = normalizeFileIds(fileIds);
        if (normalizedFileIds.isEmpty()) {
            return Collections.emptyMap();
        }
        // 说明：执行该步骤以保证业务流程正确。
        List<SysFile> fileList = sysFileMapper.selectBatchIds(normalizedFileIds);
        Map<Long, SysFile> result = new LinkedHashMap<>();
        for (SysFile sysFile : fileList) {
            if (sysFile != null) {
                // 调用getId方法，复用统一能力并保证业务规则一致。
                result.put(sysFile.getId(), sysFile);
            }
        }
        return result;
    }

    /**
     * requireActive文件。
     *
     * @return 处理结果
     */
    private SysFile requireActiveFile(Long fileId) {
        if (fileId == null) {
            throw new ServiceException("文件ID不能为空");
        }
        // 说明：执行该步骤以保证业务流程正确。
        SysFile sysFile = sysFileMapper.selectById(fileId);
        if (sysFile == null || sysFile.getStatus() != SysFileStatusEnum.ACTIVE) {
            throw new ServiceException("文件不存在或已失效");
        }
        return sysFile;
    }

    /**
     * 校验上传文件。
     *
     * @param file 参数
     */
    private void validateUploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("上传文件不能为空");
        }
        // 调用getOriginalFilename方法，复用统一能力并保证业务规则一致。
        String fileExt = resolveFileExtension(file.getOriginalFilename());
        // 调用resolveMediaType方法，复用统一能力并保证业务规则一致。
        resolveMediaType(fileExt);
    }

    /**
     * 校验文件Size。
     *
     * @param fileSize 参数
     * @param mediaType 参数
     */
    private void validateFileSize(long fileSize, SysFileMediaTypeEnum mediaType) {
        if (fileSize <= 0) {
            throw new ServiceException("上传文件不能为空");
        }
        long maxSize;
        String message;
        if (mediaType == SysFileMediaTypeEnum.IMAGE) {
            maxSize = IMAGE_MAX_SIZE;
            message = "图片大小不能超过10MB";
        } else if (mediaType == SysFileMediaTypeEnum.VIDEO) {
            maxSize = VIDEO_MAX_SIZE;
            message = "视频大小不能超过50MB";
        } else {
            maxSize = VOICE_MAX_SIZE;
            message = "语音大小不能超过10MB";
        }
        if (fileSize > maxSize) {
            throw new ServiceException(message);
        }
    }

    /**
     * 解析Media类型。
     *
     * @param fileExt 参数
     * @return 处理结果
     */
    private SysFileMediaTypeEnum resolveMediaType(String fileExt) {
        if (IMAGE_EXTENSIONS.contains(fileExt)) {
            return SysFileMediaTypeEnum.IMAGE;
        }
        if (VIDEO_EXTENSIONS.contains(fileExt)) {
            return SysFileMediaTypeEnum.VIDEO;
        }
        if (VOICE_EXTENSIONS.contains(fileExt)) {
            return SysFileMediaTypeEnum.VOICE;
        }
        throw new ServiceException("不支持的文件类型");
    }

    /**
     * 解析文件Extension。
     *
     * @param originalName 参数
     * @return 处理结果
     */
    private String resolveFileExtension(String originalName) {
        // 调用trimToNull方法，复用统一能力并保证业务规则一致。
        String normalized = StrUtil.trimToNull(originalName);
        if (normalized == null) {
            throw new ServiceException("文件名不能为空");
        }
        // 调用lastIndexOf方法，复用统一能力并保证业务规则一致。
        int index = normalized.lastIndexOf('.');
        if (index < 0 || index == normalized.length() - 1) {
            throw new ServiceException("文件扩展名不能为空");
        }
        return normalized.substring(index + 1).toLowerCase();
    }

    /**
     * 构建对象Key。
     *
     * @param bizDir 参数
     * @param fileExt 参数
     * @return 处理结果
     */
    private String buildObjectKey(String bizDir, String fileExt) {
        // 调用normalizeDir方法，复用统一能力并保证业务规则一致。
        String normalizedDir = normalizeDir(bizDir);
        return normalizedDir + "/" + LocalDate.now().format(MONTH_FORMATTER) + "/"
                // 调用fastSimpleUUID方法，复用统一能力并保证业务规则一致。
                + IdUtil.fastSimpleUUID() + "." + fileExt;
    }

    /**
     * 规范化Dir。
     *
     * @param bizDir 参数
     * @return 处理结果
     */
    private String normalizeDir(String bizDir) {
        // 调用trimToNull方法，复用统一能力并保证业务规则一致。
        String normalized = StrUtil.blankToDefault(StrUtil.trimToNull(bizDir), "misc");
        // 调用replace方法，复用统一能力并保证业务规则一致。
        normalized = normalized.replace("\\", "/");
        // 调用replaceAll方法，复用统一能力并保证业务规则一致。
        normalized = normalized.replaceAll("/+", "/");
        // 调用replaceAll方法，复用统一能力并保证业务规则一致。
        normalized = normalized.replaceAll("^/+", "");
        // 调用replaceAll方法，复用统一能力并保证业务规则一致。
        normalized = normalized.replaceAll("/+$", "");
        return normalized;
    }

    /**
     * 规范化Original名称。
     *
     * @param originalName 参数
     * @param fileExt 参数
     * @return 处理结果
     */
    private String normalizeOriginalName(String originalName, String fileExt) {
        // 调用trimToNull方法，复用统一能力并保证业务规则一致。
        String normalized = StrUtil.trimToNull(originalName);
        return normalized == null ? "file." + fileExt : normalized;
    }

    /**
     * 解析Content类型。
     *
     * @param contentType 参数
     * @return 处理结果
     */
    private String resolveContentType(String contentType) {
        return StrUtil.blankToDefault(StrUtil.trimToNull(contentType), "application/octet-stream");
    }

    /**
     * generate预览Url。
     *
     * @param sysFile 参数
     * @return 处理结果
     */
    private String generatePreviewUrl(SysFile sysFile) {
        return objectStorageService.generatePresignedPreviewUrl(sysFile.getObjectKey(), defaultPreviewExpireSeconds());
    }

    /**
     * default预览ExpireSeconds。
     *
     * @return 处理结果
     */
    private long defaultPreviewExpireSeconds() {
        // 调用getPreviewExpireSeconds方法，复用统一能力并保证业务规则一致。
        Long previewExpireSeconds = ossProperties.getPreviewExpireSeconds();
        return previewExpireSeconds == null || previewExpireSeconds <= 0 ? 1800L : previewExpireSeconds;
    }

    /**
     * 校验业务Target。
     *
     * @param bizType 参数
     */
    private void validateBizTarget(SysFileBizTypeEnum bizType, Long bizId) {
        if (bizType == null) {
            throw new ServiceException("业务类型不能为空");
        }
        if (bizId == null) {
            throw new ServiceException("业务ID不能为空");
        }
    }

    /**
     * 规范化文件Ids。
     *
     * @return 处理结果
     */
    private List<Long> normalizeFileIds(List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<Long> result = new LinkedHashSet<>();
        for (Long fileId : fileIds) {
            if (fileId != null) {
                // 调用add方法，复用统一能力并保证业务规则一致。
                result.add(fileId);
            }
        }
        return new ArrayList<>(result);
    }

    /**
     * 规范化NullableText。
     *
     * @param value 参数
     * @return 处理结果
     */
    private String normalizeNullableText(String value) {
        return StrUtil.trimToNull(value);
    }
}




