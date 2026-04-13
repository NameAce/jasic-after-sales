package com.jasic.aftersales.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jasic.aftersales.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * CRM 公司快照实体。
 *
 * <p>该表用于沉淀外部 {@code biz_company} 原始核心字段，作为本地导入公司的数据来源。
 * 快照层只负责保存外部事实，不直接驱动本地公司主数据变更。</p>
 *
 * @author Codex
 * @date 2026/04/12
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_biz_company_snapshot")
public class CrmBizCompanySnapshot extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** CRM 主键，同时作为本地导入时的客户编码来源。 */
    private Long custId;

    /** CRM 客户名称。 */
    private String custName;

    /** CRM 联系人字段，本期按业务约定映射到本地联系人姓名。 */
    private String juristicCustId;

    /** CRM 联系电话，优先作为本地联系电话来源。 */
    private String groupContactPhone;

    /** CRM 手机号，作为联系电话兜底字段。 */
    private String cellphone;

    /** CRM 公司地址。 */
    private String companyAddress;

    /** CRM 客户状态，快照层全量保留原始状态口径。 */
    private Integer custState;

    /** CRM 新增时间，对应源表 {@code add_date}。 */
    private LocalDateTime addDate;

    /** CRM 修改时间，对应源表 {@code oper_time}。 */
    private LocalDateTime operTime;

    /** 本地最近一次同步该行快照的时间。 */
    private LocalDateTime lastSyncTime;
}
