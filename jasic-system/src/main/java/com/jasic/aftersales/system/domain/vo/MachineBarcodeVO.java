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

    /** 物料编码 */
    private String productCode;

    /** 产品型号 */
    private String productModel;

    /** 品牌编码 */
    private String brandCode;

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
