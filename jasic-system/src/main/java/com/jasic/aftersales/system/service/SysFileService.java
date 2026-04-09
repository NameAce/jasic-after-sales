package com.jasic.aftersales.system.service;

import com.jasic.aftersales.common.enums.SysFileBizTypeEnum;
import com.jasic.aftersales.common.enums.SysFileUploadUserTypeEnum;
import com.jasic.aftersales.system.domain.vo.SysFileItemVO;
import com.jasic.aftersales.system.domain.vo.SysFilePreviewVO;
import com.jasic.aftersales.system.domain.vo.SysFileUploadVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 文件中台 Service
 *
 * @author Codex
 * @date 2026/04/07
 */
public interface SysFileService {

    /**
     * 上传文件
     *
     * @param file            文件
     * @param bizDir          业务目录
     * @param uploadUserId    上传用户ID
     * @param uploadUserType  上传用户类型
     * @param uploadCompanyId 上传公司ID
     * @return 上传结果
     */
    SysFileUploadVO upload(MultipartFile file, String bizDir, Long uploadUserId,
                           SysFileUploadUserTypeEnum uploadUserType, Long uploadCompanyId);

    /**
     * 生成预览地址
     *
     * @param fileId 文件ID
     * @return 预览地址
     */
    SysFilePreviewVO getPreviewUrl(Long fileId);

    /**
     * 按业务整组替换附件
     *
     * @param bizType          业务类型
     * @param bizId            业务ID
     * @param fileIds          文件ID列表
     * @param companyId        公司ID
     * @param operatorUserId   操作人ID
     * @param operatorUserType 操作人类型
     * @param remark           备注
     */
    void replaceBizFiles(SysFileBizTypeEnum bizType, Long bizId, List<Long> fileIds, Long companyId,
                         Long operatorUserId, SysFileUploadUserTypeEnum operatorUserType, String remark);

    /**
     * 解绑单个业务文件
     *
     * @param bizType 业务类型
     * @param bizId   业务ID
     * @param fileId  文件ID
     */
    void unbindBizFile(SysFileBizTypeEnum bizType, Long bizId, Long fileId);

    /**
     * 查询业务文件列表
     *
     * @param bizType 业务类型
     * @param bizId   业务ID
     * @return 文件列表
     */
    List<SysFileItemVO> listBizFiles(SysFileBizTypeEnum bizType, Long bizId);

    /**
     * 批量查询业务文件列表
     *
     * @param bizTypes 业务类型列表
     * @param bizId    业务ID
     * @return 业务文件映射
     */
    Map<SysFileBizTypeEnum, List<SysFileItemVO>> listBizFileMap(List<SysFileBizTypeEnum> bizTypes, Long bizId);
}
