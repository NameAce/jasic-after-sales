package com.jasic.aftersales.system.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 选择/切换公司请求参数
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Data
public class ChooseCompanyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 公司ID */
    @NotNull(message = "公司ID不能为空")
    private Long companyId;
}
