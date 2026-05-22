package com.jasic.aftersales.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jasic.aftersales.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * CRM 公司快照实体
 *
 * @author Zoro
 * @date 2026/04/12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_biz_company_snapshot")
public class CrmBizCompanySnapshot extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**主键ID，对应数据库中的同名或映射字段。*/
    @TableId(type = IdType.AUTO)
    private Long id;

    /**custId 字段，对应数据库中的同名或映射字段。*/
    private Long custId;

    /**custName 字段，对应数据库中的同名或映射字段。*/
    private String custName;

    /**juristicCustId 字段，对应数据库中的同名或映射字段。*/
    private String juristicCustId;

    /**groupContactPhone 字段，对应数据库中的同名或映射字段。*/
    private String groupContactPhone;

    /**cellphone 字段，对应数据库中的同名或映射字段。*/
    private String cellphone;

    /**companyAddress 字段，对应数据库中的同名或映射字段。*/
    private String companyAddress;

    /**sapCompanyCode 字段，对应数据库中的同名或映射字段。*/
    private String sapCompanyCode;

    /**custRage 字段，对应数据库中的同名或映射字段。*/
    private Integer custRage;

    /**companyShortName 字段，对应数据库中的同名或映射字段。*/
    private String companyShortName;

    /**provinceName 字段，对应数据库中的同名或映射字段。*/
    private String provinceName;

    /**cityName 字段，对应数据库中的同名或映射字段。*/
    private String cityName;

    /**districtName 字段，对应数据库中的同名或映射字段。*/
    private String districtName;

    /**custState 字段，对应数据库中的同名或映射字段。*/
    private Integer custState;

    /**addDate 字段，对应数据库中的同名或映射字段。*/
    private LocalDateTime addDate;

    /**operTime 字段，对应数据库中的同名或映射字段。*/
    private LocalDateTime operTime;

    /**lastSyncTime 字段，对应数据库中的同名或映射字段。*/
    private LocalDateTime lastSyncTime;
}
