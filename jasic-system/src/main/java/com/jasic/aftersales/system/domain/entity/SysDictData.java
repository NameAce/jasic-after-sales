package com.jasic.aftersales.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.jasic.aftersales.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典数据实体
 *
 * @author Codex
 * @date 2026/03/19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_data")
public class SysDictData extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 字典类型 */
    private String dictType;

    /** 字典标签 */
    private String dictLabel;

    /** 字典键值 */
    private String dictValue;

    /** 排序 */
    private Integer dictSort;

    /** 自定义样式 */
    private String cssClass;

    /** 标签样式 */
    private String listClass;

    /** 是否默认（1=是，0=否） */
    private Integer isDefault;

    /** 状态（1=启用，0=停用） */
    private Integer status;

    /** 备注 */
    private String remark;
}
