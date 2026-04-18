package com.jasic.aftersales.system.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.io.Serializable;
import java.util.List;

/**
 * CRM 一级二级关系批量导入参数
 *
 * @author Codex
 * @date 2026/04/17
 */
@ApiModel(description = "CRM 一级二级关系批量导入参数")
@Data
public class CrmFirstSecondRelationImportDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 选中的快照 ID 列表 */
    @ApiModelProperty(value = "选中的快照ID列表", required = true)
    @NotEmpty(message = "请选择需要导入的一级二级关系数据")
    private List<Long> snapshotIds;
}
