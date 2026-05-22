package com.jasic.aftersales.system.domain.dto;

import com.jasic.aftersales.common.enums.SysFileBizTypeEnum;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 文件业务解绑参数
 *
 * @author Zoro
 * @date 2026/04/07
 */
@ApiModel(description = "文件业务解绑参数")
@Data
public class SysFileBizUnbindDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 业务类型 */
    @ApiModelProperty(value = "业务类型", required = true)
    @NotNull(message = "业务类型不能为空")
    private SysFileBizTypeEnum bizType;

    /** 业务ID */
    @ApiModelProperty(value = "业务ID", required = true)
    @NotNull(message = "业务ID不能为空")
    private Long bizId;

    /** 文件ID */
    @ApiModelProperty(value = "文件ID", required = true)
    @NotNull(message = "文件ID不能为空")
    private Long fileId;
}
