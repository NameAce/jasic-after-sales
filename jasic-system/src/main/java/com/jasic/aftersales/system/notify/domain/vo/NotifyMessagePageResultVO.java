package com.jasic.aftersales.system.notify.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 通知消息分页返回对象。
 *
 * @author Codex
 * @date 2026/04/18
 */
@ApiModel(description = "通知消息分页返回对象")
@Data
public class NotifyMessagePageResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 总记录数 */
    @ApiModelProperty(value = "总记录数")
    private Long total;

    /** 当前页数据 */
    @ApiModelProperty(value = "当前页数据")
    private List<NotifyMessagePageVO> rows;

    public static NotifyMessagePageResultVO of(Long total, List<NotifyMessagePageVO> rows) {
        NotifyMessagePageResultVO result = new NotifyMessagePageResultVO();
        result.setTotal(total == null ? 0L : total);
        result.setRows(rows == null ? Collections.emptyList() : rows);
        return result;
    }
}
