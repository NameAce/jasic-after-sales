package com.jasic.aftersales.system.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 公司简要信息VO（用于下拉选择等场景）
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Data
public class SysCompanySimpleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 公司ID */
    private Long id;

    /** 公司名称 */
    private String companyName;

    /** 公司编码 */
    private String companyCode;

    /** 公司类型编码 */
    private String typeCode;

    /** 公司类型名称 */
    private String typeName;
}
