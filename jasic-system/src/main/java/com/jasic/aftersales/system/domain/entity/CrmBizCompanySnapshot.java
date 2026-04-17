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
 * @author Codex
 * @date 2026/04/12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_biz_company_snapshot")
public class CrmBizCompanySnapshot extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long custId;

    private String custName;

    private String juristicCustId;

    private String groupContactPhone;

    private String cellphone;

    private String companyAddress;

    private String sapCompanyCode;

    private Integer custRage;

    private String companyShortName;

    private String provinceName;

    private String cityName;

    private String districtName;

    private Integer custState;

    private LocalDateTime addDate;

    private LocalDateTime operTime;

    private LocalDateTime lastSyncTime;
}
