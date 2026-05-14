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
 * @author Codex
 * @date 2026/03/26
 */
@Mapper
public interface WorkOrderFlowMapper extends BaseMapper<WorkOrderFlow> {

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
