package com.jasic.aftersales.system.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * CRM 签约批量导入参数。
 *
 * @author Codex
 * @date 2026/04/12
 */
@ApiModel(description = "CRM 签约批量导入参数")
@Data
public class CrmHqFirstContractImportDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前选择的总部公司ID */
    @ApiModelProperty(value = "当前选择的总部公司ID", required = true)
    @NotNull(message = "请选择总部公司")
    private Long hqCompanyId;

    /** 选中的快照ID列表 */
    @ApiModelProperty(value = "选中的快照ID列表", required = true)
    @NotEmpty(message = "请选择需要导入的 CRM 签约数据")
    private List<Long> snapshotIds;
}
