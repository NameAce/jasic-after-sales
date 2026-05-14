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

    /**
     * 处理of业务逻辑。
     *
     * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
     * @param total 参数
     * @param rows 参数
     * @return 处理结果
     */
    public static NotifyMessagePageResultVO of(Long total, List<NotifyMessagePageVO> rows) {
        // 调用NotifyMessagePageResultVO方法，复用统一能力并保证业务规则一致。
        /**
         * 通知消息分页结果视图。
         */
        NotifyMessagePageResultVO result = new NotifyMessagePageResultVO();
        // 调用setTotal方法，复用统一能力并保证业务规则一致。
        result.setTotal(total == null ? 0L : total);
        // 调用emptyList方法，复用统一能力并保证业务规则一致。
        result.setRows(rows == null ? Collections.emptyList() : rows);
        return result;
    }
}




