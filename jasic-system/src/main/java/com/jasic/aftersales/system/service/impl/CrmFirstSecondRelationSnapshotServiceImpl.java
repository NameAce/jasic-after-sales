package com.jasic.aftersales.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.domain.entity.CrmFirstSecondRelationSnapshot;
import com.jasic.aftersales.system.domain.vo.CrmFirstSecondRelationSyncSummaryVO;
import com.jasic.aftersales.system.mapper.CrmFirstSecondRelationSnapshotMapper;
import com.jasic.aftersales.system.service.ICrmFirstSecondRelationSnapshotService;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * CRM 一级二级关系来源快照服务实现
 *
 * <p>仅同步 CRM 来源关系事实，供“一级-二级关系”管理中的来源导入使用。</p>
 *
 * @author Codex
 * @date 2026/04/17
 */
@Service
public class CrmFirstSecondRelationSnapshotServiceImpl implements ICrmFirstSecondRelationSnapshotService {

    private static final String CRM_RELATION_TABLE = "saas_deal_user_relation";
    private static final String CRM_BIZ_COMPANY_TABLE = "biz_company";
    private static final int FIRST_LEVEL_CUST_RAGE = 0;
    private static final int SECOND_LEVEL_CUST_RAGE = 3;
    private static final int DEFAULT_BATCH_SIZE = 500;

