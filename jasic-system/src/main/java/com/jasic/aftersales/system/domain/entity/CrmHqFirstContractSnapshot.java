package com.jasic.aftersales.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jasic.aftersales.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * CRM 总部-一级签约快照实体。
 *
 * <p>该表只沉淀 CRM 原始签约事实，供签约页“从 CRM 导入”使用，
 * 不直接替代售后正式签约关系表。</p>
 *
 * @author Zoro
 * @date 2026/04/12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_hq_first_contract_snapshot")
public class CrmHqFirstContractSnapshot extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** CRM 客户编码（一级公司编码来源） */
    private String kunnr;

    /** CRM 企业标识 */
    private Long custId;

    /** CRM 企业名称 */
    private String crmCompanyName;

    /** 销售组织 */
    private String salesOrg;

    /** CRM 大区编码 */
    private String regionCode;

    /** CRM 大区名称 */
    private String regionName;

    /** CRM 有效标识 */
    private Integer aliveFlag;

    /** CRM 新增时间 */
    private LocalDateTime crmAddTime;

    /** CRM 修改时间 */
    private LocalDateTime crmOperTime;

    /** 最近同步时间 */
    private LocalDateTime lastSyncTime;
}
