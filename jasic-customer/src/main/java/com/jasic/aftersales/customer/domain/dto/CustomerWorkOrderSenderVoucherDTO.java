package com.jasic.aftersales.customer.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * C端工单寄件凭证参数
 *
 * @author Zoro
 * @date 2026/04/08
 */
@ApiModel(description = "C端工单寄件凭证参数")
@Data
public class CustomerWorkOrderSenderVoucherDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工单ID */
    @ApiModelProperty(value = "工单ID", required = true)
    @NotNull(message = "工单ID不能为空")
    private Long workOrderId;

    /** 寄件凭证文件ID */
    @ApiModelProperty(value = "寄件凭证文件ID", required = true)
    @NotEmpty(message = "寄件凭证不能为空")
    private List<Long> senderVoucherFileIds;
}


