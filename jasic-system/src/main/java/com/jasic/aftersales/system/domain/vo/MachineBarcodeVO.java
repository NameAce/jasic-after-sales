package com.jasic.aftersales.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 条码档案返回对象
 *
 * @author Codex
 * @date 2026/04/01
 */
@Data
public class MachineBarcodeVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 机器条码 */
    private String barcode;

    /** 归属总部ID */
    private Long hqCompanyId;

    /** 归属总部名称 */
    private String hqCompanyName;

    /** CRM 公司ID */
    private String custId;

    /** 销售组织 */
    private String salesOrg;

    /** 物料编码 */
    private String productCode;

    /** 商品名称 */
    private String productName;

    /** 机器小号 */
    private String productTrumpet;

    /** 产品型号 */
    private String productModel;

    /** 机器小号 */
    private String machineNo;

    /** 品牌编码 */
    private String brandCode;

    /** 厂家最后出库日期 */
    private LocalDateTime scanDate;

    /** 经销商最新出库日期 */
    private LocalDateTime dealerOutDate;

    /** CRM 创建时间 */
    private LocalDateTime crmAddTime;

    /** 最近同步时间 */
    private LocalDateTime lastSyncTime;

    /** 质保状态 */
    private String warrantyStatus;

    /** 状态 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
