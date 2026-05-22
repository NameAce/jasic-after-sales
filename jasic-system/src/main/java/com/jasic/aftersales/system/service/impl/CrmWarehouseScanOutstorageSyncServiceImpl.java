package com.jasic.aftersales.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.domain.entity.CrmWarehouseScanOutstorageSnapshot;
import com.jasic.aftersales.system.domain.entity.MachineBarcode;
import com.jasic.aftersales.system.domain.vo.CrmWarehouseScanOutstorageSyncSummaryVO;
import com.jasic.aftersales.system.mapper.MachineBarcodeMapper;
import com.jasic.aftersales.system.service.ICrmWarehouseScanOutstorageSyncService;
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
     * CRM仓库扫描出库同步服务实现。
     *
     * <ul>
     * <li>步骤说明。</li>
     * <li>步骤说明。</li>
     * </ul>
     * @author Zoro
     * @date 2026/04/12
 */
@Service
public class CrmWarehouseScanOutstorageSyncServiceImpl implements ICrmWarehouseScanOutstorageSyncService {

    /**CRM_OUTSTORAGE_TABLE 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    private static final String CRM_OUTSTORAGE_TABLE = "saas_warehouse_scan_outstorage";
    /**DEFAULT_BATCH_SIZE 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    private static final int DEFAULT_BATCH_SIZE = 1000;

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

    /**machineBarcodeMapper 依赖，用于协同完成当前业务流程中的数据访问、规则校验或状态处理。*/
    @Resource
    private MachineBarcodeMapper machineBarcodeMapper;

