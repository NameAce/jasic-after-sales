package com.jasic.aftersales.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jasic.aftersales.system.domain.entity.WorkOrderFlow;
import com.jasic.aftersales.system.domain.vo.WorkOrderCompanyRepairHistoryStatVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * 工单流转历史 Mapper
 *
 * @author Zoro
 * @date 2026/03/26
 */
@Mapper
public interface WorkOrderFlowMapper extends BaseMapper<WorkOrderFlow> {

    /**
     * 查询指定工单最早的一条建单流转记录。
     *
     * <p>本轮寄件单号补录采用不改表的临时方案，`work_order` 主表没有正式创建人字段，
     * 因此只能回溯建单时写入的首条 `CREATE` 流转，并把其中的 `operator_user_id`
     * 临时视为建单人用户ID。按创建时间和主键同时升序，是为了在异常重复 CREATE 记录下仍稳定取最早记录。</p>
     *
     * @param workOrderId 工单ID
     * @return 首条建单流转记录；旧数据缺失 CREATE 时返回 null
     */
    @Select({
            "SELECT",
            "  id, work_order_id AS workOrderId, action_type AS actionType,",
            "  before_status AS beforeStatus, after_status AS afterStatus,",
            "  from_company_id AS fromCompanyId, to_company_id AS toCompanyId,",
            "  operator_company_id AS operatorCompanyId, operator_user_id AS operatorUserId,",
            "  remark, create_time AS createTime, update_time AS updateTime",
            "FROM work_order_flow",
            "WHERE work_order_id = #{workOrderId}",
            "  AND action_type = 'CREATE'",
            "ORDER BY create_time ASC, id ASC",
            "LIMIT 1"
    })
    WorkOrderFlow selectFirstCreateFlow(@Param("workOrderId") Long workOrderId);

    /**
     * 统计当前客户在指定服务网点集合内的建单报修历史。
     *
     * @param customerId 客户ID
     * @param companyIds 服务网点公司ID集合
     * @return 报修历史统计
     */
    @Select({
            "<script>",
            "SELECT",
            "  f.to_company_id AS companyId,",
            "  COUNT(1) AS repairCount,",
            "  MAX(f.create_time) AS lastRepairTime",
            "FROM work_order w",
            "INNER JOIN work_order_flow f ON f.work_order_id = w.id",
            "WHERE w.customer_id = #{customerId}",
            "  AND f.action_type = 'CREATE'",
            "  AND f.to_company_id IN",
            "  <foreach collection='companyIds' item='companyId' open='(' separator=',' close=')'>",
            "    #{companyId}",
            "  </foreach>",
            "GROUP BY f.to_company_id",
            "</script>"
    })
    List<WorkOrderCompanyRepairHistoryStatVO> selectCustomerCreateCompanyRepairHistory(
            @Param("customerId") Long customerId,
            @Param("companyIds") Collection<Long> companyIds);
}
