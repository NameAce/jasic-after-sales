package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 工单故障点参数
 *
 * @author Codex
 * @date 2026/03/26
 */
@Data
public class WorkOrderFaultItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 故障描述 */
    @NotBlank(message = "故障描述不能为空")
    private String faultDesc;

    /** 维修说明 */
    private String repairDesc;

    /** 配件信息 */
    private String partDesc;

    /** 图片地址集合 */
    private String imageUrls;
}
