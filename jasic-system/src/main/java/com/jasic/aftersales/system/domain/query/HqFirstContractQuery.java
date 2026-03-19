package com.jasic.aftersales.system.domain.query;

import com.jasic.aftersales.common.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 总部-一级签约查询参数
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class HqFirstContractQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 总部公司ID */
    private Long hqCompanyId;

    /** 一级网点公司ID */
    private Long firstCompanyId;

    /** 大区ID */
    private Long regionId;
}
