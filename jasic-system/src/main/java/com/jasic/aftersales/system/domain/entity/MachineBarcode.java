package com.jasic.aftersales.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jasic.aftersales.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 机器条码档案实体
 *
 * @author Codex
 * @date 2026/04/01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("machine_barcode")
public class MachineBarcode extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 机器条码 */
    private String barcode;

    /** 归属总部ID */
    private Long hqCompanyId;

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
}
