package com.jasic.aftersales.system.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * CRM 签约导入结果 VO。
 *
 * @author Zoro
 * @date 2026/04/12
 */
@ApiModel(description = "CRM 签约导入结果 VO")
@Data
public class CrmHqFirstContractImportResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 选中总数 */
    @ApiModelProperty(value = "选中总数")
    private Integer selectedCount;

    /** 成功导入条数 */
    @ApiModelProperty(value = "成功导入条数")
    private Integer successCount;

    /** 已存在跳过条数 */
    @ApiModelProperty(value = "已存在跳过条数")
    private Integer existedCount;

    /** 映射失败条数 */
    @ApiModelProperty(value = "映射失败条数")
    private Integer failedCount;
}
