package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 一级-二级从属关系新增/修改参数
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Data
public class FirstSecondRelationDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键（修改时必传） */
    private Long id;

    /** 一级网点公司ID */
    private Long firstCompanyId;

    /** 二级网点公司ID */
    private Long secondCompanyId;

    /** 状态（1=有效，0=解除） */
    private Integer status;

    /** 备注 */
    private String remark;
}
