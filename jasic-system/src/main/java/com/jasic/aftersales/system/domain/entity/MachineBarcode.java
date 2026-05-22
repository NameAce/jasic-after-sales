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
 * @author Zoro
 * @date 2026/04/01
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("machine_barcode")
public class MachineBarcode extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**主键ID，对应数据库中的同名或映射字段。*/
    @TableId(type = IdType.AUTO)
    private Long id;

    /**barcode 字段，对应数据库中的同名或映射字段。*/
    private String barcode;

    /**deliverNumber 字段，对应数据库中的同名或映射字段。*/
    private String deliverNumber;

    /**hqCompanyId 字段，对应数据库中的同名或映射字段。*/
    private Long hqCompanyId;

    /**custId 字段，对应数据库中的同名或映射字段。*/
    private String custId;

    /**salesOrg 字段，对应数据库中的同名或映射字段。*/
    private String salesOrg;

    /**productCode 字段，对应数据库中的同名或映射字段。*/
    private String productCode;

    /**productName 字段，对应数据库中的同名或映射字段。*/
    private String productName;

    /**productModel 字段，对应数据库中的同名或映射字段。*/
    private String productModel;

    /**machineNo 字段，对应数据库中的同名或映射字段。*/
    private String machineNo;

    /**brandCode 字段，对应数据库中的同名或映射字段。*/
    private String brandCode;

    /**scanDate 字段，对应数据库中的同名或映射字段。*/
    private LocalDateTime scanDate;

    /**
     * 对外展示仍叫“最后出库日期”，
     * 但为规避重复扫码出库，同步时按同条码首次有效扫码时间回写。
     */
    private LocalDateTime lastOutDate;

    /**crmAddTime 字段，对应数据库中的同名或映射字段。*/
    private LocalDateTime crmAddTime;

    /**lastSyncTime 字段，对应数据库中的同名或映射字段。*/
    private LocalDateTime lastSyncTime;

    /**状态，对应数据库中的同名或映射字段。*/
    private Integer status;

    /**备注，对应数据库中的同名或映射字段。*/
    private String remark;
}
