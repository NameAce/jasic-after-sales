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
import com.jasic.aftersales.system.domain.vo.SysFilePreviewVO;
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysFileUploadVO upload(MultipartFile file, String bizDir, Long uploadUserId,
                                  SysFileUploadUserTypeEnum uploadUserType, Long uploadCompanyId) {
        validateUploadFile(file);
        String fileExt = resolveFileExtension(file.getOriginalFilename());
        SysFileMediaTypeEnum mediaType = resolveMediaType(fileExt);
        validateFileSize(file.getSize(), mediaType);
        String fileHash = objectStorageService.calculateSha256(file);
        String objectKey = buildObjectKey(bizDir, fileExt);
        ObjectStorageUploadResult uploadResult = objectStorageService.upload(file, objectKey);

        SysFile entity = new SysFile();
        entity.setStorageType(SysFileStorageTypeEnum.OSS);
        entity.setBucket(uploadResult.getBucket());
        entity.setObjectKey(uploadResult.getObjectKey());
        entity.setOriginalName(normalizeOriginalName(file.getOriginalFilename(), fileExt));
        entity.setContentType(resolveContentType(file.getContentType()));
        entity.setFileSize(file.getSize());
        entity.setFileExt(fileExt);
        entity.setFileHash(fileHash);
        entity.setAccessLevel(SysFileAccessLevelEnum.PRIVATE);
        entity.setUploadUserId(uploadUserId);
        entity.setUploadUserType(uploadUserType);
        entity.setUploadCompanyId(uploadCompanyId);
        entity.setStatus(SysFileStatusEnum.ACTIVE);
        sysFileMapper.insert(entity);

        SysFileUploadVO vo = new SysFileUploadVO();
        vo.setFileId(entity.getId());
        vo.setOriginalName(entity.getOriginalName());
        vo.setContentType(entity.getContentType());
        vo.setFileSize(entity.getFileSize());
        vo.setFileExt(entity.getFileExt());
        vo.setFileHash(entity.getFileHash());
        vo.setPreviewUrl(generatePreviewUrl(entity));
        return vo;
    }

    @Override
    public SysFilePreviewVO getPreviewUrl(Long fileId) {
        SysFile sysFile = requireActiveFile(fileId);
        SysFilePreviewVO vo = new SysFilePreviewVO();
        vo.setFileId(fileId);
        vo.setExpireSeconds(defaultPreviewExpireSeconds());
        vo.setPreviewUrl(generatePreviewUrl(sysFile));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceBizFiles(SysFileBizTypeEnum bizType, Long bizId, List<Long> fileIds, Long companyId,
                                Long operatorUserId, SysFileUploadUserTypeEnum operatorUserType, String remark) {
        validateBizTarget(bizType, bizId);
        LambdaQueryWrapper<SysFileBiz> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(SysFileBiz::getBizType, bizType)
                .eq(SysFileBiz::getBizId, bizId);
        sysFileBizMapper.delete(deleteWrapper);

        List<Long> normalizedFileIds = normalizeFileIds(fileIds);
        if (normalizedFileIds.isEmpty()) {
            return;
        }
        Map<Long, SysFile> fileMap = loadActiveFileMap(normalizedFileIds);
        int sortNum = 1;
        for (Long fileId : normalizedFileIds) {
            SysFile sysFile = fileMap.get(fileId);
            if (sysFile == null) {
                throw new ServiceException("文件不存在或已失效");
            }
            SysFileBiz relation = new SysFileBiz();
            relation.setFileId(fileId);
            relation.setBizType(bizType);
            relation.setBizId(bizId);
            relation.setSortNum(sortNum++);
            relation.setIsPrimary(0);
            relation.setCompanyId(companyId);
            relation.setOperatorUserId(operatorUserId);
            relation.setOperatorUserType(operatorUserType);
            relation.setRemark(normalizeNullableText(remark));
            sysFileBizMapper.insert(relation);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbindBizFile(SysFileBizTypeEnum bizType, Long bizId, Long fileId) {
        validateBizTarget(bizType, bizId);
        if (fileId == null) {
            throw new ServiceException("文件ID不能为空");
        }
        LambdaQueryWrapper<SysFileBiz> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysFileBiz::getBizType, bizType)
                .eq(SysFileBiz::getBizId, bizId)
                .eq(SysFileBiz::getFileId, fileId);
        sysFileBizMapper.delete(wrapper);
    }

    @Override
    public List<SysFileItemVO> listBizFiles(SysFileBizTypeEnum bizType, Long bizId) {
        if (bizType == null || bizId == null) {
            return Collections.emptyList();
        }
        List<SysFileBiz> relations = sysFileBizMapper.selectVisibleBizRelations(bizType, bizId);
        return buildFileItems(relations);
    }

    @Override
    public Map<SysFileBizTypeEnum, List<SysFileItemVO>> listBizFileMap(List<SysFileBizTypeEnum> bizTypes, Long bizId) {
        if (bizTypes == null || bizTypes.isEmpty() || bizId == null) {
            return Collections.emptyMap();
        }
        List<SysFileBizTypeEnum> normalizedBizTypes = bizTypes.stream()
                .filter(item -> item != null)
                .distinct()
                .collect(Collectors.toList());
        if (normalizedBizTypes.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SysFileBiz> relations = sysFileBizMapper.selectVisibleBizRelationsByTypes(normalizedBizTypes, bizId);
        Map<SysFileBizTypeEnum, List<SysFileItemVO>> result = new EnumMap<>(SysFileBizTypeEnum.class);
        for (SysFileBizTypeEnum bizType : normalizedBizTypes) {
            result.put(bizType, new ArrayList<>());
        }
        if (relations.isEmpty()) {
            return result;
        }
        Map<Long, SysFile> fileMap = loadFileMap(relations.stream()
                .map(SysFileBiz::getFileId)
                .collect(Collectors.toList()));
        for (SysFileBiz relation : relations) {
            SysFile sysFile = fileMap.get(relation.getFileId());
            if (sysFile == null || sysFile.getStatus() != SysFileStatusEnum.ACTIVE) {
                continue;
            }
            result.computeIfAbsent(relation.getBizType(), key -> new ArrayList<>())
                    .add(buildFileItem(sysFile, relation));
        }
        return result;
    }

    private List<SysFileItemVO> buildFileItems(List<SysFileBiz> relations) {
        if (relations == null || relations.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, SysFile> fileMap = loadFileMap(relations.stream()
                .map(SysFileBiz::getFileId)
                .collect(Collectors.toList()));
        List<SysFileItemVO> result = new ArrayList<>();
        for (SysFileBiz relation : relations) {
            SysFile sysFile = fileMap.get(relation.getFileId());
            if (sysFile == null || sysFile.getStatus() != SysFileStatusEnum.ACTIVE) {
                continue;
            }
            result.add(buildFileItem(sysFile, relation));
        }
        return result;
    }

    private SysFileItemVO buildFileItem(SysFile sysFile, SysFileBiz relation) {
        SysFileItemVO vo = new SysFileItemVO();
        vo.setFileId(sysFile.getId());
        vo.setBizType(relation.getBizType());
        vo.setBizId(relation.getBizId());
        vo.setOriginalName(sysFile.getOriginalName());
        vo.setContentType(sysFile.getContentType());
        vo.setFileSize(sysFile.getFileSize());
        vo.setFileExt(sysFile.getFileExt());
        vo.setSortNum(relation.getSortNum());
        vo.setIsPrimary(relation.getIsPrimary());
        vo.setPreviewUrl(generatePreviewUrl(sysFile));
        return vo;
    }

    private Map<Long, SysFile> loadActiveFileMap(List<Long> fileIds) {
        Map<Long, SysFile> fileMap = loadFileMap(fileIds);
        for (Long fileId : fileIds) {
            SysFile sysFile = fileMap.get(fileId);
            if (sysFile == null || sysFile.getStatus() != SysFileStatusEnum.ACTIVE) {
                throw new ServiceException("文件不存在或已失效");
            }
        }
        return fileMap;
    }

    private Map<Long, SysFile> loadFileMap(List<Long> fileIds) {
        List<Long> normalizedFileIds = normalizeFileIds(fileIds);
        if (normalizedFileIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SysFile> fileList = sysFileMapper.selectBatchIds(normalizedFileIds);
        Map<Long, SysFile> result = new LinkedHashMap<>();
        for (SysFile sysFile : fileList) {
            if (sysFile != null) {
                result.put(sysFile.getId(), sysFile);
            }
        }
        return result;
    }

    private SysFile requireActiveFile(Long fileId) {
        if (fileId == null) {
            throw new ServiceException("文件ID不能为空");
        }
        SysFile sysFile = sysFileMapper.selectById(fileId);
        if (sysFile == null || sysFile.getStatus() != SysFileStatusEnum.ACTIVE) {
            throw new ServiceException("文件不存在或已失效");
        }
        return sysFile;
    }

    private void validateUploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("上传文件不能为空");
        }
        String fileExt = resolveFileExtension(file.getOriginalFilename());
        resolveMediaType(fileExt);
    }

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

    private String resolveFileExtension(String originalName) {
        String normalized = StrUtil.trimToNull(originalName);
        if (normalized == null) {
            throw new ServiceException("文件名不能为空");
        }
        int index = normalized.lastIndexOf('.');
        if (index < 0 || index == normalized.length() - 1) {
            throw new ServiceException("文件扩展名不能为空");
        }
        return normalized.substring(index + 1).toLowerCase();
    }

    private String buildObjectKey(String bizDir, String fileExt) {
        String normalizedDir = normalizeDir(bizDir);
        return normalizedDir + "/" + LocalDate.now().format(MONTH_FORMATTER) + "/"
                + IdUtil.fastSimpleUUID() + "." + fileExt;
    }

    private String normalizeDir(String bizDir) {
        String normalized = StrUtil.blankToDefault(StrUtil.trimToNull(bizDir), "misc");
        normalized = normalized.replace("\\", "/");
        normalized = normalized.replaceAll("/+", "/");
        normalized = normalized.replaceAll("^/+", "");
        normalized = normalized.replaceAll("/+$", "");
        return normalized;
    }

    private String normalizeOriginalName(String originalName, String fileExt) {
        String normalized = StrUtil.trimToNull(originalName);
        return normalized == null ? "file." + fileExt : normalized;
    }

    private String resolveContentType(String contentType) {
        return StrUtil.blankToDefault(StrUtil.trimToNull(contentType), "application/octet-stream");
    }

    private String generatePreviewUrl(SysFile sysFile) {
        return objectStorageService.generatePresignedPreviewUrl(sysFile.getObjectKey(), defaultPreviewExpireSeconds());
    }

    private long defaultPreviewExpireSeconds() {
        Long previewExpireSeconds = ossProperties.getPreviewExpireSeconds();
        return previewExpireSeconds == null || previewExpireSeconds <= 0 ? 1800L : previewExpireSeconds;
    }

    private void validateBizTarget(SysFileBizTypeEnum bizType, Long bizId) {
        if (bizType == null) {
            throw new ServiceException("业务类型不能为空");
        }
        if (bizId == null) {
            throw new ServiceException("业务ID不能为空");
        }
    }

    private List<Long> normalizeFileIds(List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<Long> result = new LinkedHashSet<>();
        for (Long fileId : fileIds) {
            if (fileId != null) {
                result.add(fileId);
            }
        }
        return new ArrayList<>(result);
    }

    private String normalizeNullableText(String value) {
        return StrUtil.trimToNull(value);
    }
}
