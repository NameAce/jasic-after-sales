package com.jasic.aftersales.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 一级-二级从属关系列表 VO（含关联名称）
 *
 * @author Zoro
 * @date 2026/03/18
 */
@Data
public class FirstSecondRelationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 一级网点公司ID */
    private Long firstCompanyId;

    /** 二级网点公司ID */
    private Long secondCompanyId;

    /** 一级网点名称 */
    private String firstCompanyName;

    /** 二级网点名称 */
    private String secondCompanyName;

    /** 状态（1=有效，0=解除） */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
