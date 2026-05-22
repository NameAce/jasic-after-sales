package com.jasic.aftersales.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jasic.aftersales.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * CRM 一级二级关系来源快照实体
 *
 * <p>该表仅沉淀来源关系事实，预览导入时再结合 CRM 公司快照和本地公司表动态计算匹配结果。</p>
 *
 * @author Zoro
 * @date 2026/04/17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("crm_first_second_relation_snapshot")
public class CrmFirstSecondRelationSnapshot extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 来源关系主键 */
    private Long sourceId;

    /** 一级 CRM 企业 ID */
    private Long firstCustId;

    /** 二级 CRM 企业 ID */
    private Long secondCustId;

    /** CRM 操作时间 */
    private LocalDateTime crmOperTime;

    /** 最近同步时间 */
    private LocalDateTime lastSyncTime;
}