    /**
     * 处理syncIncremental业务逻辑。
     *
     * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
     * @return 业务处理结果
     */
    @Override
    public CrmWarehouseScanOutstorageSyncSummaryVO syncIncremental() {
        JdbcTemplate crm = requireCrmJdbcTemplate();
        Long lastSourceId = getLocalMaxSourceId();
        String sql = "SELECT scan_outstorage_id, ware_id, warehouse_id, scan_code, scan_date, cust_id, product_numeric "
                + "FROM " + CRM_OUTSTORAGE_TABLE + " "
                + "WHERE scan_outstorage_id > ? "
                + "ORDER BY scan_outstorage_id ASC";

        List<CrmWarehouseScanOutstorageSnapshot> batch = new ArrayList<>(DEFAULT_BATCH_SIZE);
        SyncCounter counter = new SyncCounter();
        LocalDateTime syncTime = LocalDateTime.now();
        crm.query(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            ps.setFetchSize(Integer.MIN_VALUE);
            ps.setLong(1, lastSourceId == null ? 0L : lastSourceId);
            return ps;
        }, rs -> {
            while (rs.next()) {
                CrmWarehouseScanOutstorageSnapshot row = CRM_OUTSTORAGE_ROW_MAPPER.mapRow(rs, rs.getRow());
                if (row == null || row.getSourceId() == null) {
                    continue;
                }
                batch.add(row);
                counter.syncedDetailCount++;
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

        if (CollUtil.isNotEmpty(counter.changedBarcodes)) {
            projectLastOutDate(counter.changedBarcodes, counter);
        }

        CrmWarehouseScanOutstorageSyncSummaryVO summary = new CrmWarehouseScanOutstorageSyncSummaryVO();
        summary.setSyncedDetailCount(counter.syncedDetailCount);
        summary.setAffectedBarcodeCount(counter.changedBarcodes.size());
        summary.setUpdatedMachineBarcodeCount(counter.updatedMachineBarcodeCount);
        summary.setUnmatchedBarcodeCount(counter.unmatchedBarcodeCount);
        summary.setLatestSourceId(counter.latestSourceId);
        return summary;
    }

    /**
     * flushBatch。
     *
     * @param rows rows，当前业务处理所需的输入值。
     * @param syncTime 时间值，用于业务节点记录或时效判断。
     * @param counter counter，当前业务处理所需的输入值。
     */
    private void flushBatch(List<CrmWarehouseScanOutstorageSnapshot> rows, LocalDateTime syncTime, SyncCounter counter) {
        String sql = "INSERT INTO crm_warehouse_scan_outstorage_snapshot ("
                + "source_id, ware_id, warehouse_id, scan_code, scan_date, cust_id, product_numeric, last_sync_time, create_time, update_time"
                + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW()) "
                + "ON DUPLICATE KEY UPDATE "
                + "ware_id = VALUES(ware_id), "
                + "warehouse_id = VALUES(warehouse_id), "
                + "scan_code = VALUES(scan_code), "
                + "scan_date = VALUES(scan_date), "
                + "cust_id = VALUES(cust_id), "
                + "product_numeric = VALUES(product_numeric), "
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
                CrmWarehouseScanOutstorageSnapshot row = rows.get(i);
                ps.setObject(1, row.getSourceId());
                ps.setObject(2, row.getWareId());
                ps.setObject(3, row.getWarehouseId());
                ps.setString(4, row.getScanCode());
                ps.setTimestamp(5, toTimestamp(row.getScanDate()));
                ps.setObject(6, row.getCustId());
                ps.setString(7, row.getProductNumeric());
                ps.setTimestamp(8, toTimestamp(syncTime));
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

        for (CrmWarehouseScanOutstorageSnapshot row : rows) {
            if (StrUtil.isNotBlank(row.getScanCode())) {
                counter.changedBarcodes.add(row.getScanCode());
            }
            counter.latestSourceId = row.getSourceId();
        }
    }

    /**
     * projectLastOutDate。
     *
     * @param barcodeSet 业务编码，用于匹配枚举、配置或外部系统数据。
     * @param counter counter，当前业务处理所需的输入值。
     */
    private void projectLastOutDate(Set<String> barcodeSet, SyncCounter counter) {
        List<String> barcodes = new ArrayList<>(barcodeSet);
        int batchSize = 500;
        for (int index = 0; index < barcodes.size(); index += batchSize) {
            List<String> slice = barcodes.subList(index, Math.min(index + batchSize, barcodes.size()));
            Map<String, LocalDateTime> minScanDateMap = queryMinScanDateMap(slice);
            LambdaQueryWrapper<MachineBarcode> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(MachineBarcode::getBarcode, slice);
            List<MachineBarcode> existingBarcodes = machineBarcodeMapper.selectList(wrapper);
            Set<String> existingBarcodeSet = existingBarcodes.stream()
                    .map(MachineBarcode::getBarcode)
                    .filter(StrUtil::isNotBlank)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            counter.unmatchedBarcodeCount += slice.size() - existingBarcodeSet.size();
            if (existingBarcodeSet.isEmpty()) {
                continue;
            }

            List<Map.Entry<String, LocalDateTime>> updates = existingBarcodes.stream()
                    .map(item -> new java.util.AbstractMap.SimpleEntry<>(
                            item.getBarcode(),
                            minScanDateMap.getOrDefault(item.getBarcode(), item.getScanDate())
                    ))
                    .filter(item -> StrUtil.isNotBlank(item.getKey()) && item.getValue() != null)
                    .collect(Collectors.toList());
            if (updates.isEmpty()) {
                continue;
            }
            String updateSql = "UPDATE machine_barcode SET last_out_date = ?, last_sync_time = NOW(), update_time = NOW() "
                    + "WHERE barcode = ?";
            jdbcTemplate.batchUpdate(updateSql, new BatchPreparedStatementSetter() {
                /**
     * setValues。
     *
     * @param ps ps，当前业务处理所需的输入值。
     * @param i i，当前业务处理所需的输入值。
                 */
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    Map.Entry<String, LocalDateTime> item = updates.get(i);
                    ps.setTimestamp(1, toTimestamp(item.getValue()));
                    ps.setString(2, item.getKey());
                }

                /**
     * 获取BatchSize。
     *
     * @return 业务处理结果
                 */
                @Override
                public int getBatchSize() {
                    return updates.size();
                }
            });
            counter.updatedMachineBarcodeCount += updates.size();
        }
    }

