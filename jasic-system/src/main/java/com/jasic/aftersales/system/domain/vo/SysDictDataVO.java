package com.jasic.aftersales.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 字典数据 VO
 *
 * @author Codex
 * @date 2026/03/19
 */
@Data
public class SysDictDataVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
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

    /** 是否默认 */
    private Integer isDefault;

    /** 状态 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
