package com.jasic.aftersales.system.domain.query;

import com.jasic.aftersales.common.core.domain.PageQuery;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 我的反馈分页查询参数。
 *
 * <p>当前阶段“我的反馈”没有额外筛选条件，保留该查询对象主要是为了和现有分页接口风格保持一致，
 * 同时为后续如有稳定筛选能力时预留统一扩展入口。</p>
 *
 * @author Codex
 * @date 2026/05/28
 */
@ApiModel(description = "我的反馈分页查询参数")
@Data
@EqualsAndHashCode(callSuper = true)
public class SysFeedbackMyQuery extends PageQuery {

    private static final long serialVersionUID = 1L;
}