    @Resource(name = "jdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    /**
     * JDBC模板模板依赖。
     *
     * @return 处理结果
     */
    @Resource(name = "crmJdbcTemplate")
    private JdbcTemplate crmJdbcTemplate;

    @Resource
    private CrmFirstSecondRelationSnapshotMapper crmFirstSecondRelationSnapshotMapper;

    /**
     * 获取EarliestChangeTime相关数据。
     *
     * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
     * @return 处理结果
     */
    @Override
    public LocalDateTime getEarliestChangeTime() {
        // 说明：执行该步骤以保证业务流程正确。
        JdbcTemplate crm = requireCrmJdbcTemplate();
        Timestamp timestamp = crm.queryForObject(
                "SELECT MIN(r.oper_date) FROM " + CRM_RELATION_TABLE + " r "
                        + "WHERE r.oper_date IS NOT NULL "
                        + "AND EXISTS (SELECT 1 FROM " + CRM_BIZ_COMPANY_TABLE + " first_company "
                        + "WHERE first_company.cust_id = r.sup_cust_id "
                        + "AND first_company.cust_rage = " + FIRST_LEVEL_CUST_RAGE + ") "
                        + "AND EXISTS (SELECT 1 FROM " + CRM_BIZ_COMPANY_TABLE + " second_company "
                        + "WHERE second_company.cust_id = r.buy_cust_id "
                        + "AND second_company.cust_rage = " + SECOND_LEVEL_CUST_RAGE + ")",
                Timestamp.class);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    /**
     * 同步ByTimeRange。
     *
     * @param startInclusive 参数
     * @param endExclusive 参数
     * @return 处理结果
     */
    @Override
    public CrmFirstSecondRelationSyncSummaryVO syncByTimeRange(LocalDateTime startInclusive, LocalDateTime endExclusive) {
        if (startInclusive == null || endExclusive == null || !startInclusive.isBefore(endExclusive)) {
            throw new ServiceException("CRM 一级二级关系快照同步时间范围不合法");
        }
        // 说明：执行该步骤以保证业务流程正确。
        JdbcTemplate crm = requireCrmJdbcTemplate();
        String sql = "SELECT id, buy_cust_id, sup_cust_id, oper_date "
                + "FROM " + CRM_RELATION_TABLE + " r "
                + "WHERE oper_date >= ? AND oper_date < ? "
                + "AND EXISTS (SELECT 1 FROM " + CRM_BIZ_COMPANY_TABLE + " first_company "
                + "WHERE first_company.cust_id = r.sup_cust_id "
                + "AND first_company.cust_rage = " + FIRST_LEVEL_CUST_RAGE + ") "
                + "AND EXISTS (SELECT 1 FROM " + CRM_BIZ_COMPANY_TABLE + " second_company "
                + "WHERE second_company.cust_id = r.buy_cust_id "
                + "AND second_company.cust_rage = " + SECOND_LEVEL_CUST_RAGE + ") "
                + "ORDER BY r.buy_cust_id ASC, r.id ASC";

        List<CrmFirstSecondRelationSnapshot> batch = new ArrayList<>(DEFAULT_BATCH_SIZE);
        // 调用SyncCounter方法，复用统一能力并保证业务规则一致。
        SyncCounter counter = new SyncCounter();
        // 调用now方法，复用统一能力并保证业务规则一致。
        LocalDateTime syncTime = LocalDateTime.now();
        crm.query(connection -> {
            // 调用prepareStatement方法，复用统一能力并保证业务规则一致。
            PreparedStatement ps = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            // 调用setFetchSize方法，复用统一能力并保证业务规则一致。
            ps.setFetchSize(Integer.MIN_VALUE);
            // 调用toTimestamp方法，复用统一能力并保证业务规则一致。
            ps.setTimestamp(1, toTimestamp(startInclusive));
            // 调用toTimestamp方法，复用统一能力并保证业务规则一致。
            ps.setTimestamp(2, toTimestamp(endExclusive));
            return ps;
        }, rs -> {
            while (rs.next()) {
                // 调用getRow方法，复用统一能力并保证业务规则一致。
                CrmFirstSecondRelationSnapshot row = CRM_RELATION_ROW_MAPPER.mapRow(rs, rs.getRow());
                if (row == null || row.getFirstCustId() == null || row.getSecondCustId() == null) {
                    continue;
                }
                // 调用add方法，复用统一能力并保证业务规则一致。
                batch.add(row);
                counter.processedCount++;
                if (batch.size() >= DEFAULT_BATCH_SIZE) {
                    // 调用flushBatch方法，复用统一能力并保证业务规则一致。
                    flushBatch(batch, syncTime, counter);
                    // 调用clear方法，复用统一能力并保证业务规则一致。
                    batch.clear();
                }
            }
            return null;
        });

        if (CollUtil.isNotEmpty(batch)) {
            // 调用flushBatch方法，复用统一能力并保证业务规则一致。
            flushBatch(batch, syncTime, counter);
        }

        // 调用CrmFirstSecondRelationSyncSummaryVO方法，复用统一能力并保证业务规则一致。
        CrmFirstSecondRelationSyncSummaryVO summary = new CrmFirstSecondRelationSyncSummaryVO();
        // 调用setDataStartTime方法，复用统一能力并保证业务规则一致。
        summary.setDataStartTime(startInclusive);
        // 调用setDataEndTime方法，复用统一能力并保证业务规则一致。
        summary.setDataEndTime(endExclusive);
        // 调用setProcessedCount方法，复用统一能力并保证业务规则一致。
        summary.setProcessedCount(counter.processedCount);
        // 调用setInsertedCount方法，复用统一能力并保证业务规则一致。
        summary.setInsertedCount(counter.insertedCount);
        // 调用setUpdatedCount方法，复用统一能力并保证业务规则一致。
        summary.setUpdatedCount(counter.updatedCount);
        return summary;
    }

    /**
     * flushBatch。
     *
     * @param rows 参数
     * @param syncTime 参数
     * @param counter 参数
     */
    private void flushBatch(List<CrmFirstSecondRelationSnapshot> rows, LocalDateTime syncTime, SyncCounter counter) {
        List<Long> secondCustIds = rows.stream()
                .map(CrmFirstSecondRelationSnapshot::getSecondCustId)
                .filter(value -> value != null)
                .distinct()
                // 调用toList方法，复用统一能力并保证业务规则一致。
                .collect(Collectors.toList());
        // 调用emptySet方法，复用统一能力并保证业务规则一致。
        Set<Long> existingSecondCustIds = Collections.emptySet();
        if (CollUtil.isNotEmpty(secondCustIds)) {
            LambdaQueryWrapper<CrmFirstSecondRelationSnapshot> wrapper = new LambdaQueryWrapper<>();
            // 调用in方法，复用统一能力并保证业务规则一致。
            wrapper.in(CrmFirstSecondRelationSnapshot::getSecondCustId, secondCustIds);
            // 说明：执行该步骤以保证业务流程正确。
            existingSecondCustIds = crmFirstSecondRelationSnapshotMapper.selectList(wrapper).stream()
                    .map(CrmFirstSecondRelationSnapshot::getSecondCustId)
                    // 调用toCollection方法，复用统一能力并保证业务规则一致。
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        String sql = "INSERT INTO crm_first_second_relation_snapshot ("
                + "source_id, first_cust_id, second_cust_id, crm_oper_time, last_sync_time, create_time, update_time"
                + ") VALUES (?, ?, ?, ?, ?, NOW(), NOW()) "
                + "ON DUPLICATE KEY UPDATE "
                + "source_id = VALUES(source_id), "
                + "first_cust_id = VALUES(first_cust_id), "
                + "crm_oper_time = VALUES(crm_oper_time), "
                + "last_sync_time = VALUES(last_sync_time), "
                // 调用NOW方法，复用统一能力并保证业务规则一致。
                + "update_time = NOW()";
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            /**
     * setValues。
     *
     * @param ps 参数
     * @param i 参数
             */
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                // 调用get方法，复用统一能力并保证业务规则一致。
                CrmFirstSecondRelationSnapshot row = rows.get(i);
                // 调用getSourceId方法，复用统一能力并保证业务规则一致。
                ps.setObject(1, row.getSourceId());
                // 调用getFirstCustId方法，复用统一能力并保证业务规则一致。
                ps.setObject(2, row.getFirstCustId());
                // 调用getSecondCustId方法，复用统一能力并保证业务规则一致。
                ps.setObject(3, row.getSecondCustId());
                // 调用getCrmOperTime方法，复用统一能力并保证业务规则一致。
                ps.setTimestamp(4, toTimestamp(row.getCrmOperTime()));
                // 调用toTimestamp方法，复用统一能力并保证业务规则一致。
                ps.setTimestamp(5, toTimestamp(syncTime));
            }

            /**
     * 获取BatchSize。
     *
     * @return 处理结果
             */
            @Override
            public int getBatchSize() {
                return rows.size();
            }
        });

        for (CrmFirstSecondRelationSnapshot row : rows) {
            if (existingSecondCustIds.contains(row.getSecondCustId())) {
                counter.updatedCount++;
            } else {
                counter.insertedCount++;
            }
        }
    }

