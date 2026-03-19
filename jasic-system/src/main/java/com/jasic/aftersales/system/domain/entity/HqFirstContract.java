package com.jasic.aftersales.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jasic.aftersales.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 总部-一级签约关系实体
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("hq_first_contract")
public class HqFirstContract extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 总部公司ID */
    private Long hqCompanyId;

    /** 一级网点公司ID */
    private Long firstCompanyId;

    /** 大区ID（签约时绑定） */
    private Long regionId;

    /** 合同编号 */
    private String contractNo;

    /** 状态（1=有效，0=终止） */
    private Integer status;

    /** 备注 */
    private String remark;
}
