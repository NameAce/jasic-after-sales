package com.jasic.aftersales.system.domain.access;

import com.jasic.aftersales.common.enums.DataScopeEnum;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 工单领域访问上下文。
 *
 * @author Zoro
 * @date 2026/05/05
 */
@Data
public class WorkOrderAccessContext implements Serializable {

    private static final long serialVersionUID = 1L;

    /**currentUserId 字段，用于当前类内部业务处理。*/
    private Long currentUserId;

    /**currentCompanyId 字段，用于当前类内部业务处理。*/
    private Long currentCompanyId;

    /**subjectType 字段，用于当前类内部业务处理。*/
    private String subjectType;

    /**typeCode 字段，用于当前类内部业务处理。*/
    private String typeCode;

    /**dataScopeEnum 字段，用于当前类内部业务处理。*/
    private DataScopeEnum dataScopeEnum;

    /**dataScope 字段，用于当前类内部业务处理。*/
    private String dataScope;

    /**currentRegionIds 字段，用于当前类内部业务处理。*/
    private List<Long> currentRegionIds = Collections.emptyList();

    /**relatedCompanyIds 字段，用于当前类内部业务处理。*/
    private List<Long> relatedCompanyIds = Collections.emptyList();

    /**platformUser 字段，用于当前类内部业务处理。*/
    private boolean platformUser;
}
