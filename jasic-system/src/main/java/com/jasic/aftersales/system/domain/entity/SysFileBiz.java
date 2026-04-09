package com.jasic.aftersales.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jasic.aftersales.common.core.domain.BaseEntity;
import com.jasic.aftersales.common.enums.SysFileBizTypeEnum;
import com.jasic.aftersales.common.enums.SysFileUploadUserTypeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文件业务关系实体
 *
 * @author Codex
 * @date 2026/04/07
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_file_biz")
public class SysFileBiz extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 文件ID */
    private Long fileId;

    /** 业务类型 */
    private SysFileBizTypeEnum bizType;

    /** 业务ID */
    private Long bizId;

    /** 排序号 */
    private Integer sortNum;

    /** 是否主文件 */
    private Integer isPrimary;

    /** 公司ID */
    private Long companyId;

    /** 操作人ID */
    private Long operatorUserId;

    /** 操作人类型 */
    private SysFileUploadUserTypeEnum operatorUserType;

    /** 备注 */
    private String remark;
}
