package com.jasic.aftersales.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jasic.aftersales.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 条码档案实体
 *
 * @author Codex
 * @date 2026/04/01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("machine_barcode")
public class MachineBarcode extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String barcode;

    private String deliverNumber;

    private Long hqCompanyId;

    private String custId;

    private String salesOrg;

    private String productCode;

    private String productName;

    private String productModel;

    private String machineNo;

    private String brandCode;

    private LocalDateTime scanDate;

    /**
     * 对外展示仍叫“最后出库日期”，
     * 但为规避重复扫码出库，同步时按同条码首次有效扫码时间回写。
     */
    private LocalDateTime lastOutDate;

    private LocalDateTime crmAddTime;

    private LocalDateTime lastSyncTime;

    private Integer status;

    private String remark;
}
