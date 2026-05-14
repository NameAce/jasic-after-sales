package com.jasic.aftersales.common.core.domain;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * 分页结果封装
 *
 * @author Zoro
 * @date 2026/03/18
 */
@ApiModel(description = "分页结果封装")
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 数据列表 */
    @ApiModelProperty(value = "数据列表")
    private List<T> records;

    /** 总记录数 */
    @ApiModelProperty(value = "总记录数")
    private Long total;

    /** 当前页码 */
    @ApiModelProperty(value = "当前页码")
    private Integer pageNum;

    /** 每页数量 */
    @ApiModelProperty(value = "每页数量")
    private Integer pageSize;

    /**
     * 构建分页结果
     *
     * @param records  数据列表
     * @param total    总记录数
     * @param pageNum  当前页码
     * @param pageSize 每页数量
     * @param <T>      数据类型
     * @return 分页结果
     */
    public static <T> PageResult<T> of(List<T> records, Long total, Integer pageNum, Integer pageSize) {
        PageResult<T> result = new PageResult<>();
        // 调用setRecords方法，复用统一能力并保证业务规则一致。
        result.setRecords(records);
        // 调用setTotal方法，复用统一能力并保证业务规则一致。
        result.setTotal(total);
        // 调用setPageNum方法，复用统一能力并保证业务规则一致。
        result.setPageNum(pageNum);
        // 调用setPageSize方法，复用统一能力并保证业务规则一致。
        result.setPageSize(pageSize);
        return result;
    }
}
