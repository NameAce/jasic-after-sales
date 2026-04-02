package com.jasic.aftersales.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jasic.aftersales.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 一级-二级从属关系记录实体
 *
 * @author Codex
 * @date 2026/04/02
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("first_second_relation_record")
public class FirstSecondRelationRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 原从属关系ID */
    private Long sourceId;

    /** 一级网点公司ID */
    private Long firstCompanyId;

    /** 二级网点公司ID */
    private Long secondCompanyId;

    /** 原关系状态 */
    private Integer status;

    /** 原关系备注 */
    private String remark;

    /** 操作类型 */
    private String operationType;

    /** 操作人ID */
    private Long operatorUserId;

    /** 操作公司ID */
    private Long operatorCompanyId;
}
