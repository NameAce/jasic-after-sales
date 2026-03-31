package com.jasic.aftersales.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工单列表视图
 *
 * @author Codex
 * @date 2026/03/26
 */
@Data
public class WorkOrderListVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 工单ID */
    private Long id;

    /** 工单号 */
    private String orderNo;

    /** 客户姓名 */
    private String customerName;

    /** 客户手机号 */
    private String customerMobile;

    /** 条码 */
    private String barcode;

    /** 机器型号 */
    private String productModel;

    /** 主状态 */
    private String mainStatus;

    /** 主状态名称 */
    private String mainStatusLabel;

    /** 展示状态 */
    private String displayStatus;

    /** 当前受理公司ID */
    private Long currentAcceptCompanyId;

    /** 当前受理公司名称 */
    private String currentAcceptCompanyName;

    /** 当前维修员ID */
    private Long assignedUserId;

    /** 当前维修员姓名 */
    private String assignedUserName;

    /** 是否发生过转单 */
    private Integer hasTransfer;

    /** 转单次数 */
    private Integer transferCount;

    /** 当前关系类型 */
    private String relationType;

    /** 是否只读 */
    private Integer isReadonly;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
