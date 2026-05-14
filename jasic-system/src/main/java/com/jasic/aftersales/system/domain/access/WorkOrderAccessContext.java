package com.jasic.aftersales.system.domain.access;

import com.jasic.aftersales.common.enums.DataScopeEnum;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 工单领域访问上下文。
 *
 * @author Codex
 * @date 2026/05/05
 */
@Data
public class WorkOrderAccessContext implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long currentUserId;

    private Long currentCompanyId;

    private String subjectType;

    private String typeCode;

    private DataScopeEnum dataScopeEnum;

    private String dataScope;

    private List<Long> currentRegionIds = Collections.emptyList();

    private List<Long> relatedCompanyIds = Collections.emptyList();

    private boolean platformUser;
}
