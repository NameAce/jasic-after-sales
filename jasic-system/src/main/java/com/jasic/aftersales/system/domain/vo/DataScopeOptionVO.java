package com.jasic.aftersales.system.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 数据范围选项
 *
 * @author Zoro
 * @date 2026/03/25
 */
@Data
public class DataScopeOptionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 选项值 */
    private String value;

    /** 展示文案 */
    private String label;

    /** 是否默认选项 */
    private Boolean defaultOption;
}
