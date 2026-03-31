package com.jasic.aftersales.system.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 菜单发布公司类型选项
 *
 * @author Zoro
 * @date 2026/03/31
 */
@Data
public class SysMenuPublishTypeOptionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 公司类型编码 */
    private String typeCode;

    /** 公司类型名称 */
    private String typeName;
}
