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
 * @author Zoro
 * @date 2026/04/17
 */
@Service
public class CrmFirstSecondRelationSnapshotServiceImpl implements ICrmFirstSecondRelationSnapshotService {

    /**CRM_RELATION_TABLE 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    private static final String CRM_RELATION_TABLE = "saas_deal_user_relation";
    /**CRM_BIZ_COMPANY_TABLE 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    private static final String CRM_BIZ_COMPANY_TABLE = "biz_company";
    /**FIRST_LEVEL_CUST_RAGE 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    private static final int FIRST_LEVEL_CUST_RAGE = 0;
    /**SECOND_LEVEL_CUST_RAGE 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    private static final int SECOND_LEVEL_CUST_RAGE = 3;
    /**DEFAULT_BATCH_SIZE 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    private static final int DEFAULT_BATCH_SIZE = 500;

    /**jdbcTemplate 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource(name = "jdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    /**
     * JDBC模板模板依赖。
     *
     * @return 业务处理结果
     */
    @Resource(name = "crmJdbcTemplate")
    private JdbcTemplate crmJdbcTemplate;

    /**crmFirstSecondRelationSnapshotMapper 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private CrmFirstSecondRelationSnapshotMapper crmFirstSecondRelationSnapshotMapper;

    /**
     * 获取EarliestChangeTime相关数据。
     *
     * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
     * @return 业务处理结果
     */
    @Override
    public LocalDateTime getEarliestChangeTime() {
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
     * @param startInclusive startInclusive，当前业务处理所需的输入值。
     * @param endExclusive endExclusive，当前业务处理所需的输入值。
     * @return 业务处理结果
     */
    @Override
    public CrmFirstSecondRelationSyncSummaryVO syncByTimeRange(LocalDateTime startInclusive, LocalDateTime endExclusive) {
        if (startInclusive == null || endExclusive == null || !startInclusive.isBefore(endExclusive)) {
            throw new ServiceException("CRM 一级二级关系快照同步时间范围不合法");
        }
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
        SyncCounter counter = new SyncCounter();
        LocalDateTime syncTime = LocalDateTime.now();
        crm.query(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            ps.setFetchSize(Integer.MIN_VALUE);
            ps.setTimestamp(1, toTimestamp(startInclusive));
            ps.setTimestamp(2, toTimestamp(endExclusive));
            return ps;
        }, rs -> {
            while (rs.next()) {
                CrmFirstSecondRelationSnapshot row = CRM_RELATION_ROW_MAPPER.mapRow(rs, rs.getRow());
                if (row == null || row.getFirstCustId() == null || row.getSecondCustId() == null) {
                    continue;
                }
                batch.add(row);
                counter.processedCount++;
                if (batch.size() >= DEFAULT_BATCH_SIZE) {
                    flushBatch(batch, syncTime, counter);
                    batch.clear();
                }
            }
            return null;
        });

        if (CollUtil.isNotEmpty(batch)) {
            flushBatch(batch, syncTime, counter);
        }

        CrmFirstSecondRelationSyncSummaryVO summary = new CrmFirstSecondRelationSyncSummaryVO();
        summary.setDataStartTime(startInclusive);
        summary.setDataEndTime(endExclusive);
        summary.setProcessedCount(counter.processedCount);
        summary.setInsertedCount(counter.insertedCount);
        summary.setUpdatedCount(counter.updatedCount);
        return summary;
    }

    /**
     * flushBatch。
     *
     * @param rows rows，当前业务处理所需的输入值。
     * @param syncTime 时间值，用于业务节点记录或时效判断。
     * @param counter counter，当前业务处理所需的输入值。
     */
    private void flushBatch(List<CrmFirstSecondRelationSnapshot> rows, LocalDateTime syncTime, SyncCounter counter) {
        List<Long> secondCustIds = rows.stream()
                .map(CrmFirstSecondRelationSnapshot::getSecondCustId)
                .filter(value -> value != null)
                .distinct()
                .collect(Collectors.toList());
        Set<Long> existingSecondCustIds = Collections.emptySet();
        if (CollUtil.isNotEmpty(secondCustIds)) {
            LambdaQueryWrapper<CrmFirstSecondRelationSnapshot> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(CrmFirstSecondRelationSnapshot::getSecondCustId, secondCustIds);
            existingSecondCustIds = crmFirstSecondRelationSnapshotMapper.selectList(wrapper).stream()
                    .map(CrmFirstSecondRelationSnapshot::getSecondCustId)
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
                + "update_time = NOW()";
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            /**
     * setValues。
     *
     * @param ps ps，当前业务处理所需的输入值。
     * @param i i，当前业务处理所需的输入值。
             */
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                CrmFirstSecondRelationSnapshot row = rows.get(i);
                ps.setObject(1, row.getSourceId());
                ps.setObject(2, row.getFirstCustId());
                ps.setObject(3, row.getSecondCustId());
                ps.setTimestamp(4, toTimestamp(row.getCrmOperTime()));
                ps.setTimestamp(5, toTimestamp(syncTime));
            }

            /**
     * 获取BatchSize。
     *
     * @return 业务处理结果
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
     * @return 业务处理结果
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
     * @param value value，当前业务处理所需的输入值。
     * @return 业务处理结果
     */
    private Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    /**
     * toLocalDateTime。
     *
     * @param rs rs，当前业务处理所需的输入值。
     * @param column column，当前业务处理所需的输入值。
     * @return 业务处理结果
     */
    private static LocalDateTime toLocalDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    /**CRM_RELATION_ROW_MAPPER 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
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

    /**SyncCounter 服务实现，负责业务校验、状态流转、数据持久化和跨模块协同。

@author Zoro*/
    private static class SyncCounter {
        /**processedCount 字段，用于当前类内部业务处理。*/
        private int processedCount;
        /**insertedCount 字段，用于当前类内部业务处理。*/
        private int insertedCount;
        /**updatedCount 字段，用于当前类内部业务处理。*/
        private int updatedCount;
    }
}


