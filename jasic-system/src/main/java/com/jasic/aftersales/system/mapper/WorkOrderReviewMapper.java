package com.jasic.aftersales.system.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jasic.aftersales.system.domain.entity.WorkOrderReview;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工单复检记录 Mapper
 *
 * @author Codex
 * @date 2026/03/26
 */
@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface WorkOrderReviewMapper extends BaseMapper<WorkOrderReview> {

}
