package com.jasic.aftersales.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jasic.aftersales.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 公司地址簿实体。
 *
 * @author Codex
 * @date 2026/04/11
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("company_address")
public class CompanyAddress extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 公司ID */
    private Long companyId;

    /** 联系人 */
    private String contactName;

    /** 联系电话 */
    private String contactPhone;

    /** 详细地址 */
    private String address;

    /** 是否默认地址（1=是，0=否） */
    private Integer isDefault;
}
