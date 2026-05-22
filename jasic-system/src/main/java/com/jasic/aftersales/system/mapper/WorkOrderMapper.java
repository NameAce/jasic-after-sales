package com.jasic.aftersales.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jasic.aftersales.system.domain.entity.WorkOrder;
import com.jasic.aftersales.system.domain.query.WorkOrderHqSiteInternalQuery;
import com.jasic.aftersales.system.domain.query.WorkOrderScopedQuery;
import com.jasic.aftersales.system.domain.vo.WorkOrderDetailVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderHqSiteSummaryVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderListVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderParticipantVO;
import com.jasic.aftersales.system.domain.vo.WorkOrderStatusCountVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 工单主表 Mapper
 *
 * @author Zoro
 * @date 2026/03/26
 */
@Mapper
public interface WorkOrderMapper extends BaseMapper<WorkOrder> {

    /**
     * 分页查询工单列表
     *
     * @param page  分页参数
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<WorkOrderListVO> selectWorkOrderPage(Page<WorkOrderListVO> page, @Param("query") WorkOrderScopedQuery query);

    /**
     * 按状态统计工单数量
     *
     * @param query 查询条件
     * @return 状态统计结果
     */
    List<WorkOrderStatusCountVO> selectStatusCount(@Param("query") WorkOrderScopedQuery query);

    /**
     * 查询总部网点工单汇总。
     *
     * @param query 查询条件
     * @return 网点汇总
     */
    List<WorkOrderHqSiteSummaryVO> selectHqSiteSummary(@Param("query") WorkOrderHqSiteInternalQuery query);

    /**
     * 分页查询总部网点工单明细。
     *
     * @param page  分页参数
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<WorkOrderListVO> selectHqSiteOrderPage(Page<WorkOrderListVO> page, @Param("query") WorkOrderHqSiteInternalQuery query);

    /**
     * 查询工单详情
     *
     * @param id 工单ID
     * @return 工单详情
     */
    WorkOrderDetailVO selectDetailById(@Param("id") Long id);

    /**
     * 查询工单参与方列表
     *
     * @param workOrderId 工单ID
     * @return 参与方列表
     */
    List<WorkOrderParticipantVO> selectParticipantList(@Param("workOrderId") Long workOrderId);
}