    /**
     * requireCRMJDBC模板。
     *
     * @return 处理结果
     */
    private JdbcTemplate requireCrmJdbcTemplate() {
        if (crmJdbcTemplate == null) {
            throw new ServiceException("当前未配置客户关系管理（CRM）数据源，请先完善 jasic.crm.datasource");
        }
        return crmJdbcTemplate;
    }

    /**
     * toTimestamp。
     *
     * @param value 参数
     * @return 处理结果
     */
    private Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    /**
     * toLocalDateTime。
     *
     * @param rs 参数
     * @param column 参数
     * @return 处理结果
     */
    private static LocalDateTime toLocalDateTime(ResultSet rs, String column) throws SQLException {
        // 调用getTimestamp方法，复用统一能力并保证业务规则一致。
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    private static final RowMapper<CrmFirstSecondRelationSnapshot> CRM_RELATION_ROW_MAPPER = (rs, rowNum) -> {
        CrmFirstSecondRelationSnapshot snapshot = new CrmFirstSecondRelationSnapshot();
        long sourceId = rs.getLong("id");
        snapshot.setSourceId(rs.wasNull() ? null : sourceId);
        long secondCustId = rs.getLong("buy_cust_id");
        snapshot.setSecondCustId(rs.wasNull() ? null : secondCustId);
        long firstCustId = rs.getLong("sup_cust_id");
        snapshot.setFirstCustId(rs.wasNull() ? null : firstCustId);
        snapshot.setCrmOperTime(toLocalDateTime(rs, "oper_date"));
        return snapshot;
    };

    private static class SyncCounter {
        private int processedCount;
        private int insertedCount;
        private int updatedCount;
    }
}


