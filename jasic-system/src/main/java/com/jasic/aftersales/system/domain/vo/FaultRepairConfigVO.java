package com.jasic.aftersales.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 故障与维修配置视图
 *
 * @author Codex
 * @date 2026/04/01
 */
@Data
public class FaultRepairConfigVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 归属总部ID */
    private Long companyId;

    /** 归属总部名称 */
    private String companyName;

    /** 物料编码 */
    private String productCode;

    /** 产品型号 */
    private String productModel;

    /** 状态 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 故障摘要 */
    private String faultDescSummary;

    /** 故障项 */
    private List<FaultRepairConfigFaultVO> faults;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
