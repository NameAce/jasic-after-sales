package com.jasic.aftersales.system.domain.query;

import com.jasic.aftersales.system.domain.access.WorkOrderAccessContext;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工单内部查询参数。
 *
 * <p>前端筛选字段来自 {@link WorkOrderQuery}，权限上下文仅由服务端解析后写入。</p>
 *
 * @author Codex
 * @date 2026/05/05
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WorkOrderScopedQuery extends WorkOrderQuery {

    private static final long serialVersionUID = 1L;

    private WorkOrderAccessContext accessContext;
}
