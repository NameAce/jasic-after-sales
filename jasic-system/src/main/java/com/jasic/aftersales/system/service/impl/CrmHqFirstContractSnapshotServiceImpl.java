package com.jasic.aftersales.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.domain.entity.CrmHqFirstContractSnapshot;
import com.jasic.aftersales.system.domain.vo.CrmHqFirstContractSyncSummaryVO;
import com.jasic.aftersales.system.mapper.CrmHqFirstContractSnapshotMapper;
import com.jasic.aftersales.system.service.ICrmHqFirstContractSnapshotService;
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
 * CRM 签约快照服务实现。
 *
 * <p>仅同步 CRM 原始签约事实，为签约页“从 CRM 导入”提供来源数据。</p>
 *
 * @author Zoro
 * @date 2026/04/12
 */
@Service
public class CrmHqFirstContractSnapshotServiceImpl implements ICrmHqFirstContractSnapshotService {

    /**CRM_CONTRACT_TABLE 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    private static final String CRM_CONTRACT_TABLE = "sap_company_info_sales";
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

    /**crmHqFirstContractSnapshotMapper 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private CrmHqFirstContractSnapshotMapper crmHqFirstContractSnapshotMapper;

    /**
     * 获取EarliestChangeTime相关数据。
     *
     * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
     * @return 业务处理结果
     */
    @Override
    public LocalDateTime getEarliestChangeTime() {
        JdbcTemplate crm = requireCrmJdbcTemplate();
        String sql = "SELECT MIN(t.change_time) FROM ("
                + "SELECT add_time AS change_time FROM " + CRM_CONTRACT_TABLE + " WHERE add_time IS NOT NULL "
                + "UNION ALL "
                + "SELECT oper_time AS change_time FROM " + CRM_CONTRACT_TABLE + " WHERE oper_time IS NOT NULL"
                + ") t";
        Timestamp timestamp = crm.queryForObject(sql, Timestamp.class);
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
    public CrmHqFirstContractSyncSummaryVO syncByTimeRange(LocalDateTime startInclusive, LocalDateTime endExclusive) {
        if (startInclusive == null || endExclusive == null || !startInclusive.isBefore(endExclusive)) {
            throw new ServiceException("CRM 签约快照同步时间范围不合法");
        }
        JdbcTemplate crm = requireCrmJdbcTemplate();
        String sql = "SELECT kunnr, cust_id, name, vkorg, vkbur, vkbxt, alive_flag, add_time, oper_time "
                + "FROM " + CRM_CONTRACT_TABLE + " "
                + "WHERE (oper_time >= ? AND oper_time < ?) "
                + "OR (add_time >= ? AND add_time < ?) "
                + "ORDER BY vkorg ASC, kunnr ASC";

        List<CrmHqFirstContractSnapshot> batch = new ArrayList<>(DEFAULT_BATCH_SIZE);
        SyncCounter counter = new SyncCounter();
        LocalDateTime syncTime = LocalDateTime.now();
        crm.query(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            ps.setFetchSize(Integer.MIN_VALUE);
            ps.setTimestamp(1, toTimestamp(startInclusive));
            ps.setTimestamp(2, toTimestamp(endExclusive));
            ps.setTimestamp(3, toTimestamp(startInclusive));
            ps.setTimestamp(4, toTimestamp(endExclusive));
            return ps;
        }, rs -> {
            while (rs.next()) {
                CrmHqFirstContractSnapshot row = CRM_CONTRACT_ROW_MAPPER.mapRow(rs, rs.getRow());
                if (row == null || StrUtil.isBlank(row.getKunnr()) || StrUtil.isBlank(row.getSalesOrg())) {
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

        CrmHqFirstContractSyncSummaryVO summary = new CrmHqFirstContractSyncSummaryVO();
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
    private void flushBatch(List<CrmHqFirstContractSnapshot> rows, LocalDateTime syncTime, SyncCounter counter) {
        List<String> uniqueKeys = rows.stream()
                .map(this::buildUniqueKey)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        Set<String> existingKeys = Collections.emptySet();
        if (CollUtil.isNotEmpty(uniqueKeys)) {
            existingKeys = crmHqFirstContractSnapshotMapper.selectList(null).stream()
                    .map(this::buildUniqueKey)
                    .filter(key -> key != null && uniqueKeys.contains(key))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        String sql = "INSERT INTO crm_hq_first_contract_snapshot ("
                + "kunnr, cust_id, crm_company_name, sales_org, region_code, region_name, "
                + "alive_flag, crm_add_time, crm_oper_time, last_sync_time, create_time, update_time"
                + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW()) "
                + "ON DUPLICATE KEY UPDATE "
                + "cust_id = VALUES(cust_id), "
                + "crm_company_name = VALUES(crm_company_name), "
                + "region_code = VALUES(region_code), "
                + "region_name = VALUES(region_name), "
                + "alive_flag = VALUES(alive_flag), "
                + "crm_add_time = VALUES(crm_add_time), "
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
                CrmHqFirstContractSnapshot row = rows.get(i);
                ps.setString(1, row.getKunnr());
                ps.setObject(2, row.getCustId());
                ps.setString(3, row.getCrmCompanyName());
                ps.setString(4, row.getSalesOrg());
                ps.setString(5, row.getRegionCode());
                ps.setString(6, row.getRegionName());
                ps.setObject(7, row.getAliveFlag());
                ps.setTimestamp(8, toTimestamp(row.getCrmAddTime()));
                ps.setTimestamp(9, toTimestamp(row.getCrmOperTime()));
                ps.setTimestamp(10, toTimestamp(syncTime));
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

        for (CrmHqFirstContractSnapshot row : rows) {
            if (existingKeys.contains(buildUniqueKey(row))) {
                counter.updatedCount++;
            } else {
                counter.insertedCount++;
            }
        }
    }

    /**
     * 构建UniqueKey。
     *
     * @param row row，当前业务处理所需的输入值。
     * @return 业务处理结果
     */
    private String buildUniqueKey(CrmHqFirstContractSnapshot row) {
        if (row == null || StrUtil.isBlank(row.getKunnr()) || StrUtil.isBlank(row.getSalesOrg())) {
            return null;
        }
        return row.getKunnr().trim() + "#" + row.getSalesOrg().trim();
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

    /**CRM_CONTRACT_ROW_MAPPER 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    private static final RowMapper<CrmHqFirstContractSnapshot> CRM_CONTRACT_ROW_MAPPER = (rs, rowNum) -> {
        CrmHqFirstContractSnapshot snapshot = new CrmHqFirstContractSnapshot();
        snapshot.setKunnr(StrUtil.trim(rs.getString("kunnr")));
        long custId = rs.getLong("cust_id");
        snapshot.setCustId(rs.wasNull() ? null : custId);
        snapshot.setCrmCompanyName(StrUtil.trim(rs.getString("name")));
        snapshot.setSalesOrg(StrUtil.trim(rs.getString("vkorg")));
        snapshot.setRegionCode(StrUtil.trim(rs.getString("vkbur")));
        snapshot.setRegionName(StrUtil.trim(rs.getString("vkbxt")));
        int aliveFlag = rs.getInt("alive_flag");
        snapshot.setAliveFlag(rs.wasNull() ? null : aliveFlag);
        snapshot.setCrmAddTime(toLocalDateTime(rs, "add_time"));
        snapshot.setCrmOperTime(toLocalDateTime(rs, "oper_time"));
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


