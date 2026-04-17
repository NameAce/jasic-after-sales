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

    @TableId(type = IdType.AUTO)
    private Long id;

    private String companyName;

    private String companyShortName;

    private String companyCode;

    private String typeCode;

    private String contactName;

    private String contactPhone;

    private String address;

    private String provinceName;

    private String cityName;

    private String districtName;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private String servicePhone;

    private String sourceType;

    private String salesOrg;

    private Integer status;

    private String remark;
}
