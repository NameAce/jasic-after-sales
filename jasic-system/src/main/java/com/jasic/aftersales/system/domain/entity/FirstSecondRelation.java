package com.jasic.aftersales.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jasic.aftersales.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 一级-二级从属关系实体
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("first_second_relation")
public class FirstSecondRelation extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 一级网点公司ID */
    private Long firstCompanyId;

    /** 二级网点公司ID */
    private Long secondCompanyId;

    /** 状态（1=有效，0=解除） */
    private Integer status;

    /** 备注 */
    private String remark;
}
