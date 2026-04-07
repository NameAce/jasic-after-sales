package com.jasic.aftersales.customer.domain.dto;

import lombok.Data;

import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * C端客户资料修改参数
 *
 * @author Codex
 * @date 2026/04/06
 */
@Data
public class CustomerProfileUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 昵称 */
    @Size(max = 64, message = "昵称长度不能超过64个字符")
    private String nickname;

    /** 头像URL */
    @Size(max = 256, message = "头像URL长度不能超过256个字符")
    private String avatar;
}
