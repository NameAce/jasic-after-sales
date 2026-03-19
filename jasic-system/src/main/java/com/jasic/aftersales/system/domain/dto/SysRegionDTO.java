package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 大区新增/修改参数
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Data
public class SysRegionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键（修改时必传） */
    private Long id;

    /** 所属总部公司ID */
    private Long companyId;

    /** 大区名称 */
    private String regionName;

    /** 大区编码 */
    private String regionCode;

    /** 备注 */
    private String remark;
}
