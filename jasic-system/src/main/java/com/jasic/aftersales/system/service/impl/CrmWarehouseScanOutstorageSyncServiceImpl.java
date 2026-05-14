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
     * @author Codex
     * @date 2026/04/12
 */
@Service
public class CrmWarehouseScanOutstorageSyncServiceImpl implements ICrmWarehouseScanOutstorageSyncService {

    private static final String CRM_OUTSTORAGE_TABLE = "saas_warehouse_scan_outstorage";
    private static final int DEFAULT_BATCH_SIZE = 1000;

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
    private MachineBarcodeMapper machineBarcodeMapper;

    /**
     * 处理syncIncremental业务逻辑。
     *
     * <p>说明：该方法用于执行业务流程编排，确保调用链路清晰可维护。</p>
     * @return 处理结果
     */
    @Override
    public CrmWarehouseScanOutstorageSyncSummaryVO syncIncremental() {
        // 说明：执行该步骤以保证业务流程正确。
        JdbcTemplate crm = requireCrmJdbcTemplate();
        // 调用getLocalMaxSourceId方法，复用统一能力并保证业务规则一致。
        Long lastSourceId = getLocalMaxSourceId();
        // 说明：执行该步骤以保证业务流程正确。
        String sql = "SELECT scan_outstorage_id, ware_id, warehouse_id, scan_code, scan_date, cust_id, product_numeric "
                + "FROM " + CRM_OUTSTORAGE_TABLE + " "
                + "WHERE scan_outstorage_id > ? "
                + "ORDER BY scan_outstorage_id ASC";

        List<CrmWarehouseScanOutstorageSnapshot> batch = new ArrayList<>(DEFAULT_BATCH_SIZE);
        // 调用SyncCounter方法，复用统一能力并保证业务规则一致。
        SyncCounter counter = new SyncCounter();
        // 调用now方法，复用统一能力并保证业务规则一致。
        LocalDateTime syncTime = LocalDateTime.now();
        crm.query(connection -> {
            // 调用prepareStatement方法，复用统一能力并保证业务规则一致。
            PreparedStatement ps = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            // 调用setFetchSize方法，复用统一能力并保证业务规则一致。
            ps.setFetchSize(Integer.MIN_VALUE);
            // 调用setLong方法，复用统一能力并保证业务规则一致。
            ps.setLong(1, lastSourceId == null ? 0L : lastSourceId);
            return ps;
        }, rs -> {
            while (rs.next()) {
                // 调用getRow方法，复用统一能力并保证业务规则一致。
                CrmWarehouseScanOutstorageSnapshot row = CRM_OUTSTORAGE_ROW_MAPPER.mapRow(rs, rs.getRow());
                if (row == null || row.getSourceId() == null) {
                    continue;
                }
                // 调用add方法，复用统一能力并保证业务规则一致。
                batch.add(row);
                counter.syncedDetailCount++;
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

        if (CollUtil.isNotEmpty(counter.changedBarcodes)) {
            // 说明：执行该步骤以保证业务流程正确。
            projectLastOutDate(counter.changedBarcodes, counter);
        }

        // 调用CrmWarehouseScanOutstorageSyncSummaryVO方法，复用统一能力并保证业务规则一致。
        CrmWarehouseScanOutstorageSyncSummaryVO summary = new CrmWarehouseScanOutstorageSyncSummaryVO();
        // 调用setSyncedDetailCount方法，复用统一能力并保证业务规则一致。
        summary.setSyncedDetailCount(counter.syncedDetailCount);
        // 调用size方法，复用统一能力并保证业务规则一致。
        summary.setAffectedBarcodeCount(counter.changedBarcodes.size());
        // 调用setUpdatedMachineBarcodeCount方法，复用统一能力并保证业务规则一致。
        summary.setUpdatedMachineBarcodeCount(counter.updatedMachineBarcodeCount);
        // 调用setUnmatchedBarcodeCount方法，复用统一能力并保证业务规则一致。
        summary.setUnmatchedBarcodeCount(counter.unmatchedBarcodeCount);
        // 调用setLatestSourceId方法，复用统一能力并保证业务规则一致。
        summary.setLatestSourceId(counter.latestSourceId);
        return summary;
    }

    /**
     * flushBatch。
     *
     * @param rows 参数
     * @param syncTime 参数
     * @param counter 参数
     */
    private void flushBatch(List<CrmWarehouseScanOutstorageSnapshot> rows, LocalDateTime syncTime, SyncCounter counter) {
        // 说明：执行该步骤以保证业务流程正确。
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
                CrmWarehouseScanOutstorageSnapshot row = rows.get(i);
                // 调用getSourceId方法，复用统一能力并保证业务规则一致。
                ps.setObject(1, row.getSourceId());
                // 调用getWareId方法，复用统一能力并保证业务规则一致。
                ps.setObject(2, row.getWareId());
                // 调用getWarehouseId方法，复用统一能力并保证业务规则一致。
                ps.setObject(3, row.getWarehouseId());
                // 调用getScanCode方法，复用统一能力并保证业务规则一致。
                ps.setString(4, row.getScanCode());
                // 调用getScanDate方法，复用统一能力并保证业务规则一致。
                ps.setTimestamp(5, toTimestamp(row.getScanDate()));
                // 调用getCustId方法，复用统一能力并保证业务规则一致。
                ps.setObject(6, row.getCustId());
                // 调用getProductNumeric方法，复用统一能力并保证业务规则一致。
                ps.setString(7, row.getProductNumeric());
                // 调用toTimestamp方法，复用统一能力并保证业务规则一致。
                ps.setTimestamp(8, toTimestamp(syncTime));
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

        for (CrmWarehouseScanOutstorageSnapshot row : rows) {
            if (StrUtil.isNotBlank(row.getScanCode())) {
                // 调用getScanCode方法，复用统一能力并保证业务规则一致。
                counter.changedBarcodes.add(row.getScanCode());
            }
            // 说明：执行该步骤以保证业务流程正确。
            counter.latestSourceId = row.getSourceId();
        }
    }

    /**
     * projectLastOutDate。
     *
     * @param barcodeSet 参数
     * @param counter 参数
     */
    private void projectLastOutDate(Set<String> barcodeSet, SyncCounter counter) {
        List<String> barcodes = new ArrayList<>(barcodeSet);
        int batchSize = 500;
        for (int index = 0; index < barcodes.size(); index += batchSize) {
            // 调用size方法，复用统一能力并保证业务规则一致。
            List<String> slice = barcodes.subList(index, Math.min(index + batchSize, barcodes.size()));
            // 调用queryMinScanDateMap方法，复用统一能力并保证业务规则一致。
            Map<String, LocalDateTime> minScanDateMap = queryMinScanDateMap(slice);
            // 说明：执行该步骤以保证业务流程正确。
            LambdaQueryWrapper<MachineBarcode> wrapper = new LambdaQueryWrapper<>();
            // 调用in方法，复用统一能力并保证业务规则一致。
            wrapper.in(MachineBarcode::getBarcode, slice);
            // 说明：执行该步骤以保证业务流程正确。
            List<MachineBarcode> existingBarcodes = machineBarcodeMapper.selectList(wrapper);
            Set<String> existingBarcodeSet = existingBarcodes.stream()
                    .map(MachineBarcode::getBarcode)
                    .filter(StrUtil::isNotBlank)
                    // 调用toCollection方法，复用统一能力并保证业务规则一致。
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            // 调用size方法，复用统一能力并保证业务规则一致。
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
                    // 调用toList方法，复用统一能力并保证业务规则一致。
                    .collect(Collectors.toList());
            if (updates.isEmpty()) {
                continue;
            }
            // 说明：执行该步骤以保证业务流程正确。
            String updateSql = "UPDATE machine_barcode SET last_out_date = ?, last_sync_time = NOW(), update_time = NOW() "
                    + "WHERE barcode = ?";
            jdbcTemplate.batchUpdate(updateSql, new BatchPreparedStatementSetter() {
                /**
     * setValues。
     *
     * @param ps 参数
     * @param i 参数
                 */
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    // 调用get方法，复用统一能力并保证业务规则一致。
                    Map.Entry<String, LocalDateTime> item = updates.get(i);
                    // 调用getValue方法，复用统一能力并保证业务规则一致。
                    ps.setTimestamp(1, toTimestamp(item.getValue()));
                    // 调用getKey方法，复用统一能力并保证业务规则一致。
                    ps.setString(2, item.getKey());
                }

                /**
     * 获取BatchSize。
     *
     * @return 处理结果
                 */
                @Override
                public int getBatchSize() {
                    return updates.size();
                }
            });
            // 调用size方法，复用统一能力并保证业务规则一致。
            counter.updatedMachineBarcodeCount += updates.size();
        }
    }

    /**
     * 查询Min扫描DateMap。
     *
     * @param barcodes 参数
     * @return 处理结果
     */
    private Map<String, LocalDateTime> queryMinScanDateMap(List<String> barcodes) {
        if (CollUtil.isEmpty(barcodes)) {
            return Collections.emptyMap();
        }
        // 调用joining方法，复用统一能力并保证业务规则一致。
        String placeholders = barcodes.stream().map(item -> "?").collect(Collectors.joining(","));
        // 说明：执行该步骤以保证业务流程正确。
        String sql = "SELECT scan_code, MIN(scan_date) AS last_out_date "
                + "FROM crm_warehouse_scan_outstorage_snapshot "
                + "WHERE scan_code IN (" + placeholders + ") AND scan_date IS NOT NULL "
                + "GROUP BY scan_code";
        List<Map.Entry<String, LocalDateTime>> rows = jdbcTemplate.query(sql, ps -> {
            for (int i = 0; i < barcodes.size(); i++) {
                // 调用get方法，复用统一能力并保证业务规则一致。
                ps.setString(i + 1, barcodes.get(i));
            }
        }, (rs, rowNum) -> new java.util.AbstractMap.SimpleEntry<>(
                StrUtil.trim(rs.getString("scan_code")),
                toLocalDateTime(rs, "last_out_date")
        ));
        Map<String, LocalDateTime> result = new LinkedHashMap<>();
        for (Map.Entry<String, LocalDateTime> row : rows) {
            if (StrUtil.isNotBlank(row.getKey()) && row.getValue() != null) {
                // 调用getValue方法，复用统一能力并保证业务规则一致。
                result.put(row.getKey(), row.getValue());
            }
        }
        return result;
    }

    /**
     * 获取LocalMax来源ID。
     *
     * @return 处理结果
     */
    private Long getLocalMaxSourceId() {
        // 说明：执行该步骤以保证业务流程正确。
        String sql = "SELECT MAX(source_id) FROM crm_warehouse_scan_outstorage_snapshot";
        // 调用queryForObject方法，复用统一能力并保证业务规则一致。
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0L : value;
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

    private static class SyncCounter {
        private int syncedDetailCount;
        private final Set<String> changedBarcodes = new LinkedHashSet<>();
        private int updatedMachineBarcodeCount;
        private int unmatchedBarcodeCount;
        private Long latestSourceId;
    }
}