    /**
     * 查询Min扫描DateMap。
     *
     * @param barcodes 业务编码，用于匹配枚举、配置或外部系统数据。
     * @return 业务处理结果
     */
    private Map<String, LocalDateTime> queryMinScanDateMap(List<String> barcodes) {
        if (CollUtil.isEmpty(barcodes)) {
            return Collections.emptyMap();
        }
        String placeholders = barcodes.stream().map(item -> "?").collect(Collectors.joining(","));
        String sql = "SELECT scan_code, MIN(scan_date) AS last_out_date "
                + "FROM crm_warehouse_scan_outstorage_snapshot "
                + "WHERE scan_code IN (" + placeholders + ") AND scan_date IS NOT NULL "
                + "GROUP BY scan_code";
        List<Map.Entry<String, LocalDateTime>> rows = jdbcTemplate.query(sql, ps -> {
            for (int i = 0; i < barcodes.size(); i++) {
                ps.setString(i + 1, barcodes.get(i));
            }
        }, (rs, rowNum) -> new java.util.AbstractMap.SimpleEntry<>(
                StrUtil.trim(rs.getString("scan_code")),
                toLocalDateTime(rs, "last_out_date")
        ));
        Map<String, LocalDateTime> result = new LinkedHashMap<>();
        for (Map.Entry<String, LocalDateTime> row : rows) {
            if (StrUtil.isNotBlank(row.getKey()) && row.getValue() != null) {
                result.put(row.getKey(), row.getValue());
            }
        }
        return result;
    }

    /**
     * 获取LocalMax来源ID。
     *
     * @return 业务处理结果
     */
    private Long getLocalMaxSourceId() {
        String sql = "SELECT MAX(source_id) FROM crm_warehouse_scan_outstorage_snapshot";
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0L : value;
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

    /**CRM_OUTSTORAGE_ROW_MAPPER 常量，用于固定当前类内部复用的业务编码、默认值或配置边界。*/
    private static final RowMapper<CrmWarehouseScanOutstorageSnapshot> CRM_OUTSTORAGE_ROW_MAPPER = (rs, rowNum) -> {
        CrmWarehouseScanOutstorageSnapshot snapshot = new CrmWarehouseScanOutstorageSnapshot();
        long sourceId = rs.getLong("scan_outstorage_id");
        snapshot.setSourceId(rs.wasNull() ? null : sourceId);
        long wareId = rs.getLong("ware_id");
        snapshot.setWareId(rs.wasNull() ? null : wareId);
        long warehouseId = rs.getLong("warehouse_id");
        snapshot.setWarehouseId(rs.wasNull() ? null : warehouseId);
        snapshot.setScanCode(StrUtil.trim(rs.getString("scan_code")));
        snapshot.setScanDate(toLocalDateTime(rs, "scan_date"));
        long custId = rs.getLong("cust_id");
        snapshot.setCustId(rs.wasNull() ? null : custId);
        snapshot.setProductNumeric(StrUtil.trim(rs.getString("product_numeric")));
        return snapshot;
    };

    /**SyncCounter 服务实现，负责业务校验、状态流转、数据持久化和跨模块协同。

@author Zoro*/
    private static class SyncCounter {
        /**syncedDetailCount 字段，用于当前类内部业务处理。*/
        private int syncedDetailCount;
        /**changedBarcodes 字段，用于当前类内部业务处理。*/
        private final Set<String> changedBarcodes = new LinkedHashSet<>();
        /**updatedMachineBarcodeCount 字段，用于当前类内部业务处理。*/
        private int updatedMachineBarcodeCount;
        /**unmatchedBarcodeCount 字段，用于当前类内部业务处理。*/
        private int unmatchedBarcodeCount;
        /**latestSourceId 字段，用于当前类内部业务处理。*/
        private Long latestSourceId;
    }
}



