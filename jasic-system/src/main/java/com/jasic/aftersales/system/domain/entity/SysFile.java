package com.jasic.aftersales.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jasic.aftersales.common.core.domain.BaseEntity;
import com.jasic.aftersales.common.enums.SysFileAccessLevelEnum;
import com.jasic.aftersales.common.enums.SysFileStatusEnum;
import com.jasic.aftersales.common.enums.SysFileStorageTypeEnum;
import com.jasic.aftersales.common.enums.SysFileUploadUserTypeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件元数据实体
 *
 * @author Codex
 * @date 2026/04/07
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_file")
public class SysFile extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 存储类型 */
    private SysFileStorageTypeEnum storageType;

    /** 存储桶 */
    private String bucket;

    /** 对象键 */
    private String objectKey;

    /** 原始文件名 */
    private String originalName;

    /** 内容类型 */
    private String contentType;

    /** 文件大小 */
    private Long fileSize;

    /** 扩展名 */
    private String fileExt;

    /** 文件哈希 */
    private String fileHash;

    /** 访问级别 */
    private SysFileAccessLevelEnum accessLevel;

    /** 上传用户ID */
    private Long uploadUserId;

    /** 上传用户类型 */
    private SysFileUploadUserTypeEnum uploadUserType;

    /** 上传公司ID */
    private Long uploadCompanyId;

    /** 文件状态 */
    private SysFileStatusEnum status;
}


