package com.jasic.aftersales.customer.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jasic.aftersales.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * C端客户地址实体
 *
 * @author Zoro
 * @date 2026/04/08
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer_address")
public class CustomerAddress extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 客户ID */
    private Long customerId;

    /** 联系人 */
    private String contactName;

    /** 联系手机号 */
    private String contactMobile;

    /** 省 */
    private String province;

    /** 市 */
    private String city;

    /** 区县 */
    private String county;

    /** 详细地址 */
    private String detailAddress;

    /** 是否默认地址（1=是，0=否） */
    private Integer isDefault;
}
