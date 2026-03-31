package com.jasic.aftersales.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工单故障点视图
 *
 * @author Codex
 * @date 2026/03/26
 */
@Data
public class WorkOrderFaultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 故障点ID */
    private Long id;

    /** 登记公司ID */
    private Long companyId;

    /** 故障描述 */
    private String faultDesc;

    /** 维修说明 */
    private String repairDesc;

    /** 配件信息 */
    private String partDesc;

    /** 图片地址集合 */
    private String imageUrls;

    /** 排序号 */
    private Integer sortNum;

    /** 登记人ID */
    private Long createdBy;

    /** 登记人姓名 */
    private String createdByName;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
