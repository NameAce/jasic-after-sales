package com.jasic.aftersales.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jasic.aftersales.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 公司实体
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_company")
public class SysCompany extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**主键ID，对应数据库中的同名或映射字段。*/
    @TableId(type = IdType.AUTO)
    private Long id;

    /**companyName 字段，对应数据库中的同名或映射字段。*/
    private String companyName;

    /**companyShortName 字段，对应数据库中的同名或映射字段。*/
    private String companyShortName;

    /**companyCode 字段，对应数据库中的同名或映射字段。*/
    private String companyCode;

    /**typeCode 字段，对应数据库中的同名或映射字段。*/
    private String typeCode;

    /**contactName 字段，对应数据库中的同名或映射字段。*/
    private String contactName;

    /**contactPhone 字段，对应数据库中的同名或映射字段。*/
    private String contactPhone;

    /**provinceCode 字段，对应数据库中的同名或映射字段。*/
    private String provinceCode;

    /**provinceName 字段，对应数据库中的同名或映射字段。*/
    private String provinceName;

    /**cityCode 字段，对应数据库中的同名或映射字段。*/
    private String cityCode;

    /**cityName 字段，对应数据库中的同名或映射字段。*/
    private String cityName;

    /**districtCode 字段，对应数据库中的同名或映射字段。*/
    private String districtCode;

    /**districtName 字段，对应数据库中的同名或映射字段。*/
    private String districtName;

    /**detailAddress 字段，对应数据库中的同名或映射字段。*/
    private String detailAddress;

    /**fullAddress 字段，对应数据库中的同名或映射字段。*/
    private String fullAddress;

    /**geocodeStatus 字段，对应数据库中的同名或映射字段。*/
    private String geocodeStatus;

    /**longitude 字段，对应数据库中的同名或映射字段。*/
    private BigDecimal longitude;

    /**latitude 字段，对应数据库中的同名或映射字段。*/
    private BigDecimal latitude;

    /**servicePhone 字段，对应数据库中的同名或映射字段。*/
    private String servicePhone;

    /**sourceType 字段，对应数据库中的同名或映射字段。*/
    private String sourceType;

    /**salesOrg 字段，对应数据库中的同名或映射字段。*/
    private String salesOrg;

    /**状态，对应数据库中的同名或映射字段。*/
    private Integer status;

    /**备注，对应数据库中的同名或映射字段。*/
    private String remark;
}
