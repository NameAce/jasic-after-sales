package com.jasic.aftersales.customer.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * C端工单状态计数
 *
 * @author Codex
 * @date 2026/03/26
 */
@Data
public class CustomerWorkOrderStatusCountVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long allCount;

    private Long waitAcceptCount;

    private Long inProgressCount;

    private Long completedCount;

    private Long closedCount;
}
