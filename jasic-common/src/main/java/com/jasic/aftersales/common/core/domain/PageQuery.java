package com.jasic.aftersales.common.core.domain;

import lombok.Data;

import java.io.Serializable;

/**
 * 分页查询参数基类
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Data
public class PageQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前页码，默认1 */
    private Integer pageNum = 1;

    /** 每页数量，默认10 */
    private Integer pageSize = 10;

    /** 排序字段 */
    private String orderByColumn;

    /** 排序方向（asc/desc） */
    private String isAsc = "desc";
}
