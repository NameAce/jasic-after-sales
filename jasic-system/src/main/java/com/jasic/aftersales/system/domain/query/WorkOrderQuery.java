package com.jasic.aftersales.system.domain.query;

import com.jasic.aftersales.common.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 工单查询参数
 *
 * @author Codex
 * @date 2026/03/26
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WorkOrderQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 查看范围（CURRENT/HISTORY/ALL） */
    private String viewScope;

    /** 当前公司ID */
    private Long companyId;

    /** 当前用户ID（服务端注入） */
    private Long currentUserId;

    /** 当前主体类型（服务端注入） */
    private String subjectType;

    /** 当前有效数据范围（服务端注入） */
    private String dataScope;

    /** 关联服务公司范围（服务端注入） */
    private List<Long> relatedCompanyIds;

    /** 工单号（模糊） */
    private String orderNo;

    /** 客户姓名（模糊） */
    private String customerName;

    /** 客户手机号 */
    private String customerMobile;

    /** 条码（模糊） */
    private String barcode;

    /** 主状态 */
    private String mainStatus;

    /** 是否转单 */
    private Integer hasTransfer;
}
