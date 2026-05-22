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
 * @author Zoro
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

    /**
     * 处理of业务逻辑。
     *
     * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
     * @param total total，当前业务处理所需的输入值。
     * @param rows rows，当前业务处理所需的输入值。
     * @return 业务处理结果
     */
    public static NotifyMessagePageResultVO of(Long total, List<NotifyMessagePageVO> rows) {
        /**
         * 通知消息分页结果视图。
         */
        NotifyMessagePageResultVO result = new NotifyMessagePageResultVO();
        result.setTotal(total == null ? 0L : total);
        result.setRows(rows == null ? Collections.emptyList() : rows);
        return result;
    }
}




