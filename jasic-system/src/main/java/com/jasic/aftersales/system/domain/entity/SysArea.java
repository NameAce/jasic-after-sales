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
 * @author Zoro
 * @date 2026/04/17
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_area")
public class SysArea extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**areaCode 字段，对应数据库中的同名或映射字段。*/
    @TableId(value = "area_code", type = IdType.INPUT)
    private String areaCode;

    /**areaName 字段，对应数据库中的同名或映射字段。*/
    private String areaName;

    /**parentCode 字段，对应数据库中的同名或映射字段。*/
    private String parentCode;

    /**areaLevel 字段，对应数据库中的同名或映射字段。*/
    private String areaLevel;

    /**fullName 字段，对应数据库中的同名或映射字段。*/
    private String fullName;

    /**sortNum 字段，对应数据库中的同名或映射字段。*/
    private Integer sortNum;

    /**状态，对应数据库中的同名或映射字段。*/
    private Integer status;
}
