package com.jasic.aftersales.system.domain.query;

import com.jasic.aftersales.common.core.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 工单查询参数
 *
 * @author Zoro
 * @date 2026/03/26
 */
@ApiModel(description = "工单查询参数")
@Data
@EqualsAndHashCode(callSuper = true)
public class WorkOrderQuery extends PageQuery {

    private static final long serialVersionUID = 1L;

    /** 查看范围（CURRENT/HISTORY/ALL） */
    @ApiModelProperty(value = "查看范围", allowableValues = "CURRENT,HISTORY,ALL")
    private String viewScope;

    /** 工单号（模糊） */
    @ApiModelProperty(value = "工单号（模糊）")
    private String orderNo;

    /** 小程序单搜索框关键字，按手机号、条形码、机器型号做模糊查询。 */
    @ApiModelProperty(value = "小程序单搜索框关键字（手机号/条形码/机器型号模糊）")
    private String keyword;

    /** 客户姓名（模糊） */
    @ApiModelProperty(value = "客户姓名（模糊）")
    private String customerName;

    /** 客户手机号（模糊） */
    @ApiModelProperty(value = "客户手机号（模糊）")
    private String customerMobile;

    /** 条码（模糊） */
    @ApiModelProperty(value = "条码（模糊）")
    private String barcode;

    /** 机器型号（模糊） */
    @ApiModelProperty(value = "机器型号（模糊）")
    private String productModel;

    /** 主状态 */
    @ApiModelProperty(value = "主状态", allowableValues = "PENDING_ASSIGN,PENDING_TECH_ACCEPT,IN_PROGRESS,COMPLETED,CLOSED")
    private String mainStatus;

    /** 展示状态（ALL/WAIT_ACCEPT/IN_PROGRESS/COMPLETED/CLOSED） */
    @ApiModelProperty(value = "展示状态", allowableValues = "ALL,WAIT_ACCEPT,IN_PROGRESS,COMPLETED,CLOSED")
    private String displayStatus;

    /** 是否转单 */
    @ApiModelProperty(value = "是否转单")
    private Integer hasTransfer;

    /**
     * 转单方向。
     *
     * <p>当前仅支持 OUT，表示当前登录主体公司作为转出方的工单。
     * 该字段由后端根据当前登录公司限定 from_company_id，前端不得传任意公司 ID。</p>
     */
    @ApiModelProperty(value = "转单方向", allowableValues = "OUT")
    private String transferDirection;
}
