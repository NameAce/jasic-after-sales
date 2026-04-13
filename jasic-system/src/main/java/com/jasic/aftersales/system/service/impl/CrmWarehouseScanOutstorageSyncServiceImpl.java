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
 * CRM 销售出库扫码同步服务实现。
 *
 * <p>实现拆为两个阶段：</p>
 * <ul>
 *     <li>先按源表主键增量落地销售出库扫码明细快照；</li>
 *     <li>再基于本地快照按条码聚合最早扫码时间，覆盖回写本地销售最后出库日期。</li>
 * </ul>
 *
 * @author Codex
 * @date 2026/04/12
 */
@Service
public class CrmWarehouseScanOutstorageSyncServiceImpl implements ICrmWarehouseScanOutstorageSyncService {

    private static final String CRM_OUTSTORAGE_TABLE = "saas_warehouse_scan_outstorage";
    private static final int DEFAULT_BATCH_SIZE = 1000;

    @Resource(name = "jdbcTemplate")
    private JdbcTemplate jdbcTemplate;

    @Resource(name = "crmJdbcTemplate")
    private JdbcTemplate crmJdbcTemplate;

    @Resource
    private MachineBarcodeMapper machineBarcodeMapper;

    @Override
    public CrmWarehouseScanOutstorageSyncSummaryVO syncIncremental() {
        JdbcTemplate crm = requireCrmJdbcTemplate();
        Long lastSourceId = getLocalMaxSourceId();
        // 当前源表按 scan_outstorage_id 递增追加，故使用主键游标拉取增量明细。
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
            // 仅重算本轮涉及的条码，避免每次全表聚合。
            projectDealerOutDate(counter.changedBarcodes, counter);
        }

        CrmWarehouseScanOutstorageSyncSummaryVO summary = new CrmWarehouseScanOutstorageSyncSummaryVO();
        summary.setSyncedDetailCount(counter.syncedDetailCount);
        summary.setAffectedBarcodeCount(counter.changedBarcodes.size());
        summary.setUpdatedMachineBarcodeCount(counter.updatedMachineBarcodeCount);
        summary.setUnmatchedBarcodeCount(counter.unmatchedBarcodeCount);
        summary.setLatestSourceId(counter.latestSourceId);
        return summary;
    }

    private void flushBatch(List<CrmWarehouseScanOutstorageSnapshot> rows, LocalDateTime syncTime, SyncCounter counter) {
        // 明细快照保留 CRM 原始记录，采用 source_id 幂等更新，兼容重复执行。
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

            @Override
            public int getBatchSize() {
                return rows.size();
            }
        });

        for (CrmWarehouseScanOutstorageSnapshot row : rows) {
            if (StrUtil.isNotBlank(row.getScanCode())) {
                counter.changedBarcodes.add(row.getScanCode());
            }
            // 批次按 source_id 升序处理，最后一个值即当前同步游标上界。
            counter.latestSourceId = row.getSourceId();
        }
    }

    private void projectDealerOutDate(Set<String> barcodeSet, SyncCounter counter) {
        List<String> barcodes = new ArrayList<>(barcodeSet);
        int batchSize = 500;
        for (int index = 0; index < barcodes.size(); index += batchSize) {
            List<String> slice = barcodes.subList(index, Math.min(index + batchSize, barcodes.size()));
            Map<String, LocalDateTime> minScanDateMap = queryMinScanDateMap(slice);
            if (minScanDateMap.isEmpty()) {
                continue;
            }
            // 只更新本地已存在的条码档案；未匹配条码仅做计数，不自动建档。
            LambdaQueryWrapper<MachineBarcode> wrapper = new LambdaQueryWrapper<>();
            wrapper.in(MachineBarcode::getBarcode, minScanDateMap.keySet());
            List<MachineBarcode> existingBarcodes = machineBarcodeMapper.selectList(wrapper);
            Set<String> existingBarcodeSet = existingBarcodes.stream()
                    .map(MachineBarcode::getBarcode)
                    .filter(StrUtil::isNotBlank)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            counter.unmatchedBarcodeCount += minScanDateMap.size() - existingBarcodeSet.size();
            if (existingBarcodeSet.isEmpty()) {
                continue;
            }

            List<Map.Entry<String, LocalDateTime>> updates = minScanDateMap.entrySet().stream()
                    .filter(item -> existingBarcodeSet.contains(item.getKey()))
                    .collect(Collectors.toList());
            // dealer_out_date 允许覆盖已有值，因此这里直接按最新聚合结果回写。
            String updateSql = "UPDATE machine_barcode SET dealer_out_date = ?, last_sync_time = NOW(), update_time = NOW() "
                    + "WHERE barcode = ?";
            jdbcTemplate.batchUpdate(updateSql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    Map.Entry<String, LocalDateTime> item = updates.get(i);
                    ps.setTimestamp(1, toTimestamp(item.getValue()));
                    ps.setString(2, item.getKey());
                }

                @Override
                public int getBatchSize() {
                    return updates.size();
                }
            });
            counter.updatedMachineBarcodeCount += updates.size();
        }
    }

    private Map<String, LocalDateTime> queryMinScanDateMap(List<String> barcodes) {
        if (CollUtil.isEmpty(barcodes)) {
            return Collections.emptyMap();
        }
        String placeholders = barcodes.stream().map(item -> "?").collect(Collectors.joining(","));
        // 业务口径明确取同条码最早扫码时间，作为销售最后出库日期写回本地条码档案。
        String sql = "SELECT scan_code, MIN(scan_date) AS dealer_out_date "
                + "FROM crm_warehouse_scan_outstorage_snapshot "
                + "WHERE scan_code IN (" + placeholders + ") AND scan_date IS NOT NULL "
                + "GROUP BY scan_code";
        List<Map.Entry<String, LocalDateTime>> rows = jdbcTemplate.query(sql, ps -> {
            for (int i = 0; i < barcodes.size(); i++) {
                ps.setString(i + 1, barcodes.get(i));
            }
        }, (rs, rowNum) -> new java.util.AbstractMap.SimpleEntry<>(
                StrUtil.trim(rs.getString("scan_code")),
                toLocalDateTime(rs, "dealer_out_date")
        ));
        Map<String, LocalDateTime> result = new LinkedHashMap<>();
        for (Map.Entry<String, LocalDateTime> row : rows) {
            if (StrUtil.isNotBlank(row.getKey()) && row.getValue() != null) {
                result.put(row.getKey(), row.getValue());
            }
        }
        return result;
    }

    private Long getLocalMaxSourceId() {
        // 快照表保存过的最大 source_id 即当前增量同步游标。
        String sql = "SELECT MAX(source_id) FROM crm_warehouse_scan_outstorage_snapshot";
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0L : value;
    }

    private JdbcTemplate requireCrmJdbcTemplate() {
        if (crmJdbcTemplate == null) {
            throw new ServiceException("当前未配置客户关系管理（CRM）数据源，请先完善 jasic.crm.datasource");
        }
        return crmJdbcTemplate;
    }

    private Timestamp toTimestamp(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    private static LocalDateTime toLocalDateTime(ResultSet rs, String column) throws SQLException {
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
