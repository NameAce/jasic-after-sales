package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 总部-一级签约新增/修改参数
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Data
public class HqFirstContractDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键（修改时必传） */
    private Long id;

    /** 总部公司ID */
    private Long hqCompanyId;

    /** 一级网点公司ID */
    private Long firstCompanyId;

    /** 大区ID */
    private Long regionId;

    /** 合同编号 */
    private String contractNo;

    /** 状态（1=有效，0=终止） */
    private Integer status;

    /** 备注 */
    private String remark;
}
