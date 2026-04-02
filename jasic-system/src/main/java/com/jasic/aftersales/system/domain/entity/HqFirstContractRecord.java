package com.jasic.aftersales.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jasic.aftersales.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 总部-一级签约关系记录实体
 *
 * @author Codex
 * @date 2026/04/02
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hq_first_contract_record")
public class HqFirstContractRecord extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 原签约关系ID */
    private Long sourceId;

    /** 总部公司ID */
    private Long hqCompanyId;

    /** 一级网点公司ID */
    private Long firstCompanyId;

    /** 大区ID */
    private Long regionId;

    /** 合同编号 */
    private String contractNo;

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
