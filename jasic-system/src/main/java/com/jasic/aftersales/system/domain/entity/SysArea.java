package com.jasic.aftersales.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jasic.aftersales.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 中国行政区划标准数据
 *
 * @author Codex
 * @date 2026/04/17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_area")
public class SysArea extends BaseEntity {

    private static final long serialVersionUID = 1L;

    @TableId(value = "area_code", type = IdType.INPUT)
    private String areaCode;

    private String areaName;

    private String parentCode;

    private String areaLevel;

    private String fullName;

    private Integer sortNum;

    private Integer status;
}
