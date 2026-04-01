package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 条码档案新增/修改参数
 *
 * @author Codex
 * @date 2026/04/01
 */
@Data
public class MachineBarcodeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 机器条码 */
    @NotBlank(message = "机器条码不能为空")
    private String barcode;

    /** 归属总部ID */
    @NotNull(message = "归属总部不能为空")
    private Long hqCompanyId;

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
}
