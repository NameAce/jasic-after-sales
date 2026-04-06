package com.jasic.aftersales.customer.domain.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * C端附近服务网点选项
 *
 * @author Codex
 * @date 2026/04/06
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CustomerNearbyServiceCompanyVO extends CustomerServiceCompanyOptionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 经度 */
    private BigDecimal longitude;

    /** 纬度 */
    private BigDecimal latitude;

    /** 距离（公里，保留两位小数） */
    private BigDecimal distanceKm;
}
